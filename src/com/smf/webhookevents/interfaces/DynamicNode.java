package com.smf.webhookevents.interfaces;

import java.util.HashMap;

import org.codehaus.jettison.json.JSONObject;

public interface DynamicNode {

  public String dynamicNode(HashMap<Object, Object> params) throws Exception;

  public JSONObject[] dynamicNodeArray(HashMap<Object, Object> params) throws Exception;

}
