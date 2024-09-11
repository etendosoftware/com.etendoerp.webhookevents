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

package com.etendoerp.webhookevents.services;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.DefinedwebhookRole;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;
import com.etendoerp.webhookevents.exceptions.WebhookAuthException;
import com.etendoerp.webhookevents.exceptions.WebhookNotfoundException;
import com.etendoerp.webhookevents.exceptions.WebhookParamException;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

/**
 * Main servlet handler for webhooks. This servlet routes webhooks request configured action
 */
public class WebhookServiceHandler extends HttpBaseServlet {
  private static final Logger log = LogManager.getLogger();
  public static final String CONTENT_TYPE = "Content-Type";

  enum HttpMethod {
    GET, POST, PUT, DELETE
  }

  /**
   * Method to handle auth methods and cases
   *
   * @param apikey
   *     Received token
   * @return Token object
   */
  private DefinedwebhookToken checkUserSecurity(String apikey, String token) {
    // Check access by token
    var keyCriteria = OBDal.getInstance()
        .createCriteria(DefinedwebhookToken.class)
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .add(Restrictions.eq(DefinedwebhookToken.PROPERTY_ROLEACCESS, false));
    keyCriteria.add(Restrictions.eq(DefinedwebhookToken.PROPERTY_APIKEY, apikey));
    DefinedwebhookToken access = (DefinedwebhookToken) keyCriteria.uniqueResult();
    if (access == null && token != null) {
      try {
        DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
        if (decodedToken != null) {
          String roleId = decodedToken.getClaim("role").asString();
          String userId = decodedToken.getClaim("user").asString();
          var userRole = OBDal.getInstance()
              .createQuery(UserRoles.class,
                  "as e where e.role.id = :roleId and e.userContact.id = :userId")
              .setNamedParameter("roleId", roleId)
              .setNamedParameter("userId", userId)
              .uniqueResult();
          access = (DefinedwebhookToken) OBDal.getInstance()
              .createCriteria(DefinedwebhookToken.class)
              .setFilterOnReadableClients(false)
              .setFilterOnReadableOrganization(false)
              .add(Restrictions.eq(DefinedwebhookToken.PROPERTY_ROLEACCESS, true))
              .add(Restrictions.eq(DefinedwebhookToken.PROPERTY_USERROLE, userRole))
              .uniqueResult();
        }
      } catch (Exception e) {
        log.debug("Error decoding token", e);
      }
    }
    if (access == null) {
      return null;
    }
    var userRole = access.getUserRole();
    OBContext.setOBContext(userRole.getUserContact().getId(), userRole.getRole().getId(),
        userRole.getClient().getId(), userRole.getOrganization().getId());
    return access;
  }

  /**
   * Cross filtering to find a called action and if the user is allowed to call it
   *
   * @param name
   *     Webhook name
   * @return OBObject of the called webhook
   * @throws WebhookNotfoundException
   *     Exception triggered in case of unexisting webhook
   * @throws WebhookAuthException
   *     Exception triggered in case of auth issues
   */
  private DefinedWebHook getAction(String name) throws WebhookNotfoundException {
    var criteria = OBDal.getInstance().createQuery(DefinedWebHook.class, "name = :name");
    criteria.setNamedParameter("name", name);
    var action = criteria.uniqueResult();
    if (action == null) {
      var message = Utility.messageBD(new DalConnectionProvider(false), "smfwhe_actionNotFound",
          OBContext.getOBContext().getLanguage().getLanguage());
      message = String.format(message, name);
      log.error(message);
      throw new WebhookNotfoundException(message);
    }
    return action;
  }

  /**
   * Weld helper to load configured webhook handler
   *
   * @param javaClass
   *     Java class name of handler. Must extends {@link com.etendoerp.webhookevents.services.BaseWebhookService}
   * @return Instances of handle
   * @throws ClassNotFoundException
   *     triggerd in case of instancing problems
   */
  private BaseWebhookService getInstance(String javaClass) throws ClassNotFoundException {

    @SuppressWarnings("unchecked") final var handlerClass = (Class<BaseWebhookService>) OBClassLoader.getInstance()
        .loadClass(javaClass);
    return WeldUtils.getInstanceFromStaticBeanManager(handlerClass);

  }

  /**
   * Build an standard json response
   *
   * @param response
   *     Http Object needed to obtain the writer
   * @param code
   *     HTTP code of the response (200, 203, etc)
   * @param responseVars
   *     Map containing key value tuple to include in the response
   * @throws JSONException
   *     Triggered in case of cannot generate a valid JSON String
   * @throws IOException
   *     Triggerd in case of issues with response writer
   */
  private void buildResponse(HttpServletResponse response, int code,
      Map<String, String> responseVars) throws JSONException, IOException {
    response.setStatus(code);
    response.addHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
    JSONObject responseBody = new JSONObject();
    for (var entry : responseVars.entrySet()) {
      responseBody.put(entry.getKey(), entry.getValue());
    }
    PrintWriter out = response.getWriter();
    out.print(responseBody);
  }

  /**
   * Build an standard json response
   *
   * @param response
   *     Http Object needed to obtain the writer
   * @param code
   *     HTTP code of the response (200, 203, etc)
   * @param responseMessage
   *     String containing response message
   * @throws JSONException
   *     Triggered in case of cannot generate a valid JSON String
   * @throws IOException
   *     Triggerd in case of issues with response writer
   */
  private void buildResponse(HttpServletResponse response, int code, String responseMessage)
      throws JSONException, IOException {
    Map<String, String> responseVars = new HashMap<>();
    responseVars.put("message", responseMessage);
    buildResponse(response, code, responseVars);
  }

  /**
   * Handle the request
   *
   * @param httpMethod
   *     Http method of the request
   * @param request
   *     Http request object
   * @param response
   *     Http response object
   */
  private void handleRequest(HttpMethod httpMethod, HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    try {
      OBContext.setAdminMode();
      // Check if webhook exists
      String name = request.getParameter("name");
      if (StringUtils.isEmpty(name)) {
        name = request.getPathInfo().substring(1);
      }
      var webHook = getAction(name);

      // Get JWT token if exists
      String token = obtainToken(request);
      boolean allow = isAllowed(request, token, webHook);
      if (!allow) {
        // User is not allowed to call webhook
        var message = Utility.messageBD(new DalConnectionProvider(false),
            "smfwhe_unauthorizedToken", OBContext.getOBContext().getLanguage().getLanguage());
        if (webHook.isAllowGroupAccess()) {
          String roleMessage = Utility.messageBD(new DalConnectionProvider(false),
              "smfwhe_unauthorizedRole", OBContext.getOBContext().getLanguage().getLanguage());
          message += " " + String.format(roleMessage, OBContext.getOBContext().getRole().getName(), webHook.getName());
        }
        log.error(message);
        throw new WebhookAuthException(message);
      }
      // Get handler
      var action = getInstance(webHook.getJavaClass());
      JSONObject body = extractBodyData(httpMethod, request);
      Map<String, String> requestParams = getRequestParams(webHook, body);
      Map<String, String> responseVars = new HashMap<>();
      action.get(requestParams, responseVars);
      buildResponse(response, HttpStatus.SC_OK, responseVars);
    } catch (WebhookAuthException e) {
      buildErrorResponse(response, HttpStatus.SC_UNAUTHORIZED, e.getMessage());
    } catch (WebhookNotfoundException e) {
      buildErrorResponse(response, HttpStatus.SC_NOT_FOUND, e.getMessage());
    } catch (ClassNotFoundException e) {
      buildErrorResponse(response, HttpStatus.SC_INTERNAL_SERVER_ERROR,
          String.format(OBMessageUtils.messageBD("smfwhe_classNotFound"), e.getMessage()));
    } catch (Exception e) {
      buildErrorResponse(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void buildErrorResponse(HttpServletResponse response, int status, String msg) {
    try {
      buildResponse(response, status, msg);
    } catch (IOException | JSONException ex) {
      throw new OBException(ex);
    }
  }

  private static Map<String, String> getRequestParams(DefinedWebHook webHook, JSONObject body)
      throws JSONException, WebhookParamException {
    Map<String, String> requestParams = new HashMap<>();
    var paramList = webHook.getSmfwheDefinedwebhookParamList();
    for (DefinedWebhookParam param : paramList) {
      String val = null;
      if (body.has(param.getName())) {
        val = body.getString(param.getName());
      }
      if (BooleanUtils.isTrue(param.isRequired()) && StringUtils.isEmpty(val)) {
        var message = Utility.messageBD(new DalConnectionProvider(false),
            "smfwhe_missingParameter", OBContext.getOBContext().getLanguage().getLanguage());
        message = String.format(message, param.getName());
        log.error(message);
        throw new WebhookParamException(message);
      }
      if (val != null) {
        requestParams.put(param.getName(), val);
      }
    }
    return requestParams;
  }

  private static String obtainToken(HttpServletRequest request) {
    String authStr = request.getHeader("Authorization");
    String token = null;
    if (authStr != null && authStr.startsWith("Bearer ")) {
      token = authStr.substring(7);
    }
    return token;
  }

  private boolean isAllowed(HttpServletRequest request, String token, DefinedWebHook webHook) {
    // Get API Key if exists
    String apikey = request.getParameter("apikey");
    // Check if user is allowed to call webhook
    var definedwebhookToken = checkUserSecurity(apikey, token);
    boolean allow = false;
    if (definedwebhookToken != null) {
      allow = webHook.getSmfwheDefinedwebhookAccessList()
          .stream()
          .filter(p -> p.getSmfwheDefinedwebhookToken()
              .getId()
              .compareTo(definedwebhookToken.getId()) == 0)
          .count() == 1;
    }
    if (!allow) {
      // Check if user is allowed to call webhook by role
      var groupAccess = checkRoleSecurity(request, token, webHook);
      if (groupAccess != null) {
        allow = true;
      }
    }
    return allow;
  }

  private static JSONObject extractBodyData(HttpMethod httpMethod, HttpServletRequest request)
      throws IOException, WebhookParamException {
    JSONObject body;
    try {
      if (httpMethod == HttpMethod.GET) {
        body = new JSONObject();
        for (String key : request.getParameterMap().keySet()) {
          body.put(key, request.getParameter(key));
        }
      } else {
        body = new JSONObject(request.getReader().lines().collect(Collectors.joining()));
      }
    } catch (JSONException e) {
      var message = Utility.messageBD(new DalConnectionProvider(false),
          "smfwhe_cannotCollectData", OBContext.getOBContext().getLanguage().getLanguage());
      log.error(message);
      throw new WebhookParamException(message);
    }
    return body;
  }

  /**
   * This method checks the security of the role based on the provided HttpServletRequest, token, and DefinedWebHook.
   * It first decodes the token to get the user ID, role ID, organization ID, warehouse ID, and client ID.
   * If any of these are null or empty, it throws an OBException indicating that the token is not valid.
   * It then sets the OBContext based on these IDs and logs the user ID.
   * It retrieves the Role object based on the role ID and returns the DefinedwebhookRole object that matches the provided DefinedWebHook and the retrieved Role.
   * If an exception occurs during the execution of the method, it logs the error and returns null.
   *
   * @param request
   *     The HttpServletRequest object that contains the request the client has made of the servlet.
   * @param token
   *     The token string to decode and use for setting the OBContext.
   * @param webHook
   *     The DefinedWebHook object to match in the getDefinedwebhookRole method.
   * @return The matching DefinedwebhookRole object, or null if no match is found, an exception occurs, or the token is not valid.
   */
  private DefinedwebhookRole checkRoleSecurity(HttpServletRequest request, String token,
      DefinedWebHook webHook) {
    try {
      DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
      String userId = decodedToken.getClaim("user").asString();
      String roleId = decodedToken.getClaim("role").asString();
      String orgId = decodedToken.getClaim("organization").asString();
      String warehouseId = decodedToken.getClaim("warehouse").asString();
      String clientId = decodedToken.getClaim("client").asString();
      if (userId == null || userId.isEmpty() || roleId == null || roleId.isEmpty() || orgId == null || orgId.isEmpty() || warehouseId == null || warehouseId.isEmpty() || clientId == null || clientId.isEmpty()) {
        throw new OBException("SWS - Token is not valid");
      }
      log.debug("SWS accessed by userId {}", userId);
      OBContext.setOBContext(
          SecureWebServicesUtils.createContext(userId, roleId, orgId, warehouseId, clientId));
      OBContext.setOBContextInSession(request, OBContext.getOBContext());
      SessionInfo.setUserId(userId);
      SessionInfo.setProcessType("WS");
      SessionInfo.setProcessId("DAL");

      Role role = OBDal.getInstance().get(Role.class, roleId);

      return getDefinedwebhookRole(webHook, role);
    } catch (Exception e) {
      log.debug("Error decoding token", e);
    }
    return null;
  }

  /**
   * This method retrieves a DefinedwebhookRole object based on the provided DefinedWebHook and Role objects.
   * It sets the OBContext to admin mode to bypass security checks, then creates a criteria query on the DefinedwebhookRole class.
   * The query filters for a DefinedwebhookRole object that has the same DefinedWebHook and Role as the provided ones.
   * If such a DefinedwebhookRole object is found, it is returned. Otherwise, null is returned.
   * If an exception occurs during the execution of the method, it is logged and null is returned.
   * The OBContext is restored to its previous mode at the end of the method, regardless of whether an exception occurred or not.
   *
   * @param webHook
   *     The DefinedWebHook object to match in the query.
   * @param role
   *     The Role object to match in the query.
   * @return The matching DefinedwebhookRole object, or null if no match is found or an exception occurs.
   */
  private DefinedwebhookRole getDefinedwebhookRole(DefinedWebHook webHook, Role role) {
    DefinedwebhookRole definedwebhookRole = null;

    try {
      OBContext.setAdminMode();
      definedwebhookRole = (DefinedwebhookRole) OBDal.getInstance()
          .createCriteria(DefinedwebhookRole.class)
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .add(Restrictions.eq(DefinedwebhookRole.PROPERTY_SMFWHEDEFINEDWEBHOOK, webHook))
          .add(Restrictions.eq(DefinedwebhookRole.PROPERTY_ROLE, role))
          .uniqueResult();
      return definedwebhookRole;
    } catch (Exception e) {
      log.error("Error getting definedwebhookRole", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Handler of GET requests.
   *
   * @param request
   *     an {@link HttpServletRequest} object that
   *     contains the request the client has made
   *     of the servlet
   * @param response
   *     an {@link HttpServletResponse} object that
   *     contains the response the servlet sends
   *     to the client
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      if (StringUtils.startsWithIgnoreCase(request.getPathInfo(), "/docs")) {
        handleDocs(request, response);
        return;
      }
      handleRequest(HttpMethod.GET, request, response);
    } catch (IOException | JSONException e) {
      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      response.setHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
    }
  }

  private void handleDocs(HttpServletRequest request, HttpServletResponse response) throws JSONException {
    String hooklist = request.getParameter("hooks");
    String[] hooks = hooklist.split(",");
    if (hooks.length == 0) {
      return;
    }
    try {
      OBContext.setAdminMode();
      JSONArray infoWebhooksArray = new JSONArray();

      OBCriteria<DefinedWebHook> webhookCrit = OBDal.getInstance().createCriteria(
          DefinedWebHook.class);
      webhookCrit.add(Restrictions.in(DefinedWebHook.PROPERTY_NAME, hooks));
      List<DefinedWebHook> webhooks = webhookCrit.list();
      for (DefinedWebHook webhook : webhooks) {
        JSONObject info = new JSONObject();
        info.put("name", webhook.getName());
        info.put("description", webhook.getDescription());
        info.put("javaClass", webhook.getJavaClass());

        JSONArray info_params = new JSONArray();
        for (DefinedWebhookParam param : webhook.getSmfwheDefinedwebhookParamList()) {
          JSONObject paramInfo = new JSONObject();
          paramInfo.put("name", param.getName());
          paramInfo.put("type", "string");
          paramInfo.put("required", param.isRequired());
          info_params.put(paramInfo);
        }

        info.put("params", info_params);
        infoWebhooksArray.put(info);
      }
      OBPropertiesProvider prop = OBPropertiesProvider.getInstance();
      String jsonOpenAPI = jsonOpenAPISpec(
          prop.getOpenbravoProperties().getProperty("ETENDO_HOST", "http://localhost:8080/etendo"), "Webhooks API",
          "API to execute EtendoERP webhooks", "1.0.0", "/webhooks", infoWebhooksArray);

      response.setStatus(HttpStatus.SC_OK);
      response.setHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
      PrintWriter out = response.getWriter();
      out.print(jsonOpenAPI);
    } catch (IOException e) {
      log.error("Error sending response", e);
    } finally {
      OBContext.restorePreviousMode();
    }


  }


  /**
   * Generates a JSON representation of the OpenAPI specification based on the provided parameters.
   *
   * @param host
   *     The host URL for the API.
   * @param title
   *     The title of the API.
   * @param description
   *     The description of the API.
   * @param apiVersion
   *     The version of the API.
   * @param prefixParentPath
   *     The prefix parent path for the API endpoints.
   * @param infoWebhooksArray
   *     The array of webhook information.
   * @return A JSON string representing the OpenAPI specification.
   * @throws JSONException
   *     If there is an error while creating the JSON objects.
   */

  public String jsonOpenAPISpec(String host, String title, String description,
      String apiVersion, String prefixParentPath, JSONArray infoWebhooksArray) throws JSONException {
    JSONObject openApiSpec = new JSONObject();

    // Basic OpenAPI configuration
    openApiSpec.put("openapi", "3.0.1");
    JSONObject info = new JSONObject();
    info.put("title", title);
    info.put("description", description);
    info.put("version", apiVersion);
    openApiSpec.put("info", info);
    String prefixPath = StringUtils.isEmpty(prefixParentPath) ? "" : prefixParentPath;

    //server
    JSONObject server = new JSONObject();
    server.put("url", host);
    openApiSpec.put("servers", new JSONArray().put(server));

    JSONObject paths = new JSONObject();
    JSONObject components = new JSONObject();
    JSONObject schemas = new JSONObject();

    for (int i = 0; i < infoWebhooksArray.length(); i++) {
      JSONObject webhook = infoWebhooksArray.getJSONObject(i);
      String name = webhook.getString("name");

      // Creates the schema for the request body directly within each path
      JSONObject webhookSchema = new JSONObject();
      webhookSchema.put("type", "object");
      JSONObject properties = new JSONObject();
      JSONArray requiredFields = new JSONArray();

      JSONArray params = webhook.getJSONArray("params");
      for (int j = 0; j < params.length(); j++) {
        JSONObject param = params.getJSONObject(j);
        JSONObject paramSchema = new JSONObject();
        paramSchema.put("type", param.getString("type"));
        properties.put(param.getString("name"), paramSchema);

        if (param.getBoolean("required")) {
          requiredFields.put(param.getString("name"));
        }
      }
      webhookSchema.put("properties", properties);

      // Only add the `required` field if it has elements
      if (requiredFields.length() > 0) {
        webhookSchema.put("required", requiredFields);
      }

      // Define the path for each webhook as /webhooks/{name}

      String webhookPath = String.format("%s%s", prefixPath, StringUtils.startsWith(name, "/") ? name : "/" + name);
      JSONObject postPath = new JSONObject();
      postPath.put("summary", "Execute the " + name + " webhook");

      // Configure the request body directly in the endpoint definition
      JSONObject requestBody = new JSONObject();
      requestBody.put("required", true);
      JSONObject content = new JSONObject();
      JSONObject applicationJson = new JSONObject();
      applicationJson.put("schema", webhookSchema);
      content.put("application/json", applicationJson);
      requestBody.put("content", content);
      postPath.put("requestBody", requestBody);

      JSONObject responses = new JSONObject();
      JSONObject response200 = new JSONObject();
      response200.put("description", "Webhook response");
      responses.put("200", response200);
      postPath.put("responses", responses);

      paths.put(webhookPath, new JSONObject().put("post", postPath));
    }

    openApiSpec.put("paths", paths);

    // Add security schema for Bearer token
    JSONObject securitySchemas = new JSONObject();
    JSONObject bearerToken = new JSONObject();
    bearerToken.put("type", "http");
    bearerToken.put("scheme", "bearer");
    bearerToken.put("bearerFormat", "JWT");
    securitySchemas.put("secureWSToken", bearerToken);
    components.put("securitySchemes", securitySchemas);
    openApiSpec.put("components", components);

    // Add security to each path
    JSONObject security = new JSONObject();
    JSONArray securityArray = new JSONArray();
    JSONObject secureWSToken = new JSONObject();
    secureWSToken.put("secureWSToken", new JSONArray());
    securityArray.put(secureWSToken);
    security.put("secureWSToken", securityArray);
    Iterator<String> keys = paths.keys();

    while (keys.hasNext()) {
      String key = keys.next();
      JSONObject path = paths.getJSONObject(key);
      path.getJSONObject("post").put("security", security);
    }
    return openApiSpec.toString(4);
  }

  /**
   * Handler of POST requests.
   *
   * @param request
   *     an {@link HttpServletRequest} object that
   *     contains the request the client has made
   *     of the servlet
   * @param response
   *     an {@link HttpServletResponse} object that
   *     contains the response the servlet sends
   *     to the client
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      handleRequest(HttpMethod.POST, request, response);
    } catch (IOException e) {
      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      response.setHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
    }
  }
}
