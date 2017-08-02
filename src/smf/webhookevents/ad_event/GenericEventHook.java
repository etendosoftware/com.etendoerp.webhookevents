package smf.webhookevents.ad_event;

import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

import smf.webhookevents.webhook_util.Constants;
import smf.webhookevents.webhook_util.WebHookUtil;

import com.smf.webhookevents.data.Events;
import com.smf.webhookevents.data.QueueEventHook;

public class GenericEventHook extends EntityPersistenceEventObserver {
  private static Entity[] entities = WebHookUtil.getEntities();
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      BaseOBObject bob = event.getTargetInstance();
      List<Events> lEvents = WebHookUtil.eventsFromBaseOBObject(Constants.CREATE, bob.getEntity()
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
        OBDal.getInstance().flush();
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
      List<Events> lEvents = WebHookUtil.eventsFromBaseOBObject(Constants.UPDATE, event
          .getTargetInstance().getEntity().getTableName());
      if (!lEvents.isEmpty()) {
        if (lEvents.get(0).isAllrecord()) {
          WebHookUtil.callWebHook(lEvents.get(0), event.getTargetInstance(), logger);
        }
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
      List<Events> lEvents = WebHookUtil.eventsFromBaseOBObject(Constants.DELETE, event
          .getTargetInstance().getEntity().getTableName());
      if (!lEvents.isEmpty()) {
        if (lEvents.get(0).isAllrecord()) {
          WebHookUtil.callWebHook(lEvents.get(0), event.getTargetInstance(), logger);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
