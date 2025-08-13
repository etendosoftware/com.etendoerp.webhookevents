package com.etendoerp.webhookevents.services;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Claim;
import com.etendoerp.webhookevents.data.DefinedWebHook;
import com.etendoerp.webhookevents.data.DefinedwebhookToken;
import com.etendoerp.webhookevents.exceptions.WebhookNotfoundException;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Unit tests for the WebhookServiceHandler class.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class WebhookServiceHandlerTest {

  @InjectMocks
  private WebhookServiceHandler webhookServiceHandler;

  @Mock
  private HttpServletRequest mockRequest;
  @Mock
  private HttpServletResponse mockResponse;
  @Mock
  private PrintWriter mockPrintWriter;
  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private OBCriteria<DefinedwebhookToken> mockTokenCriteria;
  @Mock
  private OBQuery<DefinedWebHook> mockWebhookQuery;
  @Mock
  private OBQuery<UserRoles> mockUserRolesQuery;
  @Mock
  private DefinedwebhookToken mockDefinedwebhookToken;
  @Mock
  private DefinedWebHook mockDefinedWebHook;
  @Mock
  private UserRoles mockUserRoles;
  @Mock
  private User mockUser;
  @Mock
  private Role mockRole;
  @Mock
  private Client mockClient;
  @Mock
  private Organization mockOrganization;
  @Mock
  private Language mockLanguage;
  @Mock
  private DecodedJWT mockDecodedJWT;
  @Mock
  private Claim mockClaim;
  @Mock
  private BaseWebhookService mockBaseWebhookService;
  @Mock
  private BufferedReader mockBufferedReader;
  @Mock
  private OBClassLoader mockOBClassLoader;
  @Mock
  private OBPropertiesProvider mockOBPropertiesProvider;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<SecureWebServicesUtils> mockedSecureWebServicesUtils;
  private MockedStatic<WeldUtils> mockedWeldUtils;
  private MockedStatic<Utility> mockedUtility;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<SessionInfo> mockedSessionInfo;
  private MockedStatic<OBClassLoader> mockedOBClassLoader;
  private MockedStatic<OBPropertiesProvider> mockedOBPropertiesProvider;

  private static final String TEST_WEBHOOK_NAME = "testWebhook";
  private static final String TEST_API_KEY = "testApiKey";
  private static final String TEST_TOKEN = "testToken";
  private static final String TEST_USER_ID = "testUserId";
  private static final String TEST_ROLE_ID = "testRoleId";
  private static final String TEST_CLIENT_ID = "testClientId";
  private static final String TEST_ORG_ID = "testOrgId";
  private static final String TEST_JAVA_CLASS = "com.test.TestWebhookService";
  private static final String TEST_WEBHOOK_PATH = "/testWebhook";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_API_KEY = "apikey";
  private static final String QUERY_NAME_FILTER = "name = :name";
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String LANGUAGE_EN_US = "en_US";

  /**
   * Sets up the test environment before each test method execution.
   * Initializes all static mocks and configures default behavior for common dependencies.
   *
   * @throws Exception
   *     if an error occurs during test setup
   */
  @BeforeEach
  void setUp() throws Exception {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedSecureWebServicesUtils = mockStatic(SecureWebServicesUtils.class);
    mockedWeldUtils = mockStatic(WeldUtils.class);
    mockedUtility = mockStatic(Utility.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedSessionInfo = mockStatic(SessionInfo.class);
    mockedOBClassLoader = mockStatic(OBClassLoader.class);
    mockedOBPropertiesProvider = mockStatic(OBPropertiesProvider.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
    mockedOBContext.when(OBContext::setAdminMode).thenAnswer(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);
    mockedOBClassLoader.when(OBClassLoader::getInstance).thenReturn(mockOBClassLoader);
    mockedOBPropertiesProvider.when(OBPropertiesProvider::getInstance).thenReturn(mockOBPropertiesProvider);

    when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
    when(mockOBContext.getLanguage()).thenReturn(mockLanguage);
    when(mockLanguage.getLanguage()).thenReturn(LANGUAGE_EN_US);
    when(mockOBContext.getRole()).thenReturn(mockRole);
    when(mockRole.getName()).thenReturn("TestRole");
  }

  /**
   * Cleans up after each test by closing all mocked static instances.
   */
  @AfterEach
  void tearDown() {
    if (mockedOBDal != null) mockedOBDal.close();
    if (mockedOBContext != null) mockedOBContext.close();
    if (mockedSecureWebServicesUtils != null) mockedSecureWebServicesUtils.close();
    if (mockedWeldUtils != null) mockedWeldUtils.close();
    if (mockedUtility != null) mockedUtility.close();
    if (mockedOBMessageUtils != null) mockedOBMessageUtils.close();
    if (mockedSessionInfo != null) mockedSessionInfo.close();
    if (mockedOBClassLoader != null) mockedOBClassLoader.close();
    if (mockedOBPropertiesProvider != null) mockedOBPropertiesProvider.close();
  }

  /**
   * Tests the doGet method with a valid webhook configuration.
   * Verifies that the webhook is properly executed and returns a successful response.
   *
   * @throws Exception
   *     if an error occurs during webhook execution
   */
  @Test
  void testDoGetWithValidWebhook() throws Exception {
    setupValidWebhookScenario();
    when(mockRequest.getParameter(PARAM_NAME)).thenReturn(TEST_WEBHOOK_NAME);
    when(mockRequest.getParameter(PARAM_API_KEY)).thenReturn(TEST_API_KEY);
    when(mockRequest.getPathInfo()).thenReturn(TEST_WEBHOOK_PATH);
    when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

    webhookServiceHandler.doGet(mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpStatus.SC_OK);
    verify(mockResponse).addHeader("Content-Type", CONTENT_TYPE_JSON);
    verify(mockPrintWriter).print(any(JSONObject.class));
  }

  /**
   * Tests the doGet method with a webhook that does not exist.
   * It should return a 404 status code.
   */
  @Test
  void testDoGetWithWebhookNotFound() {
    when(mockRequest.getParameter(PARAM_NAME)).thenReturn("nonexistent");
    when(mockRequest.getPathInfo()).thenReturn("/nonexistent");
    when(mockOBDal.createQuery(DefinedWebHook.class, QUERY_NAME_FILTER)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setNamedParameter(PARAM_NAME, "nonexistent")).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setMaxResult(1)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.uniqueResult()).thenReturn(null);

    mockedUtility.when(() -> Utility.messageBD(any(DalConnectionProvider.class),
            eq("smfwhe_actionNotFound"), eq(LANGUAGE_EN_US)))
        .thenReturn("Action not found: %s");

    webhookServiceHandler.doGet(mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpStatus.SC_NOT_FOUND);
  }

  /**
   * Tests the doGet method with a webhook that is not accessible due to lack of permissions.
   * It should return a 401 status code.
   */
  @Test
  void testDoGetWithUnauthorizedAccess() {
    setupWebhookWithoutAccess();
    when(mockRequest.getParameter(PARAM_NAME)).thenReturn(TEST_WEBHOOK_NAME);
    when(mockRequest.getParameter(PARAM_API_KEY)).thenReturn("invalidApiKey");
    when(mockRequest.getPathInfo()).thenReturn(TEST_WEBHOOK_PATH);

    webhookServiceHandler.doGet(mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpStatus.SC_UNAUTHORIZED);
  }

  /**
   * Tests the doPost method with a valid webhook configuration.
   * Verifies that the webhook properly processes POST requests and returns a successful response.
   *
   * @throws Exception
   *     if an error occurs during webhook execution
   */
  @Test
  void testDoPostWithValidWebhook() throws Exception {
    setupValidWebhookScenario();
    when(mockRequest.getParameter(PARAM_NAME)).thenReturn(TEST_WEBHOOK_NAME);
    when(mockRequest.getParameter(PARAM_API_KEY)).thenReturn(TEST_API_KEY);
    when(mockRequest.getPathInfo()).thenReturn(TEST_WEBHOOK_PATH);
    when(mockRequest.getReader()).thenReturn(mockBufferedReader);
    when(mockBufferedReader.lines()).thenReturn(java.util.stream.Stream.of("{\"param1\":\"value1\"}"));

    webhookServiceHandler.doPost(mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpStatus.SC_OK);
    verify(mockResponse).addHeader("Content-Type", CONTENT_TYPE_JSON);
  }

  /**
   * Tests the doPost method with a webhook that does not exist.
   * It should return a 404 status code.
   */
  @Test
  void testDoGetWithDocsPath() {
    when(mockRequest.getPathInfo()).thenReturn("/docs");
    when(mockRequest.getParameter("hooks")).thenReturn(null);
    when(mockRequest.getParameter("host")).thenReturn("localhost");
    when(mockOBDal.createCriteria(DefinedWebHook.class)).thenReturn(mock(OBCriteria.class));
    when(mockOBDal.createCriteria(DefinedWebHook.class).list()).thenReturn(new ArrayList<>());

    webhookServiceHandler.doGet(mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpStatus.SC_OK);
    verify(mockResponse).setHeader("Content-Type", CONTENT_TYPE_JSON);
  }

  /**
   * Tests the checkUserSecurity method with a valid API key.
   * Verifies that the method correctly validates and returns the appropriate token for a valid API key.
   *
   * @throws Exception
   *     if an error occurs during security validation
   */
  @Test
  void testCheckUserSecurityWithValidApiKey() throws Exception {
    setupTokenCriteria();
    when(mockTokenCriteria.uniqueResult()).thenReturn(mockDefinedwebhookToken);
    when(mockDefinedwebhookToken.getUserRole()).thenReturn(mockUserRoles);
    when(mockUserRoles.getUserContact()).thenReturn(mockUser);
    when(mockUserRoles.getRole()).thenReturn(mockRole);
    when(mockUserRoles.getClient()).thenReturn(mockClient);
    when(mockUserRoles.getOrganization()).thenReturn(mockOrganization);
    when(mockUser.getId()).thenReturn(TEST_USER_ID);
    when(mockRole.getId()).thenReturn(TEST_ROLE_ID);
    when(mockClient.getId()).thenReturn(TEST_CLIENT_ID);
    when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);

    var method = WebhookServiceHandler.class.getDeclaredMethod("checkUserSecurity", String.class, String.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, TEST_API_KEY, null);

    assertNotNull(result);
    assertEquals(mockDefinedwebhookToken, result);
  }

  /**
   * Tests the checkUserSecurity method with a valid JWT token.
   * Verifies that the method correctly validates and returns the appropriate token for a valid JWT token.
   *
   * @throws Exception
   *     if an error occurs during token validation or security check
   */
  @Test
  void testCheckUserSecurityWithValidToken() throws Exception {
    setupTokenCriteria();
    when(mockTokenCriteria.uniqueResult()).thenReturn(null);

    mockedSecureWebServicesUtils.when(() -> SecureWebServicesUtils.decodeToken(TEST_TOKEN))
        .thenReturn(mockDecodedJWT);
    when(mockDecodedJWT.getClaim("role")).thenReturn(mockClaim);
    when(mockDecodedJWT.getClaim("user")).thenReturn(mockClaim);
    when(mockClaim.asString()).thenReturn(TEST_ROLE_ID).thenReturn(TEST_USER_ID);

    when(mockOBDal.createQuery(UserRoles.class, "as e where e.role.id = :roleId and e.userContact.id = :userId"))
        .thenReturn(mockUserRolesQuery);
    when(mockUserRolesQuery.setNamedParameter("roleId", TEST_ROLE_ID)).thenReturn(mockUserRolesQuery);
    when(mockUserRolesQuery.setNamedParameter("userId", TEST_USER_ID)).thenReturn(mockUserRolesQuery);
    when(mockUserRolesQuery.setMaxResult(1)).thenReturn(mockUserRolesQuery);
    when(mockUserRolesQuery.uniqueResult()).thenReturn(mockUserRoles);

    setupTokenCriteriaForRoleAccess();

    when(mockUserRoles.getUserContact()).thenReturn(mockUser);
    when(mockUserRoles.getRole()).thenReturn(mockRole);
    when(mockUserRoles.getClient()).thenReturn(mockClient);
    when(mockUserRoles.getOrganization()).thenReturn(mockOrganization);
    when(mockUser.getId()).thenReturn(TEST_USER_ID);
    when(mockRole.getId()).thenReturn(TEST_ROLE_ID);
    when(mockClient.getId()).thenReturn(TEST_CLIENT_ID);
    when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);

    when(mockDefinedwebhookToken.getUserRole()).thenReturn(mockUserRoles);

    var method = WebhookServiceHandler.class.getDeclaredMethod("checkUserSecurity", String.class, String.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, TEST_API_KEY, TEST_TOKEN);

    assertNotNull(result);
  }

  /**
   * Tests the getAction method with a valid webhook name.
   * Verifies that the method correctly retrieves the webhook definition for a valid name.
   *
   * @throws Exception
   *     if an error occurs during webhook retrieval
   */
  @Test
  void testGetActionWithValidName() throws Exception {
    when(mockOBDal.createQuery(DefinedWebHook.class, QUERY_NAME_FILTER)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setNamedParameter(PARAM_NAME, TEST_WEBHOOK_NAME)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setMaxResult(1)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.uniqueResult()).thenReturn(mockDefinedWebHook);

    var method = WebhookServiceHandler.class.getDeclaredMethod("getAction", String.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, TEST_WEBHOOK_NAME);

    assertNotNull(result);
    assertEquals(mockDefinedWebHook, result);
  }

  /**
   * Tests the getAction method with an invalid webhook name.
   * Verifies that the method throws a WebhookNotfoundException when the webhook does not exist.
   *
   * @throws Exception
   *     if an error occurs during webhook retrieval
   */
  @Test
  void testGetActionWithInvalidName() throws Exception {
    // Given
    when(mockOBDal.createQuery(DefinedWebHook.class, QUERY_NAME_FILTER)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setNamedParameter(PARAM_NAME, "invalid")).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setMaxResult(1)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.uniqueResult()).thenReturn(null);

    mockedUtility.when(() -> Utility.messageBD(any(DalConnectionProvider.class),
            eq("smfwhe_actionNotFound"), eq(LANGUAGE_EN_US)))
        .thenReturn("Action not found: %s");

    var method = WebhookServiceHandler.class.getDeclaredMethod("getAction", String.class);
    method.setAccessible(true);

    var exception = assertThrows(InvocationTargetException.class,
        () -> method.invoke(webhookServiceHandler, "invalid"));

    assertInstanceOf(WebhookNotfoundException.class, exception.getCause());
  }

  /**
   * Tests the getInstance method with a valid Java class.
   * Verifies that the method correctly loads and returns an instance of the specified webhook service class.
   *
   * @throws Exception
   *     if an error occurs during class loading or instance creation
   */
  @Test
  void testGetInstanceWithValidClass() throws Exception {
    when(mockOBClassLoader.loadClass(TEST_JAVA_CLASS)).thenReturn((Class) BaseWebhookService.class);
    mockedWeldUtils.when(() -> WeldUtils.getInstanceFromStaticBeanManager(BaseWebhookService.class))
        .thenReturn(mockBaseWebhookService);

    var method = WebhookServiceHandler.class.getDeclaredMethod("getInstance", String.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, TEST_JAVA_CLASS);

    assertNotNull(result);
    assertEquals(mockBaseWebhookService, result);
  }

  /**
   * Tests the buildResponse method with a Map parameter.
   * Verifies that the method correctly builds a JSON response from a Map of response variables.
   *
   * @throws Exception
   *     if an error occurs during response building
   */
  @Test
  void testBuildResponseWithMap() throws Exception {
    Map<String, String> responseVars = new HashMap<>();
    responseVars.put("status", "success");
    responseVars.put("message", "test message");

    var method = WebhookServiceHandler.class.getDeclaredMethod("buildResponse",
        HttpServletResponse.class, int.class, Map.class);
    method.setAccessible(true);
    method.invoke(webhookServiceHandler, mockResponse, HttpStatus.SC_OK, responseVars);

    verify(mockResponse).setStatus(HttpStatus.SC_OK);
    verify(mockResponse).addHeader("Content-Type", CONTENT_TYPE_JSON);
    verify(mockPrintWriter).print(any(JSONObject.class));
  }

  /**
   * Tests the buildResponse method with a String parameter.
   * Verifies that the method correctly builds a JSON response from a String message.
   *
   * @throws Exception
   *     if an error occurs during response building
   */
  @Test
  void testBuildResponseWithString() throws Exception {
    String message = "Test response message";

    var method = WebhookServiceHandler.class.getDeclaredMethod("buildResponse",
        HttpServletResponse.class, int.class, String.class);
    method.setAccessible(true);
    method.invoke(webhookServiceHandler, mockResponse, HttpStatus.SC_OK, message);

    verify(mockResponse).setStatus(HttpStatus.SC_OK);
    verify(mockResponse).addHeader("Content-Type", CONTENT_TYPE_JSON);
    verify(mockPrintWriter).print(any(JSONObject.class));
  }


  /**
   * Tests the getDefinedwebhookRole method with a valid DefinedWebHook and Role.
   * Verifies that the method retrieves the correct DefinedwebhookRole based on the provided parameters.
   *
   * @throws Exception
   *     if an error occurs during role retrieval
   */
  @Test
  void testGetDefinedwebhookRoleWithValidData() throws Exception {
    var mockDefinedwebhookRole = mock(com.etendoerp.webhookevents.data.DefinedwebhookRole.class);
    var mockRoleCriteria = mock(OBCriteria.class);

    when(mockOBDal.createCriteria(com.etendoerp.webhookevents.data.DefinedwebhookRole.class))
        .thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setFilterOnReadableClients(false)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setFilterOnReadableOrganization(false)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.add(any())).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setMaxResults(1)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.uniqueResult()).thenReturn(mockDefinedwebhookRole);

    var method = WebhookServiceHandler.class.getDeclaredMethod("getDefinedwebhookRole",
        DefinedWebHook.class, Role.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, mockDefinedWebHook, mockRole);

    assertNotNull(result);
    assertEquals(mockDefinedwebhookRole, result);
    verify(mockRoleCriteria).setFilterOnReadableClients(false);
    verify(mockRoleCriteria).setFilterOnReadableOrganization(false);
  }

  /**
   * Tests the getDefinedwebhookRole method when no matching role is found.
   * Verifies that the method returns null when there is no matching DefinedwebhookRole.
   *
   * @throws Exception
   *     if an error occurs during role retrieval
   */
  @Test
  void testGetDefinedwebhookRoleWithNoMatch() throws Exception {
    var mockRoleCriteria = mock(OBCriteria.class);

    when(mockOBDal.createCriteria(com.etendoerp.webhookevents.data.DefinedwebhookRole.class))
        .thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setFilterOnReadableClients(false)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setFilterOnReadableOrganization(false)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.add(any())).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setMaxResults(1)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.uniqueResult()).thenReturn(null);

    var method = WebhookServiceHandler.class.getDeclaredMethod("getDefinedwebhookRole",
        DefinedWebHook.class, Role.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, mockDefinedWebHook, mockRole);

    assertNull(result);
  }

  /**
   * Tests the getDefinedwebhookRole method when an exception occurs during role retrieval.
   * Verifies that the method returns null when an exception is thrown.
   *
   * @throws Exception
   *     if an error occurs during role retrieval
   */
  @Test
  void testGetDefinedwebhookRoleWithException() throws Exception {
    when(mockOBDal.createCriteria(com.etendoerp.webhookevents.data.DefinedwebhookRole.class))
        .thenThrow(new RuntimeException("Database error"));

    var method = WebhookServiceHandler.class.getDeclaredMethod("getDefinedwebhookRole",
        DefinedWebHook.class, Role.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, mockDefinedWebHook, mockRole);

    assertNull(result);
  }

  /**
   * Tests the checkRoleSecurity method with a complete token.
   * Verifies that the method correctly decodes the token and retrieves the appropriate DefinedwebhookRole.
   *
   * @throws Exception
   *     if an error occurs during role security check
   */
  @Test
  void testCheckRoleSecurityWithCompleteToken() throws Exception {
    var mockDefinedwebhookRole = mock(com.etendoerp.webhookevents.data.DefinedwebhookRole.class);
    var mockRoleCriteria = mock(OBCriteria.class);

    mockedSecureWebServicesUtils.when(() -> SecureWebServicesUtils.decodeToken(TEST_TOKEN))
        .thenReturn(mockDecodedJWT);

    var userClaim = mock(Claim.class);
    var roleClaim = mock(Claim.class);
    var orgClaim = mock(Claim.class);
    var warehouseClaim = mock(Claim.class);
    var clientClaim = mock(Claim.class);

    when(mockDecodedJWT.getClaim("user")).thenReturn(userClaim);
    when(mockDecodedJWT.getClaim("role")).thenReturn(roleClaim);
    when(mockDecodedJWT.getClaim("organization")).thenReturn(orgClaim);
    when(mockDecodedJWT.getClaim("warehouse")).thenReturn(warehouseClaim);
    when(mockDecodedJWT.getClaim("client")).thenReturn(clientClaim);

    when(userClaim.asString()).thenReturn(TEST_USER_ID);
    when(roleClaim.asString()).thenReturn(TEST_ROLE_ID);
    when(orgClaim.asString()).thenReturn(TEST_ORG_ID);
    when(warehouseClaim.asString()).thenReturn("testWarehouseId");
    when(clientClaim.asString()).thenReturn(TEST_CLIENT_ID);

    mockedSecureWebServicesUtils.when(() -> SecureWebServicesUtils.createContext(
            TEST_USER_ID, TEST_ROLE_ID, TEST_ORG_ID, "testWarehouseId", TEST_CLIENT_ID))
        .thenReturn(mockOBContext);

    when(mockOBDal.get(Role.class, TEST_ROLE_ID)).thenReturn(mockRole);

    when(mockOBDal.createCriteria(com.etendoerp.webhookevents.data.DefinedwebhookRole.class))
        .thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setFilterOnReadableClients(false)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setFilterOnReadableOrganization(false)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.add(any())).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.setMaxResults(1)).thenReturn(mockRoleCriteria);
    when(mockRoleCriteria.uniqueResult()).thenReturn(mockDefinedwebhookRole);

    var method = WebhookServiceHandler.class.getDeclaredMethod("checkRoleSecurity",
        HttpServletRequest.class, String.class, DefinedWebHook.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, mockRequest, TEST_TOKEN, mockDefinedWebHook);

    assertNotNull(result);
    assertEquals(mockDefinedwebhookRole, result);
    mockedSessionInfo.verify(() -> SessionInfo.setUserId(TEST_USER_ID));
    mockedSessionInfo.verify(() -> SessionInfo.setProcessType("WS"));
    mockedSessionInfo.verify(() -> SessionInfo.setProcessId("DAL"));
  }

  /**
   * Tests the checkRoleSecurity method with invalid token claims.
   * Verifies that the method returns null when the token claims are empty or invalid.
   *
   * @throws Exception
   *     if an error occurs during role security check
   */
  @Test
  void testCheckRoleSecurityWithInvalidTokenClaims() throws Exception {
    mockedSecureWebServicesUtils.when(() -> SecureWebServicesUtils.decodeToken(TEST_TOKEN))
        .thenReturn(mockDecodedJWT);

    var emptyClaim = mock(Claim.class);
    when(emptyClaim.asString()).thenReturn("");

    when(mockDecodedJWT.getClaim("user")).thenReturn(emptyClaim);
    when(mockDecodedJWT.getClaim("role")).thenReturn(emptyClaim);
    when(mockDecodedJWT.getClaim("organization")).thenReturn(emptyClaim);
    when(mockDecodedJWT.getClaim("warehouse")).thenReturn(emptyClaim);
    when(mockDecodedJWT.getClaim("client")).thenReturn(emptyClaim);

    var method = WebhookServiceHandler.class.getDeclaredMethod("checkRoleSecurity",
        HttpServletRequest.class, String.class, DefinedWebHook.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, mockRequest, TEST_TOKEN, mockDefinedWebHook);

    assertNull(result);
  }

  /**
   * Tests the checkRoleSecurity method when token decoding fails.
   * Verifies that the method returns null when JWT decoding throws an exception.
   */
  @Test
  void testCheckRoleSecurityWithTokenDecodingFailure() throws Exception {
    mockedSecureWebServicesUtils.when(() -> SecureWebServicesUtils.decodeToken(TEST_TOKEN))
        .thenThrow(new RuntimeException("Invalid token"));

    var method = WebhookServiceHandler.class.getDeclaredMethod("checkRoleSecurity",
        HttpServletRequest.class, String.class, DefinedWebHook.class);
    method.setAccessible(true);
    var result = method.invoke(webhookServiceHandler, mockRequest, TEST_TOKEN, mockDefinedWebHook);

    assertNull(result);
  }

  /**
   * Tests the doPost method with an invalid JSON body.
   * Verifies that the method handles invalid JSON input and returns an appropriate error response.
   *
   * @throws Exception
   *     if an error occurs during webhook execution
   */
  @Test
  void testDoPostWithInvalidJsonBody() throws Exception {
    // Setup
    setupValidWebhookWithoutAccessCheck();
    when(mockRequest.getParameter(PARAM_NAME)).thenReturn(TEST_WEBHOOK_NAME);
    when(mockRequest.getParameter(PARAM_API_KEY)).thenReturn(TEST_API_KEY);
    when(mockRequest.getPathInfo()).thenReturn(TEST_WEBHOOK_PATH);
    when(mockRequest.getReader()).thenReturn(mockBufferedReader);
    when(mockBufferedReader.lines()).thenReturn(java.util.stream.Stream.of("invalid json"));

    mockedUtility.when(() -> Utility.messageBD(any(DalConnectionProvider.class),
            eq("smfwhe_cannotCollectData"), eq(LANGUAGE_EN_US)))
        .thenReturn("Cannot collect data");

    webhookServiceHandler.doPost(mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  /**
   * Tests the doGet method with missing required parameters.
   * Verifies that the method handles missing parameters appropriately.
   */
  @Test
  void testDoGetWithMissingRequiredParams() {
    var mockParam = mock(com.etendoerp.webhookevents.data.DefinedWebhookParam.class);
    var paramList = List.of(mockParam);

    setupValidWebhookWithParams(paramList);
    when(mockParam.getName()).thenReturn("requiredParam");
    when(mockParam.isRequired()).thenReturn(true);

    when(mockRequest.getParameter(PARAM_NAME)).thenReturn(TEST_WEBHOOK_NAME);
    when(mockRequest.getParameter(PARAM_API_KEY)).thenReturn(TEST_API_KEY);
    when(mockRequest.getPathInfo()).thenReturn(TEST_WEBHOOK_PATH);
    when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());

    mockedUtility.when(() -> Utility.messageBD(any(DalConnectionProvider.class),
            eq("smfwhe_missingParameter"), eq(LANGUAGE_EN_US)))
        .thenReturn("Missing parameter: %s");

    webhookServiceHandler.doGet(mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  /**
   * Tests the handleDocs method with specific webhook list.
   * Verifies that the method generates OpenAPI documentation for specified webhooks.
   */
  @Test
  void testHandleDocsWithSpecificWebhooks() {
    when(mockRequest.getPathInfo()).thenReturn("/docs");
    when(mockRequest.getParameter("hooks")).thenReturn("webhook1,webhook2");
    when(mockRequest.getParameter("host")).thenReturn("test.example.com");

    var mockWebhook1 = mock(DefinedWebHook.class);
    var mockWebhook2 = mock(DefinedWebHook.class);
    var webhookList = List.of(mockWebhook1, mockWebhook2);

    var mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(DefinedWebHook.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any())).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(webhookList);

    when(mockWebhook1.getName()).thenReturn("webhook1");
    when(mockWebhook1.getDescription()).thenReturn("Description 1");
    when(mockWebhook1.getJavaClass()).thenReturn("com.test.Webhook1");
    when(mockWebhook1.getSmfwheDefinedwebhookParamList()).thenReturn(new ArrayList<>());

    when(mockWebhook2.getName()).thenReturn("webhook2");
    when(mockWebhook2.getDescription()).thenReturn("Description 2");
    when(mockWebhook2.getJavaClass()).thenReturn("com.test.Webhook2");
    when(mockWebhook2.getSmfwheDefinedwebhookParamList()).thenReturn(new ArrayList<>());

    webhookServiceHandler.doGet(mockRequest, mockResponse);

    verify(mockResponse).setStatus(HttpStatus.SC_OK);
    verify(mockResponse).setHeader("Content-Type", CONTENT_TYPE_JSON);
    verify(mockPrintWriter).print(any(String.class));
  }

  /**
   * Helper method to setup a valid webhook scenario without access check complications.
   */
  private void setupValidWebhookWithoutAccessCheck() {
    when(mockOBDal.createQuery(DefinedWebHook.class, QUERY_NAME_FILTER)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setNamedParameter(PARAM_NAME, TEST_WEBHOOK_NAME)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setMaxResult(1)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.uniqueResult()).thenReturn(mockDefinedWebHook);
    when(mockDefinedWebHook.getJavaClass()).thenReturn(TEST_JAVA_CLASS);
    when(mockDefinedWebHook.getName()).thenReturn(TEST_WEBHOOK_NAME);
    when(mockDefinedWebHook.getSmfwheDefinedwebhookParamList()).thenReturn(new ArrayList<>());
    when(mockDefinedWebHook.getSmfwheDefinedwebhookAccessList()).thenReturn(new ArrayList<>());
    when(mockDefinedWebHook.isAllowGroupAccess()).thenReturn(true);

    setupTokenCriteria();
    when(mockTokenCriteria.uniqueResult()).thenReturn(mockDefinedwebhookToken);
    when(mockDefinedwebhookToken.getUserRole()).thenReturn(mockUserRoles);
    setupUserRolesAndContext();
  }

  /**
   * Helper method to setup a valid webhook with parameters.
   */
  private void setupValidWebhookWithParams(List<com.etendoerp.webhookevents.data.DefinedWebhookParam> params) {
    when(mockOBDal.createQuery(DefinedWebHook.class, QUERY_NAME_FILTER)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setNamedParameter(PARAM_NAME, TEST_WEBHOOK_NAME)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setMaxResult(1)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.uniqueResult()).thenReturn(mockDefinedWebHook);
    when(mockDefinedWebHook.getJavaClass()).thenReturn(TEST_JAVA_CLASS);
    when(mockDefinedWebHook.getName()).thenReturn(TEST_WEBHOOK_NAME);
    when(mockDefinedWebHook.getSmfwheDefinedwebhookParamList()).thenReturn(params);
    when(mockDefinedWebHook.getSmfwheDefinedwebhookAccessList()).thenReturn(new ArrayList<>());
    when(mockDefinedWebHook.isAllowGroupAccess()).thenReturn(true);

    setupTokenCriteria();
    when(mockTokenCriteria.uniqueResult()).thenReturn(mockDefinedwebhookToken);
    when(mockDefinedwebhookToken.getUserRole()).thenReturn(mockUserRoles);
    setupUserRolesAndContext();
  }

  /**
   * Helper method to setup user roles and context.
   */
  private void setupUserRolesAndContext() {
    when(mockUserRoles.getUserContact()).thenReturn(mockUser);
    when(mockUserRoles.getRole()).thenReturn(mockRole);
    when(mockUserRoles.getClient()).thenReturn(mockClient);
    when(mockUserRoles.getOrganization()).thenReturn(mockOrganization);
    when(mockUser.getId()).thenReturn(TEST_USER_ID);
    when(mockRole.getId()).thenReturn(TEST_ROLE_ID);
    when(mockClient.getId()).thenReturn(TEST_CLIENT_ID);
    when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);
  }

  /**
   * Sets up the scenario where a valid webhook is configured.
   * This method simulates the case where a webhook with the specified name exists and is accessible.
   */
  private void setupValidWebhookScenario() throws Exception {
    when(mockOBDal.createQuery(DefinedWebHook.class, QUERY_NAME_FILTER)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setNamedParameter(PARAM_NAME, TEST_WEBHOOK_NAME)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setMaxResult(1)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.uniqueResult()).thenReturn(mockDefinedWebHook);
    when(mockDefinedWebHook.getJavaClass()).thenReturn(TEST_JAVA_CLASS);
    when(mockDefinedWebHook.getName()).thenReturn(TEST_WEBHOOK_NAME);
    when(mockDefinedWebHook.getSmfwheDefinedwebhookParamList()).thenReturn(new ArrayList<>());
    when(mockDefinedWebHook.getSmfwheDefinedwebhookAccessList()).thenReturn(new ArrayList<>());

    setupTokenCriteria();
    when(mockTokenCriteria.uniqueResult()).thenReturn(mockDefinedwebhookToken);
    when(mockDefinedwebhookToken.getUserRole()).thenReturn(mockUserRoles);
    when(mockUserRoles.getUserContact()).thenReturn(mockUser);
    when(mockUserRoles.getRole()).thenReturn(mockRole);
    when(mockUserRoles.getClient()).thenReturn(mockClient);
    when(mockUserRoles.getOrganization()).thenReturn(mockOrganization);
    when(mockUser.getId()).thenReturn(TEST_USER_ID);
    when(mockRole.getId()).thenReturn(TEST_ROLE_ID);
    when(mockClient.getId()).thenReturn(TEST_CLIENT_ID);
    when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);

    var mockAccessList = List.of(mock(com.etendoerp.webhookevents.data.DefinedwebhookAccess.class));
    when(mockDefinedWebHook.getSmfwheDefinedwebhookAccessList()).thenReturn(mockAccessList);
    when(mockAccessList.get(0).getSmfwheDefinedwebhookToken()).thenReturn(mockDefinedwebhookToken);
    when(mockDefinedwebhookToken.getId()).thenReturn("tokenId");

    when(mockOBClassLoader.loadClass(TEST_JAVA_CLASS)).thenReturn((Class) BaseWebhookService.class);
    mockedWeldUtils.when(() -> WeldUtils.getInstanceFromStaticBeanManager(BaseWebhookService.class))
        .thenReturn(mockBaseWebhookService);

    doAnswer(invocation -> {
      Map<String, String> responseVars = invocation.getArgument(1);
      responseVars.put("status", "success");
      return null;
    }).when(mockBaseWebhookService).get(any(Map.class), any(Map.class));
  }

  /**
   * Sets up the scenario where a webhook is accessed without the necessary permissions.
   * This method simulates the case where a user tries to access a webhook that does not allow group access.
   */
  private void setupWebhookWithoutAccess() {
    when(mockOBDal.createQuery(DefinedWebHook.class, QUERY_NAME_FILTER)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setNamedParameter(PARAM_NAME, TEST_WEBHOOK_NAME)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.setMaxResult(1)).thenReturn(mockWebhookQuery);
    when(mockWebhookQuery.uniqueResult()).thenReturn(mockDefinedWebHook);
    when(mockDefinedWebHook.getName()).thenReturn(TEST_WEBHOOK_NAME);
    when(mockDefinedWebHook.isAllowGroupAccess()).thenReturn(false);

    setupTokenCriteria();
    when(mockTokenCriteria.uniqueResult()).thenReturn(null);

    when(mockDefinedWebHook.getSmfwheDefinedwebhookAccessList()).thenReturn(new ArrayList<>());

    mockedUtility.when(() -> Utility.messageBD(any(DalConnectionProvider.class),
            eq("smfwhe_unauthorizedToken"), eq(LANGUAGE_EN_US)))
        .thenReturn("Unauthorized token");
  }

  /**
   * Sets up the token criteria for user-based access.
   * This method is used to simulate the scenario where a user token is used for access control.
   */
  private void setupTokenCriteria() {
    when(mockOBDal.createCriteria(DefinedwebhookToken.class)).thenReturn(mockTokenCriteria);
    when(mockTokenCriteria.setFilterOnReadableClients(false)).thenReturn(mockTokenCriteria);
    when(mockTokenCriteria.setFilterOnReadableOrganization(false)).thenReturn(mockTokenCriteria);
    when(mockTokenCriteria.add(any())).thenReturn(mockTokenCriteria);
    when(mockTokenCriteria.setMaxResults(1)).thenReturn(mockTokenCriteria);
  }

  /**
   * Sets up the token criteria for role-based access.
   * This method is used to simulate the scenario where a role token is used for access control.
   */
  private void setupTokenCriteriaForRoleAccess() {
    var mockRoleTokenCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(DefinedwebhookToken.class))
        .thenReturn(mockTokenCriteria)
        .thenReturn(mockRoleTokenCriteria);
    when(mockRoleTokenCriteria.setFilterOnReadableClients(false)).thenReturn(mockRoleTokenCriteria);
    when(mockRoleTokenCriteria.setFilterOnReadableOrganization(false)).thenReturn(mockRoleTokenCriteria);
    when(mockRoleTokenCriteria.add(any())).thenReturn(mockRoleTokenCriteria);
    when(mockRoleTokenCriteria.setMaxResults(1)).thenReturn(mockRoleTokenCriteria);
    when(mockRoleTokenCriteria.uniqueResult()).thenReturn(mockDefinedwebhookToken);
  }

}
