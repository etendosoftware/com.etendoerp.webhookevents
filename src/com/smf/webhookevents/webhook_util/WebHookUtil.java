package com.smf.webhookevents.webhook_util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.webhookevents.annotation.InjectHook;
import com.smf.webhookevents.data.Arguments;
import com.smf.webhookevents.data.ArgumentsData;
import com.smf.webhookevents.data.EventType;
import com.smf.webhookevents.data.Events;
import com.smf.webhookevents.data.JsonXmlData;
import com.smf.webhookevents.data.UrlPathParam;
import com.smf.webhookevents.data.Webhook;
import com.smf.webhookevents.interfaces.ComputedFunction;
import com.smf.webhookevents.interfaces.DynamicNode;
import com.smf.webhookevents.interfaces.IChangeDataHook;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class WebHookUtil {
  final private static String language = OBContext.getOBContext().getLanguage().getLanguage();
  final private static ConnectionProvider conn = new DalConnectionProvider(false);
  final private static Class<ComputedFunction> computedFunction = com.smf.webhookevents.interfaces.ComputedFunction.class;
  final private static Class<DynamicNode> dynamicNode = com.smf.webhookevents.interfaces.DynamicNode.class;

  @InjectHook
  @Any
  private static List<IChangeDataHook> hooks;

  /**
   * Call the all webhook defined in this event
   * 
   * @param Event
   *          Events
   * @param Bob
   *          BaseOBObject to generate data (JSON or XML)
   * @param Logger
   *          Info logger in log
   * @throws Exception
   */
  public static void callWebHook(Events event, BaseOBObject bob, Logger logger) throws Exception {
    OBCriteria<Webhook> cWebhook = OBDal.getInstance().createCriteria(Webhook.class);
    cWebhook.add(Restrictions.eq(Webhook.PROPERTY_SMFWHEEVENTS, event));
    for (Webhook hook : cWebhook.list()) {
      try {
        if (hook.isActive()) {
          sendEvent(hook, bob, logger);
        }
      } catch (Exception e) {
        throw new Exception(e);
      }
    }
  }

  /**
   * Send the request to url to notify defined in webhook
   * 
   * @param Hook
   *          Webhook defined in events
   * @param Bob
   *          BaseOBObject to generate data (JSON or XML)
   * @param Logger
   *          Info logger in log
   * @throws Exception
   */
  public static void sendEvent(Webhook hook, BaseOBObject bob, Logger logger) throws Exception {

    OBCriteria<UrlPathParam> cUrlPathParam = OBDal.getInstance().createCriteria(UrlPathParam.class);
    cUrlPathParam.add(Restrictions.eq(UrlPathParam.PROPERTY_TYPEPARAMETER, "P"));

    String url = generateUrlParameter(cUrlPathParam.list(), hook.getUrlnotify(), bob, logger)
        .toLowerCase();
    URL obj = new URL(url);
    HttpURLConnection con = null;
    if (url.contains("http")) {
      con = (HttpURLConnection) obj.openConnection();
    } else if (url.contains("https")) {
      con = (HttpsURLConnection) obj.openConnection();
    }

    // Setting basic post request
    cUrlPathParam = OBDal.getInstance().createCriteria(UrlPathParam.class);
    cUrlPathParam.add(Restrictions.eq(UrlPathParam.PROPERTY_TYPEPARAMETER, "H"));
    con.setRequestMethod(hook.getSmfwheEvents().getMethod());
    setHeaderConnection(con, cUrlPathParam.list(), logger, bob);

    // Verify if can data is json or xml
    String sendData = "";
    // Get the treeNode
    OBCriteria<TreeNode> cTreeNode = OBDal.getInstance().createCriteria(TreeNode.class);
    cTreeNode.add(Restrictions.eq(TreeNode.PROPERTY_TREE + ".id", Constants.TREE_ID));
    cTreeNode.add(Restrictions.eq(TreeNode.PROPERTY_REPORTSET, "0"));
    cTreeNode.add(Restrictions.eq(TreeNode.PROPERTY_ACTIVE, true));
    WebHookInitializer.initialize();
    if (hook.getTypedata().equals(Constants.STRING_JSON)) {
      JSONObject jsonResult = null;
      Object res = generateDataParametersJSON(cTreeNode.list(), bob, logger);
      if (hooks != null) {
        for (IChangeDataHook hookJava : hooks) {
          res = hookJava.postProcessJSON(res);
        }
      }
      jsonResult = (JSONObject) res;
      sendData = jsonResult.toString();
    } else if (hook.getTypedata().equals(Constants.STRING_XML)) {
      Object res = generateDataParametersJSON(cTreeNode.list(), bob, logger);
      if (hooks != null) {
        for (IChangeDataHook hookJava : hooks) {
          res = hookJava.postProcessJSON(res);
        }
      }
      XStream xstream = new XStream(new DomDriver());
      sendData = xstream.toXML(res);
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
    logger.debug("Response:" + con.getContent());

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String output;
    StringBuffer response = new StringBuffer();

    while ((output = in.readLine()) != null) {
      response.append(output);
    }
    in.close();

    // printing result from response
    logger.debug(response.toString());
  }

  /**
   * Generate a JSON data parameter, take a StandardParameter list and return json with
   * StandardParameter set
   * 
   * @param ListParameters
   *          TreeNode list
   * @param Bob
   *          BaseOBObject to generate data in XML
   * @param Logger
   *          Info logger in log
   * @return Return data in format JSON
   * @throws Exception
   */
  public static Object generateDataParametersJSON(List<TreeNode> list, BaseOBObject bob,
      Logger logger) throws Exception {
    JSONObject jsonMap = new JSONObject();
    JsonXmlData node = null;
    LinkedList<Object> staticValues = new LinkedList<Object>();
    try {
      for (TreeNode treeNode : list) {
        node = OBDal.getInstance().get(JsonXmlData.class, treeNode.getNode());
        if (node.isSummaryLevel()) {
          OBCriteria<TreeNode> cTreeNode = OBDal.getInstance().createCriteria(TreeNode.class);
          cTreeNode.add(Restrictions.eq(TreeNode.PROPERTY_REPORTSET, treeNode.getNode()));
          if (node.isArray()) {
            Object res = generateDataParametersJSON(cTreeNode.list(), bob, logger);
            if (res instanceof LinkedList) {
              jsonMap.put(node.getName(), new JSONArray((LinkedList<?>) res));
            } else {
              jsonMap.put(node.getName(), new JSONArray().put(res));
            }
          } else {
            jsonMap.put(node.getName(), generateDataParametersJSON(cTreeNode.list(), bob, logger));
          }
        } else {
          if (node.getName() == null || node.getName().isEmpty()) {
            if (Constants.TYPE_VALUE_STRING.equals(node.getTypeValue())) {
              staticValues.add(replaceValueData(node.getValue(), bob, logger));
            } else if (Constants.TYPE_VALUE_PROPERTY.equals(node.getTypeValue())) {
              staticValues.add(DalUtil.getValueFromPath(bob, node.getProperty()).toString());
            } else if (Constants.TYPE_VALUE_DYNAMIC_NODE.equals(node.getTypeValue())) {
              // call the function
              staticValues.add(getValueExecuteMethod(node, bob, logger, dynamicNode));
            }
          } else {
            if (Constants.TYPE_VALUE_STRING.equals(node.getTypeValue())) {
              jsonMap.put(node.getName(), replaceValueData(node.getValue(), bob, logger));
            } else if (Constants.TYPE_VALUE_PROPERTY.equals(node.getTypeValue())) {
              jsonMap.put(node.getName(),
                  DalUtil.getValueFromPath(bob, node.getProperty()).toString());
            } else if (Constants.TYPE_VALUE_DYNAMIC_NODE.equals(node.getTypeValue())) {
              // call the function
              jsonMap.put(node.getName(), getValueExecuteMethod(node, bob, logger, dynamicNode));
            }
          }
        }
      }
      if (!staticValues.isEmpty()) {
        return staticValues;
      }
    } catch (Exception e) {
      String message = String.format(Utility.messageBD(conn, "smfwhe_errorGenerateJson", language),
          bob.getIdentifier());
      logger.error(message, e);
      throw new Exception(message);
    }
    return jsonMap;
  }

  /**
   * Generate a UrlPathParam, take a UrlPathParam list and return url modify you can added in the
   * url
   * 
   * @param lUrlPathParam
   *          Url Path Parameter list
   * @param url
   *          Url to send request
   * @param bob
   *          BaseOBObject
   * @param logger
   *          Logger in log
   * @return Return the url with parameters set
   * @throws Exception
   */
  public static String generateUrlParameter(List<UrlPathParam> lUrlPathParam, String url,
      BaseOBObject bob, Logger logger) throws Exception {
    String result = url;
    for (UrlPathParam param : lUrlPathParam) {
      try {
        if (param.isActive() && Constants.TYPE_PARAMETER_PATH.equals(param.getTypeParameter())) {
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
            Utility.messageBD(conn, "smfwhe_errorReplacePathParameter", language), param.getName());
        logger.error(message, e);
        throw new Exception(message);
      }
    }
    return result;
  }

  /**
   * Return an Event list from a table name with the event type.
   * 
   * @param eventTypeId
   *          ID for EventType object (see Constants class for defaults)
   * @param TableName
   * @return Return the Event list
   */
  public static List<Events> eventsFromTableName(String eventTypeId, String tableName) {
    OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
    cEvents.createAlias(Events.PROPERTY_TABLE, "table");
    cEvents.add(Restrictions.eq(Events.PROPERTY_ACTIVE, true));
    cEvents.add(Restrictions.eq(Events.PROPERTY_SMFWHEEVENTTYPE + "." + EventType.PROPERTY_ID,
        eventTypeId));
    cEvents.add(Restrictions.eq("table." + Table.PROPERTY_DBTABLENAME, tableName));
    return cEvents.list();
  }

  /**
   * Return string concat with property value set
   * 
   * @param Value
   *          String value defined in user defined data
   * @param Bob
   *          BaseOBObject to get the events defined
   * @param Logger
   *          Info logger in log
   * @return Return a string with parameters set
   * @throws Exception
   */
  public static String replaceValueData(String value, BaseOBObject bob, Logger logger)
      throws Exception {
    StringBuilder result = new StringBuilder();
    String propertyError = null;
    try {
      for (String s : value.split(" ")) {
        if (s.contains(Constants.AT)
            && DalUtil.getValueFromPath(bob, s.split(Constants.AT)[1]) == null) {
          propertyError = s;
          throw new Exception();
        } else {
          result.append(s.contains(Constants.AT)
              ? DalUtil.getValueFromPath(bob, s.split(Constants.AT)[1]) : s).append(" ");
        }
      }
    } catch (Exception e) {
      String message = String
          .format(Utility.messageBD(conn, "smfwhe_errorParserParameter", language), propertyError);
      logger.error(message, e);
      throw new Exception(message);
    }
    return result.toString().substring(0, result.length() - 1);
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
   * @param Data
   *          This is an object JsonXmlData or UrlPathParam
   * @param Bob
   *          BaseOBObject to get the events defined
   * @param Logger
   *          Info logger in log
   * @param CompareClass
   *          class name for compare with interface extend
   * @return Return the string for set in json or xml
   * @throws Exception
   */
  public static Object getValueExecuteMethod(Object data, BaseOBObject bob, Logger logger,
      Object compareClass) throws Exception {
    Object result = null;
    JsonXmlData recordData = null;
    UrlPathParam recordParam = null;
    if (dynamicNode.equals(compareClass)) {
      recordData = (JsonXmlData) data;
    } else if (computedFunction.equals(compareClass)) {
      recordParam = (UrlPathParam) data;
    }
    String classMethodName = recordParam == null ? recordData.getJavaClassName()
        : recordParam.getJavaClassName();
    String className = classMethodName;
    Class<?> clazz; // convert string classname to class
    String message = "";
    try {
      clazz = Class.forName(className);
      Object dog = clazz.newInstance(); // invoke empty constructor
      if (dog.getClass().getInterfaces()[0].equals(compareClass)) {
        String methodName = "";
        if (Constants.TYPE_VALUE_COMPUTED
            .equals(recordParam == null ? recordData.getTypeValue() : recordParam.getTypeValue())) {
          methodName = Constants.METHOD_NAME;
        } else if (Constants.TYPE_VALUE_DYNAMIC_NODE
            .equals(recordParam == null ? recordData.getTypeValue() : recordParam.getTypeValue())) {
          methodName = Constants.METHOD_NAME_DYNAMIC_NODE;
        }
        Method setNameMethod = dog.getClass().getMethod(methodName, HashMap.class);
        // set the parameters in hashmap
        HashMap<Object, Object> params = recordParam == null
            ? getArgumentsForMethodData(recordData.getSmfwheArgsDataList(), bob, logger)
            : getArgumentsForMethod(recordParam.getSmfwheArgsList(), bob, logger);
        result = setNameMethod.invoke(dog, params); // pass arg
      } else {
        message = String.format(
            Utility.messageBD(conn, "smfwhe_errorParserClassMethodName", language),
            classMethodName);
        throw new Exception(message);
      }
    } catch (Exception e) {
      logger.error(message, e);
      throw e;
    }
    return result;
  }

  /**
   * 
   * @param Con
   *          HttpURLConnection
   * @param lUrlPathParam
   *          List the UrlPathParam
   * @param Logger
   *          Info logger in log
   * @param Bob
   *          BaseOBObject to get the events defined
   * @throws Exception
   */
  public static void setHeaderConnection(HttpURLConnection con, List<UrlPathParam> lUrlPathParam,
      Logger logger, BaseOBObject bob) throws Exception {
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
   * @param Param
   *          List the arguments for method
   * @param Bob
   *          BaseOBObject to get the events defined
   * @param Logger
   *          Info logger in log
   * @return Return the hashmap with parameters for execute method
   * @throws Exception
   */
  public static HashMap<Object, Object> getArgumentsForMethod(List<Arguments> args,
      BaseOBObject bob, Logger logger) throws Exception {
    HashMap<Object, Object> result = new HashMap<Object, Object>();
    for (Arguments arg : args) {
      if (arg.isActive()) {
        result.put(arg.getName(), replaceValueData(arg.getValueParameter(), bob, logger));
      }
    }
    return result;
  }

  /**
   * Get the arguments for method
   * 
   * @param Param
   *          List the arguments for method
   * @param Bob
   *          BaseOBObject to get the events defined
   * @param Logger
   *          Info logger in log
   * @return Return the hashmap with parameters for execute method
   * @throws Exception
   */
  public static HashMap<Object, Object> getArgumentsForMethodData(List<ArgumentsData> args,
      BaseOBObject bob, Logger logger) throws Exception {
    HashMap<Object, Object> result = new HashMap<Object, Object>();
    for (ArgumentsData arg : args) {
      if (arg.isActive()) {
        result.put(arg.getName(), replaceValueData(arg.getValue(), bob, logger));
      }
    }
    return result;
  }

}
