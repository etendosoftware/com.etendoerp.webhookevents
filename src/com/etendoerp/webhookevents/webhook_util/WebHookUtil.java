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

package com.etendoerp.webhookevents.webhook_util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import javax.enterprise.inject.Any;
import javax.net.ssl.HttpsURLConnection;

import com.etendoerp.webhookevents.interfaces.DynamicEventHandler;
import com.etendoerp.webhookevents.interfaces.DynamicNode;
import com.etendoerp.webhookevents.interfaces.IChangeDataHook;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

import com.etendoerp.webhookevents.annotation.InjectHook;
import com.etendoerp.webhookevents.data.Arguments;
import com.etendoerp.webhookevents.data.ArgumentsData;
import com.etendoerp.webhookevents.data.EventType;
import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.JsonXmlData;
import com.etendoerp.webhookevents.data.QueueEventHook;
import com.etendoerp.webhookevents.data.UrlPathParam;
import com.etendoerp.webhookevents.data.Webhook;
import com.etendoerp.webhookevents.interfaces.ComputedFunction;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class WebHookUtil {
  private WebHookUtil() {
  }

  private static final String LANGUAGE = OBContext.getOBContext().getLanguage().getLanguage();
  private static final ConnectionProvider conn = new DalConnectionProvider(false);
  private static final Class<ComputedFunction> computedFunction = ComputedFunction.class;
  private static final Class<DynamicNode> dynamicNode = DynamicNode.class;
  public static final String TABLE = "table";

  @InjectHook
  @Any
  private static List<IChangeDataHook> hooks;

  /**
   * Inserts an event record in the queue.
   *
   * @param tableId     Table the event is defined for
   * @param eventTypeId (On Create/Update/Delete, see Constant class for defaults).
   * @param eventClass  Event Handler,Java,Stored Procedure(see Constant class or Reference List)
   * @param recordId    ID of the record affected
   */
  public static void queueEvent(String tableId, String eventTypeId, String eventClass,
      String recordId) {
    Table table = OBDal.getInstance().get(Table.class, tableId);
    EventType eventType = OBDal.getInstance().get(EventType.class, eventTypeId);

    queueEvent(table, eventType, eventClass, recordId);

  }

  /**
   * Inserts an event record in the queue. Special for events handlers, include Event Handler and
   * Dynamic Event Handler Types
   *
   * @param tableName   Table Name the event is defined for
   * @param tableId     Table ID the event is defined for
   * @param eventTypeId (On Create/Update/Delete, see Constant class for defaults).
   * @param recordId    ID of the record affected
   */
  public static void queueEventFromEventHandler(String tableName, String tableId,
      String eventTypeId, String recordId) {
    List<Events> lEvents = WebHookUtil.getEventHandlerClassEvents(eventTypeId, tableName);
    if (!lEvents.isEmpty()) {
      QueueEventHook obj = OBProvider.getInstance().get(QueueEventHook.class);
      Events event = lEvents.get(0);
      String javaClass = event.getDynamicEventJavaclass();
      boolean dynamicEventSuccess = true;

      if (javaClass != null && !StringUtils.isEmpty(javaClass)) {
        try {
          @SuppressWarnings("unchecked") final Class<DynamicEventHandler> dynamicEventHandlerClass = (Class<DynamicEventHandler>) OBClassLoader.getInstance()
              .loadClass(javaClass);
          final DynamicEventHandler dynamicEventHandler = dynamicEventHandlerClass.getDeclaredConstructor()
              .newInstance();

          dynamicEventSuccess = dynamicEventHandler.execute(event.getTable(),
              event.getSmfwheEventType(), recordId);

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
          dynamicEventSuccess = false;
        } catch (InvocationTargetException e) {
          throw new OBException(e);
        }
      }

      if (dynamicEventSuccess) {
        obj.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
        obj.setCreationDate(new Date());
        obj.setUpdated(new Date());
        obj.setRecord(recordId);
        obj.setTable(OBDal.getInstance().get(Table.class, tableId));
        obj.setSmfwheEvents(event);

        OBDal.getInstance().save(obj);
      }
    }
  }

  /**
   * Inserts an event record in the queue.
   *
   * @param table      Table the event is defined for
   * @param eventType  (On Create/Update/Delete, see Constant class for defaults).
   * @param eventClass Event Handler,Java,Stored Procedure(see Constant class or Reference List)
   * @param recordId   ID of the record affected
   */
  public static void queueEvent(Table table, EventType eventType, String eventClass,
      String recordId) {
    if (eventType != null && table != null) {
      List<Events> lEvents = WebHookUtil.eventsFromTableName(eventType.getId(),
          table.getDBTableName(), eventClass);
      if (!lEvents.isEmpty()) {
        QueueEventHook obj = OBProvider.getInstance().get(QueueEventHook.class);

        obj.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
        obj.setCreationDate(new Date());
        obj.setUpdated(new Date());
        obj.setRecord(recordId);
        obj.setTable(table);
        obj.setSmfwheEvents(lEvents.get(0));

        OBDal.getInstance().save(obj);
      }
    }
  }

  /**
   * Call the all webhook defined in this event
   *
   * @param event  Events
   * @param bob    BaseOBObject to generate data (JSON or XML)
   * @param logger Info logger in log
   * @throws OBException
   */
  public static void callWebHook(Events event, BaseOBObject bob, Logger logger) throws OBException {
    OBCriteria<Webhook> cWebhook = OBDal.getInstance().createCriteria(Webhook.class);
    cWebhook.add(Restrictions.eq(Webhook.PROPERTY_SMFWHEEVENTS, event));
    for (Webhook hook : cWebhook.list()) {
      try {
        if (BooleanUtils.isTrue(hook.isActive())) {
          sendEvent(hook, bob, logger);
        }
      } catch (Exception e) {
        throw new OBException(e);
      }
    }
  }

  /**
   * Send the request to url to notify defined in webhook
   *
   * @param hook   Webhook defined in events
   * @param bob    BaseOBObject to generate data (JSON or XML)
   * @param logger Info logger in log
   * @throws Exception
   */
  public static void sendEvent(Webhook hook, BaseOBObject bob, Logger logger) throws Exception {

    OBCriteria<UrlPathParam> cUrlPathParam = OBDal.getInstance().createCriteria(UrlPathParam.class);
    cUrlPathParam.add(Restrictions.eq(UrlPathParam.PROPERTY_TYPEPARAMETER, "P"));

    String url = generateUrlParameter(cUrlPathParam.list(), hook.getUrlnotify(), bob,
        logger).toLowerCase();
    URL obj = new URL(url);
    HttpURLConnection con = getConnection(url, obj);
    // Setting basic post request
    cUrlPathParam = OBDal.getInstance().createCriteria(UrlPathParam.class);
    cUrlPathParam.add(Restrictions.eq(UrlPathParam.PROPERTY_TYPEPARAMETER, "H"));
    con.setRequestMethod(hook.getSmfwheEvents().getMethod());
    setHeaderConnection(con, cUrlPathParam.list(), logger, bob);

    // Verify if can data is json or xml
    String sendData = "";
    WebHookInitializer.initialize();
    if (hook.getTypedata().equals(Constants.STRING_JSON)) {
      sendData = getJSONToSend(hook, bob, logger);
    } else if (hook.getTypedata().equals(Constants.STRING_XML)) {
      sendData = getXMLToSend(hook, bob, logger);
    }

    // Send post request
    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.writeBytes(sendData);
    wr.flush();
    wr.close();

    int responseCode = con.getResponseCode();
    logger.debug("nSending " + hook.getSmfwheEvents().getMethod() + "request to URL : " + url);
    logger.debug("Post Data : " + sendData);
    logger.debug("Response Code : " + responseCode);

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String output;
    StringBuilder response = new StringBuilder();
    while ((output = in.readLine()) != null) {
      response.append(output);
    }
    in.close();

    // printing result from response
    logger.debug(response.toString());
  }

  private static String getXMLToSend(Webhook hook, BaseOBObject bob, Logger logger) throws Exception {
    String sendData;
    Object res = generateDataParametersJSON(hook.getSmfwheJsonDataList(), bob, logger);
    if (hooks != null) {
      for (IChangeDataHook hookJava : hooks) {
        res = hookJava.postProcessJSON(res);
      }
    }
    XStream xstream = new XStream(new DomDriver());
    sendData = xstream.toXML(res);
    return sendData;
  }

  private static String getJSONToSend(Webhook hook, BaseOBObject bob, Logger logger) throws Exception {
    String sendData;
    JSONObject jsonResult = null;
    Object res = generateDataParametersJSON(hook.getSmfwheJsonDataList(), bob, logger);
    if (hooks != null) {
      for (IChangeDataHook hookJava : hooks) {
        res = hookJava.postProcessJSON(res);
      }
    }
    jsonResult = (JSONObject) res;
    sendData = jsonResult.toString();
    return sendData;
  }

  private static HttpURLConnection getConnection(String url, URL obj) throws IOException {
    HttpURLConnection con = null;
    if (url.contains("http")) {
      con = (HttpURLConnection) obj.openConnection();
    } else if (url.contains("https")) {
      con = (HttpsURLConnection) obj.openConnection();
    }
    if (con == null) {
      throw new OBException("Invalid URL: " + url);
    }
    return con;
  }

  /**
   * Generate a JSON data parameter, take a StandardParameter list and return json with
   * StandardParameter set
   *
   * @param list   JsonXmlData list
   * @param bob    BaseOBObject to generate data in XML
   * @param logger Info logger in log
   * @return Return data in format JSON
   * @throws Exception
   */
  public static Object generateDataParametersJSON(List<JsonXmlData> list, BaseOBObject bob,
      Logger logger) throws Exception {
    JSONObject jsonMap = new JSONObject();
    LinkedList<Object> staticValues = new LinkedList<>();
    try {
      for (JsonXmlData node : list) {
        handleJsonNode(bob, logger, node, jsonMap, staticValues);
      }
      if (!staticValues.isEmpty()) {
        return staticValues;
      }
    } catch (Exception e) {
      String message = String.format(Utility.messageBD(conn, "smfwhe_errorGenerateJson", LANGUAGE),
          bob.getIdentifier());
      logger.error(message, e);
      throw new OBException(message);
    }
    return jsonMap;
  }

  private static void handleJsonNode(BaseOBObject bob, Logger logger, JsonXmlData node,
      JSONObject jsonMap, LinkedList<Object> staticValues) throws Exception {
    if (BooleanUtils.isTrue(node.isSummaryLevel())) {
      OBCriteria<JsonXmlData> cParentNodes = OBDal.getInstance()
          .createCriteria(JsonXmlData.class);
      cParentNodes.add(Restrictions.eq(JsonXmlData.PROPERTY_PARENT, node));
      if (BooleanUtils.isTrue(node.isArray())) {
        Object res = generateDataParametersJSON(cParentNodes.list(), bob, logger);
        if (res instanceof LinkedList) {
          jsonMap.put(node.getName(), new JSONArray((LinkedList<?>) res));
        } else {
          jsonMap.put(node.getName(), new JSONArray().put(res));
        }
      } else {
        jsonMap.put(node.getName(),
            generateDataParametersJSON(cParentNodes.list(), bob, logger));
      }
    } else {
      if (node.getName() == null || node.getName().isEmpty()) {
        addStaticValue(bob, logger, node, staticValues);
      } else {
        addValue(bob, logger, node, jsonMap);
      }
    }
  }

  private static void addValue(BaseOBObject bob, Logger logger, JsonXmlData node,
      JSONObject jsonMap) throws JSONException {
    String typeValue = node.getTypeValue();
    Object valueToAdd;
    if (Constants.TYPE_VALUE_STRING.equals(typeValue)) {
      valueToAdd = replaceValueData(node.getValue(), bob, logger);
    } else if (Constants.TYPE_VALUE_PROPERTY.equals(typeValue)) {
      valueToAdd = DalUtil.getValueFromPath(bob, node.getProperty()).toString();
    } else if (Constants.TYPE_VALUE_DYNAMIC_NODE.equals(typeValue)) {
      valueToAdd = getValueExecuteMethod(node, bob, logger, dynamicNode);
    } else {
      valueToAdd = null;
    }
    if (valueToAdd != null) {
      jsonMap.put(node.getName(), valueToAdd);
    }
  }

  private static void addStaticValue(BaseOBObject bob, Logger logger, JsonXmlData node,
      LinkedList<Object> staticValues) {
    String typeValue = node.getTypeValue();
    Object valueToAdd;
    if (Constants.TYPE_VALUE_STRING.equals(typeValue)) {
      valueToAdd = replaceValueData(node.getValue(), bob, logger);
    } else if (Constants.TYPE_VALUE_PROPERTY.equals(typeValue)) {
      valueToAdd = DalUtil.getValueFromPath(bob, node.getProperty()).toString();
    } else if (Constants.TYPE_VALUE_DYNAMIC_NODE.equals(typeValue)) {
      valueToAdd = getValueExecuteMethod(node, bob, logger, dynamicNode);
    } else {
      valueToAdd = null;
    }
    if (valueToAdd != null) {
      staticValues.add(valueToAdd);
    }
  }

  /**
   * Generate a UrlPathParam, take a UrlPathParam list and return url modify you can added in the
   * url
   *
   * @param lUrlPathParam Url Path Parameter list
   * @param url           Url to send request
   * @param bob           BaseOBObject
   * @param logger        Logger in log
   * @return Return the url with parameters set
   * @throws Exception
   */
  public static String generateUrlParameter(List<UrlPathParam> lUrlPathParam, String url,
      BaseOBObject bob, Logger logger) throws OBException {
    String result = url;
    for (UrlPathParam param : lUrlPathParam) {
      try {
        if (BooleanUtils.isTrue(param.isActive()) && Constants.TYPE_PARAMETER_PATH.equals(
            param.getTypeParameter())) {
          if (Constants.TYPE_VALUE_STRING.equals(param.getTypeValue())) {
            result = result.replace("{" + param.getName() + "}",
                replaceValueData(param.getValue(), bob, logger));
          } else if (Constants.TYPE_VALUE_PROPERTY.equals(param.getTypeValue())) {
            result = result.replace("{" + param.getName() + "}",
                DalUtil.getValueFromPath(bob, param.getProperty()).toString());
          } else if (Constants.TYPE_VALUE_COMPUTED.equals(param.getTypeValue())) {
            // call the function
            result = result.replace("{" + param.getName() + "}",
                getValueExecuteMethod(param, bob, logger, computedFunction).toString());
          }
        }
      } catch (Exception e) {
        String message = String.format(
            Utility.messageBD(conn, "smfwhe_errorReplacePathParameter", LANGUAGE), param.getName());
        logger.error(message, e);
        throw new OBException(message);
      }
    }
    return result;
  }

  /**
   * Return an Event list from a table name with the event type.
   *
   * @param eventTypeId ID for EventType object (see Constants class for defaults)
   * @param tableName
   * @return Return the Event list
   */
  public static List<Events> eventsFromTableName(String eventTypeId, String tableName) {
    OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
    cEvents.createAlias(Events.PROPERTY_TABLE, TABLE);
    cEvents.add(Restrictions.eq(Events.PROPERTY_ACTIVE, true));
    cEvents.add(Restrictions.eq(Events.PROPERTY_SMFWHEEVENTTYPE + "." + EventType.PROPERTY_ID,
        eventTypeId));
    cEvents.add(Restrictions.eq(TABLE + "." + Table.PROPERTY_DBTABLENAME, tableName));
    return cEvents.list();
  }

  /**
   * Returns an Event list from a table name with the event type, and event class.
   *
   * @param eventTypeId ID for EventType object (see Constants class for defaults)
   * @param tableName
   * @param eventClass  event class (Event Handler, Stored Procedure, see Constants class or reference list
   *                    for details)
   * @return
   */
  public static List<Events> eventsFromTableName(String eventTypeId, String tableName,
      String eventClass) {
    OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
    cEvents.createAlias(Events.PROPERTY_TABLE, TABLE);
    cEvents.add(Restrictions.eq(Events.PROPERTY_ACTIVE, true));
    cEvents.add(Restrictions.eq(Events.PROPERTY_SMFWHEEVENTTYPE + "." + EventType.PROPERTY_ID,
        eventTypeId));
    cEvents.add(Restrictions.eq(Events.PROPERTY_EVENTCLASS, eventClass));
    cEvents.add(Restrictions.eq("table." + Table.PROPERTY_DBTABLENAME, tableName));
    return cEvents.list();
  }

  /**
   * Returns an event list for all (default) event handler types.
   *
   * @param eventTypeId ID for EventType object (see Constants class for defaults)
   * @param tableName   Table Name
   * @return
   */
  public static List<Events> getEventHandlerClassEvents(String eventTypeId, String tableName) {
    OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
    cEvents.createAlias(Events.PROPERTY_TABLE, TABLE);
    cEvents.add(Restrictions.eq(Events.PROPERTY_ACTIVE, true));
    cEvents.add(Restrictions.eq(Events.PROPERTY_SMFWHEEVENTTYPE + "." + EventType.PROPERTY_ID,
        eventTypeId));
    cEvents.add(Restrictions.in(Events.PROPERTY_EVENTCLASS,
        (Object[]) new String[] { Constants.DYNAMIC_EVENT_HANDLER, Constants.EVENT_HANDLER }));
    cEvents.add(Restrictions.eq("table." + Table.PROPERTY_DBTABLENAME, tableName));
    return cEvents.list();
  }

  /**
   * Return string concat with property value set
   *
   * @param value  String value defined in user defined data
   * @param bob    BaseOBObject to get the events defined
   * @param logger Info logger in log
   * @return Return a string with parameters set
   * @throws Exception
   */
  public static String replaceValueData(String value, BaseOBObject bob, Logger logger)
      throws OBException {
    StringBuilder result = new StringBuilder();
    String propertyError = null;
    try {
      for (String s : value.split(" ")) {
        if (s.contains(Constants.AT) && DalUtil.getValueFromPath(bob,
            s.split(Constants.AT)[1]) == null) {
          propertyError = s;
          throw new OBException();
        } else {
          result.append(s.contains(Constants.AT) ?
              DalUtil.getValueFromPath(bob, s.split(Constants.AT)[1]) :
              s).append(" ");
        }
      }
    } catch (Exception e) {
      String message = String.format(
          Utility.messageBD(conn, "smfwhe_errorParserParameter", LANGUAGE), propertyError);
      logger.error(message, e);
      throw new OBException(message);
    }
    return result.substring(0, result.length() - 1);
  }

  /**
   * Array of entities defined in events
   *
   * @return Return array of entities defined in events
   */
  public static Entity[] getEntities() {
    Entity[] entities = null;
    try {
      OBContext.setAdminMode();
      OBCriteria<Events> cEvent = OBDal.getInstance().createCriteria(Events.class);
      List<Events> lEvents = cEvent.list();
      entities = new Entity[lEvents.size()];
      int i = 0;
      for (Events e : lEvents) {
        entities[i] = ModelProvider.getInstance()
            .getEntityByTableName(e.getTable().getDBTableName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return entities;
  }

  /**
   * Execute the method and return the string for set in json or xml
   *
   * @param data         This is an object JsonXmlData or UrlPathParam
   * @param bob          BaseOBObject to get the events defined
   * @param logger       Info logger in log
   * @param compareClass class name for compare with interface extend
   * @return Return the string for set in json or xml
   * @throws Exception
   */
  public static Object getValueExecuteMethod(Object data, BaseOBObject bob, Logger logger,
      Object compareClass) throws OBException {
    Object result = null;
    JsonXmlData recordData = null;
    UrlPathParam recordParam = null;
    if (dynamicNode.equals(compareClass)) {
      recordData = (JsonXmlData) data;
    } else if (computedFunction.equals(compareClass)) {
      recordParam = (UrlPathParam) data;
    }
    if (recordData == null && recordParam == null) {
      throw new OBException("Data is null");
    }
    String classMethodName = recordParam == null ?
        recordData.getJavaClassName() :
        recordParam.getJavaClassName();
    Class<?> clazz; // convert string classname to class
    String message = "";
    try {
      clazz = Class.forName(classMethodName);
      Object dog = clazz.getDeclaredConstructor().newInstance();
      if (dog.getClass().getInterfaces()[0].equals(compareClass)) {
        Method setNameMethod = getMethod(recordParam, recordData, dog);
        // set the parameters in hashmap
        Map<Object, Object> params = recordParam == null ?
            getArgumentsForMethodData(recordData.getSmfwheArgsDataList(), bob, logger) :
            getArgumentsForMethod(recordParam.getSmfwheArgsList(), bob, logger);
        result = setNameMethod.invoke(dog, params); // pass arg
      } else {
        message = String.format(
            Utility.messageBD(conn, "smfwhe_errorParserClassMethodName", LANGUAGE),
            classMethodName);
        throw new OBException(message);
      }
    } catch (Exception e) {
      logger.error(message, e);
      throw new OBException(e);
    }
    return result;
  }

  private static Method getMethod(UrlPathParam recordParam, JsonXmlData recordData, Object dog)
      throws NoSuchMethodException {
    String methodName = "";
    if (Constants.TYPE_VALUE_COMPUTED.equals(
        recordParam == null ? recordData.getTypeValue() : recordParam.getTypeValue())) {
      methodName = Constants.METHOD_NAME;
    } else if (Constants.TYPE_VALUE_DYNAMIC_NODE.equals(
        recordParam == null ? recordData.getTypeValue() : recordParam.getTypeValue())) {
      methodName = Constants.METHOD_NAME_DYNAMIC_NODE;
    }
    return dog.getClass().getMethod(methodName, HashMap.class);
  }

  /**
   * @param con           HttpURLConnection
   * @param lUrlPathParam List the UrlPathParam
   * @param logger        Info logger in log
   * @param bob           BaseOBObject to get the events defined
   * @throws Exception
   */
  public static void setHeaderConnection(HttpURLConnection con, List<UrlPathParam> lUrlPathParam,
      Logger logger, BaseOBObject bob) throws OBException {
    String result = "";
    for (UrlPathParam param : lUrlPathParam) {
      if (Constants.TYPE_VALUE_STRING.equals(param.getTypeValue())) {
        result = replaceValueData(param.getValue(), bob, logger);
      } else if (Constants.TYPE_VALUE_PROPERTY.equals(param.getTypeValue())) {
        result = DalUtil.getValueFromPath(bob, param.getProperty()).toString();
      } else if (Constants.TYPE_VALUE_COMPUTED.equals(param.getTypeValue())) {
        // call the function
        result = getValueExecuteMethod(param, bob, logger, computedFunction).toString();
      }
      con.setRequestProperty(param.getName(), result);
    }
  }

  /**
   * Get the arguments for method
   *
   * @param args   List the arguments for method
   * @param bob    BaseOBObject to get the events defined
   * @param logger Info logger in log
   * @return Return the hashmap with parameters for execute method
   * @throws Exception
   */
  public static Map<Object, Object> getArgumentsForMethod(List<Arguments> args, BaseOBObject bob,
      Logger logger) throws OBException {
    HashMap<Object, Object> result = new HashMap<>();
    for (Arguments arg : args) {
      if (BooleanUtils.isTrue(arg.isActive())) {
        result.put(arg.getName(), replaceValueData(arg.getValueParameter(), bob, logger));
      }
    }
    return result;
  }

  /**
   * Get the arguments for method
   *
   * @param args   List the arguments for method
   * @param bob    BaseOBObject to get the events defined
   * @param logger Info logger in log
   * @return Return the hashmap with parameters for execute method
   * @throws Exception
   */
  public static Map<Object, Object> getArgumentsForMethodData(List<ArgumentsData> args,
      BaseOBObject bob, Logger logger) {
    HashMap<Object, Object> result = new HashMap<>();
    for (ArgumentsData arg : args) {
      if (BooleanUtils.isTrue(arg.isActive())) {
        result.put(arg.getName(), replaceValueData(arg.getValue(), bob, logger));
      }
    }
    return result;
  }

}
