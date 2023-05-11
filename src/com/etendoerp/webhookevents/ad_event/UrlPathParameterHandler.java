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

import java.util.HashMap;

import javax.enterprise.event.Observes;

import com.etendoerp.webhookevents.interfaces.ComputedFunction;
import com.etendoerp.webhookevents.webhook_util.Constants;
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

import com.etendoerp.webhookevents.data.UrlPathParam;

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
            .equals(ComputedFunction.class)) {
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
