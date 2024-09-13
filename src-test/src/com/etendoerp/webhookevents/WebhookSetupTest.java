package com.etendoerp.webhookevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;

import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.DefinedwebhookAccess;
import com.etendoerp.webhookevents.data.DefinedwebhookRole;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;

public class WebhookSetupTest extends WeldBaseTest {
  WebhookUtils webhookUtils;
  DefinedWebHook webhook;
  DefinedwebhookToken token;
  DefinedWebhookParam webhookParamName;
  DefinedWebhookParam webhookParamDescription;
  DefinedWebhookParam webhookParamRule;
  DefinedwebhookAccess webhookAccess;
  DefinedwebhookRole webhookRole;
  Alert alert;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    webhookUtils = new WebhookUtils();
    webhookUtils.setupUserSystem();
  }

  @Test
  @DisplayName("[WHE-002] Create Api Token")
  public void testCreateApiToken() {
    try {
      token = webhookUtils.createApiToken();

      assertEquals(WebhookUtils.EXPECTED_TOKEN_NAME, token.getName());
      assertNotNull(token.getAPIKey());
    } finally {
      webhookUtils.addObjectToDelete(token);
    }
  }

  @Test
  @DisplayName("[WHE-003] Setup Webhook")
  public void testSetupWebhook() {
    try {
      webhook = webhookUtils.createWebhook(TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN, TestConstants.Users.SYSTEM);

      assertEquals(WebhookUtils.WEBHOOK_NAME, webhook.getName());
      assertEquals(WebhookUtils.WEBHOOK_DESCRIPTION, webhook.getDescription());
      assertEquals(WebhookUtils.WEBHOOK_EVENTCLASS, webhook.getEventClass());
    } finally {
      webhookUtils.addObjectToDelete(webhook);
    }
  }

  @Test
  @DisplayName("[WHE-006], [WHE-007], [WHE-010] Configure Webhook params & access token, and create alert with webhook")
  public void testConfigureWebhookParams() {
    try {
      webhook = webhookUtils.createWebhook(TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN, TestConstants.Users.SYSTEM);
      webhookUtils.setupUserSystem();
      token = webhookUtils.createApiToken();
      webhookParamName = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_NAME, true);
      webhookParamDescription = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_DESCRIPTION, true);
      webhookParamRule = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_RULE, true);
      webhookAccess = webhookUtils.createWebhookAccess(webhook, token);
      webhookRole = webhookUtils.createWebhookRole(webhook);

      OBDal.getInstance().commitAndClose();

      webhookUtils.assertWebhookParams(webhookParamName.getName(), webhookParamDescription.getName(),
          webhookParamRule.getName());
      assertEquals(WebhookUtils.EXPECTED_TOKEN_NAME, webhookAccess.getSmfwheDefinedwebhookToken().getName());

      String baseUrl = WebhookUtils.BASE_URL;
      String name = webhook.getName();
      String apiKey = token.getAPIKey();
      String description = webhook.getDescription();
      String rule = WebhookUtils.ALERT_RULE;

      WebhookHttpResponse response = webhookUtils.sendGetRequest(baseUrl, name, apiKey, description, rule);
      String alertId = response.getMessage();
      assertNotNull(alertId);
      assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());

      OBCriteria<Alert> alertCriteria = OBDal.getInstance().createCriteria(Alert.class);
      alertCriteria.add(Restrictions.eq(Alert.PROPERTY_ID, alertId));
      alertCriteria.setMaxResults(1);

      alert = (Alert) alertCriteria.uniqueResult();
      assertEquals(description, alert.getDescription());
      assertEquals(rule, alert.getAlertRule().getId());
      assertEquals(alertId, alert.getId());
    } finally {
      webhookUtils.addObjectToDelete(webhookRole);
      webhookUtils.addObjectToDelete(webhookAccess);
      webhookUtils.addObjectToDelete(token);
      webhookUtils.addObjectToDelete(webhookParamName);
      webhookUtils.addObjectToDelete(webhookParamDescription);
      webhookUtils.addObjectToDelete(webhookParamRule);
      webhookUtils.addObjectToDelete(webhook);
      webhookUtils.addObjectToDelete(alert);
    }
  }

  @Test
  @DisplayName("[WHE-015] Make a Get Request without a parameter marked as no required")
  public void testMakeGetRequestWithMissingParameterNotRequired() {
    DefinedWebhookParam webhookParamNoRequired = null;
    try {
      webhookUtils.setupUserSystem();
      webhook = webhookUtils.createWebhook(TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN, TestConstants.Users.SYSTEM);
      token = webhookUtils.createApiToken();
      webhookParamName = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_NAME, true);
      webhookParamDescription = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_DESCRIPTION, true);
      webhookParamRule = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_RULE, true);
      webhookParamNoRequired = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_NO_REQUIRED, false);
      webhookAccess = webhookUtils.createWebhookAccess(webhook, token);

      webhookRole = webhookUtils.createWebhookRole(webhook);

      OBDal.getInstance().commitAndClose();

      webhookUtils.assertWebhookParams(webhookParamName.getName(), webhookParamDescription.getName(),
          webhookParamRule.getName(), webhookParamNoRequired.getName());
      assertEquals(WebhookUtils.EXPECTED_TOKEN_NAME, webhookAccess.getSmfwheDefinedwebhookToken().getName());

      String baseUrl = WebhookUtils.BASE_URL;
      String name = webhook.getName();
      String apiKey = token.getAPIKey();
      String description = webhook.getDescription();
      String rule = WebhookUtils.ALERT_RULE;

      // Create alert without the webhook param no required
      WebhookHttpResponse response = webhookUtils.sendGetRequest(baseUrl, name, apiKey, description, rule);

      String alertId = response.getMessage();
      assertNotNull(alertId);
      assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());

      OBCriteria<Alert> alertCriteria = OBDal.getInstance().createCriteria(Alert.class);
      alertCriteria.add(Restrictions.eq(Alert.PROPERTY_ID, alertId));
      alertCriteria.setMaxResults(1);

      alert = (Alert) alertCriteria.uniqueResult();

      assertEquals(description, alert.getDescription());
      assertEquals(rule, alert.getAlertRule().getId());
      assertEquals(alertId, alert.getId());
    } finally {
      webhookUtils.addObjectToDelete(webhookRole);
      webhookUtils.addObjectToDelete(webhookAccess);
      webhookUtils.addObjectToDelete(token);
      webhookUtils.addObjectToDelete(webhookParamName);
      webhookUtils.addObjectToDelete(webhookParamDescription);
      webhookUtils.addObjectToDelete(webhookParamRule);
      webhookUtils.addObjectToDelete(webhookParamNoRequired);
      webhookUtils.addObjectToDelete(webhook);
      webhookUtils.addObjectToDelete(alert);
    }
  }

  @After
  public void tearDown() {
    webhookUtils.setupUserSystem();
    webhookUtils.deleteAll();
    OBDal.getInstance().commitAndClose();
  }
}
