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

package com.etendoerp.webhookevents.interfaces;

import org.openbravo.model.ad.datamodel.Table;

import com.etendoerp.webhookevents.data.EventType;

/**
 * Class to extend when defining Dynamic Event Handlers. The execute() methods can return false in
 * case the event must not be inserted in the queue
 * 
 *
 */
public abstract class DynamicEventHandler {

  /**
   * Method to override when defining Dynamic Event Handlers.
   * 
   * @param recordId
   *          current ID for the object being treated
   * @param eventType
   *          type of event (On Create, On Update or On Delete. To use custom Event Types, this
   *          class doesn't have to be extended, a new event handler calling
   *          WebHookUtil.queueEventFromEventHandler() must be created)
   * @param table
   *          Table object where the event is listening.
   * 
   * @return The execute() methods can return false in case the event must not be inserted in the
   *         queue
   */
  public abstract boolean execute(Table table, EventType eventType, String recordId);

}
