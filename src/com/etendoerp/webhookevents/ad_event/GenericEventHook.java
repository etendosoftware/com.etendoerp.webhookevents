package com.etendoerp.webhookevents.ad_event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;

import com.etendoerp.webhookevents.webhook_util.Constants;
import com.etendoerp.webhookevents.webhook_util.WebHookUtil;

public class GenericEventHook extends EntityPersistenceEventObserver {
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return WebHookUtil.getEntities();
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      BaseOBObject bob = event.getTargetInstance();
      WebHookUtil.queueEventFromEventHandler(bob.getEntity().getTableName(),
          bob.getEntity().getTableId(), Constants.CREATE, (String) bob.get("id"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      BaseOBObject bob = event.getTargetInstance();
      WebHookUtil.queueEventFromEventHandler(bob.getEntity().getTableName(),
          bob.getEntity().getTableId(), Constants.UPDATE, (String) bob.get("id"));
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      BaseOBObject bob = event.getTargetInstance();
      WebHookUtil.queueEventFromEventHandler(bob.getEntity().getTableName(),
          bob.getEntity().getTableId(), Constants.DELETE, (String) bob.get("id"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
