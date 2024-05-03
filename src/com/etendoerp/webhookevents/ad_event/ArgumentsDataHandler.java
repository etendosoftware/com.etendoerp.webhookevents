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

import com.etendoerp.webhookevents.webhook_util.Constants;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.etendoerp.webhookevents.data.ArgumentsData;

public class ArgumentsDataHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ArgumentsData.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());
  private static final String LANGUAGE = OBContext.getOBContext().getLanguage().getLanguage();
  private static final ConnectionProvider conn = new DalConnectionProvider(false);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws OBException {
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

  public void onUpdate(@Observes EntityUpdateEvent event) throws OBException {
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

  public void valid(Entity entity, ArgumentsData arg) {
    String message = "";
    for (String s : arg.getValue().split(" ")) {
      if (s.contains(Constants.AT) && DalUtil.getPropertyFromPath(entity, s.split(Constants.AT)[1]) == null) {
        message = String.format(Utility.messageBD(conn, "smfwhe_ErrorProperty", LANGUAGE),
            s.split(Constants.AT)[1]);
        throw new OBException(message);
      }
    }
  }
}
