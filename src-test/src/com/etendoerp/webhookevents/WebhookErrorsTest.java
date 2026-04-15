package com.etendoerp.webhookevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.DefinedwebhookAccess;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;

public class WebhookErrorsTest extends WeldBaseTest {

  @Override
  protected void setTestUserContext() {
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
  }

  WebhookUtils webhookUtils;
  DefinedWebHook webhook;
  DefinedwebhookToken token;
  DefinedWebhookParam webhookParamName;
  DefinedWebhookParam webhookParamDescription;
  DefinedWebhookParam webhookParamRule;
  DefinedwebhookAccess webhookAccess;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    ensureWebhookUtils();
  }

  private void ensureWebhookUtils() {
    if (webhookUtils == null) {
      webhookUtils = new WebhookUtils();
    }
    webhookUtils.setupUserSystem();
  }

  @Test
  @Issue("#12")
  @DisplayName("[ETP-110] Setup Webhook not allow")
  public void testSetupWebhookNotAllow() {
    try {
      ensureWebhookUtils();
      webhookUtils.createWebhookThrowError(TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN, TestConstants.Users.ADMIN);
    } catch (OBSecurityException e) {
      assertEquals(WebhookUtils.ERROR_MSG_NOT_ALLOW, e.getMessage());
    }
  }

  @Test
  @DisplayName("[WHE-011] Make a Get Request with incorrect token")
  public void testMakeGetRequestWithIncorrectToken() {
    try {
      ensureWebhookUtils();
      webhook = webhookUtils.createWebhook(TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN, TestConstants.Users.SYSTEM);
      token = webhookUtils.createApiToken();
      webhookParamName = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_NAME, true);
      webhookParamDescription = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_DESCRIPTION, true);
      webhookParamRule = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_RULE, true);
      webhookAccess = webhookUtils.createWebhookAccess(webhook, token);

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
      assertTrue(response.getMessage().contains(OBMessageUtils.messageBD("smfwhe_unauthorizedToken")));
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());
    } finally {
      webhookUtils.addObjectToDelete(webhookAccess);
      webhookUtils.addObjectToDelete(token);
      webhookUtils.addObjectToDelete(webhookParamName);
      webhookUtils.addObjectToDelete(webhookParamDescription);
      webhookUtils.addObjectToDelete(webhookParamRule);
      webhookUtils.addObjectToDelete(webhook);
    }
  }

  @Test
  @DisplayName("[WHE-012] Make a Get Request with incorrect webhook name")
  public void testMakeGetRequestWithIncorrectName() {
    try {
      ensureWebhookUtils();
      webhook = webhookUtils.createWebhook(TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN, TestConstants.Users.SYSTEM);
      token = webhookUtils.createApiToken();
      webhookParamName = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_NAME, true);
      webhookParamDescription = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_DESCRIPTION, true);
      webhookParamRule = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_RULE, true);
      webhookAccess = webhookUtils.createWebhookAccess(webhook, token);

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
      assertEquals(String.format(OBMessageUtils.messageBD("smfwhe_actionNotFound"), name), response.getMessage());
      assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode());
    } finally {
      webhookUtils.addObjectToDelete(webhookAccess);
      webhookUtils.addObjectToDelete(token);
      webhookUtils.addObjectToDelete(webhookParamName);
      webhookUtils.addObjectToDelete(webhookParamDescription);
      webhookUtils.addObjectToDelete(webhookParamRule);
      webhookUtils.addObjectToDelete(webhook);
    }
  }

  @Test
  @DisplayName("[WHE-013] Make a Get Request without access")
  public void testMakeGetRequestWithoutAccess() {
    try {
      ensureWebhookUtils();
      webhook = webhookUtils.createWebhook(TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN, TestConstants.Users.SYSTEM);
      token = webhookUtils.createApiToken();
      webhookParamName = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_NAME, true);
      webhookParamDescription = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_DESCRIPTION, true);
      webhookParamRule = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_RULE, true);

      OBDal.getInstance().commitAndClose();

      webhookUtils.assertWebhookParams(webhookParamName.getName(), webhookParamDescription.getName(),
          webhookParamRule.getName());

      String baseUrl = WebhookUtils.BASE_URL;
      String name = webhook.getName();
      String apiKey = token.getAPIKey();
      String description = webhook.getDescription();
      String rule = WebhookUtils.ALERT_RULE;

      WebhookHttpResponse response = webhookUtils.sendGetRequest(baseUrl, name, apiKey, description, rule);
      assertTrue(response.getMessage().contains(OBMessageUtils.messageBD("smfwhe_unauthorizedToken")));
      assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());
    } finally {
      webhookUtils.addObjectToDelete(token);
      webhookUtils.addObjectToDelete(webhookParamName);
      webhookUtils.addObjectToDelete(webhookParamDescription);
      webhookUtils.addObjectToDelete(webhookParamRule);
      webhookUtils.addObjectToDelete(webhook);
    }
  }

  @Test
  @DisplayName("[WHE-014] Make a Get Request with a missing parameter marked as required")
  public void testMakeGetRequestWithMissingParameter() {
    try {
      ensureWebhookUtils();
      webhook = webhookUtils.createWebhook(TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN, TestConstants.Users.SYSTEM);
      token = webhookUtils.createApiToken();
      webhookParamName = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_NAME, true);
      webhookParamDescription = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_DESCRIPTION, true);
      webhookParamRule = webhookUtils.createWebhookParam(webhook, WebhookUtils.PARAM_RULE, true);
      webhookAccess = webhookUtils.createWebhookAccess(webhook, token);
      OBDal.getInstance().commitAndClose();

      webhookUtils.assertWebhookParams(webhookParamName.getName(), webhookParamDescription.getName(),
          webhookParamRule.getName());
      assertEquals(WebhookUtils.EXPECTED_TOKEN_NAME, webhookAccess.getSmfwheDefinedwebhookToken().getName());

      String baseUrl = WebhookUtils.BASE_URL;
      String name = webhook.getName();
      String apiKey = token.getAPIKey();
      String rule = WebhookUtils.ALERT_RULE;

      WebhookHttpResponse response = webhookUtils.sendGetRequest(baseUrl, name, apiKey, null, rule);
      String expectedMessage = String.format(OBMessageUtils.messageBD("smfwhe_missingParameter"), webhookParamDescription.getName());
      assertEquals(expectedMessage, response.getMessage());
      assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, response.getStatusCode());
    } finally {
      webhookUtils.addObjectToDelete(webhookAccess);
      webhookUtils.addObjectToDelete(token);
      webhookUtils.addObjectToDelete(webhookParamName);
      webhookUtils.addObjectToDelete(webhookParamDescription);
      webhookUtils.addObjectToDelete(webhookParamRule);
      webhookUtils.addObjectToDelete(webhook);
    }
  }

  @AfterEach
  public void tearDown() {
    try {
      if (webhookUtils != null) {
        ensureWebhookUtils();
        webhookUtils.deleteAll();
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
    }
  }
}
