package smf.webhookevents.ad_event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import smf.webhookevents.webhook_util.WebHookUtil;

import com.smf.webhookevents.data.Events;

public class BusinessPartnerHook extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      BusinessPartner.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());
  private final static String CREATE = "C";
  private final static String UPDATE = "U";
  private final static String DELETE = "D";

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
      OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
      cEvents.createAlias(Events.PROPERTY_TABLE, "table");
      cEvents.add(Restrictions.eq(Events.PROPERTY_ACTIVE, true));
      cEvents.add(Restrictions.eq(Events.PROPERTY_EXECUTEON, CREATE));
      cEvents.add(Restrictions
          .eq("table." + Table.PROPERTY_DBTABLENAME, BusinessPartner.TABLE_NAME));

      List<Events> lEvents = cEvents.list();
      if (!lEvents.isEmpty()) {
        if (lEvents.get(0).isAllrecord()) {
          WebHookUtil.callWebHook(lEvents.get(0), bPartner);
        } else {
          // ACA IRIA LA PARTE DEL CRITERIA EVENT
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
