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

package com.etendoerp.webhookevents.ad_alert;

import com.etendoerp.webhookevents.services.BaseWebhookService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRule;

import java.util.Map;

/**
 * Example webhoook to take as a starting point.
 * This service receive a description and a alert rule ID and insert one standard alert
 */
public class AdAlertWebhookService extends BaseWebhookService {

  @Override
  public void get(Map<String, String> parameter, Map<String, String> responseVars) {
    Alert alert = new Alert();
    alert.setAlertRule(OBDal.getInstance().get(AlertRule.class, parameter.get("rule")));
    alert.setDescription(parameter.get("description"));
    alert.setReferenceSearchKey("");
    OBDal.getInstance().save(alert);
    OBDal.getInstance().flush();
    responseVars.put("created", alert.getId());
  }
}
