package com.smf.webhookevents.interfaces;


public interface IChangeDataHook {

  public Object postProcessJSON(Object obj) throws Exception;

  public Object postProcessXML(Object xmlObject) throws Exception;

}
