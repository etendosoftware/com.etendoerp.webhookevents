package com.etendoerp.webhookevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;

import org.hibernate.criterion.Restrictions;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.DefinedwebhookAccess;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;

public class WebhookSetupTest extends WeldBaseTest {
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN,
        TestConstants.Clients.FB_GRP, TestConstants.Orgs.ESP_NORTE);
    VariablesSecureApp vsa = new VariablesSecureApp(
        OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getRole().getId()
    );
    RequestContext.get().setVariableSecureApp(vsa);
  }

  @Test
  @DisplayName("[WHE-002] Create Api Token")
  public void testCreateApiToken() {
    WebhookUtils webhookUtils = new WebhookUtils();
    DefinedwebhookToken token = webhookUtils.createApiToken();

    assertEquals("Etendo token", token.getName());
    assertNotNull(token.getAPIKey());

    webhookUtils.deleteWebhookToken(token);
  }

  @Test
  @DisplayName("[WHE-003] Setup Webhook")
  public void testSetupWebhook() {
    WebhookUtils webhookUtils = new WebhookUtils();
    DefinedWebHook webhook = webhookUtils.createWebhook();

    assertEquals("Alert", webhook.getName());
    assertEquals("Create an alert with custom message", webhook.getDescription());
    assertEquals("JAVA", webhook.getEventClass());

    webhookUtils.deleteWebhook(webhook);
  }

  @Test
  @DisplayName("[WHE-006], [WHE-007], [WHE-010] Configure Webhook params & access token, and create alert with webhook")
  public void testConfigureWebhookParams() {
    WebhookUtils webhookUtils = new WebhookUtils();
    DefinedWebHook webhook = webhookUtils.createWebhook();
    DefinedwebhookToken token = webhookUtils.createApiToken();
    DefinedWebhookParam webhookParamName = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_NAME);
    DefinedWebhookParam webhookParamDescription = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_DESCRIPTION);
    DefinedWebhookParam webhookParamRule = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_RULE);
    DefinedwebhookAccess webhookAccess = webhookUtils.createWebhookAccess(webhook, token);

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
    alertCriteria.add(Restrictions.eq(Alert.ID, alertId));
    alertCriteria.setMaxResults(1);

    Alert alert = (Alert) alertCriteria.uniqueResult();
    assertEquals(description, alert.getDescription());
    assertEquals(rule, alert.getAlertRule().getId());
    assertEquals(alertId, alert.getId());

    webhookUtils.deleteAll(webhookAccess, token, webhookParamName, webhookParamDescription, webhookParamRule, webhook,
        alert);
  }
}
