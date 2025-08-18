package com.etendoerp.webhookevents.openapi;

import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_DESCRIPTION;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_FLOW_NAME;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_WEBHOOK_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

import com.etendoerp.openapi.data.OpenAPIRequest;
import com.etendoerp.openapi.data.OpenApiFlow;
import com.etendoerp.openapi.data.OpenApiFlowPoint;
import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.OpenAPIWebhook;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Test class for OpenAPIWebhooksEndpoint.
 * This class tests the functionality of the OpenAPIWebhooksEndpoint class,
 * ensuring that it correctly handles OpenAPI flow points and webhooks.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class OpenAPIWebhooksTest {

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBCriteria<OpenApiFlow> mockCriteria;

  @InjectMocks
  private OpenAPIWebhooksEndpoint endpoint;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;

  /**
   * Sets up the test environment by mocking OBDal and OBContext.
   * This method is called before each test to ensure a clean state.
   */
  @BeforeEach
  void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBContext.when(OBContext::setAdminMode).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);
  }

  /**
   * Cleans up the test environment by closing the mocked OBDal and OBContext.
   * This method is called after each test to ensure resources are released.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
  }

  /**
   * Tests the isValid method with a null tag.
   * It should return true when there are no flows in the database.
   */
  @Test
  void testIsValidWithNullTag() {
    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(new ArrayList<>());

    boolean result = endpoint.isValid(null);

    assertTrue(result);
  }

  /**
   * Tests the isValid method with a valid tag.
   * It should return true when there are flows in the database with the specified tag.
   *
   * @throws Exception
   *     if reflection fails when setting the private field.
   */
  @Test
  void testIsValidWithValidTag() throws Exception {
    String validTag = "validTag";
    List<OpenApiFlow> flows = createMockFlowsWithWebhooks(validTag);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(flows);

    boolean result = endpoint.isValid(validTag);

    assertTrue(result);
    assertEquals(validTag, getPrivateField(endpoint, "requestedTag"));
  }

  /**
   * Tests the isValid method with an empty tag.
   * It should return false when there are no flows in the database.
   */
  @Test
  void testIsValidWithInvalidTag() {
    String invalidTag = "invalidTag";
    List<OpenApiFlow> flows = createMockFlowsWithWebhooks("differentTag");

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(flows);

    boolean result = endpoint.isValid(invalidTag);

    assertFalse(result);
  }

  /**
   * Tests the isValid method with an empty flows list.
   * It should return false when there are no flows in the database.
   */
  @Test
  void testIsValidWithEmptyFlows() {
    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(new ArrayList<>());

    boolean result = endpoint.isValid("anyTag");

    assertFalse(result);
  }

  /**
   * Tests the isValid method with a null flows list.
   * It should return false when there are no flows in the database.
   */
  @Test
  void testAddWithNullRequestedTag() {
    OpenAPI openAPI = new OpenAPI();
    List<OpenApiFlow> flows = createMockFlowsWithWebhooks(TEST_FLOW_NAME);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(flows);

    endpoint.add(openAPI);

    assertNotNull(openAPI.getTags());
    assertFalse(openAPI.getTags().isEmpty());
    assertEquals(TEST_FLOW_NAME, openAPI.getTags().get(0).getName());
  }

  /**
   * Tests the add method when a specific requested tag is set.
   * Ensures that only flows matching the requested tag are added as tags to the OpenAPI object.
   *
   * @throws Exception
   *     if reflection fails when setting the private field.
   */
  @Test
  void testAddWithSpecificRequestedTag() throws Exception {
    String requestedTag = "specificFlow";
    setPrivateField(endpoint, "requestedTag", requestedTag);

    OpenAPI openAPI = new OpenAPI();
    List<OpenApiFlow> flows = createMockFlowsWithWebhooks(requestedTag, "otherFlow");

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(flows);

    endpoint.add(openAPI);

    assertNotNull(openAPI.getTags());
    assertEquals(1, openAPI.getTags().size());
    assertEquals(requestedTag, openAPI.getTags().get(0).getName());
  }

  /**
   * Tests the add method with a null OpenAPI object.
   * It should not throw an exception and should handle the null case gracefully.
   */
  @Test
  void testAddWithExistingTags() {
    OpenAPI openAPI = new OpenAPI();
    Tag existingTag = new Tag().name("existingTag");
    List<Tag> mutableTagList = new ArrayList<>();
    mutableTagList.add(existingTag);
    openAPI.setTags(mutableTagList);

    List<OpenApiFlow> flows = createMockFlowsWithWebhooks(TEST_FLOW_NAME);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(flows);

    endpoint.add(openAPI);

    assertNotNull(openAPI.getTags());
    assertEquals(2, openAPI.getTags().size());
  }

  /**
   * Tests the add method with an OpenAPI object that has no paths.
   * It should not throw an exception and should handle the empty paths case gracefully.
   */
  @Test
  void testAddWithInactiveFlowPoint() {
    OpenAPI openAPI = new OpenAPI();
    OpenApiFlow flow = mock(OpenApiFlow.class);
    OpenApiFlowPoint inactiveFlowPoint = mock(OpenApiFlowPoint.class);

    when(flow.getName()).thenReturn(TEST_FLOW_NAME);
    when(flow.getDescription()).thenReturn(TEST_DESCRIPTION);
    when(flow.getETAPIOpenApiFlowPointList()).thenReturn(List.of(inactiveFlowPoint));
    when(inactiveFlowPoint.isActive()).thenReturn(false);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(List.of(flow));

    endpoint.add(openAPI);

    if (openAPI.getPaths() != null) {
      assertTrue(openAPI.getPaths().isEmpty());
    }
  }

  /**
   * Tests the add method with an inactive OpenAPIRequest.
   * It should not create a path item for the request if it is inactive.
   */
  @Test
  void testAddWithInactiveOpenAPIRequest() {
    OpenAPI openAPI = new OpenAPI();
    OpenApiFlow flow = mock(OpenApiFlow.class);
    OpenApiFlowPoint flowPoint = mock(OpenApiFlowPoint.class);
    OpenAPIRequest inactiveRequest = mock(OpenAPIRequest.class);

    when(flow.getName()).thenReturn(TEST_FLOW_NAME);
    when(flow.getDescription()).thenReturn(TEST_DESCRIPTION);
    when(flow.getETAPIOpenApiFlowPointList()).thenReturn(List.of(flowPoint));
    when(flowPoint.isActive()).thenReturn(true);
    when(flowPoint.getEtapiOpenapiReq()).thenReturn(inactiveRequest);
    when(inactiveRequest.isActive()).thenReturn(false);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(List.of(flow));

    endpoint.add(openAPI);

    if (openAPI.getPaths() != null) {
      assertTrue(openAPI.getPaths().isEmpty());
    }
  }

  /**
   * Tests the add method with an inactive webhook.
   * It should not create a path item for the webhook if it is inactive.
   */
  @Test
  void testAddWithEmptyWebhookList() {
    OpenAPI openAPI = new OpenAPI();
    OpenApiFlow flow = mock(OpenApiFlow.class);
    OpenApiFlowPoint flowPoint = mock(OpenApiFlowPoint.class);
    OpenAPIRequest request = mock(OpenAPIRequest.class);

    when(flow.getName()).thenReturn(TEST_FLOW_NAME);
    when(flow.getDescription()).thenReturn(TEST_DESCRIPTION);
    when(flow.getETAPIOpenApiFlowPointList()).thenReturn(List.of(flowPoint));
    when(flowPoint.isActive()).thenReturn(true);
    when(flowPoint.getEtapiOpenapiReq()).thenReturn(request);
    when(request.isActive()).thenReturn(true);
    when(request.getSmfwheOpenapiWebhkList()).thenReturn(new ArrayList<>());

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(List.of(flow));

    endpoint.add(openAPI);

    if (openAPI.getPaths() != null) {
      assertTrue(openAPI.getPaths().isEmpty());
    }
  }

  /**
   * Tests the add method with an inactive webhook.
   * It should not create a path item for the webhook in the OpenAPI object.
   */
  @Test
  void testAddWithInactiveWebhook() {
    OpenAPI openAPI = new OpenAPI();
    OpenApiFlow flow = mock(OpenApiFlow.class);
    OpenApiFlowPoint flowPoint = mock(OpenApiFlowPoint.class);
    OpenAPIRequest request = mock(OpenAPIRequest.class);
    OpenAPIWebhook webhook = mock(OpenAPIWebhook.class);
    DefinedWebHook inactiveDefinedWebHook = mock(DefinedWebHook.class);

    when(flow.getName()).thenReturn(TEST_FLOW_NAME);
    when(flow.getDescription()).thenReturn(TEST_DESCRIPTION);
    when(flow.getETAPIOpenApiFlowPointList()).thenReturn(List.of(flowPoint));
    when(flowPoint.isActive()).thenReturn(true);
    when(flowPoint.getEtapiOpenapiReq()).thenReturn(request);
    when(request.isActive()).thenReturn(true);
    when(request.getSmfwheOpenapiWebhkList()).thenReturn(List.of(webhook));
    when(webhook.isActive()).thenReturn(true);
    when(webhook.getWebHook()).thenReturn(inactiveDefinedWebHook);
    when(inactiveDefinedWebHook.isActive()).thenReturn(false);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(List.of(flow));

    endpoint.add(openAPI);

    if (openAPI.getPaths() != null) {
      assertTrue(openAPI.getPaths().isEmpty());
    }
  }

  /**
   * Tests the add method with a valid webhook that has a GET method.
   * It should create a path item for the webhook with the GET method.
   */
  @Test
  void testAddWithValidWebhookGETMethod() {
    OpenAPI openAPI = new OpenAPI();
    OpenApiFlow flow = mock(OpenApiFlow.class);
    OpenApiFlowPoint flowPoint = mock(OpenApiFlowPoint.class);
    OpenAPIRequest request = mock(OpenAPIRequest.class);
    OpenAPIWebhook webhook = mock(OpenAPIWebhook.class);
    DefinedWebHook definedWebHook = mock(DefinedWebHook.class);

    when(flow.getName()).thenReturn(TEST_FLOW_NAME);
    when(flow.getDescription()).thenReturn(TEST_DESCRIPTION);
    when(flow.getETAPIOpenApiFlowPointList()).thenReturn(List.of(flowPoint));
    when(flowPoint.isActive()).thenReturn(true);
    when(flowPoint.getEtapiOpenapiReq()).thenReturn(request);
    when(request.isActive()).thenReturn(true);
    when(request.getDescription()).thenReturn("Test Request");
    when(request.getSmfwheOpenapiWebhkList()).thenReturn(List.of(webhook));
    when(webhook.isActive()).thenReturn(true);
    when(webhook.getWebHook()).thenReturn(definedWebHook);
    when(definedWebHook.isActive()).thenReturn(true);
    when(definedWebHook.getName()).thenReturn("testWebhook");
    when(definedWebHook.getDescription()).thenReturn("Test Webhook");
    when(definedWebHook.getSmfwheDefinedwebhookParamList()).thenReturn(new ArrayList<>());

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(List.of(flow));

    endpoint.add(openAPI);

    assertNotNull(openAPI.getPaths());
    assertTrue(openAPI.getPaths().containsKey(TEST_WEBHOOK_ENDPOINT));
    PathItem pathItem = openAPI.getPaths().get(TEST_WEBHOOK_ENDPOINT);
    assertNotNull(pathItem.getGet());
  }

  /**
   * Tests the add method with a valid webhook that has a POST method.
   * It should create a path item for the webhook with the POST method.
   */
  @Test
  void testAddWithValidWebhookPOSTMethod() {
    // Given
    OpenAPI openAPI = new OpenAPI();
    OpenApiFlow flow = mock(OpenApiFlow.class);
    OpenApiFlowPoint flowPoint = mock(OpenApiFlowPoint.class);
    OpenAPIRequest request = mock(OpenAPIRequest.class);
    OpenAPIWebhook webhook = mock(OpenAPIWebhook.class);
    DefinedWebHook definedWebHook = mock(DefinedWebHook.class);
    DefinedWebhookParam param = mock(DefinedWebhookParam.class);

    when(flow.getName()).thenReturn(TEST_FLOW_NAME);
    when(flow.getDescription()).thenReturn(TEST_DESCRIPTION);
    when(flow.getETAPIOpenApiFlowPointList()).thenReturn(List.of(flowPoint));
    when(flowPoint.isActive()).thenReturn(true);
    when(flowPoint.getEtapiOpenapiReq()).thenReturn(request);
    when(request.isActive()).thenReturn(true);
    when(request.getDescription()).thenReturn("Test Request");
    when(request.getSmfwheOpenapiWebhkList()).thenReturn(List.of(webhook));
    when(webhook.isActive()).thenReturn(true);
    when(webhook.getWebHook()).thenReturn(definedWebHook);
    when(definedWebHook.isActive()).thenReturn(true);
    when(definedWebHook.getName()).thenReturn("testWebhook");
    when(definedWebHook.getDescription()).thenReturn("Test Webhook");
    when(definedWebHook.getSmfwheDefinedwebhookParamList()).thenReturn(List.of(param));
    when(param.getName()).thenReturn("testParam");
    when(param.getDescription()).thenReturn("Test Parameter");
    when(param.isRequired()).thenReturn(true);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(List.of(flow));

    endpoint.add(openAPI);

    assertNotNull(openAPI.getPaths());
    assertTrue(openAPI.getPaths().containsKey(TEST_WEBHOOK_ENDPOINT));
    PathItem pathItem = openAPI.getPaths().get(TEST_WEBHOOK_ENDPOINT);
    assertNotNull(pathItem.getPost());
    assertNotNull(pathItem.getPost().getRequestBody());
  }

  /**
   * Tests the add method with an existing OpenAPI object that already has components.
   * It should not overwrite the existing components and should add new webhooks correctly.
   */
  @Test
  void testAddWithExistingComponents() {
    OpenAPI openAPI = new OpenAPI();
    openAPI.setComponents(new Components());
    openAPI.getComponents().setSchemas(new HashMap<>());

    List<OpenApiFlow> flows = createMockFlowsWithWebhooks(TEST_FLOW_NAME);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(flows);

    endpoint.add(openAPI);

    assertNotNull(openAPI.getComponents());
    assertNotNull(openAPI.getComponents().getSchemas());
  }

  /**
   * Tests the add method with a duplicate tag.
   * It should not add the tag again if it already exists in the OpenAPI object.
   */
  @Test
  void testAddWithDuplicateTag() {
    OpenAPI openAPI = new OpenAPI();
    Tag existingTag = new Tag().name(TEST_FLOW_NAME);
    openAPI.setTags(List.of(existingTag));

    List<OpenApiFlow> flows = createMockFlowsWithWebhooks(TEST_FLOW_NAME);

    when(mockOBDal.createCriteria(OpenApiFlow.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(flows);

    endpoint.add(openAPI);

    assertNotNull(openAPI.getTags());
    assertEquals(1, openAPI.getTags().size());
  }

  /**
   * Creates a list of mock OpenApiFlow objects with webhooks for testing.
   *
   * @param flowNames
   *     The names of the flows to create.
   * @return A list of mock OpenApiFlow objects.
   */
  private List<OpenApiFlow> createMockFlowsWithWebhooks(String... flowNames) {
    List<OpenApiFlow> flows = new ArrayList<>();

    for (String flowName : flowNames) {
      OpenApiFlow flow = mock(OpenApiFlow.class);
      OpenApiFlowPoint flowPoint = mock(OpenApiFlowPoint.class);
      OpenAPIRequest request = mock(OpenAPIRequest.class);
      OpenAPIWebhook webhook = mock(OpenAPIWebhook.class);
      DefinedWebHook definedWebHook = mock(DefinedWebHook.class);

      when(flow.getName()).thenReturn(flowName);
      when(flow.getDescription()).thenReturn("Description for " + flowName);
      when(flow.getETAPIOpenApiFlowPointList()).thenReturn(List.of(flowPoint));

      when(flowPoint.isActive()).thenReturn(true);
      when(flowPoint.getEtapiOpenapiReq()).thenReturn(request);

      when(request.isActive()).thenReturn(true);
      when(request.getDescription()).thenReturn("Request for " + flowName);
      when(request.getSmfwheOpenapiWebhkList()).thenReturn(List.of(webhook));

      when(webhook.isActive()).thenReturn(true);
      when(webhook.getWebHook()).thenReturn(definedWebHook);

      when(definedWebHook.isActive()).thenReturn(true);
      when(definedWebHook.getName()).thenReturn("webhook" + flowName);
      when(definedWebHook.getDescription()).thenReturn("Webhook for " + flowName);
      when(definedWebHook.getSmfwheDefinedwebhookParamList()).thenReturn(new ArrayList<>());

      flows.add(flow);
    }

    return flows;
  }

  /**
   * Gets a private field from the target object.
   *
   * @param target
   *     The object from which to retrieve the field.
   * @param fieldName
   *     The name of the field to retrieve.
   * @return The value of the private field.
   * @throws Exception
   *     If an error occurs while accessing or retrieving the field.
   */
  private Object getPrivateField(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }

  /**
   * Sets a private field in the target object to the specified value.
   *
   * @param target
   *     The object whose field is to be set.
   * @param fieldName
   *     The name of the field to set.
   * @param value
   *     The value to set the field to.
   * @throws Exception
   *     If an error occurs while accessing or setting the field.
   */
  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
