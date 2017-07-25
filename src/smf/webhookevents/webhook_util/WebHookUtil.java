package smf.webhookevents.webhook_util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.webhookevents.data.Customparam;
import com.smf.webhookevents.data.Events;
import com.smf.webhookevents.data.StandardParameter;
import com.smf.webhookevents.data.Webhook;

public class WebHookUtil {
  final private static String language = OBContext.getOBContext().getLanguage().getLanguage();
  final private static ConnectionProvider conn = new DalConnectionProvider(false);

  public final static String USER_AGENT = "Mozilla/5.0";
  public final static String ACCEPT_LANGUAGE = "en-US,en;q=0.5";
  public final static String CONTENT_TYPE = "application/json";
  public final static String START_PARAMETER = "?";
  public final static String AMPERSAND = "&";
  public final static String EQUALS = "=";
  public final static String STRING_JSON = "JSON";
  public final static String STRING_XML = "XML";

  /* Call the all webhook defined in this event */
  public static void callWebHook(Events event, BaseOBObject bob) throws Exception {
    OBCriteria<Webhook> cWebhook = OBDal.getInstance().createCriteria(Webhook.class);
    cWebhook.add(Restrictions.eq(Webhook.PROPERTY_SMFWHEEVENTS, event));

    for (Webhook hook : cWebhook.list()) {
      try {
        if (hook.isActive()) {
          sendEvent(hook, bob);
        }
      } catch (Exception e) {
        throw new Exception(e);
      }
    }
  }

  /* Send the request to url to notify defined in webhook */
  public static void sendEvent(Webhook hook, BaseOBObject bob) throws Exception {

    String url = hook.getUrlnotify() + generateCustomParameter(hook.getSmfwheCustomparamList());
    URL obj = new URL(url);
    HttpURLConnection con = (HttpsURLConnection) obj.openConnection();

    // Setting basic post request
    con.setRequestMethod(hook.getSmfwheEvents().getMethod());
    con.setRequestProperty("User-Agent", USER_AGENT);
    con.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
    con.setRequestProperty("Content-Type", CONTENT_TYPE);

    // Verify if can data is json or xml
    String postJsonData = "";
    if (hook.getTypedata().equals(STRING_JSON)) {
      postJsonData = generateDataParametersJSON(hook.getSmfwheStdparamList(), bob);
    } else if (hook.getTypedata().equals(STRING_XML)) {
      postJsonData = generateDataParametersXML(hook.getSmfwheStdparamList(), bob);
    }

    // Send post request
    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.writeBytes(postJsonData);
    wr.flush();
    wr.close();

    int responseCode = con.getResponseCode();
    System.out.println("nSending 'POST' request to URL : " + url);
    System.out.println("Post Data : " + postJsonData);
    System.out.println("Response Code : " + responseCode);

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String output;
    StringBuffer response = new StringBuffer();

    while ((output = in.readLine()) != null) {
      response.append(output);
    }
    in.close();

    // printing result from response
    System.out.println(response.toString());

  }

  /*
   * Generate a JSON data parameter, take a StandardParameter list and return json with
   * StandardParameter set
   */
  public static String generateDataParametersJSON(List<StandardParameter> listParam,
      BaseOBObject bob) throws Exception {
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
      throw new Exception(message);
    }
    return json;
  }

  /*
   * Generate a XML data parameter, take a StandardParameter list and return XML with
   * StandardParameter set
   */
  public static String generateDataParametersXML(List<StandardParameter> listParam, BaseOBObject bob)
      throws Exception {
    String json = "";
    return json;
  }

  /*
   * Generate a Customparam, take a Customparam list and return string you can added in the url
   */
  public static String generateCustomParameter(List<Customparam> lCustomParam) {
    String parameters = START_PARAMETER;
    for (Customparam param : lCustomParam) {
      if (param.isActive()) {
        parameters += param.getName() + EQUALS + param.getValue() + AMPERSAND;
      }
    }
    return parameters.substring(0, parameters.length() - 1);
  }
}
