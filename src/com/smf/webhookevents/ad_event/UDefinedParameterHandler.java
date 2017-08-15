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

import com.smf.webhookevents.data.UserDefinedParameter;
import com.smf.webhookevents.webhook_util.WebHookUtil;

public class UDefinedParameterHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      UserDefinedParameter.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    logger.info("User Defined Parameter"
        + ((UserDefinedParameter) event.getTargetInstance()).getName() + " is being created");
    final UserDefinedParameter uDefinedParam = (UserDefinedParameter) event.getTargetInstance();
    try {
      WebHookUtil.replaceValueData(uDefinedParam.getValueParameter(), uDefinedParam, logger);
    } catch (Exception e) {
      logger.error(e.toString(), e);
      throw e;
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    logger.info("User Defined Parameter"
        + ((UserDefinedParameter) event.getTargetInstance()).getName() + " is being updated");
    final UserDefinedParameter uDefinedParam = (UserDefinedParameter) event.getTargetInstance();
    try {
      WebHookUtil.replaceValueData(uDefinedParam.getValueParameter(), uDefinedParam, logger);
    } catch (Exception e) {
      logger.error(e.toString(), e);
      throw new OBException(e.toString());
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    logger.info("User Defined Parameter"
        + ((UserDefinedParameter) event.getTargetInstance()).getName() + " is being deleted");

  }
}
