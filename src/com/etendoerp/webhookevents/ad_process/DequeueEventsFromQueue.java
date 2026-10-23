/*
 * Copyright (c) 2022 Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.etendoerp.webhookevents.ad_process;

import java.util.List;

import com.etendoerp.webhookevents.webhook_util.Constants;
import com.etendoerp.webhookevents.webhook_util.WebHookUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.QueueEventHook;

/**
 * Background process that delivers the webhook events waiting in the queue
 * ({@code SMFWHE_QUEUEEVENTHOOK}).
 * <ul>
 * <li>It only processes the queue entries visible to the client it is scheduled for, so it has to
 * be scheduled in each client that queues events.</li>
 * <li>An entry is removed once all its webhooks were notified. If any webhook fails, the entry is
 * kept and the next run notifies every webhook of the event again, including the ones that already
 * received it: receivers must be idempotent (use the record id and the event type as the key).</li>
 * <li>On Delete entries are delivered with a minimal JSON payload, without evaluating the event's
 * HQL where clause (see {@link WebHookUtil#callWebHookForDelete}).</li>
 * <li>Entries of other event types whose record no longer exists, or no longer matches the where
 * clause, are logged and discarded.</li>
 * </ul>
 */
public class DequeueEventsFromQueue extends DalBaseProcess {
  private static final ConnectionProvider conn = new DalConnectionProvider(false);
  private static final Logger log = Logger.getLogger(DequeueEventsFromQueue.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    ProcessLogger logger = bundle.getLogger();
    logger.logln("Init dequeue process.");
    int i = 0;
    QueueEventHook obj;
    Events event;
    OBContext.setAdminMode();
    OBCriteria<QueueEventHook> cQueue = OBDal.getInstance().createCriteria(QueueEventHook.class);
    cQueue.setFetchSize(1000);
    try (ScrollableResults scroller = cQueue.scroll()) {
      while (scroller.next()) {
        obj = (QueueEventHook) scroller.get()[0];
        event = obj.getSmfwheEvents();

        processQueueEntry(event, obj, logger);
        i++;
        if (i % 100 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
    } catch (Exception e) {
      logger.logln("Error getting scrollable results: " + e.getMessage());
      throw new OBException(e);
    }

    OBDal.getInstance().flush();
    OBContext.restorePreviousMode();
    logger.logln("Finish dequeue process.");
  }

  /**
   * Processes one queue entry, delivering its webhook and removing it from the queue. If the
   * delivery fails the error is logged and the entry is kept in the queue, so the batch can continue
   * with the remaining entries and the failure is not silently discarded.
   *
   * @param event  Event configured for the queued record
   * @param obj    Queue entry to process
   * @param logger Process logger
   */
  private static void processQueueEntry(Events event, QueueEventHook obj, ProcessLogger logger) {
    try {
      handleDequeueEvent(event, obj, logger);
      OBDal.getInstance().remove(obj);
    } catch (Exception e) {
      logger.logln("Error processing queue entry " + obj.getId() + ": " + e.getMessage());
    }
  }

  private static void handleDequeueEvent(Events event, QueueEventHook obj, ProcessLogger logger)
      throws Exception {
    String whereClause;
    OBQuery<BaseOBObject> qBob;
    whereClause = event.getHQLWhereClause() == null ?
        " as e where id = :id " :
        " as e where " + event.getHQLWhereClause() + " and id = :id ";
    qBob = OBDal.getInstance()
        .createQuery(ModelProvider.getInstance()
            .getEntityByTableName(obj.getTable().getDBTableName())
            .getName(), whereClause);
    qBob.setNamedParameter("id", obj.getRecord());

    List<BaseOBObject> results = qBob.list();
    if (!results.isEmpty()) {
      BaseOBObject entity = results.get(0);
      WebHookUtil.callWebHook(event, entity, log);
      String entityName = entity.getEntityName();
      String identifier = entity.getIdentifier();
      if (StringUtils.isEmpty(entityName) || StringUtils.isEmpty(identifier)) {
        throw new OBException("Entity name or identifier is empty for id: " + obj.getRecord());
      }
      String message = String.format(
          Utility.messageBD(conn, "smfwhe_SendCallWebHook", getLanguage()), event.getName(),
          entityName + " " + identifier);
      logger.logln(message);
    } else if (isDeleteEvent(event)) {
      WebHookUtil.callWebHookForDelete(event, obj.getRecord(), log);
      String message = String.format(
          Utility.messageBD(conn, "smfwhe_SendCallWebHook", getLanguage()), event.getName(),
          "record " + obj.getRecord());
      logger.logln(message);
    } else {
      // deleted before dequeue or filtered out by the HQL where clause: a retry can never succeed
      logger.logln("No record found for id " + obj.getRecord() + " in event " + event.getName()
          + ", queue entry discarded.");
    }
  }

  /**
   * Resolves the language of the context running the process. It is read on each execution, not in
   * a static field, because the scheduler loads this class on startup without an OBContext.
   *
   * @return the language code of the current context
   */
  private static String getLanguage() {
    return OBContext.getOBContext().getLanguage().getLanguage();
  }

  /**
   * Checks whether the event was configured to be fired on the deletion of a record. For those
   * events the affected row no longer exists when the queue is processed, so the payload is built
   * from the data captured when the event was queued instead of from a re-query.
   *
   * @param event the queued event
   * @return true when the event type is On Delete
   */
  private static boolean isDeleteEvent(Events event) {
    return event != null && event.getSmfwheEventType() != null
        && Constants.DELETE.equals(event.getSmfwheEventType().getId());
  }
}
