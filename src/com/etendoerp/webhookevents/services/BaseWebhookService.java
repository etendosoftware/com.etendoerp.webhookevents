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

package com.etendoerp.webhookevents.services;

import java.util.Map;

/**
 * Each developed webhook must extends this abstract class to be a valid webhook class
 */
public abstract class BaseWebhookService {
  public abstract void get(Map<String, String> parameter, Map<String, String> responseVars);
}
