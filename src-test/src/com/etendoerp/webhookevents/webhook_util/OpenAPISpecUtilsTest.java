package com.etendoerp.webhookevents.webhook_util;

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
    String prefixParentPath = "/api";
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
    String prefixParentPath = "/api";
    JSONArray webhooksArray = new JSONArray();
    JSONObject webhook = new JSONObject()
        .put("name", "testWebhook")
        .put("description", "Test webhook description")
        .put("params", new JSONArray());
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    assertNotNull(result);
    assertEquals(1, result.length());
    assertTrue(result.has("/api/testWebhook"));

    JSONObject webhookPath = result.getJSONObject("/api/testWebhook");
    assertTrue(webhookPath.has("post"));

    JSONObject postMethod = webhookPath.getJSONObject("post");
    assertEquals("Test webhook description", postMethod.getString("summary"));
    assertTrue(postMethod.has("requestBody"));
    assertTrue(postMethod.has("responses"));

    JSONObject requestBody = postMethod.getJSONObject("requestBody");
    assertTrue(requestBody.getBoolean("required"));

    JSONObject schema = requestBody.getJSONObject("content")
        .getJSONObject("application/json")
        .getJSONObject("schema");
    assertEquals("object", schema.getString("type"));
    assertFalse(schema.has("required"));
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
        .put("name", "userId")
        .put("type", "string")
        .put("description", "User ID parameter")
        .put("required", true);

    JSONObject param2 = new JSONObject()
        .put("name", "amount")
        .put("type", "number")
        .put("description", "Amount parameter")
        .put("required", false);

    params.put(param1);
    params.put(param2);

    JSONObject webhook = new JSONObject()
        .put("name", "payment")
        .put("params", params);
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    assertNotNull(result);
    assertTrue(result.has("/payment"));

    JSONObject webhookPath = result.getJSONObject("/payment");
    JSONObject postMethod = webhookPath.getJSONObject("post");
    assertEquals("Executes the payment WebHook", postMethod.getString("summary"));

    JSONObject schema = postMethod.getJSONObject("requestBody")
        .getJSONObject("content")
        .getJSONObject("application/json")
        .getJSONObject("schema");

    assertTrue(schema.has("required"));
    JSONArray requiredFields = schema.getJSONArray("required");
    assertEquals(1, requiredFields.length());
    assertEquals("userId", requiredFields.getString(0));

    JSONObject properties = schema.getJSONObject("properties");
    assertTrue(properties.has("userId"));
    assertTrue(properties.has("amount"));

    JSONObject userIdProp = properties.getJSONObject("userId");
    assertEquals("string", userIdProp.getString("type"));
    assertEquals("User ID parameter", userIdProp.getString("description"));

    JSONObject amountProp = properties.getJSONObject("amount");
    assertEquals("number", amountProp.getString("type"));
    assertEquals("Amount parameter", amountProp.getString("description"));
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
    String prefixParentPath = "/v1";
    JSONArray webhooksArray = new JSONArray();
    JSONArray params = new JSONArray();

    JSONObject param1 = new JSONObject()
        .put("name", "email")
        .put("type", "string")
        .put("required", true);

    JSONObject param2 = new JSONObject()
        .put("name", "password")
        .put("type", "string")
        .put("required", true);

    params.put(param1);
    params.put(param2);

    JSONObject webhook = new JSONObject()
        .put("name", "/auth/login")
        .put("description", "User login webhook")
        .put("params", params);
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    assertNotNull(result);
    assertTrue(result.has("/v1/auth/login"));

    JSONObject schema = result.getJSONObject("/v1/auth/login")
        .getJSONObject("post")
        .getJSONObject("requestBody")
        .getJSONObject("content")
        .getJSONObject("application/json")
        .getJSONObject("schema");

    JSONArray requiredFields = schema.getJSONArray("required");
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
        .put("required", false);

    params.put(param);

    JSONObject webhook = new JSONObject()
        .put("name", "webhook1")
        .put("params", params);
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    JSONObject properties = result.getJSONObject("/webhook1")
        .getJSONObject("post")
        .getJSONObject("requestBody")
        .getJSONObject("content")
        .getJSONObject("application/json")
        .getJSONObject("schema")
        .getJSONObject("properties");

    JSONObject dataProp = properties.getJSONObject("data");
    assertEquals("", dataProp.getString("description"));
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
    String prefixParentPath = "/api/v2";
    JSONArray webhooksArray = new JSONArray();

    JSONObject webhook1 = new JSONObject()
        .put("name", "hook1")
        .put("description", "First hook")
        .put("params", new JSONArray());

    JSONObject webhook2 = new JSONObject()
        .put("name", "hook2")
        .put("description", "Second hook")
        .put("params", new JSONArray());

    webhooksArray.put(webhook1);
    webhooksArray.put(webhook2);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    assertEquals(2, result.length());
    assertTrue(result.has("/api/v2/hook1"));
    assertTrue(result.has("/api/v2/hook2"));

    assertEquals("First hook", result.getJSONObject("/api/v2/hook1")
        .getJSONObject("post").getString("summary"));
    assertEquals("Second hook", result.getJSONObject("/api/v2/hook2")
        .getJSONObject("post").getString("summary"));
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
        .put("params", new JSONArray());
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    JSONObject responses = result.getJSONObject("/test")
        .getJSONObject("post")
        .getJSONObject("responses");

    assertTrue(responses.has("200"));
    JSONObject response200 = responses.getJSONObject("200");
    assertEquals("Webhook response", response200.getString("description"));
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
        .put("params", new JSONArray());
    webhooksArray.put(webhook);

    JSONObject result = invokeCreatePaths(prefixParentPath, webhooksArray);

    JSONObject requestBody = result.getJSONObject("/test")
        .getJSONObject("post")
        .getJSONObject("requestBody");

    assertTrue(requestBody.getBoolean("required"));
    assertTrue(requestBody.has("content"));

    JSONObject content = requestBody.getJSONObject("content");
    assertTrue(content.has("application/json"));

    JSONObject applicationJson = content.getJSONObject("application/json");
    assertTrue(applicationJson.has("schema"));
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
    String name = "webhook";

    String result = invokeGetWebhookPath(prefixParentPath, name);

    assertEquals("/webhook", result);
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
    String name = "webhook";

    String result = invokeGetWebhookPath(prefixParentPath, name);

    assertEquals("/webhook", result);
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
    String prefixParentPath = "/api/v1";
    String name = "webhook";

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
    String prefixParentPath = "/api";
    String name = "/webhook";

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
        .put("params", params);
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
