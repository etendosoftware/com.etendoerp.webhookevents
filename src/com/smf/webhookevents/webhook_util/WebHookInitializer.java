package com.smf.webhookevents.webhook_util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;

import org.openbravo.base.weld.WeldUtils;

public class WebHookInitializer {
  @SuppressWarnings("serial")
  private static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
  };
  private static WebHookInitializer instance = null;

  private WebHookInitializer() {
    WebHookUtil webHookUtil = new WebHookUtil();
    Class<?> main = WebHookUtil.class;
    for (Field field : main.getDeclaredFields()) {
      if (field.isAnnotationPresent(InjectHook.class)) {
        field.setAccessible(true);
        ParameterizedType listHooksType = (ParameterizedType) field.getGenericType();
        Class<?> currentHook = (Class<?>) listHooksType.getActualTypeArguments()[0];
        final Set<Bean<?>> beans = WeldUtils.getStaticInstanceBeanManager().getBeans(currentHook,
            ANY);
        List<Object> addToField = new ArrayList<Object>();
        for (Bean<?> bean : beans) {
          try {
            Object reference = WeldUtils.getStaticInstanceBeanManager()
                .getReference(bean, currentHook,
                    WeldUtils.getStaticInstanceBeanManager().createCreationalContext(null));
            addToField.add(reference);
          } catch (IllegalArgumentException e) {
            e.printStackTrace();
          } catch (SecurityException e) {
            e.printStackTrace();
          }
        }
        try {
          field.set(webHookUtil, addToField);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static WebHookInitializer initialize() {
    if (instance == null) {
      instance = new WebHookInitializer();
    }
    return instance;
  }
}
