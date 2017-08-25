package com.smf.webhookevents.ad_event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.DalUtil;

import com.smf.webhookevents.data.ArgumentsData;
import com.smf.webhookevents.webhook_util.Constants;

public class ArgumentsDataHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ArgumentsData.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    final ArgumentsData arg = (ArgumentsData) event.getTargetInstance();
    try {
      Entity entity = ModelProvider.getInstance().getEntityByTableName(
          arg.getSmfwheJsonData().getSmfwheWebhook().getSmfwheEvents().getTable().getDBTableName());
      valid(entity, arg);
    } catch (Exception e) {
      logger.error(e.toString(), e);
      throw new OBException(e);
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    final ArgumentsData arg = (ArgumentsData) event.getTargetInstance();
    try {
      Entity entity = ModelProvider.getInstance().getEntityByTableName(
          arg.getSmfwheJsonData().getSmfwheWebhook().getSmfwheEvents().getTable().getDBTableName());
      valid(entity, arg);
    } catch (Exception e) {
      logger.error(e.toString(), e);
      throw new OBException(e);
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  public void valid(Entity entity, ArgumentsData arg) throws Exception {
    for (String s : arg.getValue().split(" ")) {
      if (s.contains(Constants.AT)) {
        if (DalUtil.getPropertyFromPath(entity, s.split(Constants.AT)[1]) == null) {
          throw new Exception(s.split(Constants.AT)[1].toString());
        }
      }
    }
  }
}
