package smf.webhookevents.webhook_util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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

import com.smf.webhookevents.data.Customparam;
import com.smf.webhookevents.data.Events;
import com.smf.webhookevents.data.StandardParameter;
import com.smf.webhookevents.data.UserDefinedParameter;
import com.smf.webhookevents.data.Webhook;

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

    String url = hook.getUrlnotify() + generateCustomParameter(hook.getSmfwheCustomparamList());
    URL obj = new URL(url);
    HttpURLConnection con = (HttpsURLConnection) obj.openConnection();

    // Setting basic post request
    con.setRequestMethod(hook.getSmfwheEvents().getMethod());
    con.setRequestProperty("User-Agent", Constants.USER_AGENT);
    con.setRequestProperty("Accept-Language", Constants.ACCEPT_LANGUAGE);
    con.setRequestProperty("Content-Type", Constants.CONTENT_TYPE);

    // Verify if can data is json or xml
    String postJsonData = "";
    List<StandardParameter> lStdParameters = hook.getSmfwheStdparamList();
    List<UserDefinedParameter> lUDefinedParameters = hook.getSmfwheUdefinedparamList();
    WebHookInitializer.initialize();
    if (hook.getTypedata().equals(Constants.STRING_JSON)) {
      if (isOneRowActive(lStdParameters)) {
        postJsonData = generateDataParametersJSON(lStdParameters, bob, logger);
      } else {
        postJsonData = generateUserDefinedDataParametersJSON(lUDefinedParameters, bob, logger);
      }
    } else if (hook.getTypedata().equals(Constants.STRING_XML)) {
      if (isOneRowActive(lStdParameters)) {
        postJsonData = generateDataParametersXML(bob.getEntityName(), lStdParameters, bob, logger);
      } else {
        postJsonData = generateUserDefinedDataParametersXML(bob.getEntityName(),
            lUDefinedParameters, bob, logger);
      }
    }

    // Send post request
    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.writeBytes(postJsonData);
    wr.flush();
    wr.close();

    int responseCode = con.getResponseCode();
    logger.info("nSending " + hook.getSmfwheEvents().getMethod() + "request to URL : " + url);
    logger.info("Post Data : " + postJsonData);
    logger.info("Response Code : " + responseCode);

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String output;
    StringBuffer response = new StringBuffer();

    while ((output = in.readLine()) != null) {
      response.append(output);
    }
    in.close();

    // printing result from response
    logger.info(response.toString());
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
          hook.postProcessJSON(jsonMap);
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
  public static String generateUserDefinedDataParametersJSON(
      List<UserDefinedParameter> listParameters, BaseOBObject bob, Logger logger) throws Exception {
    String json = "";
    JSONObject jsonMap = new JSONObject();
    try {
      for (UserDefinedParameter param : listParameters) {
        if (param.isActive()) {
          jsonMap.put(param.getName(),
              WebHookUtil.replaceValueData(param.getValueParameter(), bob, logger));
        }
      }
      if (hooks != null) {
        for (IChangeDataHook hook : hooks) {
          hook.postProcessJSON(jsonMap);
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
            hook.postProcessXML(document);
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
  public static String generateUserDefinedDataParametersXML(String name,
      List<UserDefinedParameter> listParameters, BaseOBObject bob, Logger logger) throws Exception {
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
        for (UserDefinedParameter param : listParameters) {
          // Item Node
          Element node = document.createElement(param.getName());
          Text nodeValueValue = document.createTextNode(WebHookUtil.replaceValueData(
              param.getValueParameter(), bob, logger));
          node.appendChild(nodeValueValue);
          // append itemNode to root
          root.appendChild(node); // add the element in root node "Document"
        }
        // Hook that allows you to modify or change the xml
        if (hooks != null) {
          for (IChangeDataHook hook : hooks) {
            hook.postProcessXML(document);
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
   * Generate a Customparam, take a Customparam list and return string you can added in the url
   * 
   * @param LCustomParameters
   *          Custom Parameter list
   * @return Return string appended with all the custom parameter
   */
  public static String generateCustomParameter(List<Customparam> lCustomParameters) {
    StringBuilder parameters = new StringBuilder(Constants.START_PARAMETER);
    for (Customparam param : lCustomParameters) {
      if (param.isActive()) {
        parameters.append(param.getName() + Constants.EQUALS + param.getValue()
            + Constants.AMPERSAND);
      }
    }
    return parameters.toString().substring(0, parameters.length() - 1);
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
    String[] sValue = value.split(" ");
    StringBuilder result = new StringBuilder();
    try {
      for (String s : sValue) {
        result.append(
            s.contains(Constants.AT) ? DalUtil.getValueFromPath(bob, s.split(Constants.AT)[1]) : s)
            .append(" ");
      }
    } catch (Exception e) {
      String message = String.format(
          Utility.messageBD(conn, "smfwhe_errorParserParameter", language), value);
      logger.error(message, e);
      throw new Exception(message);
    }
    return result.toString().substring(0, result.length() - 1);
  }

  /**
   * Return true if at least row is active other case false
   * 
   * @param ListParameters
   *          Standard Parameter list
   * @return Return true if at least row is active other case false
   * @throws Exception
   */
  public static boolean isOneRowActive(List<StandardParameter> listParameters) {
    boolean isActive = false;
    for (StandardParameter p : listParameters) {
      if (p.isActive()) {
        isActive = true;
        break;
      }
    }
    return isActive;
  }
}
