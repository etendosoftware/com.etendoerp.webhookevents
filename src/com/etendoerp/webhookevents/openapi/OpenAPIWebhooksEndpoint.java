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
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;

import javax.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.etendoerp.etendorx.services.DataSourceServlet.normalizedName;

@ApplicationScoped
public class OpenAPIWebhooksEndpoint implements OpenAPIEndpoint {

  public static final String DEFAULT_TAG = "Webhooks";
  private String requestedTag = null;
  private static final List<String> extraFields = List.of("_identifier", "$ref", "active",
      "creationDate", "createdBy", "createdBy$_identifier", "updated", "updatedBy",
      "updatedBy$_identifier");
  public static final String GET = "GET";
  public static final String POST = "POST";

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

  @Override
  public void add(OpenAPI openAPI) {
    try {
      OBContext.setAdminMode();
      List<OpenApiFlow> flows = getFlows();
      //add default tag globally
      Tag defaultTag = new Tag().name(DEFAULT_TAG);
      addTagsIncrementally(openAPI, defaultTag);
      //if the tag is present and is not the default tag, add only the endpoints of that tag
      if (requestedTag != null && !requestedTag.equals(DEFAULT_TAG)) {
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
          addDefinition(openAPI, flow.getName(), etapiOpenapiReq.getName(), openAPIWebhook.getSmfwheDefinedwebhook());
        }
      });
    } finally {
      OBContext.restorePreviousMode();
    }
  }

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

  private boolean tagIsPresent(Tag tag, List<Tag> currentGlobalTags) {
    return currentGlobalTags.stream().noneMatch(
        sometag -> StringUtils.equalsIgnoreCase(sometag.getName(), tag.getName()));
  }

  private void addDefinition(OpenAPI openAPI, String tag, String entityName, DefinedWebHook webHook) {

    // Form init
    Schema<?> formInitResponseSchema;
    Schema<?> formInitRequestSchema;

    JSONObject formInitJSON = new JSONObject();
    JSONObject formInitResponseExample = new JSONObject();
    List<DefinedWebhookParam> webhooksParameterList = webHook.getSmfwheDefinedwebhookParamList();
    formInitRequestSchema = defineFormInitRequestSchema(webhooksParameterList);
    formInitResponseSchema = new Schema<>();

    String method = webhooksParameterList.isEmpty() ? GET : POST;

    String formInitRequestExample = formInitJSON.toString();
    List<Parameter> formInitParams = new ArrayList<>();

    String webhook = webHook.getName();
    createEndpoint(openAPI, tag, webhook, "",
        webHook.getDescription(), formInitResponseSchema,
        formInitResponseExample.toString(), webhook + "Response", formInitParams,
        formInitRequestSchema, formInitRequestExample, method);

  }


  private void createEndpoint(OpenAPI openAPI, String tag, String actionValue, String summary,
      String description, Schema<?> responseSchema, String responseExample, String schemaKey,
      List<Parameter> parameters, Schema<?> requestBodySchema, String requestBodyExample,
      String httpMethod) {

    ApiResponses apiResponses = new ApiResponses().addApiResponse("200",
            createApiResponse("Successful response.", responseSchema, responseExample))
        .addApiResponse("400", new ApiResponse().description("Unsuccessful request."))
        .addApiResponse("500", new ApiResponse().description("Internal server error."));

    Operation operation = new Operation().summary(summary).description(description);

    if (operation.getTags() == null) {
      operation.setTags(new ArrayList<>());
    }
    //add default tag and the tag of the flow
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
    String path = String.format("/sws/webhooks/%s", actionValue);
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

  private String getContextName() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("context.name");
  }

  private ApiResponse createApiResponse(String description, Schema<?> schema, String example) {
    return new ApiResponse().description(description)
        .content(new Content().addMediaType("application/json",
            new MediaType().schema(schema).example(example)));
  }


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

  private Schema<?> defineFormInitRequestSchema(List<DefinedWebhookParam> params) {
    Schema<Object> schema = new Schema<>();
    schema.type("object");
    List<String> required = new ArrayList<>();
    for (DefinedWebhookParam parameter : params) {
      String name = parameter.getName();
      schema.addProperty(name, new Schema<>().type("string"));
      if (parameter.isRequired()) {
        required.add(name);
      }
    }
    schema.required(required);
    return schema;
  }


  private Schema<?> defineDataItemSchema(List<Field> fields) {
    Schema<Object> dataItemSchema = new Schema<>();
    dataItemSchema.type("object");
    dataItemSchema.description("Entity data");
    for (String extraField : extraFields) {
      dataItemSchema.addProperty(extraField, new Schema<>().type("string").example(""));
    }
    for (Field field : fields) {
      dataItemSchema.addProperty(normalizedName(field.getColumn().getName()),
          new Schema<>().type("string").example(""));
    }
    return dataItemSchema;
  }

  private Schema<?> defineResponseSchema(List<Field> fields) {
    Schema<Object> responseSchema = new Schema<>();
    responseSchema.type("object");
    responseSchema.description("Main object of the response");

    Schema<Integer> statusSchema = new Schema<>();
    statusSchema.type("integer").format("int32").example(0);
    responseSchema.addProperty("status", statusSchema);

    ArraySchema dataArraySchema = new ArraySchema();
    dataArraySchema.items(defineDataItemSchema(fields));
    responseSchema.addProperty("data", dataArraySchema);

    return responseSchema;
  }

}
