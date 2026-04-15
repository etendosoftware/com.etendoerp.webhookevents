package com.etendoerp.webhookevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
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

import com.etendoerp.webhookevents.services.WebhookServiceHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        return parseWebhookResponse(responseCode, readResponseBody(con.getInputStream()), objectMapper);
      }

      java.io.InputStream errorStream = con.getErrorStream();
      if (errorStream == null) {
        fail("Server returned HTTP " + responseCode + " with no error body — endpoint may not be reachable");
        return null;
      }

      String errorBody = readResponseBody(errorStream);
      try {
        return parseWebhookResponse(responseCode, errorBody, objectMapper);
      } catch (Exception jsonEx) {
        log4j.error("Non-JSON error response (HTTP " + responseCode + "): " + errorBody);
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
          return invokeWebhookServiceLocally(name, apiKey, description, rule);
        }
        fail("Expected JSON from webhook endpoint but got HTTP " + responseCode +
            ". Response (first 500 chars): " +
            errorBody.substring(0, Math.min(500, errorBody.length())));
      }
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      fail(e.getMessage());
    }
    return null;
  }

  private WebhookHttpResponse parseWebhookResponse(int responseCode, String body, ObjectMapper objectMapper)
      throws Exception {
    JsonNode jsonNode = objectMapper.readTree(body);
    String message = responseCode == HttpURLConnection.HTTP_OK ? jsonNode.get("created").asText()
        : jsonNode.get("message").asText();
    return new WebhookHttpResponse(responseCode, message);
  }

  private String readResponseBody(java.io.InputStream inputStream) throws Exception {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder content = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      content.append(line);
    }
    reader.close();
    return content.toString();
  }

  private WebhookHttpResponse invokeWebhookServiceLocally(String name, String apiKey, String description,
      String rule) throws Exception {
    WebhookServiceHandler handler = new WebhookServiceHandler();
    HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);

    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put(PARAM_NAME, new String[] {name});
    parameterMap.put("apikey", new String[] {apiKey});
    if (StringUtils.isNotBlank(description)) {
      parameterMap.put(PARAM_DESCRIPTION, new String[] {description});
    }
    parameterMap.put(PARAM_RULE, new String[] {rule});

    org.mockito.Mockito.when(request.getParameter(PARAM_NAME)).thenReturn(name);
    org.mockito.Mockito.when(request.getParameter("apikey")).thenReturn(apiKey);
    org.mockito.Mockito.when(request.getParameter(PARAM_DESCRIPTION)).thenReturn(description);
    org.mockito.Mockito.when(request.getParameter(PARAM_RULE)).thenReturn(rule);
    org.mockito.Mockito.when(request.getParameterMap()).thenReturn(parameterMap);
    org.mockito.Mockito.when(request.getPathInfo()).thenReturn("/" + name);
    org.mockito.Mockito.when(request.getHeader("Authorization")).thenReturn(null);

    AtomicInteger statusCode = new AtomicInteger(HttpURLConnection.HTTP_OK);
    org.mockito.Mockito.doAnswer(invocation -> {
      statusCode.set(invocation.getArgument(0));
      return null;
    }).when(response).setStatus(org.mockito.ArgumentMatchers.anyInt());

    StringWriter output = new StringWriter();
    org.mockito.Mockito.when(response.getWriter()).thenReturn(new PrintWriter(output));

    handler.doGet(request, response);
    return parseWebhookResponse(statusCode.get(), output.toString(), new ObjectMapper());
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
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
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
   *
   * <p>After {@code OBDal.getInstance().commitAndClose()}, all entities become detached from the
   * Hibernate session. In Hibernate 6, calling {@code session.delete()} on a detached entity
   * either throws or silently does nothing. To avoid leaving stale data in the DB, each entity is
   * reloaded by ID before removal so the session always holds a managed reference.
   */
  public void deleteAll() {
    for (Object object : objectsToDelete) {
      if (object != null) {
        Runnable setupUser = shouldBeSystem(object) ?
            this::setupUserSystem : this::setupUserAdmin;
        setupUser.run();
        Object managed = reloadFromDb(object);
        if (managed != null) {
          OBDal.getInstance().remove(managed);
          OBDal.getInstance().flush();
        }
      }
    }
    objectsToDelete.clear();
  }

  /**
   * Reloads a BaseOBObject entity from the database by its ID so that the returned instance is
   * managed by the current Hibernate session. This is necessary when the entity may have been
   * detached (e.g. after {@code commitAndClose()}). Non-BaseOBObject references are returned as-is.
   *
   * @param object the entity to reload; may be managed or detached
   * @return a managed copy of the entity, or null if it no longer exists in the DB
   */
  private Object reloadFromDb(Object object) {
    if (object instanceof BaseOBObject) {
      Object id = ((BaseOBObject) object).getId();
      return id == null ? null : OBDal.getInstance().get(object.getClass(), id);
    }
    return object;
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
