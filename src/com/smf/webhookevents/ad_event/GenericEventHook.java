package com.smf.webhookevents.ad_event;

import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

import com.smf.webhookevents.data.Events;
import com.smf.webhookevents.data.QueueEventHook;
import com.smf.webhookevents.webhook_util.Constants;
import com.smf.webhookevents.webhook_util.WebHookUtil;

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
      List<Events> lEvents = WebHookUtil.eventsFromTableName(Constants.CREATE, bob.getEntity()
          .getTableName());
      if (!lEvents.isEmpty()) {
        QueueEventHook obj = OBProvider.getInstance().get(QueueEventHook.class);

        obj.setClient((Client) bob.get("client"));
        obj.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
        obj.setCreationDate(new Date());
        obj.setCreatedBy(OBDal.getInstance().get(User.class, "100"));
        obj.setUpdated(new Date());
        obj.setUpdatedBy(OBDal.getInstance().get(User.class, "100"));
        obj.setRecord(bob.get("id").toString());
        obj.setTable(OBDal.getInstance().get(Table.class, bob.getEntity().getTableId()));
        obj.setSmfwheEvents(lEvents.get(0));

        OBDal.getInstance().save(obj);
      }
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
      List<Events> lEvents = WebHookUtil.eventsFromTableName(Constants.UPDATE, bob.getEntity()
          .getTableName());
      if (!lEvents.isEmpty()) {
        QueueEventHook obj = OBProvider.getInstance().get(QueueEventHook.class);

        obj.setClient((Client) bob.get("client"));
        obj.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
        obj.setCreationDate(new Date());
        obj.setCreatedBy(OBDal.getInstance().get(User.class, "100"));
        obj.setUpdated(new Date());
        obj.setUpdatedBy(OBDal.getInstance().get(User.class, "100"));
        obj.setRecord(bob.get("id").toString());
        obj.setTable(OBDal.getInstance().get(Table.class, bob.getEntity().getTableId()));
        obj.setSmfwheEvents(lEvents.get(0));

        OBDal.getInstance().save(obj);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      List<Events> lEvents = WebHookUtil.eventsFromTableName(Constants.DELETE, event
          .getTargetInstance().getEntity().getTableName());
      if (!lEvents.isEmpty()) {
        OBQuery<BaseOBObject> qBob = null;
        String whereClause = "";
        Events events = lEvents.get(0);
        if (events.getHQLWhereClause() == null) {
          whereClause = " as e where id = :id ";
        } else {
          whereClause = " as e where " + events.getHQLWhereClause() + " and id = :id ";
        }
        qBob = OBDal.getInstance().createQuery(
            ModelProvider.getInstance()
                .getEntityByTableName(event.getTargetInstance().getEntity().getTableName())
                .getName(), whereClause);
        qBob.setNamedParameter("id", event.getTargetInstance().getId());

        if (!qBob.list().isEmpty()) {
          WebHookUtil.callWebHook(events, qBob.list().get(0), logger);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
