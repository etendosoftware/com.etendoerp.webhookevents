package com.smf.webhookevents.ad_process;

import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;
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

import com.smf.webhookevents.data.Events;
import com.smf.webhookevents.data.QueueEventHook;
import com.smf.webhookevents.webhook_util.WebHookUtil;

public class DequeueEventsFromQueue extends DalBaseProcess {
  final private static String language = OBContext.getOBContext().getLanguage().getLanguage();
  final private static ConnectionProvider conn = new DalConnectionProvider(false);
  private static final Logger log = Logger.getLogger(DequeueEventsFromQueue.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    ProcessLogger logger = bundle.getLogger();
    logger.logln("Init dequeue process.");
    int i = 0;
    QueueEventHook obj = null;
    Events event = null;
    OBQuery<BaseOBObject> qBob = null;
    String whereClause = "";
    try {
      OBContext.setAdminMode();
      OBCriteria<QueueEventHook> cQueue = OBDal.getInstance().createCriteria(QueueEventHook.class);
      cQueue.setFetchSize(1000);
      ScrollableResults scroller = cQueue.scroll();
      while (scroller.next()) {
        try {
          obj = (QueueEventHook) scroller.get()[0];
          event = obj.getSmfwheEvents();
          if (event.isAllrecord() || (!event.isAllrecord() && event.getHQLWhereClause() == null)) {
            whereClause = " as e where id = :id ";
          } else {
            whereClause = " as e where " + event.getHQLWhereClause() + " and id = :id ";
          }
          qBob = OBDal.getInstance().createQuery(
              ModelProvider.getInstance().getEntityByTableName(obj.getTable().getDBTableName())
                  .getName(), whereClause);
          qBob.setNamedParameter("id", obj.getRecord());

          if (!qBob.list().isEmpty()) {
            WebHookUtil.callWebHook(event, qBob.list().get(0), log);
            String message = String.format(
                Utility.messageBD(conn, "smfwhe_SendCallWebHook", language), qBob.list().get(0)
                    .getIdentifier());
            logger.logln(message);
          }
          OBDal.getInstance().remove(obj);
          i++;
          if (i % 100 == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          }
        } catch (Exception ex) {
          String message = String.format(
              Utility.messageBD(conn, "smfwhe_errorSendCallWebHook", language), qBob == null
                  || qBob.list().isEmpty() ? "" : qBob.list().get(0).getIdentifier());
          log.error(message, ex);
          logger.logln(message);
          i++;
        }
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      String message = String.format(
          Utility.messageBD(conn, "smfwhe_errorSendCallWebHook", language), qBob == null
              || qBob.list().isEmpty() ? "" : qBob.list().get(0).getIdentifier());
      log.error(message, e);
      logger.logln(message);
      throw e;
    } finally {
      OBContext.restorePreviousMode();
      logger.logln("Finish dequeue process.");
    }

  }

}
