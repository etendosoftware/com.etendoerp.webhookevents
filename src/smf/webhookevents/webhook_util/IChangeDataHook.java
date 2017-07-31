package smf.webhookevents.webhook_util;

import java.io.StringWriter;

import org.codehaus.jettison.json.JSONObject;

public interface IChangeDataHook {

  public void postProcessJSON(JSONObject jsonObject);

  public void postProcessXML(StringWriter xmlObject);

}
