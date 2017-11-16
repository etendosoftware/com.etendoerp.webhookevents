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
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.webhookevents.data.UrlPathParam;
import com.smf.webhookevents.webhook_util.Constants;

public class UrlPathParameterHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      UrlPathParam.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());
  final private static String language = OBContext.getOBContext().getLanguage().getLanguage();
  final private static ConnectionProvider conn = new DalConnectionProvider(false);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    final UrlPathParam pathParam = (UrlPathParam) event.getTargetInstance();
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
    final UrlPathParam pathParam = (UrlPathParam) event.getTargetInstance();
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

  public void valid(Entity entity, UrlPathParam pathParam) throws Exception {
    String message = "";
    if (Constants.TYPE_VALUE_STRING.equals(pathParam.getTypeValue())) {
      for (String s : pathParam.getValue().split(" ")) {
        if (s.contains(Constants.AT)) {
          if (DalUtil.getPropertyFromPath(entity, s.split(Constants.AT)[1]) == null) {
            message = String.format(Utility.messageBD(conn, "smfwhe_ErrorProperty", language),
                s.split(Constants.AT)[1].toString());
            // throw new Exception(s.split(Constants.AT)[1].toString());
            throw new Exception(message);
          }
        }
      }
    } else if (Constants.TYPE_VALUE_COMPUTED.equals(pathParam.getTypeValue())) {
      String className = pathParam.getJavaClassName();
      Class<?> clazz; // convert string classname to class
      try {
        clazz = Class.forName(className);
        Object dog = clazz.newInstance(); // invoke empty constructor
        if (dog.getClass().getInterfaces()[0]
            .equals(com.smf.webhookevents.interfaces.ComputedFunction.class)) {
          String methodName = Constants.METHOD_NAME;
          dog.getClass().getMethod(methodName, HashMap.class);
        }
      } catch (Exception e1) {
        throw e1;
      }
    } else if (Constants.TYPE_VALUE_PROPERTY.equals(pathParam.getTypeValue())) {
      if (DalUtil.getPropertyFromPath(entity, pathParam.getProperty()) == null) {
        message = String.format(Utility.messageBD(conn, "smfwhe_ErrorProperty", language),
            pathParam.getProperty());
        // throw new Exception(pathParam.getProperty());
        throw new Exception(message);
      }
    }
  }
}
