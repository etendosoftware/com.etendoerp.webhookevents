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

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;

import com.etendoerp.webhookevents.annotation.InjectHook;

public class WebHookInitializer {

  private static final Logger log = Logger.getLogger(WebHookInitializer.class);
  private static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
  };
  private static WebHookInitializer instance = null;

  private WebHookInitializer() {
    Class<?> main = WebHookUtil.class;
    for (Field field : main.getDeclaredFields()) {
      if (field.isAnnotationPresent(InjectHook.class)) {
        ParameterizedType listHooksType = (ParameterizedType) field.getGenericType();
        Class<?> currentHook = (Class<?>) listHooksType.getActualTypeArguments()[0];
        final Set<Bean<?>> beans = WeldUtils.getStaticInstanceBeanManager().getBeans(currentHook,
            ANY);
        List<Object> addToField = new ArrayList<>();
        for (Bean<?> bean : beans) {
          try {
            Object reference = WeldUtils.getStaticInstanceBeanManager()
                .getReference(bean, currentHook,
                    WeldUtils.getStaticInstanceBeanManager().createCreationalContext(null));
            addToField.add(reference);
          } catch (IllegalArgumentException | SecurityException e) {
            log.error(e.getMessage(), e);
            throw new OBException(e);
          }
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
