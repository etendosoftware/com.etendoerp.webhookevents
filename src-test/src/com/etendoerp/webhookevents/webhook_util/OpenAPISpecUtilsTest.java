package com.etendoerp.webhookevents.webhook_util;

import static com.etendoerp.webhookevents.WebhookTestConstants.AMOUNT;
import static com.etendoerp.webhookevents.WebhookTestConstants.API_V1_PREFIX;
import static com.etendoerp.webhookevents.WebhookTestConstants.API_V2_PREFIX;
import static com.etendoerp.webhookevents.WebhookTestConstants.APPLICATION_JSON;
import static com.etendoerp.webhookevents.WebhookTestConstants.CONTENT;
import static com.etendoerp.webhookevents.WebhookTestConstants.DEFAULT_API_PREFIX;
import static com.etendoerp.webhookevents.WebhookTestConstants.DESCRIPTION;
import static com.etendoerp.webhookevents.WebhookTestConstants.LEGACY_V1_PREFIX;
import static com.etendoerp.webhookevents.WebhookTestConstants.PARAMS;
import static com.etendoerp.webhookevents.WebhookTestConstants.REQUEST_BODY;
import static com.etendoerp.webhookevents.WebhookTestConstants.REQUIRED;
import static com.etendoerp.webhookevents.WebhookTestConstants.SCHEMA;
import static com.etendoerp.webhookevents.WebhookTestConstants.STRING;
import static com.etendoerp.webhookevents.WebhookTestConstants.SUMMARY;
import static com.etendoerp.webhookevents.WebhookTestConstants.USER_ID;
import static com.etendoerp.webhookevents.WebhookTestConstants.WEBHOOK;
import static com.etendoerp.webhookevents.WebhookTestConstants.WEBHOOK_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Tests for the OpenAPISpecUtils class, specifically for the createPaths method and related functionality.
 */
class OpenAPISpecUtilsTest {

  /**
   * Tests the createPaths method with an empty webhook array.
   * It checks if the resulting JSONObject is empty as expected.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsWithEmptyWebhookArray() throws Exception {
    String prefixParentPath = DEFAULT_API_PREFIX;
    JSONArray emptyWebhooksArray = new JSONArray();

    JSONObject result = invokeCreatePaths(prefixParentPath, emptyWebhooksArray);

    assertNotNull(result);
    assertEquals(0, result.length());
  }

  /**
   * Tests the createPaths method with a single webhook that has no parameters.
   * It checks if the path is created correctly and if the request body schema is as expected.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsWithSingleWebhookNoParameters() throws Exception {
    String prefixParentPath = DEFAULT_API_PREFIX;
    JSONArray webhooksArray = new JSONArray();
    JSONObject webhook = new JSONObject()
        .put("name", "testWebhook")
        .put(DESCRIPTION, "Test webhook description")
        .put(PARAMS, new JSONArray());
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    assertNotNull(result);
    assertEquals(1, result.length());
    assertTrue(result.has("/api/testWebhook"));

    JSONObject webhookPath = result.getJSONObject("/api/testWebhook");
    assertTrue(webhookPath.has("post"));

    JSONObject postMethod = webhookPath.getJSONObject("post");
    assertEquals("Test webhook description", postMethod.getString(SUMMARY));
    assertTrue(postMethod.has(REQUEST_BODY));
    assertTrue(postMethod.has("responses"));

    JSONObject requestBody = postMethod.getJSONObject(REQUEST_BODY);
    assertTrue(requestBody.getBoolean(REQUIRED));

    JSONObject schema = requestBody.getJSONObject(CONTENT)
        .getJSONObject(APPLICATION_JSON)
        .getJSONObject(SCHEMA);
    assertEquals("object", schema.getString("type"));
    assertFalse(schema.has(REQUIRED));
  }

  /**
   * Tests the createPaths method with a single webhook that has parameters.
   * It checks if the path is created correctly and if the request body schema is as expected.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsWithWebhookRequiredParameters() throws Exception {
    String prefixParentPath = "";
    JSONArray webhooksArray = new JSONArray();
    JSONArray params = new JSONArray();

    JSONObject param1 = new JSONObject()
        .put("name", USER_ID)
        .put("type", STRING)
        .put(DESCRIPTION, "User ID parameter")
        .put(REQUIRED, true);

    JSONObject param2 = new JSONObject()
        .put("name", AMOUNT)
        .put("type", "number")
        .put(DESCRIPTION, "Amount parameter")
        .put(REQUIRED, false);

    params.put(param1);
    params.put(param2);

    JSONObject webhook = new JSONObject()
        .put("name", "payment")
        .put(PARAMS, params);
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    assertNotNull(result);
    assertTrue(result.has("/payment"));

    JSONObject webhookPath = result.getJSONObject("/payment");
    JSONObject postMethod = webhookPath.getJSONObject("post");
    assertEquals("Executes the payment WebHook", postMethod.getString(SUMMARY));

    JSONObject schema = postMethod.getJSONObject(REQUEST_BODY)
        .getJSONObject(CONTENT)
        .getJSONObject(APPLICATION_JSON)
        .getJSONObject(SCHEMA);

    assertTrue(schema.has(REQUIRED));
    JSONArray requiredFields = schema.getJSONArray(REQUIRED);
    assertEquals(1, requiredFields.length());
    assertEquals(USER_ID, requiredFields.getString(0));

    JSONObject properties = schema.getJSONObject("properties");
    assertTrue(properties.has(USER_ID));
    assertTrue(properties.has(AMOUNT));

    JSONObject userIdProp = properties.getJSONObject(USER_ID);
    assertEquals(STRING, userIdProp.getString("type"));
    assertEquals("User ID parameter", userIdProp.getString(DESCRIPTION));

    JSONObject amountProp = properties.getJSONObject(AMOUNT);
    assertEquals("number", amountProp.getString("type"));
    assertEquals("Amount parameter", amountProp.getString(DESCRIPTION));
  }

  /**
   * Tests the createPaths method with a webhook that has all required parameters.
   * It checks if the path is created correctly and if the request body schema includes all required fields.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsWithWebhookAllRequiredParameters() throws Exception {
    String prefixParentPath = LEGACY_V1_PREFIX;
    JSONArray webhooksArray = new JSONArray();
    JSONArray params = new JSONArray();

    JSONObject param1 = new JSONObject()
        .put("name", "email")
        .put("type", STRING)
        .put(REQUIRED, true);

    JSONObject param2 = new JSONObject()
        .put("name", "password")
        .put("type", STRING)
        .put(REQUIRED, true);

    params.put(param1);
    params.put(param2);

    JSONObject webhook = new JSONObject()
        .put("name", "/auth/login")
        .put(DESCRIPTION, "User login webhook")
        .put(PARAMS, params);
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    assertNotNull(result);
    assertTrue(result.has("/v1/auth/login"));

    JSONObject schema = result.getJSONObject("/v1/auth/login")
        .getJSONObject("post")
        .getJSONObject(REQUEST_BODY)
        .getJSONObject(CONTENT)
        .getJSONObject(APPLICATION_JSON)
        .getJSONObject(SCHEMA);

    JSONArray requiredFields = schema.getJSONArray(REQUIRED);
    assertEquals(2, requiredFields.length());
    assertTrue(requiredFields.toString().contains("email"));
    assertTrue(requiredFields.toString().contains("password"));
  }

  /**
   * Tests the createPaths method with a webhook that has no parameters.
   * It checks if the path is created correctly and if the request body schema is as expected.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsWithParameterMissingDescription() throws Exception {
    String prefixParentPath = null;
    JSONArray webhooksArray = new JSONArray();
    JSONArray params = new JSONArray();

    JSONObject param = new JSONObject()
        .put("name", "data")
        .put("type", "object")
        .put(REQUIRED, false);

    params.put(param);

    JSONObject webhook = new JSONObject()
        .put("name", "webhook1")
        .put(PARAMS, params);
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    JSONObject properties = result.getJSONObject("/webhook1")
        .getJSONObject("post")
        .getJSONObject(REQUEST_BODY)
        .getJSONObject(CONTENT)
        .getJSONObject(APPLICATION_JSON)
        .getJSONObject(SCHEMA)
        .getJSONObject("properties");

    JSONObject dataProp = properties.getJSONObject("data");
    assertEquals("", dataProp.getString(DESCRIPTION));
  }

  /**
   * Tests the createPaths method with multiple webhooks.
   * It checks if the paths are created correctly and if the summaries are as expected.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsWithMultipleWebhooks() throws Exception {
    String prefixParentPath = API_V2_PREFIX;
    JSONArray webhooksArray = new JSONArray();

    JSONObject webhook1 = new JSONObject()
        .put("name", "hook1")
        .put(DESCRIPTION, "First hook")
        .put(PARAMS, new JSONArray());

    JSONObject webhook2 = new JSONObject()
        .put("name", "hook2")
        .put(DESCRIPTION, "Second hook")
        .put(PARAMS, new JSONArray());

    webhooksArray.put(webhook1);
    webhooksArray.put(webhook2);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    assertEquals(2, result.length());
    assertTrue(result.has("/api/v2/hook1"));
    assertTrue(result.has("/api/v2/hook2"));

    assertEquals("First hook", result.getJSONObject("/api/v2/hook1")
        .getJSONObject("post").getString(SUMMARY));
    assertEquals("Second hook", result.getJSONObject("/api/v2/hook2")
        .getJSONObject("post").getString(SUMMARY));
  }

  /**
   * Tests the createPaths method with a webhook that has no parameters.
   * It checks if the path is created correctly and if the request body schema is as expected.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsResponseStructure() throws Exception {
    String prefixParentPath = "";
    JSONArray webhooksArray = new JSONArray();
    JSONObject webhook = new JSONObject()
        .put("name", "test")
        .put(PARAMS, new JSONArray());
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    JSONObject responses = result.getJSONObject("/test")
        .getJSONObject("post")
        .getJSONObject("responses");

    assertTrue(responses.has("200"));
    JSONObject response200 = responses.getJSONObject("200");
    assertEquals("Webhook response", response200.getString(DESCRIPTION));
  }

  /**
   * Tests the createPaths method with a webhook that has no parameters.
   * It checks if the path is created correctly and if the request body schema is as expected.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsRequestBodyStructure() throws Exception {
    String prefixParentPath = "";
    JSONArray webhooksArray = new JSONArray();
    JSONObject webhook = new JSONObject()
        .put("name", "test")
        .put(PARAMS, new JSONArray());
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    JSONObject requestBody = result.getJSONObject("/test")
        .getJSONObject("post")
        .getJSONObject(REQUEST_BODY);

    assertTrue(requestBody.getBoolean(REQUIRED));
    assertTrue(requestBody.has(CONTENT));

    JSONObject content = requestBody.getJSONObject(CONTENT);
    assertTrue(content.has(APPLICATION_JSON));

    JSONObject applicationJson = content.getJSONObject(APPLICATION_JSON);
    assertTrue(applicationJson.has(SCHEMA));
  }

  /**
   * Tests the getWebhookPath method with various scenarios.
   * It checks if the constructed paths are as expected based on the prefix and name.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testGetWebhookPathWithEmptyPrefix() throws Exception {
    String prefixParentPath = "";
    String name = WEBHOOK;

    String result = invokeGetWebhookPath(prefixParentPath, name);

    assertEquals(WEBHOOK_PATH, result);
  }

  /**
   * Tests the getWebhookPath method with a null prefix.
   * It checks if the constructed path defaults to the webhook name.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testGetWebhookPathWithNullPrefix() throws Exception {
    String prefixParentPath = null;
    String name = WEBHOOK;

    String result = invokeGetWebhookPath(prefixParentPath, name);

    assertEquals(WEBHOOK_PATH, result);
  }

  /**
   * Tests the getWebhookPath method with a prefix that ends with a slash.
   * It checks if the constructed path correctly appends the name to the prefix.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testGetWebhookPathWithPrefix() throws Exception {
    String prefixParentPath = API_V1_PREFIX;
    String name = WEBHOOK;

    String result = invokeGetWebhookPath(prefixParentPath, name);

    assertEquals("/api/v1/webhook", result);
  }

  /**
   * Tests the getWebhookPath method with a name that starts with a slash.
   * It checks if the constructed path correctly appends the name to the prefix.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testGetWebhookPathWithNameStartingWithSlash() throws Exception {
    String prefixParentPath = DEFAULT_API_PREFIX;
    String name = WEBHOOK_PATH;

    String result = invokeGetWebhookPath(prefixParentPath, name);

    assertEquals("/api/webhook", result);
  }

  /**
   * Tests the getWebhookPath method with a name that does not start with a slash.
   */
  @Test
  void testCreatePathsWithMalformedWebhookThrowsException() {
    String prefixParentPath = "";
    JSONArray webhooksArray = new JSONArray();
    JSONObject malformedWebhook = new JSONObject();
    webhooksArray.put(malformedWebhook);

    Exception exception = assertThrows(Exception.class, () ->
        invokeCreatePaths(prefixParentPath, webhooksArray));

    Throwable rootCause = exception;
    while (rootCause.getCause() != null) {
      rootCause = rootCause.getCause();
    }

    assertInstanceOf(JSONException.class, rootCause);
    assertTrue(rootCause.getMessage().contains("name"));
  }

  /**
   * Tests the createPaths method with a malformed parameter in the webhook.
   * It checks if an exception is thrown when the parameter is missing required fields.
   *
   * @throws Exception
   *     If any error occurs during the test.
   */
  @Test
  void testCreatePathsWithMalformedParameterThrowsException() throws Exception {
    String prefixParentPath = "";
    JSONArray webhooksArray = new JSONArray();
    JSONArray params = new JSONArray();

    JSONObject malformedParam = new JSONObject()
        .put("name", "param1");

    params.put(malformedParam);

    JSONObject webhook = new JSONObject()
        .put("name", "test")
        .put(PARAMS, params);
    webhooksArray.put(webhook);

    Exception exception = assertThrows(Exception.class, () ->
        invokeCreatePaths(prefixParentPath, webhooksArray));

    Throwable rootCause = exception;
    while (rootCause.getCause() != null) {
      rootCause = rootCause.getCause();
    }

    assertInstanceOf(JSONException.class, rootCause);
    assertTrue(rootCause.getMessage().contains("type"));
  }

  /**
   * Invokes the private method createPaths using reflection.
   *
   * @param prefixParentPath
   *     The prefix path to be used.
   * @param infoWebhooksArray
   *     The JSONArray containing webhook information.
   * @return A JSONObject representing the created paths.
   * @throws Exception
   *     If any error occurs during reflection or method invocation.
   */
  private JSONObject invokeCreatePaths(String prefixParentPath, JSONArray infoWebhooksArray) throws Exception {
    Method createPathsMethod = OpenAPISpecUtils.class.getDeclaredMethod("createPaths", String.class, JSONArray.class);
    createPathsMethod.setAccessible(true);
    return (JSONObject) createPathsMethod.invoke(null, prefixParentPath, infoWebhooksArray);
  }

  /**
   * Invokes the private method getWebhookPath using reflection.
   *
   * @param prefixParentPath
   *     The prefix path to be used.
   * @param name
   *     The name of the webhook.
   * @return The constructed webhook path as a String.
   * @throws Exception
   *     If any error occurs during reflection or method invocation.
   */
  private String invokeGetWebhookPath(String prefixParentPath, String name) throws Exception {
    Method getWebhookPathMethod = OpenAPISpecUtils.class.getDeclaredMethod("getWebhookPath", String.class,
        String.class);
    getWebhookPathMethod.setAccessible(true);
    return (String) getWebhookPathMethod.invoke(null, prefixParentPath, name);
  }
}
