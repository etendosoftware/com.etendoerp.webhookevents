package com.smf.webhookevents.webhook_util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.enterprise.inject.Any;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
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
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.smf.webhookevents.annotation.InjectHook;
import com.smf.webhookevents.data.Events;
import com.smf.webhookevents.data.StandardParameter;
import com.smf.webhookevents.data.UrlPathParam;
import com.smf.webhookevents.data.Webhook;
import com.smf.webhookevents.interfaces.IChangeDataHook;

public class WebHookUtil {
  final private static String language = OBContext.getOBContext().getLanguage().getLanguage();
  final private static ConnectionProvider conn = new DalConnectionProvider(false);

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
    URL obj = new URL(URLEncoder.encode(url, "UTF-8"));
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
    String postJsonData = "";
    List<StandardParameter> lStdParameters = hook.getSmfwheStdparamList();
    WebHookInitializer.initialize();
    if (hook.getTypedata().equals(Constants.STRING_JSON)) {
      postJsonData = generateDataParametersJSON(lStdParameters, bob, logger);
    } else if (hook.getTypedata().equals(Constants.STRING_XML)) {
      postJsonData = generateDataParametersXML(bob.getEntityName(), lStdParameters, bob, logger);
    }

    // Send post request
    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.writeBytes(postJsonData);
    wr.flush();
    wr.close();

    int responseCode = con.getResponseCode();
    logger.debug("nSending " + hook.getSmfwheEvents().getMethod() + "request to URL : " + url);
    logger.debug("Post Data : " + postJsonData);
    logger.debug("Response Code : " + responseCode);

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
   *          Standard Parameter list
   * @param Bob
   *          BaseOBObject to generate data in XML
   * @param Logger
   *          Info logger in log
   * @return Return data in format JSON
   * @throws Exception
   */
  public static String generateDataParametersJSON(List<StandardParameter> listParameters,
      BaseOBObject bob, Logger logger) throws Exception {
    String json = "";
    JSONObject jsonMap = new JSONObject();
    try {
      for (StandardParameter stdParam : listParameters) {
        if (stdParam.isActive()) {
          jsonMap.put(stdParam.getName(), DalUtil.getValueFromPath(bob, stdParam.getProperty()));
        }
      }
      if (hooks != null) {
        for (IChangeDataHook hook : hooks) {
          jsonMap = hook.postProcessJSON(jsonMap);
        }
      }
      json = jsonMap.toString();
    } catch (Exception e) {
      String message = String.format(Utility.messageBD(conn, "smfwhe_errorGenerateJson", language),
          bob.getIdentifier());
      logger.error(message, e);
      throw new Exception(message);
    }
    return json;
  }

  /**
   * Generate a XML data parameter, take a StandardParameter list and return XML with
   * StandardParameter set
   *
   * @param Name
   *          Name to root node
   * @param ListParameters
   *          Standard Parameter list
   * @param Bob
   *          BaseOBObject to generate data in XML
   * @param Logger
   *          Info logger in log
   * @return Return data in format XML
   * @throws Exception
   */
  public static String generateDataParametersXML(String name,
      List<StandardParameter> listParameters, BaseOBObject bob, Logger logger) throws Exception {
    StringWriter xml = new StringWriter();
    try {
      if (listParameters.isEmpty()) {
        logger.info("empty Standard Parameter List");
      } else {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation implementation = builder.getDOMImplementation();
        Document document = implementation.createDocument(null, name, null);
        document.setXmlVersion(Constants.XML_VERSION);

        // Main Node
        Element root = document.getDocumentElement();
        for (StandardParameter param : listParameters) {
          // Item Node
          Element node = document.createElement(param.getName());
          Text nodeValueValue = document.createTextNode(DalUtil.getValueFromPath(bob,
              param.getProperty()).toString());
          node.appendChild(nodeValueValue);
          // append itemNode to root
          root.appendChild(node); // add the element in root node "Document"
        }
        // Hook that allows you to modify or change the xml
        if (hooks != null) {
          for (IChangeDataHook hook : hooks) {
            document = hook.postProcessXML(document);
          }
        }
        // Generate XML
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(xml));
      }
    } catch (Exception e) {
      String message = String.format(Utility.messageBD(conn, "smfwhe_errorGenerateXml", language),
          bob.getIdentifier());
      logger.error(message, e);
      throw new Exception(message);
    }

    return xml.toString();
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
                replaceValueData(param.getValueParameter(), bob, logger));
          } else if (Constants.TYPE_VALUE_PROPERTY.equals(param.getTypeValue())) {
            result = result.replace("{" + param.getName() + "}",
                DalUtil.getValueFromPath(bob, param.getProperty()).toString());
          } else if (Constants.TYPE_VALUE_COMPUTED.equals(param.getTypeValue())) {
            // call the function
            result = result.replace("{" + param.getName() + "}",
                getValueExecuteMethod(param.getFunctionCode(), logger));
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
   * Return a Events list from BaseOBObject send for parameter
   * 
   * @param Action
   *          Defined in this class
   * @param TableName
   * @return Return the Events list
   */
  public static List<Events> eventsFromBaseOBObject(String action, String tableName) {
    OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
    cEvents.createAlias(Events.PROPERTY_TABLE, "table");
    cEvents.add(Restrictions.eq(Events.PROPERTY_ACTIVE, true));
    if (Constants.CREATE.equals(action) || Constants.UPDATE.equals(action)) {
      cEvents.add(Restrictions.or(Restrictions.eq(Events.PROPERTY_EXECUTEON, action),
          Restrictions.eq(Events.PROPERTY_EXECUTEON, Constants.CREATE_OR_UPDATE)));
    } else {
      cEvents.add(Restrictions.eq(Events.PROPERTY_EXECUTEON, action));
    }
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
          result.append(
              s.contains(Constants.AT) ? DalUtil.getValueFromPath(bob, s.split(Constants.AT)[1])
                  : s).append(" ");
        }
      }
    } catch (Exception e) {
      String message = String.format(
          Utility.messageBD(conn, "smfwhe_errorParserParameter", language), propertyError);
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
        entities[i] = ModelProvider.getInstance().getEntityByTableName(
            e.getTable().getDBTableName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return entities;
  }

  public static String getValueExecuteMethod(String classMethodName, Logger logger)
      throws Exception {
    String result = "";
    String className = getClassName(classMethodName);
    Class<?> clazz; // convert string classname to class
    String message = "";
    try {
      clazz = Class.forName(className);
      Object dog = clazz.newInstance(); // invoke empty constructor
      if (dog.getClass().getInterfaces()[0]
          .equals(com.smf.webhookevents.interfaces.ComputedFunction.class)) {
        String methodName = Constants.METHOD_NAME;
        Method setNameMethod = dog.getClass().getMethod(methodName, HashMap.class);
        // set the parameters in hashmap
        HashMap<Object, Object> params = null;
        result = (String) setNameMethod.invoke(dog, params); // pass arg
      } else {
        message = String
            .format(Utility.messageBD(conn, "smfwhe_errorParserClassMethodName", language),
                classMethodName);
        throw new Exception(message);
      }
    } catch (Exception e) {
      logger.error(message, e);
      throw e;
    }
    return result;
  }

  public static String getClassName(String path) {
    StringBuilder resul = new StringBuilder();
    String[] splitString = path.split(Pattern.quote("."));
    for (int i = 0; i < splitString.length; i++) {
      resul.append(splitString[i] + ".");
    }
    return resul.toString().substring(0, resul.toString().length() - 1);
  }

  public static void setHeaderConnection(HttpURLConnection con, List<UrlPathParam> lUrlPathParam,
      Logger logger, BaseOBObject bob) throws Exception {
    String result = "";
    for (UrlPathParam param : lUrlPathParam) {
      if (Constants.TYPE_VALUE_STRING.equals(param.getTypeValue())) {
        result = replaceValueData(param.getValueParameter(), bob, logger);
      } else if (Constants.TYPE_VALUE_PROPERTY.equals(param.getTypeValue())) {
        result = DalUtil.getValueFromPath(bob, param.getProperty()).toString();
      } else if (Constants.TYPE_VALUE_COMPUTED.equals(param.getTypeValue())) {
        // call the function
        result = getValueExecuteMethod(param.getFunctionCode(), logger);
      }
      con.setRequestProperty(param.getName(), result);
    }
  }

}
