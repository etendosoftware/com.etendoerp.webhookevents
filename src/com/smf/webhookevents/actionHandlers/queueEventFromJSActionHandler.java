package com.smf.webhookevents.actionHandlers;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;

import com.smf.webhookevents.webhook_util.WebHookUtil;

/**
 * Queues an event from a javascript call. To call, use:
 * OB.RemoteCallManager.call('com.smf.webhookevents.actionHandlers.queueEventFromJSActionHandler', {
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
