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

package com.etendoerp.webhookevents.ad_process;

import com.etendoerp.webhookevents.data.DefinedwebhookToken;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * This action handle when a user needs to view the generated API Key
 */
public class GetApikeyProcess extends Action  {
  private static final Logger log = LogManager.getLogger();

  @Override protected Data preRun(JSONObject jsonContent) {
    jsonContent.remove("_entityName");
    Data tmp = null;
    try {
      tmp = new Data(jsonContent, getInputClass());
    } catch (JSONException e) {
      log.error(e.getMessage());
    }
    return tmp;
  }

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    var result = new ActionResult();
    try {
      var input = getInputContents(getInputClass());
      var jsonMessage = new JSONObject();
      StringBuilder message = new StringBuilder();
      for (DefinedwebhookToken token : input) {
        message.append(token.getAPIKey()).append("<br>");
      }
      jsonMessage.put("message", buildMessage(message.toString()));
      result.setResponseActionsBuilder(
          getResponseBuilder().addCustomResponseAction("smartclientSay", jsonMessage));
      result.setOutput(getInput());
      result.setType(Result.Type.SUCCESS);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      result.setType(Result.Type.ERROR);
      result.setMessage(e.getMessage());
    }
    return result;
  }

  private String buildMessage(String content) {
    String prefix = "<span style=\"width:300px; word-wrap:break-word; display:inline-block;\"> \n";
    String suffix = "</span>\n";

    return prefix + content + suffix;
  }

  @Override
  protected Class<DefinedwebhookToken> getInputClass() {
    return DefinedwebhookToken.class;
  }

}
