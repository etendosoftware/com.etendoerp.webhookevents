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
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.DefinedwebhookAccess;
import com.etendoerp.webhookevents.data.DefinedwebhookRole;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openbravo.dal.security.SecurityChecker;


public class WebhookUtils {
  private static final Logger log4j = Logger.getLogger(WebhookSetupTest.class);

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
  static final String ERROR_MSG_NOT_ALLOW = "Entity smfwhe_definedwebhook may only have instances with client 0";

  /**
   * Creates a new DefinedwebhookToken and sets its attributes.
   *
   * @return the created DefinedwebhookToken.
   */
  public DefinedwebhookToken createApiToken() {
    DefinedwebhookToken token = OBProvider.getInstance().get(DefinedwebhookToken.class);
    Client client = OBDal.getInstance().get(Client.class, TestConstants.Clients.SYSTEM);
    Organization org = OBDal.getInstance().get(Organization.class, TestConstants.Orgs.MAIN);
    User user = OBDal.getInstance().get(User.class, TestConstants.Users.SYSTEM);

    try {
      token.setName(EXPECTED_TOKEN_NAME);
      // Get the System User Role
      token.setUserRole(user.getADUserRolesList().get(0));
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

/**
   * Creates a new DefinedWebHook and sets its attributes.
   *
   * @param clientID
   *     the ID of the client to associate with the new webhook.
   * @param orgID
   *     the ID of the organization to associate with the new webhook.
   * @param userID
   *     the ID of the user to associate with the new webhook.
   * @return the created DefinedWebHook.
   */
  public DefinedWebHook createWebhook(String clientID, String orgID, String userID) {
    DefinedWebHook webHook = OBProvider.getInstance().get(DefinedWebHook.class);

    Client client = OBDal.getInstance().get(Client.class, clientID);
    Organization org = OBDal.getInstance().get(Organization.class, orgID);
    User user = OBDal.getInstance().get(User.class, userID);

    try {
      webHook.setName(WEBHOOK_NAME);
      webHook.setClient(client);
      webHook.setOrganization(org);
      webHook.setCreatedBy(user);
      webHook.setDescription(WEBHOOK_DESCRIPTION);
      webHook.setJavaClass(WEBHOOK_JAVACLASS);
      webHook.setEventClass(WEBHOOK_EVENTCLASS);
      webHook.setAllowGroupAccess(true);

      OBDal.getInstance().save(webHook);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(webHook);
    } catch (Exception e) {
      log4j.error(e.getMessage());
      fail(e.getMessage());
    }
    return webHook;
  }

  /**
   *
   * Creates a new DefinedWebHook, sets its attributes, and checks if the current user has write access to the DefinedWebHook instance.
   * If the user does not have write access, a security exception will be thrown.
   *
   * @param clientID
   *     the ID of the client to associate with the new webhook.
   * @param orgID
   *     the ID of the organization to associate with the new webhook.
   * @param userID
   *     the ID of the user to associate with the new webhook.
   */
  public void createWebhookThrowError(String clientID, String orgID, String userID) {
    DefinedWebHook webHook = OBProvider.getInstance().get(DefinedWebHook.class);
    Client client = OBDal.getInstance().get(Client.class, clientID);
    Organization org = OBDal.getInstance().get(Organization.class, orgID);
    User user = OBDal.getInstance().get(User.class, userID);
    webHook.setName(WEBHOOK_NAME);
    webHook.setClient(client);
    webHook.setOrganization(org);
    webHook.setCreatedBy(user);
    webHook.setDescription(WEBHOOK_DESCRIPTION);
    webHook.setJavaClass(WEBHOOK_JAVACLASS);
    webHook.setEventClass(WEBHOOK_EVENTCLASS);
    webHook.setAllowGroupAccess(true);

    SecurityChecker.getInstance().checkWriteAccess(webHook);
  }

  /**
   * Creates a new DefinedWebhookParam associated with the given webhook and sets its attributes.
   *
   * @param webhook
   *     the DefinedWebHook to associate with the new webhook parameter.
   * @param name
   *     the name of the webhook parameter.
   * @param isRequired
   *     whether the webhook parameter is required.
   * @return the created DefinedWebhookParam.
   */
  public DefinedWebhookParam createWebhookParam(DefinedWebHook webhook, String name, boolean isRequired) {
    DefinedWebhookParam webhookParam = OBProvider.getInstance().get(DefinedWebhookParam.class);
    Client client = OBDal.getInstance().get(Client.class, TestConstants.Clients.SYSTEM);
    Organization org = OBDal.getInstance().get(Organization.class, TestConstants.Orgs.MAIN);
    User user = OBDal.getInstance().get(User.class, TestConstants.Users.SYSTEM);

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

  /**
   * Creates a new DefinedwebhookAccess associated with the given webhook and token, and sets its attributes.
   *
   * @param webhook
   *     the DefinedWebHook to associate with the new webhook access.
   * @param token
   *     the DefinedwebhookToken to associate with the new webhook access.
   * @return the created DefinedwebhookAccess.
   */
  public DefinedwebhookAccess createWebhookAccess(DefinedWebHook webhook, DefinedwebhookToken token) {
    DefinedwebhookAccess webhookAccess = OBProvider.getInstance().get(DefinedwebhookAccess.class);
    Client client = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
    Organization org = OBDal.getInstance().get(Organization.class, TestConstants.Orgs.MAIN);
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

  /**
   * Creates a new DefinedwebhookRole associated with the given webhook and sets its attributes.
   *
   * @param webhook
   *     the DefinedWebHook to associate with the new webhook role.
   * @return the created DefinedwebhookRole.
   */
  public DefinedwebhookRole createWebhookRole(DefinedWebHook webhook) {
    DefinedwebhookRole webhookRole = OBProvider.getInstance().get(DefinedwebhookRole.class);
    Client client = OBDal.getInstance().get(Client.class, TestConstants.Clients.FB_GRP);
    Organization org = OBDal.getInstance().get(Organization.class, TestConstants.Orgs.MAIN);
    User user = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);
    Role role = OBDal.getInstance().get(Role.class, TestConstants.Roles.FB_GRP_ADMIN);

    try {
      webhookRole.setSmfwheDefinedwebhook(webhook);
      webhookRole.setClient(client);
      webhookRole.setOrganization(org);
      webhookRole.setCreatedBy(user);
      webhookRole.setRole(role);

      OBDal.getInstance().save(webhookRole);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(webhookRole);
    } catch (Exception e) {
      log4j.error(e.getMessage());
      fail(e.getMessage());
    }
    return webhookRole;
  }

  /**
   * Sends a GET request to the specified URL with the provided parameters and returns the response.
   *
   * @param baseUrl
   *     the base URL to send the request to.
   * @param name
   *     the name parameter for the request.
   * @param apiKey
   *     the API key parameter for the request.
   * @param description
   *     the description parameter for the request.
   * @param rule
   *     the rule parameter for the request.
   * @return a WebhookHttpResponse containing the response code and message.
   */
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

  /**
   * Sets up the OBContext for the system user.
   * This includes setting the user, role, client, and organization, and updating the RequestContext with a VariablesSecureApp instance.
   */
  public void setupUserAdmin() {
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

  /**
   * Sets up the OBContext for the admin user.
   * This includes setting the user, role, client, and organization, and updating the RequestContext with a VariablesSecureApp instance.
   */
  public void setupUserSystem() {
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.FB_GRP, TestConstants.Orgs.MAIN);
    VariablesSecureApp vsa = new VariablesSecureApp(
        OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getRole().getId()
    );
    RequestContext.get().setVariableSecureApp(vsa);
  }

  /**
   * Asserts that the webhook parameters match the expected values.
   *
   * @param name
   *     the expected name of the webhook parameter.
   * @param description
   *     the expected description of the webhook parameter.
   * @param rule
   *     the expected rule of the webhook parameter.
   */
  public void assertWebhookParams(String name, String description, String rule) {
    assertEquals(PARAM_NAME, name);
    assertEquals(PARAM_DESCRIPTION, description);
    assertEquals(PARAM_RULE, rule);
  }

  /**
   * Asserts that the webhook parameters match the expected values, including an optional non-required parameter.
   *
   * @param name
   *     the expected name of the webhook parameter.
   * @param description
   *     the expected description of the webhook parameter.
   * @param rule
   *     the expected rule of the webhook parameter.
   * @param paramNoRequired
   *     the expected value of the non-required parameter.
   */
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

  /**
   * Deletes all objects in the objectsToDelete list.
   * For each object, if it is an instance of DefinedWebHook, it sets up the system user context,
   * otherwise, it sets up the admin user context. Then it removes the object from the database and flushes the session.
   */
  public void deleteAll() {
    for (Object object : objectsToDelete) {
      if (object != null) {
        Runnable setupUser = shouldBeSystem(object) ?
            this::setupUserSystem : this::setupUserAdmin;
        setupUser.run();
        OBDal.getInstance().remove(object);
        OBDal.getInstance().flush();
      }
    }
    objectsToDelete.clear();
  }

  /**
   * Returns whether the object should be associated with the system user.
   * @param object the object to check.
   * @return true if the object should be associated with the system user, false otherwise.
   */
  private static boolean shouldBeSystem(Object object) {
    return object instanceof DefinedWebHook
        || object instanceof DefinedWebhookParam
        || object instanceof DefinedwebhookToken
        || object instanceof Alert;
  }

}
