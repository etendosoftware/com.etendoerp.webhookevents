package com.smf.webhookevents.webhook_util;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

public interface IChangeDataHook {

  public JSONObject postProcessJSON(JSONObject jsonObject);

  public Document postProcessXML(Document xmlObject);

}
