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
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main servlet handler for webhooks. This servlet routes webhooks request configured action
 */
public class WebhookServiceHandler extends HttpBaseServlet {
  private static final Logger log = LogManager.getLogger();
  enum HttpMethod {
    GET, POST, PUT, DELETE
  }

  /**
   * Method to handle auth methods and cases
   *
   * @param apikey
   *     Received token
   *
   * @return Token object
   */
  private DefinedwebhookToken checkUserSecurity(String apikey, String token) throws WebhookAuthException {
    try {
      // Check access by token
      var keyCriteria = OBDal.getInstance()
          .createCriteria(DefinedwebhookToken.class)
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .add(Restrictions.eq(DefinedwebhookToken.PROPERTY_ROLEACCESS, false));
      keyCriteria.add(Restrictions.eq(DefinedwebhookToken.PROPERTY_APIKEY, apikey));
      DefinedwebhookToken access = (DefinedwebhookToken) keyCriteria.uniqueResult();
      if(access == null && token != null) {
        try {
          DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
          if (decodedToken != null) {
            String roleId = decodedToken.getClaim("role").asString();
            String userId = decodedToken.getClaim("user").asString();
            var userRole = OBDal.getInstance().createQuery(UserRoles.class, "as e where e.role.id = :roleId and e.userContact.id = :userId")
                .setNamedParameter("roleId", roleId)
                .setNamedParameter("userId", userId)
                .uniqueResult();
            access = (DefinedwebhookToken) OBDal.getInstance().createCriteria(DefinedwebhookToken.class)
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
      OBContext.setOBContext(userRole.getUserContact().getId(), userRole.getRole().getId(), userRole.getClient().getId(), userRole.getOrganization().getId());
      return access;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Cross filtering to find a called action and if the user is allowed to call it
   *
   * @param name
   *     Webhook name
   *
   * @return OBObject of the called webhook
   *
   * @exception WebhookNotfoundException
   *     Exception triggered in case of unexisting webhook
   * @exception WebhookAuthException
   *     Exception triggered in case of auth issues
   */
  private DefinedWebHook getAction(String name) throws WebhookNotfoundException, WebhookAuthException {
    var criteria = OBDal.getInstance().createQuery(DefinedWebHook.class, "name = :name");
    criteria.setNamedParameter("name", name);
    var action = (DefinedWebHook) criteria.uniqueResult();
    if (action == null) {
      var message = Utility.messageBD(new DalConnectionProvider(false),
          "smfwhe_actionNotFound", OBContext.getOBContext().getLanguage().getLanguage());
      log.error(message);
      throw new WebhookNotfoundException(message);
    }
    if (action.getSmfwheDefinedwebhookAccessList().stream()
        .filter(p -> p.getSmfwheDefinedwebhookToken().getId().compareTo(access.getId()) == 0)
        .count() == 0) {
      var message = Utility.messageBD(new DalConnectionProvider(false),
          "smfwhe_unauthorizedToken", OBContext.getOBContext().getLanguage().getLanguage());
      log.error(message);
      throw new WebhookAuthException(message);
    }
    return action;
  }

  /**
   * Weld helper to load configured webhook handler
   *
   * @param javaClass
   *     Java class name of handler. Must extends {@link com.etendoerp.webhookevents.services.BaseWebhookService}
   *
   * @return Instances of handle
   *
   * @exception ClassNotFoundException
   *     triggerd in case of instancing problems
   */
  private BaseWebhookService getInstance(String javaClass) throws ClassNotFoundException {

    @SuppressWarnings("unchecked") final var handlerClass = (Class<BaseWebhookService>) OBClassLoader
        .getInstance()
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
   *
   * @exception JSONException
   *     Triggered in case of cannot generate a valid JSON String
   * @exception IOException
   *     Triggerd in case of issues with response writer
   */
  private void buildResponse(HttpServletResponse response, int code, Map<String, String> responseVars) throws JSONException, IOException {
    response.setStatus(code);
    response.setHeader("Content-Type", "application/json");
    JSONObject responseBody = new JSONObject();
    for (String key : responseVars.keySet()) {
      responseBody.put(key, responseVars.get(key));
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
   *
   * @exception JSONException
   *     Triggered in case of cannot generate a valid JSON String
   * @exception IOException
   *     Triggerd in case of issues with response writer
   */
  private void buildResponse(HttpServletResponse response, int code, String responseMessage) throws JSONException, IOException {
    Map<String, String> responseVars = new HashMap<>();
    responseVars.put("message", responseMessage);
    buildResponse(response, code, responseVars);
  }

  /**
   * Handle the request
   * @param httpMethod
   *    Http method of the request
   * @param request
   *   Http request object
   * @param response
   *   Http response object
   */
  private void handleRequest(HttpMethod httpMethod, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    try {
      OBContext.setAdminMode();
      // Check if webhook exists
      String name = request.getParameter("name");
      var webHook = getAction(name);

      // Get JWT token if exists
      String authStr = request.getHeader("Authorization");
      String token = null;
      if (authStr != null && authStr.startsWith("Bearer ")) {
        token = authStr.substring(7);
      }
      // Get API Key if exists
      String apikey = request.getParameter("apikey");
      // Check if user is allowed to call webhook
      var definedwebhookToken = checkUserSecurity(apikey, token);
      boolean allow = false;
      if(definedwebhookToken != null) {
        allow = webHook.getSmfwheDefinedwebhookAccessList()
          .stream()
          .filter(p -> p.getSmfwheDefinedwebhookToken()
              .getId()
              .compareTo(definedwebhookToken.getId()) == 0)
          .count() == 1;
      }
      if(!allow) {
        // Check if user is allowed to call webhook by role
        var groupAccess = checkGroupSecurity(request, token, webHook);
        if(groupAccess != null) {
          allow = true;
        }
      }
      if(!allow) {
        // User is not allowed to call webhook
        var message = Utility.messageBD(new DalConnectionProvider(false),
            "smfwhe_unauthorizedToken", OBContext.getOBContext().getLanguage().getLanguage());
        log.error(message);
        throw new WebhookNotfoundException(message);
      }
      // Get handler
      var action = getInstance(webHook.getJavaClass());
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

      Map<String, String> requestVars = new HashMap<>();
      var paramList = webHook.getSmfwheDefinedwebhookParamList();
      for (DefinedWebhookParam param : paramList) {
        String val = null;
        if(body.has(param.getName())) {
          val = body.getString(param.getName());
        }
        if (param.isRequired() && StringUtils.isEmpty(val)) {
          var message = Utility.messageBD(new DalConnectionProvider(false),
              "smfwhe_missingParameter", OBContext.getOBContext().getLanguage().getLanguage());
          log.error(message);
          throw new WebhookParamException(message);
        }
        if (val != null) {
          requestVars.put(param.getName(), val);
        }
      }
      Map<String, String> responseVars = new HashMap<>();
      action.get(requestVars, responseVars);
      buildResponse(response, HttpStatus.SC_OK , responseVars);
    } catch (WebhookAuthException e) {
      try {
        buildResponse(response, HttpStatus.SC_UNAUTHORIZED, e.getMessage());
      } catch (JSONException ex) {
        throw new RuntimeException(ex);
      }
    } catch (WebhookNotfoundException e) {
      try {
        buildResponse(response, HttpStatus.SC_NOT_FOUND, e.getMessage());
      } catch (JSONException ex) {
        throw new RuntimeException(ex);
      }
    } catch (Exception e) {
      try {
        buildResponse(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      } catch (JSONException ex) {
        throw new OBException(ex);
      }
    }
  }

  private DefinedwebhookRole checkGroupSecurity(HttpServletRequest request, String token,
      DefinedWebHook webHook) {
    try {
      DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
      String userId = decodedToken.getClaim("user").asString();
      String roleId = decodedToken.getClaim("role").asString();
      String orgId = decodedToken.getClaim("organization").asString();
      String warehouseId = decodedToken.getClaim("warehouse").asString();
      String clientId = decodedToken.getClaim("client").asString();
      if (userId == null || userId.isEmpty() || roleId == null || roleId.isEmpty() || orgId == null
          || orgId.isEmpty() || warehouseId == null || warehouseId.isEmpty() || clientId == null || clientId.isEmpty()) {
        throw new OBException("SWS - Token is not valid");
      }
      log.debug("SWS accessed by userId " + userId);
      OBContext.setOBContext(
          SecureWebServicesUtils.createContext(userId, roleId, orgId, warehouseId, clientId));
      OBContext.setOBContextInSession(request, OBContext.getOBContext());
      SessionInfo.setUserId(userId);
      SessionInfo.setProcessType("WS");
      SessionInfo.setProcessId("DAL");

      if (decodedToken != null) {
        Role role = OBDal.getInstance().get(Role.class, roleId);
        return (DefinedwebhookRole) OBDal.getInstance().createCriteria(DefinedwebhookRole.class)
            .setFilterOnReadableClients(false)
            .setFilterOnReadableOrganization(false)
            .add(Restrictions.eq(DefinedwebhookRole.PROPERTY_SMFWHEDEFINEDWEBHOOK, webHook))
            .add(Restrictions.eq(DefinedwebhookRole.PROPERTY_ROLE, role))
            .uniqueResult();
      }
    } catch (Exception e) {
      log.debug("Error decoding token", e);
    }
    return null;
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
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    handleRequest(HttpMethod.GET, request, response);
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
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    handleRequest(HttpMethod.POST, request, response);
  }
}
