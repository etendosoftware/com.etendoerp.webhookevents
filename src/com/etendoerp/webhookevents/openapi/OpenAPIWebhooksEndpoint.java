package com.etendoerp.webhookevents.openapi;

import com.etendoerp.openapi.data.OpenAPIRequest;
import com.etendoerp.openapi.data.OpenApiFlow;
import com.etendoerp.openapi.data.OpenApiFlowPoint;
import com.etendoerp.openapi.model.OpenAPIEndpoint;
import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.OpenAPIWebhook;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import javax.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
/**
 * Class that implements the OpenAPIEndpoint interface to handle OpenAPI webhook endpoints.
 */
public class OpenAPIWebhooksEndpoint implements OpenAPIEndpoint {

  public static final String DEFAULT_TAG = "Webhooks";
  public static final String OBJECT = "object";
  public static final String STRING = "string";
  private String requestedTag = null;

  public static final String GET = "GET";
  public static final String POST = "POST";

  /**
   * Retrieves a list of OpenApiFlow entities from the database.
   *
   * @return a list of OpenApiFlow entities
   */
  private List<OpenApiFlow> getFlows() {
    return OBDal.getInstance().createCriteria(OpenApiFlow.class).list();
  }

  /**
   * Retrieves a list of tags from the available flows.
   * <p>
   * This method filters the flows to include only those that have at least one
   * endpoint with non-empty webhook lists. It then maps the filtered flows to their names
   * and adds a default tag to the list.
   *
   * @return a list of tag names including the default tag
   */
  private List<String> getTags() {
    List<String> listFlows = getFlows().stream()
        .filter(flow -> flow.getETAPIOpenApiFlowPointList().stream()
            .anyMatch(point -> !point.getEtapiOpenapiReq().getSmfwheOpenapiWebhkList().isEmpty()))
        .map(OpenApiFlow::getName)
        .collect(Collectors.toList());
    listFlows.add(DEFAULT_TAG);
    return listFlows;
  }

  /**
   * Validates if the provided tag is valid.
   * <p>
   * This method checks if the provided tag is null or exists in the list of tags.
   * If the tag is valid, it sets the requestedTag to the provided tag.
   *
   * @param tag
   *     the tag to validate
   * @return true if the tag is valid, false otherwise
   */
  @Override
  public boolean isValid(String tag) {
    try {
      OBContext.setAdminMode();
      if (tag == null) {
        return true;
      }
      if (getTags().contains(tag)) {
        requestedTag = tag;
        return true;
      }
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Adds OpenAPI definitions based on the available flows.
   * <p>
   * This method adds a default tag globally and filters the flows based on the requested tag.
   * It then iterates through the flows and their endpoints to add OpenAPI definitions.
   *
   * @param openAPI
   *     the OpenAPI object to add definitions to
   */
  @Override
  public void add(OpenAPI openAPI) {
    try {
      OBContext.setAdminMode();
      List<OpenApiFlow> flows = getFlows();
      // Add default tag globally
      Tag defaultTag = new Tag().name(DEFAULT_TAG);
      addTagsIncrementally(openAPI, defaultTag);
      // If the tag is present and is not the default tag, add only the endpoints of that tag
      if (requestedTag != null && !StringUtils.equals(requestedTag, DEFAULT_TAG)) {
        flows = flows.stream()
            .filter(flow -> StringUtils.equalsIgnoreCase(flow.getName(), requestedTag))
            .collect(Collectors.toList());
      }
      flows.forEach(flow -> {
        var endpoints = flow.getETAPIOpenApiFlowPointList();
        for (OpenApiFlowPoint endpoint : endpoints) {
          Tag newTag = new Tag().name(flow.getName()).description(flow.getDescription());
          addTagsIncrementally(openAPI, newTag);
          OpenAPIRequest etapiOpenapiReq = endpoint.getEtapiOpenapiReq();

          List<OpenAPIWebhook> webhookOpenApiList = etapiOpenapiReq.getSmfwheOpenapiWebhkList();
          if (webhookOpenApiList.isEmpty()) {
            continue;
          }
          // At this point, the list should have only one element
          OpenAPIWebhook openAPIWebhook = webhookOpenApiList.get(0);
          addDefinition(openAPI, flow.getName(), etapiOpenapiReq, openAPIWebhook.getWebHook());
        }
      });
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Adds tags to the OpenAPI object incrementally.
   * <p>
   * This method checks if the tag is already present in the OpenAPI object and adds it if not.
   *
   * @param openAPI
   *     the OpenAPI object to add tags to
   * @param tag
   *     the tag to add
   */
  private void addTagsIncrementally(OpenAPI openAPI, Tag tag) {
    List<Tag> currentGlobalTags = openAPI.getTags();
    if (currentGlobalTags == null) {
      openAPI.setTags(new ArrayList<>());
      currentGlobalTags = openAPI.getTags();
    }
    if (tagIsPresent(tag, currentGlobalTags)) {
      currentGlobalTags.add(tag);
    }
  }

  /**
   * Checks if a tag is already present in the list of tags.
   *
   * @param tag
   *     the tag to check
   * @param currentGlobalTags
   *     the list of current global tags
   * @return true if the tag is not present, false otherwise
   */
  private boolean tagIsPresent(Tag tag, List<Tag> currentGlobalTags) {
    return currentGlobalTags.stream().noneMatch(
        sometag -> StringUtils.equalsIgnoreCase(sometag.getName(), tag.getName()));
  }

  /**
   * Adds an OpenAPI definition for a specific webhook.
   * <p>
   * This method creates the request and response schemas and adds the endpoint to the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the definition to
   * @param tag
   *     the tag associated with the endpoint
   * @param request
   *     the OpenAPIRequest object containing the request details
   * @param webHook
   *     the DefinedWebHook object containing the webhook details
   */
  private void addDefinition(OpenAPI openAPI, String tag, OpenAPIRequest request, DefinedWebHook webHook) {

    // Form init
    Schema<?> formInitResponseSchema;
    Schema<?> formInitRequestSchema;

    List<DefinedWebhookParam> webhooksParameterList = webHook.getSmfwheDefinedwebhookParamList();
    formInitRequestSchema = defineFormInitRequestSchema(webhooksParameterList);
    formInitResponseSchema = new Schema<>();

    String method = webhooksParameterList.isEmpty() ? GET : POST;

    createEndpoint(openAPI,
        tag,
        request.getDescription(),
        formInitResponseSchema,
        formInitRequestSchema,
        method,
        webHook);

  }

  /**
   * Creates an endpoint in the OpenAPI object.
   * <p>
   * This method sets up the operation, request body, and responses for the endpoint and adds it to the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the endpoint to
   * @param tag
   *     the tag associated with the endpoint
   * @param summary
   *     the summary of the endpoint
   * @param responseSchema
   *     the schema for the response
   * @param requestBodySchema
   *     the schema for the request body
   * @param httpMethod
   *     the HTTP method for the endpoint (GET or POST)
   * @param webHook
   *     the DefinedWebHook object containing the webhook details
   */
  private void createEndpoint(OpenAPI openAPI,
      String tag,
      String summary,
      Schema<?> responseSchema,
      Schema<?> requestBodySchema,
      String httpMethod,
      DefinedWebHook webHook) {

    String actionValue = webHook.getName();
    String description = webHook.getDescription();
    String schemaKey = actionValue + "Response";

    String responseExample = new JSONObject().toString();
    String requestBodyExample = new JSONObject().toString();

    ApiResponses apiResponses = new ApiResponses().addApiResponse("200",
            createApiResponse("Successful response.", responseSchema, responseExample))
        .addApiResponse("400", new ApiResponse().description("Unsuccessful request."))
        .addApiResponse("500", new ApiResponse().description("Internal server error."));

    Operation operation = new Operation().summary(summary).description(description);

    if (operation.getTags() == null) {
      operation.setTags(new ArrayList<>());
    }
    // Add default tag and the tag of the flow
    operation.getTags().add(DEFAULT_TAG);
    operation.getTags().add(tag);

    if (requestBodySchema != null) {
      RequestBody requestBody = new RequestBody().description(
              "Request body for request " + actionValue)
          .content(new Content().addMediaType("application/json",
              new MediaType().schema(requestBodySchema).example(requestBodyExample)))
          .required(true);
      operation.setRequestBody(requestBody);
    }

    operation.responses(apiResponses);
    String path = String.format("/webhooks/%s", actionValue);
    PathItem pathItem;
    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }
    if (openAPI.getPaths().containsKey(path)) {
      pathItem = openAPI.getPaths().get(path);
    } else {
      pathItem = new PathItem();
    }

    if (StringUtils.equals(httpMethod, GET)) {
      pathItem.get(operation);
    }
    if (StringUtils.equals(httpMethod, POST)) {
      pathItem.post(operation);
    }

    if (openAPI.getPaths() == null) {
      openAPI.setPaths(new Paths());
    }

    openAPI.getPaths().addPathItem(path, pathItem);

    addSchema(openAPI, schemaKey, responseSchema);
  }

  /**
   * Creates an ApiResponse object for the given description, schema, and example.
   *
   * @param description
   *     the description of the response
   * @param schema
   *     the schema of the response
   * @param example
   *     the example of the response
   * @return the created ApiResponse object
   */
  private ApiResponse createApiResponse(String description, Schema<?> schema, String example) {
    return new ApiResponse().description(description)
        .content(new Content().addMediaType("application/json",
            new MediaType().schema(schema).example(example)));
  }

  /**
   * Adds a schema to the OpenAPI object.
   *
   * @param openAPI
   *     the OpenAPI object to add the schema to
   * @param key
   *     the key for the schema
   * @param schema
   *     the schema to add
   */
  private void addSchema(OpenAPI openAPI, String key, Schema<?> schema) {
    if (openAPI.getComponents() == null) {
      openAPI.setComponents(new io.swagger.v3.oas.models.Components());
    }
    if (openAPI.getComponents().getSchemas() == null) {
      openAPI.getComponents().setSchemas(new HashMap<>());
    }
    if (!openAPI.getComponents().getSchemas().containsKey(key)) {
      openAPI.getComponents().addSchemas(key, schema);
    }
  }

  /**
   * Defines the request schema for the form initialization.
   * <p>
   * This method creates a schema for the request body based on the provided parameters.
   *
   * @param params
   *     the list of DefinedWebhookParam objects
   * @return the created request schema
   */
  private Schema<?> defineFormInitRequestSchema(List<DefinedWebhookParam> params) {
    Schema<Object> schema = new Schema<>();
    schema.type(OBJECT);
    List<String> required = new ArrayList<>();
    for (DefinedWebhookParam parameter : params) {
      String name = parameter.getName();
      Schema parameterSchema = new Schema<>();
      parameterSchema.type(STRING);
      parameterSchema.description(parameter.getDescription());
      schema.addProperty(name, parameterSchema);
      if (parameter.isRequired()) {
        required.add(name);
      }
    }
    schema.required(required);
    return schema;
  }

}