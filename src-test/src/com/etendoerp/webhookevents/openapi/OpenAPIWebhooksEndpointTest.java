package com.etendoerp.webhookevents.openapi;

import static com.etendoerp.webhookevents.openapi.OpenAPIWebhooksEndpoint.DEFAULT_TAG;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.openapi.OpenAPIController;
import com.etendoerp.openapi.data.OpenAPIRequest;
import com.etendoerp.openapi.data.OpenApiFlow;
import com.etendoerp.openapi.data.OpenApiFlowPoint;
import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedWebhookParam;
import com.etendoerp.webhookevents.data.OpenAPIWebhook;

import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Unit tests for the OpenAPIWebhooksEndpoint class.
 */
public class OpenAPIWebhooksEndpointTest extends WeldBaseTest {

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBCriteria mockCriteria;

  @Mock
  private OpenAPI mockOpenAPI;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize mocks
    MockitoAnnotations.openMocks(this);
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }

  /**
   * Tests the getWebhooks method to ensure it retrieves webhooks correctly.
   *
   * @throws OpenApiConfigurationException if an OpenAPI configuration error occurs
   * @throws IOException if an I/O error occurs
   * @throws JSONException if a JSON processing error occurs
   */
  @Test
  public void test_getWebhooks() throws OpenApiConfigurationException, IOException, JSONException {
    List<BaseOBObject> itemsToClean = new ArrayList<>();
    // Given
    DefinedWebHook webhook = OBProvider.getInstance().get(DefinedWebHook.class);
    webhook.setName("MyWebhook");
    webhook.setActive(true);
    webhook.setNewOBObject(true);
    webhook.setJavaClass("com.etendoerp.webhookevents.example.MyWebhook");
    webhook.setDescription("This is a webhooks made for testing");
    OBDal.getInstance().save(webhook);

    DefinedWebhookParam param = OBProvider.getInstance().get(DefinedWebhookParam.class);
    param.setNewOBObject(true);
    param.setRequired(true);
    param.setName("parameterFirst");
    param.setDescription("First parameter");
    param.setSmfwheDefinedwebhook(webhook);
    OBDal.getInstance().save(param);

    OpenAPIRequest req = OBProvider.getInstance().get(OpenAPIRequest.class);
    req.setNewOBObject(true);
    req.setType("SMFWHE_WBHK");
    req.setName("MyWebhookRequest");
    req.setActive(true);
    req.setDescription("My webhook request description.");
    OBDal.getInstance().save(req);

    OpenAPIWebhook webhookEndpointLink = OBProvider.getInstance().get(OpenAPIWebhook.class);
    webhookEndpointLink.setNewOBObject(true);
    webhookEndpointLink.setOpenAPIRequest(req);
    webhookEndpointLink.setWebHook(webhook);
    OBDal.getInstance().save(webhookEndpointLink);

    OpenApiFlow flow = OBProvider.getInstance().get(OpenApiFlow.class);
    flow.setNewOBObject(true);
    flow.setName("TempTestFlow");
    flow.setActive(true);
    flow.setDescription("This is a test flow");
    OBDal.getInstance().save(flow);

    OpenApiFlowPoint flowPoint = OBProvider.getInstance().get(OpenApiFlowPoint.class);
    flowPoint.setNewOBObject(true);
    flowPoint.setEtapiOpenapiFlow(flow);
    flowPoint.setEtapiOpenapiReq(req);
    flowPoint.setPost(true);
    flowPoint.setGet(true);
    OBDal.getInstance().save(flowPoint);

    OBDal.getInstance().flush();

    // When
    String jsonString = new OpenAPIController().getOpenAPIJson(DEFAULT_TAG, "http://localhost:8080");
    JSONObject json = new JSONObject(jsonString);
    assertTrue(json.names().length() > 0);

    // CleanUp
    // Add items to clean, in order
    itemsToClean.add(param);
    itemsToClean.add(webhookEndpointLink);
    itemsToClean.add(webhook);
    itemsToClean.add(req);
    itemsToClean.add(flowPoint);
    itemsToClean.add(flow);
    cleanUpItems(itemsToClean);
  }

  /**
   * Cleans up the specified items after a test.
   *
   * @param itemsToClean the list of items to clean up
   */
  private void cleanUpItems(List<BaseOBObject> itemsToClean) {
    for (BaseOBObject item : itemsToClean) {
      OBDal.getInstance().remove(item);
    }
    OBDal.getInstance().flush();
  }
}