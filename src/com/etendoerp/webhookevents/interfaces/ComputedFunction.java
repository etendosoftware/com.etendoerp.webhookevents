package com.etendoerp.webhookevents.interfaces;

import java.util.HashMap;

public interface ComputedFunction {

  public String execute(HashMap<Object, Object> params) throws Exception;

}
