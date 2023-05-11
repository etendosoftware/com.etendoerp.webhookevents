/*
 * Copyright (c) 2022 Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.etendoerp.webhookevents.webhook_util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;

import org.openbravo.base.weld.WeldUtils;

import com.etendoerp.webhookevents.annotation.InjectHook;

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
