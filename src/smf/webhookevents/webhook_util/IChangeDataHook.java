package smf.webhookevents.webhook_util;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

public interface IChangeDataHook {

  public void postProcessJSON(JSONObject jsonObject);

  public void postProcessXML(Document xmlObject);

}
