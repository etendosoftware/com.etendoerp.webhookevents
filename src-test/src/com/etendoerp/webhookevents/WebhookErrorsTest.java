package com.etendoerp.webhookevents;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.DefinedwebhookAccess;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;

public class WebhookErrorsTest extends WeldBaseTest {
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
  @DisplayName("[WHE-011] Make a Get Request with incorrect token")
  public void testMakeGetRequestWithIncorrectToken() {
    WebhookUtils webhookUtils = new WebhookUtils();
    DefinedWebHook webhook = webhookUtils.createWebhook();
    DefinedwebhookToken token = webhookUtils.createApiToken();
    DefinedWebhookParam webhookParamName = webhookUtils.createWebhookParam(webhook, "name");
    DefinedWebhookParam webhookParamDescription = webhookUtils.createWebhookParam(webhook, "description");
    DefinedWebhookParam webhookParamRule = webhookUtils.createWebhookParam(webhook, "rule");
    DefinedwebhookAccess webhookAccess = webhookUtils.createWebhookAccess(webhook, token);

    OBDal.getInstance().commitAndClose();

    webhookUtils.assertWebhookParams(webhookParamName.getName(), webhookParamDescription.getName(),
        webhookParamRule.getName());
    assertEquals(WebhookUtils.EXPECTED_TOKEN_NAME, webhookAccess.getSmfwheDefinedwebhookToken().getName());

    String baseUrl = WebhookUtils.BASE_URL;
    String name = webhook.getName();
    String apiKey = "incorrect_key";
    String description = webhook.getDescription();
    String rule = WebhookUtils.ALERT_RULE;

    WebhookHttpResponse response = webhookUtils.sendGetRequest(baseUrl, name, apiKey, description, rule);
    assertEquals(OBMessageUtils.messageBD("smfwhe_apiKeyNotFound"), response.getMessage());
    assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());

    webhookUtils.deleteAll(webhookAccess, token, webhookParamName, webhookParamDescription, webhookParamRule, webhook,
        null);
  }

  @Test
  @DisplayName("[WHE-012] Make a Get Request with incorrect webhook name")
  public void testMakeGetRequestWithIncorrectName() {
    WebhookUtils webhookUtils = new WebhookUtils();
    DefinedWebHook webhook = webhookUtils.createWebhook();
    DefinedwebhookToken token = webhookUtils.createApiToken();
    DefinedWebhookParam webhookParamName = webhookUtils.createWebhookParam(webhook, "name");
    DefinedWebhookParam webhookParamDescription = webhookUtils.createWebhookParam(webhook, "description");
    DefinedWebhookParam webhookParamRule = webhookUtils.createWebhookParam(webhook, "rule");
    DefinedwebhookAccess webhookAccess = webhookUtils.createWebhookAccess(webhook, token);

    OBDal.getInstance().commitAndClose();

    webhookUtils.assertWebhookParams(webhookParamName.getName(), webhookParamDescription.getName(),
        webhookParamRule.getName());
    assertEquals(WebhookUtils.EXPECTED_TOKEN_NAME, webhookAccess.getSmfwheDefinedwebhookToken().getName());

    String baseUrl = WebhookUtils.BASE_URL;
    String name = "Incorrect Name";
    String apiKey = token.getAPIKey();
    String description = webhook.getDescription();
    String rule = WebhookUtils.ALERT_RULE;

    WebhookHttpResponse response = webhookUtils.sendGetRequest(baseUrl, name, apiKey, description, rule);
    assertEquals(OBMessageUtils.messageBD("smfwhe_actionNotFound"), response.getMessage());
    assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode());

    webhookUtils.deleteAll(webhookAccess, token, webhookParamName, webhookParamDescription, webhookParamRule, webhook,
        null);
  }

  @Test
  @DisplayName("[WHE-013] Make a Get Request without access")
  public void testMakeGetRequestWithoutAccess() {
    WebhookUtils webhookUtils = new WebhookUtils();
    DefinedWebHook webhook = webhookUtils.createWebhook();
    DefinedwebhookToken token = webhookUtils.createApiToken();
    DefinedWebhookParam webhookParamName = webhookUtils.createWebhookParam(webhook, "name");
    DefinedWebhookParam webhookParamDescription = webhookUtils.createWebhookParam(webhook, "description");
    DefinedWebhookParam webhookParamRule = webhookUtils.createWebhookParam(webhook, "rule");

    OBDal.getInstance().commitAndClose();

    webhookUtils.assertWebhookParams(webhookParamName.getName(), webhookParamDescription.getName(),
        webhookParamRule.getName());

    String baseUrl = WebhookUtils.BASE_URL;
    String name = webhook.getName();
    String apiKey = token.getAPIKey();
    String description = webhook.getDescription();
    String rule = WebhookUtils.ALERT_RULE;

    WebhookHttpResponse response = webhookUtils.sendGetRequest(baseUrl, name, apiKey, description, rule);
    assertEquals(OBMessageUtils.messageBD("smfwhe_unauthorizedToken"), response.getMessage());
    assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());

    webhookUtils.deleteAll(null, token, webhookParamName, webhookParamDescription, webhookParamRule, webhook, null);
  }

  @Test
  @DisplayName("[WHE-014] Make a Get Request with a missing parameter")
  public void testMakeGetRequestWithMissingParameter() {
    WebhookUtils webhookUtils = new WebhookUtils();
    DefinedWebHook webhook = webhookUtils.createWebhook();
    DefinedwebhookToken token = webhookUtils.createApiToken();
    DefinedWebhookParam webhookParamName = webhookUtils.createWebhookParam(webhook, "name");
    DefinedWebhookParam webhookParamDescription = webhookUtils.createWebhookParam(webhook, "description");
    DefinedWebhookParam webhookParamRule = webhookUtils.createWebhookParam(webhook, "rule");
    DefinedwebhookAccess webhookAccess = webhookUtils.createWebhookAccess(webhook, token);

    OBDal.getInstance().commitAndClose();

    webhookUtils.assertWebhookParams(webhookParamName.getName(), webhookParamDescription.getName(),
        webhookParamRule.getName());
    assertEquals(WebhookUtils.EXPECTED_TOKEN_NAME, webhookAccess.getSmfwheDefinedwebhookToken().getName());

    String baseUrl = WebhookUtils.BASE_URL;
    String name = webhook.getName();
    String apiKey = token.getAPIKey();
    String rule = WebhookUtils.ALERT_RULE;

    WebhookHttpResponse response = webhookUtils.sendGetRequest(baseUrl, name, apiKey, null, rule);
    assertEquals(OBMessageUtils.messageBD("smfwhe_missingParameter"), response.getMessage());
    assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, response.getStatusCode());

    webhookUtils.deleteAll(webhookAccess, token, webhookParamName, webhookParamDescription, webhookParamRule, webhook,
        null);
  }
}
