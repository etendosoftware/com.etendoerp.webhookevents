package com.smf.webhookevents.interfaces;

import java.util.HashMap;

public interface DynamicNode {

  public Object dynamicNode(HashMap<Object, Object> params) throws Exception;

}