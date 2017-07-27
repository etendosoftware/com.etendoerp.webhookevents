package smf.webhookevents.ad_event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import smf.webhookevents.webhook_util.Constants;
import smf.webhookevents.webhook_util.WebHookUtil;

import com.smf.webhookevents.data.Events;

public class BusinessPartnerHook extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      BusinessPartner.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {

      return;
    }
    logger.info("Business Partner " + ((BusinessPartner) event.getTargetInstance()).getName()
        + " is being created");
    try {
      final BusinessPartner bPartner = (BusinessPartner) event.getTargetInstance();

      List<Events> lEvents = WebHookUtil.eventsFromBaseOBObject(bPartner, Constants.CREATE,
          WebHookUtil.getTableName(getObservedEntities()));
      if (!lEvents.isEmpty()) {
        if (lEvents.get(0).isAllrecord()) {
          WebHookUtil.callWebHook(lEvents.get(0), bPartner, logger);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    logger.info("Business Partner " + event.getTargetInstance().getId() + " is being updated");

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    logger.info("Business Partner " + event.getTargetInstance().getId() + " is being deleted");

  }

}
