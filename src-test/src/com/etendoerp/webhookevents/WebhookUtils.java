package com.etendoerp.webhookevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.DefinedwebhookAccess;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebhookUtils {
  private static final Logger log4j = Logger.getLogger(WebhookSetupTest.class);

  static final String USER_ROLE_FBADMIN = "451EF9CDDFE54DEEAF5636CCADC0D7BB";
  static final String AD_ORG_ID = "0"; // *
  static final String BASE_URL = "http://localhost:8080/etendo/webhooks/";
  static final String ALERT_RULE = "1000013"; // Products without defined price
  static final String EXPECTED_TOKEN_NAME = "Etendo token";
  static final String PARAM_NAME = "name";
  static final String PARAM_DESCRIPTION = "description";
  static final String PARAM_RULE = "rule";
  static final String PARAM_NO_REQUIRED = "noRequired";
  static final String WEBHOOK_NAME = "Alert";
  static final String WEBHOOK_DESCRIPTION = "Create an alert with custom message";
  static final String WEBHOOK_JAVACLASS = "com.etendoerp.webhookevents.ad_alert.AdAlertWebhookService";
  static final String WEBHOOK_EVENTCLASS = "JAVA";

  public DefinedwebhookToken createApiToken() {
    DefinedwebhookToken token = OBProvider.getInstance().get(DefinedwebhookToken.class);
    Client client = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
    UserRoles userRole = OBDal.getInstance().get(UserRoles.class, WebhookUtils.USER_ROLE_FBADMIN);
    Organization org = OBDal.getInstance().get(Organization.class, TestConstants.Orgs.ESP_NORTE);
    User user = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);

    try {
      token.setName(EXPECTED_TOKEN_NAME);
      token.setUserRole(userRole);
      token.setClient(client);
      token.setOrganization(org);
      token.setCreatedBy(user);

      OBDal.getInstance().save(token);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(token);
    } catch (Exception e) {
      log4j.error(e.getMessage());
      fail(e.getMessage());
    }
    return token;
  }

  public DefinedWebHook createWebhook() {
    DefinedWebHook webHook = OBProvider.getInstance().get(DefinedWebHook.class);

    Client client = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
    Organization org = OBDal.getInstance().get(Organization.class, WebhookUtils.AD_ORG_ID);
    User user = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);

    try {
      webHook.setName(WEBHOOK_NAME);
      webHook.setClient(client);
      webHook.setOrganization(org);
      webHook.setCreatedBy(user);
      webHook.setDescription(WEBHOOK_DESCRIPTION);
      webHook.setJavaClass(WEBHOOK_JAVACLASS);
      webHook.setEventClass(WEBHOOK_EVENTCLASS);

      OBDal.getInstance().save(webHook);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(webHook);
    } catch (Exception e) {
      log4j.error(e.getMessage());
      fail(e.getMessage());
    }
    return webHook;
  }

  public DefinedWebhookParam createWebhookParam(DefinedWebHook webhook, String name, boolean isRequired) {
    DefinedWebhookParam webhookParam = OBProvider.getInstance().get(DefinedWebhookParam.class);
    Client client = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
    Organization org = OBDal.getInstance().get(Organization.class, WebhookUtils.AD_ORG_ID);
    User user = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);

    try {
      webhookParam.setName(name);
      webhookParam.setSmfwheDefinedwebhook(webhook);
      webhookParam.setClient(client);
      webhookParam.setOrganization(org);
      webhookParam.setCreatedBy(user);
      webhookParam.setRequired(isRequired);

      OBDal.getInstance().save(webhookParam);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(webhookParam);
    } catch (Exception e) {
      log4j.error(e.getMessage());
      fail(e.getMessage());
    }
    return webhookParam;
  }

  public DefinedwebhookAccess createWebhookAccess(DefinedWebHook webhook, DefinedwebhookToken token) {
    DefinedwebhookAccess webhookAccess = OBProvider.getInstance().get(DefinedwebhookAccess.class);
    Client client = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
    Organization org = OBDal.getInstance().get(Organization.class, WebhookUtils.AD_ORG_ID);
    User user = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);

    try {
      webhookAccess.setSmfwheDefinedwebhook(webhook);
      webhookAccess.setClient(client);
      webhookAccess.setOrganization(org);
      webhookAccess.setCreatedBy(user);
      webhookAccess.setSmfwheDefinedwebhookToken(token);

      OBDal.getInstance().save(webhookAccess);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(webhookAccess);
    } catch (Exception e) {
      log4j.error(e.getMessage());
      fail(e.getMessage());
    }
    return webhookAccess;
  }

  public WebhookHttpResponse sendGetRequest(String baseUrl, String name, String apiKey, String description,
      String rule) {
    try {
      String urlString = baseUrl + "?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8) +
          "&apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
      if (StringUtils.isNotBlank(description)) {
        urlString += "&description=" + URLEncoder.encode(description, StandardCharsets.UTF_8);
      }
      urlString += "&rule=" + URLEncoder.encode(rule, StandardCharsets.UTF_8);

      URL url = new URL(urlString);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");

      ObjectMapper objectMapper = new ObjectMapper();
      int responseCode = con.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
          content.append(inputLine);
        }
        in.close();
        JsonNode jsonNode = objectMapper.readTree(content.toString());
        return new WebhookHttpResponse(responseCode, jsonNode.get("created").asText());
      } else {
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
        String errorInputLine;
        StringBuilder errorContent = new StringBuilder();
        while ((errorInputLine = errorReader.readLine()) != null) {
          errorContent.append(errorInputLine);
        }
        errorReader.close();
        JsonNode jsonNode = objectMapper.readTree(errorContent.toString());
        return new WebhookHttpResponse(responseCode, jsonNode.get("message").asText());
      }
    } catch (Exception e) {
      log4j.error(e.getMessage());
      fail(e.getMessage());
    }
    return null;
  }

  public void assertWebhookParams(String name, String description, String rule) {
    assertEquals(PARAM_NAME, name);
    assertEquals(PARAM_DESCRIPTION, description);
    assertEquals(PARAM_RULE, rule);
  }

  public void assertWebhookParams(String name, String description, String rule, String paramNoRequired) {
    assertEquals(PARAM_NAME, name);
    assertEquals(PARAM_DESCRIPTION, description);
    assertEquals(PARAM_RULE, rule);
    assertEquals(PARAM_NO_REQUIRED, paramNoRequired);

  }

  private final List<Object> objectsToDelete = new ArrayList<>();

  public void addObjectToDelete(Object object) {
    objectsToDelete.add(object);
  }

  public void deleteAll() {
    for (Object object : objectsToDelete) {
      if (object != null) {
        OBDal.getInstance().remove(object);
        OBDal.getInstance().flush();
      }
    }
    objectsToDelete.clear();
  }
}
