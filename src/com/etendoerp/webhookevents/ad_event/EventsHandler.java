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

package com.etendoerp.webhookevents.ad_event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.etendoerp.webhookevents.data.Events;

public class EventsHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Events.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {

      return;
    }
    final Events events = (Events) event.getTargetInstance();
    OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
    cEvents.add(Restrictions.eq(Events.PROPERTY_SMFWHEEVENTTYPE, events.getSmfwheEventType()));
    cEvents.add(Restrictions.eq(Events.PROPERTY_TABLE, events.getTable()));

    if (!cEvents.list().isEmpty()) {
      String message = String.format(Utility.messageBD(new DalConnectionProvider(false),
          "smfwhe_eventAlreadyExists", OBContext.getOBContext().getLanguage().getLanguage()),
          events.getIdentifier());
      logger.info(message);
      try {
        throw new OBException(message);
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Events events = (Events) event.getTargetInstance();
    OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
    cEvents.add(Restrictions.eq(Events.PROPERTY_SMFWHEEVENTTYPE, events.getSmfwheEventType()));
    cEvents.add(Restrictions.eq(Events.PROPERTY_TABLE, events.getTable()));
    cEvents.add(Restrictions.ne(Events.PROPERTY_ID, events.getId()));
    if (!cEvents.list().isEmpty()) {
      String message = String.format(Utility.messageBD(new DalConnectionProvider(false),
          "smfwhe_eventAlreadyExists", OBContext.getOBContext().getLanguage().getLanguage()),
          events.getIdentifier());
      logger.info(message);
      try {
        throw new OBException(message);
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }
}
