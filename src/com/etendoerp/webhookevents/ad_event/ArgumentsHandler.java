package com.etendoerp.webhookevents.ad_event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.etendoerp.webhookevents.data.Arguments;
import com.etendoerp.webhookevents.webhook_util.Constants;

public class ArgumentsHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Arguments.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());
  final private static String language = OBContext.getOBContext().getLanguage().getLanguage();
  final private static ConnectionProvider conn = new DalConnectionProvider(false);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    final Arguments arg = (Arguments) event.getTargetInstance();
    try {
      Entity entity = ModelProvider.getInstance().getEntityByTableName(
          arg.getSmfwheUrlpathparam().getSmfwheWebhook().getSmfwheEvents().getTable()
              .getDBTableName());
      valid(entity, arg);
    } catch (Exception e) {
      logger.error(e.toString(), e);
      throw new OBException(e);
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws Exception {
    if (!isValidEvent(event)) {
      return;
    }
    final Arguments arg = (Arguments) event.getTargetInstance();
    try {
      Entity entity = ModelProvider.getInstance().getEntityByTableName(
          arg.getSmfwheUrlpathparam().getSmfwheWebhook().getSmfwheEvents().getTable()
              .getDBTableName());
      valid(entity, arg);
    } catch (Exception e) {
      logger.error(e.toString(), e);
      throw new OBException(e);
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  public void valid(Entity entity, Arguments arg) throws Exception {
    String message = "";
    for (String s : arg.getValueParameter().split(" ")) {
      if (s.contains(Constants.AT)) {
        if (DalUtil.getPropertyFromPath(entity, s.split(Constants.AT)[1]) == null) {
          message = String.format(Utility.messageBD(conn, "smfwhe_ErrorProperty", language),
              s.split(Constants.AT)[1].toString());
          // throw new Exception(s.split(Constants.AT)[1].toString());
          throw new Exception(message);
        }
      }
    }
  }
}
