package com.smf.webhookevents.interfaces;

import org.w3c.dom.Document;

public interface IChangeDataHook {

  public Object postProcessJSON(Object obj) throws Exception;

  public Document postProcessXML(Document xmlObject) throws Exception;

}
