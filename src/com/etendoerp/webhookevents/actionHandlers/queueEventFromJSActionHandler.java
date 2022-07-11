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

package com.etendoerp.webhookevents.actionHandlers;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;

import com.etendoerp.webhookevents.webhook_util.WebHookUtil;

/**
 * Queues an event from a javascript call. To call, use:
 * OB.RemoteCallManager.call('com.etendoerp.webhookevents.actionHandlers.queueEventFromJSActionHandler', {
 * tableId: tableId, eventTypeId: eventTypeId, eventClass: eventClass, recordId: recordId }, {},
 * callback);
 *
 */
public class queueEventFromJSActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    try {

      // get the data as json
      final JSONObject jsonData = new JSONObject(content);
      final String tableId = jsonData.getString("tableId");
      final String eventTypeId = jsonData.getString("eventTypeId");
      final String eventClass = jsonData.getString("eventClass");
      final String recordId = jsonData.getString("recordId");

      WebHookUtil.queueEvent(tableId, eventTypeId, eventClass, recordId);

      // create the result
      JSONObject json = new JSONObject();

      // and return it
      return json;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

}
