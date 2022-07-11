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
