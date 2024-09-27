package com.etendoerp.webhookevents.webhook_util;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Utility class to generate OpenAPI specifications in JSON format for Webhook APIs.
 */
public class OpenAPISpecUtils {
  public static final String SECURE_WS_TOKEN = "secureWSToken";
  public static final String PROP_NAME = "name";
  public static final String PROP_TYPE = "type";
  public static final String PROP_REQUIRED = "required";
  public static final String PROP_DESCRIPTION = "description";

  /**
   * Private constructor to prevent instantiation of the utility class.
   */
  private OpenAPISpecUtils() {
  }

  /**
   * Generates a JSON representation of the OpenAPI specification based on the provided parameters.
   *
   * @param host
   *     The host URL for the API.
   * @param title
   *     The title of the API.
   * @param description
   *     The description of the API.
   * @param apiVersion
   *     The version of the API.
   * @param prefixParentPath
   *     The prefix parent path for the API endpoints.
   * @param infoWebhooksArray
   *     The array of webhook information.
   * @return A JSON string representing the OpenAPI specification.
   * @throws JSONException
   *     If there is an error while creating the JSON objects.
   */
  public static String generateJSONOpenAPISpec(String host, String title, String description,
      String apiVersion, String prefixParentPath, JSONArray infoWebhooksArray) throws JSONException {

    JSONObject openApiSpec = createMainJson(title, description, apiVersion);
    openApiSpec.put("servers", createServerArray(host));
    JSONObject components = new JSONObject();
    JSONObject paths = createPaths(prefixParentPath, infoWebhooksArray);
    openApiSpec.put("paths", paths);
    addBearerTokenSecurityScheme(components, openApiSpec);
    addSecurityToPaths(paths);
    return openApiSpec.toString(4);
  }

  /**
   * Adds security schema to the API paths using bearer token authentication.
   *
   * @param paths
   *     The paths object in the OpenAPI spec.
   * @throws JSONException
   *     If there is an error while creating the JSON objects.
   */
  private static void addSecurityToPaths(JSONObject paths) throws JSONException {
    JSONArray securityArray = new JSONArray().put(new JSONObject().put(SECURE_WS_TOKEN, new JSONArray()));
    for (Iterator<String> keys = paths.keys(); keys.hasNext(); ) {
      String key = keys.next();
      paths.getJSONObject(key).getJSONObject("post").put("security", securityArray);
    }
  }

  /**
   * Creates a server array with the host URL.
   *
   * @param host
   *     The host URL for the API.
   * @return A JSON array containing the server information.
   * @throws JSONException
   *     If there is an error while creating the JSON objects.
   */
  private static JSONArray createServerArray(String host) throws JSONException {
    return new JSONArray().put(new JSONObject().put("url", host));
  }

  /**
   * Creates the main JSON structure for the OpenAPI specification.
   *
   * @param title
   *     The title of the API.
   * @param description
   *     The description of the API.
   * @param apiVersion
   *     The version of the API.
   * @return A JSON object containing the basic OpenAPI information.
   * @throws JSONException
   *     If there is an error while creating the JSON objects.
   */
  private static JSONObject createMainJson(String title, String description, String apiVersion) throws JSONException {
    return new JSONObject()
        .put("openapi", "3.0.1")
        .put("info", new JSONObject()
            .put("title", title)
            .put(PROP_DESCRIPTION, description)
            .put("version", apiVersion));
  }

  /**
   * Adds a Bearer token security scheme to the OpenAPI spec.
   *
   * @param components
   *     The components object in the OpenAPI spec.
   * @param openApiSpec
   *     The OpenAPI spec object.
   * @throws JSONException
   *     If there is an error while creating the JSON objects.
   */
  private static void addBearerTokenSecurityScheme(JSONObject components, JSONObject openApiSpec) throws JSONException {
    JSONObject bearerToken = new JSONObject()
        .put(PROP_TYPE, "http")
        .put("scheme", "bearer")
        .put("bearerFormat", "JWT");

    components.put("securitySchemes", new JSONObject().put(SECURE_WS_TOKEN, bearerToken));
    openApiSpec.put("components", components);
  }

  /**
   * Generates the OpenAPI paths based on webhook information.
   *
   * @param prefixParentPath
   *     The parent path for the API endpoints.
   * @param infoWebhooksArray
   *     The array of webhook information.
   * @return A JSON object representing the OpenAPI paths.
   * @throws JSONException
   *     If there is an error while creating the JSON objects.
   */
  private static JSONObject createPaths(String prefixParentPath, JSONArray infoWebhooksArray) throws JSONException {
    JSONObject paths = new JSONObject();
    for (int i = 0; i < infoWebhooksArray.length(); i++) {
      JSONObject webhook = infoWebhooksArray.getJSONObject(i);
      String name = webhook.getString(PROP_NAME);

      JSONObject webhookSchema = new JSONObject().put(PROP_TYPE, "object");
      JSONObject properties = new JSONObject();
      JSONArray requiredFields = new JSONArray();

      JSONArray params = webhook.getJSONArray("params");
      for (int j = 0; j < params.length(); j++) {
        JSONObject param = params.getJSONObject(j);
        properties.put(param.getString(PROP_NAME), new JSONObject().put(PROP_TYPE, param.getString(PROP_TYPE)));

        if (param.getBoolean(PROP_REQUIRED)) {
          requiredFields.put(param.getString(PROP_NAME));
        }
      }

      if (requiredFields.length() > 0) {
        webhookSchema.put(PROP_REQUIRED, requiredFields);
      }

      webhookSchema.put("properties", properties);
      String webhookPath = getWebhookPath(prefixParentPath, name);
      String hookDescription = webhook.optString(PROP_DESCRIPTION, String.format("Executes the %s WebHook", name));
      JSONObject postPath = new JSONObject().put("summary", hookDescription)
          .put("requestBody", new JSONObject().put(PROP_REQUIRED, true)
              .put("content", new JSONObject().put("application/json", new JSONObject().put("schema", webhookSchema))))
          .put("responses", new JSONObject().put("200", new JSONObject().put(PROP_DESCRIPTION, "Webhook response")));

      paths.put(webhookPath, new JSONObject().put("post", postPath));
    }
    return paths;
  }

  /**
   * Constructs the full webhook path by combining the prefix parent path and the webhook name.
   *
   * @param prefixParentPath
   *     The parent path prefix for the API endpoints.
   * @param name
   *     The name of the webhook.
   * @return The full webhook path as a string.
   */
  private static String getWebhookPath(String prefixParentPath, String name) {
    String prefix = StringUtils.isEmpty(prefixParentPath) ? "" : prefixParentPath;
    String path = StringUtils.startsWith(name, "/") ? name : ("/" + name);
    return prefix + path;
  }
}