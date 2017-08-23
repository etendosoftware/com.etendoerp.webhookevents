package com.smf.webhookevents.ad_event;

import java.util.HashMap;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;

import com.smf.webhookevents.data.JsonXmlData;
import com.smf.webhookevents.webhook_util.Constants;

public class JsonXmlDataHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      JsonXmlData.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    final JsonXmlData pathParam = (JsonXmlData) event.getTargetInstance();
    try {
      Entity entity = ModelProvider.getInstance().getEntityByTableName(
          pathParam.getSmfwheWebhook().getSmfwheEvents().getTable().getDBTableName());
      valid(entity, pathParam);
    } catch (Exception e) {
      logger.error(e.toString(), e);
      throw new OBException(e);
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    final JsonXmlData pathParam = (JsonXmlData) event.getTargetInstance();
    try {
      Entity entity = ModelProvider.getInstance().getEntityByTableName(
          pathParam.getSmfwheWebhook().getSmfwheEvents().getTable().getDBTableName());
      valid(entity, pathParam);
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

  public void valid(Entity entity, JsonXmlData pathParam) throws Exception {
    if (Constants.TYPE_VALUE_STRING.equals(pathParam.getTypeValue())) {
      for (String s : pathParam.getValue().split(" ")) {
        if (s.contains(Constants.AT)) {
          entity.checkIsValidProperty(s.split(Constants.AT)[1]);
        }
      }
    } else if (Constants.TYPE_VALUE_DYNAMIC_NODE.equals(pathParam.getTypeValue())
        || Constants.TYPE_VALUE_DYNAMIC_NODE_ARRAY.equals(pathParam.getTypeValue())) {
      String className = pathParam.getJavaClassName();
      Class<?> clazz; // convert string classname to class
      try {
        clazz = Class.forName(className);
        Object dog = clazz.newInstance(); // invoke empty constructor
        if (dog.getClass().getInterfaces()[0]
            .equals(com.smf.webhookevents.interfaces.DynamicNode.class)) {
          String methodName = Constants.METHOD_NAME;
          dog.getClass().getMethod(methodName, HashMap.class);
        }
      } catch (Exception e1) {
        throw e1;
      }
    } else if (Constants.TYPE_VALUE_PROPERTY.equals(pathParam.getTypeValue())) {
      entity.checkIsValidProperty(pathParam.getProperty());
    }
  }
}
