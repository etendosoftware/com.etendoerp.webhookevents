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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;

import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;

/**
 * Observes entity persistence events for DefinedWebHook and DefinedWebhookParam entities.
 * This class ensures that certain actions are only performed by the system administrator.
 */
public class DefinedWebHookModifications extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(DefinedWebHook.ENTITY_NAME),
      ModelProvider.getInstance().getEntity(DefinedWebhookParam.ENTITY_NAME)
  };
  protected Logger logger = Logger.getLogger(this.getClass());

  /**
   * Returns the entities observed by this event observer.
   *
   * @return An array of entities observed by this class.
   */
  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  /**
   * Handles the save event for the observed entities.
   *
   * @param event
   *     The entity new event.
   */
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkSysAdminRole();
  }

  /**
   * Handles the update event for the observed entities.
   *
   * @param event
   *     The entity update event.
   */
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkSysAdminRole();
  }

  /**
   * Handles the delete event for the observed entities.
   *
   * @param event
   *     The entity delete event.
   */
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkSysAdminRole();
  }

  /**
   * Checks if the current user has the system administrator role.
   * Throws an OBException if the user does not have the required role.
   */
  private void checkSysAdminRole() {
    Client currentClient = OBContext.getOBContext().getCurrentClient();
    var sysClient = OBDal.getInstance().get(Client.class, "0");
    if (!StringUtils.equalsIgnoreCase(currentClient.getId(), sysClient.getId())) {
      throw new OBException(OBMessageUtils.messageBD("smfwhe_errorSysAdminRole"));
    }
  }
}