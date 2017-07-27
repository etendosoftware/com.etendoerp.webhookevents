package smf.webhookevents.webhook_util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.webhookevents.data.Customparam;
import com.smf.webhookevents.data.Events;
import com.smf.webhookevents.data.StandardParameter;
import com.smf.webhookevents.data.UserDefinedParameter;
import com.smf.webhookevents.data.Webhook;

public class WebHookUtil {
  final private static String language = OBContext.getOBContext().getLanguage().getLanguage();
  final private static ConnectionProvider conn = new DalConnectionProvider(false);

  /**
   * Call the all webhook defined in this event
   * 
   * @param event
   *          Events
   * @param bob
   *          BaseOBObject to generate data (JSON or XML)
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
   * @param hook
   *          Webhook defined in events
   * @param bob
   *          BaseOBObject to generate data (JSON or XML)
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

    if (hook.getTypedata().equals(Constants.STRING_JSON)) {
      if (isOneRowActive(lStdParameters)) {
        postJsonData = generateDataParametersJSON(lStdParameters, bob, logger);
      } else {
        postJsonData = generateUserDefinedDataParametersJSON(lUDefinedParameters, bob, logger);
      }
    } else if (hook.getTypedata().equals(Constants.STRING_XML)) {
      if (isOneRowActive(lStdParameters)) {
        postJsonData = generateDataParametersXML(lStdParameters, bob, logger);
      } else {
        postJsonData = generateUserDefinedDataParametersXML(lUDefinedParameters, bob, logger);
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
   * @param listParam
   *          Standard Parameter list
   * @param bob
   *          BaseOBObject to generate data in XML
   * @return return data in format JSON
   * @throws Exception
   */
  public static String generateDataParametersJSON(List<StandardParameter> listParam,
      BaseOBObject bob, Logger logger) throws Exception {
    String json = "";
    JSONObject jsonMap = new JSONObject();
    try {
      for (StandardParameter stdParam : listParam) {
        if (stdParam.isActive()) {
          jsonMap.put(stdParam.getName(), DalUtil.getValueFromPath(bob, stdParam.getProperty()));
        }
      }
      json = jsonMap.toString();
    } catch (JSONException e) {
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
   * @param listParam
   *          Standard Parameter list
   * @param bob
   *          BaseOBObject to generate data in XML
   * @return return data in format JSON
   * @throws Exception
   */
  public static String generateUserDefinedDataParametersJSON(List<UserDefinedParameter> listParam,
      BaseOBObject bob, Logger logger) throws Exception {
    String json = "";
    JSONObject jsonMap = new JSONObject();
    try {
      for (UserDefinedParameter param : listParam) {
        if (param.isActive()) {
          jsonMap.put(param.getName(), replaceValueData(param.getValueParameter(), bob, logger));
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
   * @param listParam
   *          Standard Parameter list
   * @param bob
   *          BaseOBObject to generate data in XML
   * @return return data in format XML
   * @throws Exception
   */
  public static String generateDataParametersXML(List<StandardParameter> listParam,
      BaseOBObject bob, Logger logger) throws Exception {
    String json = "";
    return json;
  }

  /**
   * Generate a XML data parameter, take a StandardParameter list and return XML with
   * StandardParameter set
   * 
   * @param listParam
   *          Standard Parameter list
   * @param bob
   *          BaseOBObject to generate data in XML
   * @return return data in format XML
   * @throws Exception
   */
  public static String generateUserDefinedDataParametersXML(List<UserDefinedParameter> listParam,
      BaseOBObject bob, Logger logger) throws Exception {
    String json = "";
    return json;
  }

  /**
   * Generate a Customparam, take a Customparam list and return string you can added in the url
   * 
   * @param lCustomParam
   *          Custom Parameter list
   * @return return string appended with all the custom parameter
   */
  public static String generateCustomParameter(List<Customparam> lCustomParam) {
    String parameters = Constants.START_PARAMETER;
    for (Customparam param : lCustomParam) {
      if (param.isActive()) {
        parameters += param.getName() + Constants.EQUALS + param.getValue() + Constants.AMPERSAND;
      }
    }
    return parameters.substring(0, parameters.length() - 1);
  }

  /**
   * Return a Events list from BaseOBObject send for parameter
   * 
   * @param bob
   *          BaseOBObject to get the events defined
   * @param action
   *          defined in this class
   * @param tableNames
   * @return return de Events list
   */
  public static List<Events> eventsFromBaseOBObject(BaseOBObject bob, String action,
      List<String> tableNames) {
    OBCriteria<Events> cEvents = OBDal.getInstance().createCriteria(Events.class);
    cEvents.createAlias(Events.PROPERTY_TABLE, "table");
    cEvents.add(Restrictions.eq(Events.PROPERTY_ACTIVE, true));
    cEvents.add(Restrictions.eq(Events.PROPERTY_EXECUTEON, action));
    cEvents.add(Restrictions.in("table." + Table.PROPERTY_DBTABLENAME, tableNames));

    return cEvents.list();
  }

  /**
   * Return a Table Name list
   * 
   * @param entities
   *          array
   * @return return a Table Name list
   */
  public static List<String> getTableName(Entity[] entities) {
    List<String> lTableName = new LinkedList<String>();
    for (Entity e : entities) {
      lTableName.add(e.getTableName());
    }
    return lTableName;
  }

  /**
   * Return string concat with property value set
   * 
   * @param value
   *          string value defined in user defined data
   * @return return a string with parameters set
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
   * @param listParam
   *          Standard Parameter list
   * @return return true if at least row is active other case false
   * @throws Exception
   */
  public static boolean isOneRowActive(List<StandardParameter> listParam) {
    boolean isActive = false;
    for (StandardParameter p : listParam) {
      if (p.isActive()) {
        isActive = true;
        break;
      }
    }
    return isActive;
  }
}
