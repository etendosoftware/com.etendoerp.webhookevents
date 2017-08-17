package com.smf.webhookevents.interfaces;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

public interface IChangeDataHook {

  public JSONObject postProcessJSON(JSONObject jsonObject) throws Exception;

  public Document postProcessXML(Document xmlObject) throws Exception;

}
