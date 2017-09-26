package com.smf.webhookevents.componentProviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(WebHookComponentProvider.WEBHOOK_COMPONENT_TYPE)
public class WebHookComponentProvider extends BaseComponentProvider {

  public static final String WEBHOOK_COMPONENT_TYPE = "SMFWHE_WebHookComponentType";

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources
        .add(createStaticResource("web/com.smf.webhookevents/js/queueEventFunction.js", false));

    return globalResources;
  }

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<String> getTestResources() {
    final List<String> testResources = new ArrayList<String>();
    return testResources;
  }

}
