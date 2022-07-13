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

import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;
import com.etendoerp.webhookevents.exceptions.WebhookAuthException;
import com.etendoerp.webhookevents.exceptions.WebhookNotfoundException;
import com.etendoerp.webhookevents.exceptions.WebhookParamException;
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
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Main servlet handler for webhooks. This servlet routes webhooks request configured action
 */
public class WebhookServiceHandler extends HttpBaseServlet {
  private static final Logger log = LogManager.getLogger();

  /**
   * Method to handle auth methods and cases
   *
   * @param apikey
   *     Received token
   *
   * @return Token object
   */
  private DefinedwebhookToken checkKey(String apikey) throws WebhookAuthException {
    try {
      OBContext.setAdminMode();
      var keyCriteria = OBDal.getInstance()
          .createCriteria(DefinedwebhookToken.class)
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false);
      keyCriteria.add(Restrictions.eq(DefinedwebhookToken.PROPERTY_APIKEY, apikey));
      DefinedwebhookToken access = (DefinedwebhookToken) keyCriteria.uniqueResult();
      if (access == null) {
        var message = Utility.messageBD(new DalConnectionProvider(false),
            "smfwhe_apiKeyNotFound", OBContext.getOBContext().getLanguage().getLanguage());
        log.error(message);
        throw new WebhookAuthException(message);
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
   * @param access
   *     Token Object to check allowance
   *
   * @return OBObject of the called webhook
   *
   * @exception WebhookNotfoundException
   *     Exception triggered in case of unexisting webhook
   * @exception WebhookAuthException
   *     Exception triggered in case of auth issues
   */
  private DefinedWebHook getAction(String name, DefinedwebhookToken access) throws WebhookNotfoundException, WebhookAuthException {
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
   * Handler of GET requests. This is the entry point of the functionality
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
    try {
      String apikey = request.getParameter("apikey");
      var access = checkKey(apikey);

      String name = request.getParameter("name");
      var webHook = getAction(name, access);
      var action = getInstance(webHook.getJavaClass());

      Map<String, String> requestVars = new HashMap<>();
      var paramList = webHook.getSmfwheDefinedwebhookParamList();
      for (DefinedWebhookParam param : paramList) {
        String val = request.getParameter(param.getName());
        if (param.isRequired() && val == null) {
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
}
