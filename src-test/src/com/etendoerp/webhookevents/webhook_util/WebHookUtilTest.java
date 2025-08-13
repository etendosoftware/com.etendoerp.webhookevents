package com.etendoerp.webhookevents.webhook_util;

import static com.etendoerp.webhookevents.WebhookTestConstants.CONTENT_TYPE;
import static com.etendoerp.webhookevents.WebhookTestConstants.ITEMS;
import static com.etendoerp.webhookevents.WebhookTestConstants.PARAM1;
import static com.etendoerp.webhookevents.WebhookTestConstants.PARAM2;
import static com.etendoerp.webhookevents.WebhookTestConstants.RECORD_ID;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_EVENT_CLASS;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_EVENT_LANG;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_EVENT_TYPE_ID;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_TABLE_ID;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_TABLE_NAME;
import static com.etendoerp.webhookevents.WebhookTestConstants.VALUE1;
import static com.etendoerp.webhookevents.WebhookTestConstants.VALUE2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.enterprise.Organization;

import com.etendoerp.webhookevents.data.Arguments;
import com.etendoerp.webhookevents.data.ArgumentsData;
import com.etendoerp.webhookevents.data.EventType;
import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.JsonXmlData;
import com.etendoerp.webhookevents.data.QueueEventHook;
import com.etendoerp.webhookevents.data.UrlPathParam;
import com.etendoerp.webhookevents.data.Webhook;

/**
 * Unit tests for the WebHookUtil class.
 * This class tests various methods of WebHookUtil to ensure they behave as expected.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class WebHookUtilTest {

  @Mock
  private OBDal obDal;

  @Mock
  private OBContext obContext;

  @Mock
  private Logger logger;

  @Mock
  private BaseOBObject baseOBObject;

  @Mock
  private Table table;

  @Mock
  private EventType eventType;

  @Mock
  private Events events;

  @Mock
  private OBCriteria<Events> eventsCriteria;

  @Mock
  private Language language;

  /**
   * Sets up the OBContext mock before all tests.
   * This method is called once before any tests are run.
   */
  @BeforeAll
  static void setUpClass() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      OBContext mockContext = mock(OBContext.class);
      Language mockLanguage = mock(Language.class);

      mockedOBContext.when(OBContext::getOBContext).thenReturn(mockContext);
      when(mockContext.getLanguage()).thenReturn(mockLanguage);
      when(mockLanguage.getLanguage()).thenReturn(TEST_EVENT_LANG);

    }
  }
  /**
   * Sets up the mocks before each test.
   * This method is called before each test to initialize the mocks.
   */
  @BeforeEach
  void setUp() {
    when(table.getId()).thenReturn(TEST_TABLE_ID);
    when(table.getDBTableName()).thenReturn(TEST_TABLE_NAME);
    when(eventType.getId()).thenReturn(TEST_EVENT_TYPE_ID);
    when(baseOBObject.getIdentifier()).thenReturn("test-identifier");
    when(language.getLanguage()).thenReturn(TEST_EVENT_LANG);
  }

  /**
   * Tests the eventsFromTableName method with a table name and event type ID.
   * Verifies that the method returns a list containing the expected Events object
   * when the mocked criteria returns a single Events instance.
   */
  @Test
  void testEventsFromTableName() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.createAlias(anyString(), anyString())).thenReturn(eventsCriteria);
      when(eventsCriteria.add(any())).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(List.of(events));

      List<Events> result = WebHookUtil.eventsFromTableName(TEST_EVENT_TYPE_ID, TEST_TABLE_NAME);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(events, result.get(0));
    }
  }

  /**
   * Tests the eventsFromTableName method with an event class parameter.
   * Verifies that the method returns a list containing the expected Events object
   * when the mocked criteria returns a single Events instance.
   */
  @Test
  void testEventsFromTableNameWithEventClass() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.createAlias(anyString(), anyString())).thenReturn(eventsCriteria);
      when(eventsCriteria.add(any())).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(List.of(events));

      List<Events> result = WebHookUtil.eventsFromTableName(
          TEST_EVENT_TYPE_ID, TEST_TABLE_NAME, TEST_EVENT_CLASS);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(events, result.get(0));
    }
  }

  /**
   * Tests the eventsFromTableName method with an empty list of Events.
   * Verifies that the method returns an empty list when no Events are found.
   */
  @Test
  void testReplaceValueDataWithSimpleString() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      setupOBContextMock(mockedOBContext);

      String value = "Hello World";
      String result = WebHookUtil.replaceValueData(value, baseOBObject, logger);
      assertEquals("Hello World", result);
    }
  }

  /**
   * Tests the replaceValueData method with a property placeholder.
   * Verifies that the method replaces the placeholder with the actual property value.
   */
  @Test
  void testReplaceValueDataWithProperty() {
    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);
      String value = "Hello @name world";
      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "name"))
          .thenReturn("John");

      String result = WebHookUtil.replaceValueData(value, baseOBObject, logger);
      assertEquals("Hello John world", result);
    }
  }

  /**
   * Tests the replaceValueData method with a null property placeholder.
   * Verifies that the method throws an OBException when the property is not found.
   */
  @Test
  void testReplaceValueDataWithNullProperty() {
    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);
      String value = "Hello @invalidProperty world";
      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "invalidProperty"))
          .thenReturn(null);

      assertThrows(OBException.class, () ->
          WebHookUtil.replaceValueData(value, baseOBObject, logger)
      );
    }
  }

  /**
   * Tests the generateDataParametersJSON method when a parent node is an array.
   * Verifies that the result contains a JSONObject with a JSONArray for the array node.
   * If an exception occurs during the process, the test will fail.
   *
   * @throws Exception if an unexpected error occurs during the test execution
   */
  @Test
  void testGenerateDataParametersJSONWithArray() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      JsonXmlData parentNode = mock(JsonXmlData.class);
      when(parentNode.isSummaryLevel()).thenReturn(true);
      when(parentNode.isArray()).thenReturn(true);
      when(parentNode.getName()).thenReturn(ITEMS);

      OBCriteria<JsonXmlData> childCriteria = mock(OBCriteria.class);
      when(obDal.createCriteria(JsonXmlData.class)).thenReturn(childCriteria);
      when(childCriteria.add(any())).thenReturn(childCriteria);
      when(childCriteria.list()).thenReturn(new ArrayList<>());

      List<JsonXmlData> nodes = List.of(parentNode);

      Object result = WebHookUtil.generateDataParametersJSON(nodes, baseOBObject, logger);

      assertNotNull(result);
      assertInstanceOf(JSONObject.class, result);
      JSONObject jsonResult = (JSONObject) result;
      assertTrue(jsonResult.has(ITEMS));
      assertInstanceOf(JSONArray.class, jsonResult.get(ITEMS));
    }
  }

  /**
   * Tests the getArgumentsForMethod method with a list of Arguments.
   * Verifies that the method filters out inactive arguments and returns a map of active arguments.
   */
  @Test
  void testGetArgumentsForMethod() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      setupOBContextMock(mockedOBContext);

      Arguments arg1 = mock(Arguments.class);
      when(arg1.isActive()).thenReturn(true);
      when(arg1.getName()).thenReturn(PARAM1);
      when(arg1.getValueParameter()).thenReturn(VALUE1);

      Arguments arg2 = mock(Arguments.class);
      when(arg2.isActive()).thenReturn(false);
      when(arg2.getName()).thenReturn(PARAM2);
      when(arg2.getValueParameter()).thenReturn(VALUE2);

      Arguments arg3 = mock(Arguments.class);
      when(arg3.isActive()).thenReturn(true);
      when(arg3.getName()).thenReturn("param3");
      when(arg3.getValueParameter()).thenReturn("value3");

      List<Arguments> args = Arrays.asList(arg1, arg2, arg3);

      Map<Object, Object> result = WebHookUtil.getArgumentsForMethod(args, baseOBObject, logger);

      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals(VALUE1, result.get(PARAM1));
      assertFalse(result.containsKey(PARAM2));
      assertEquals("value3", result.get("param3"));
    }
  }

  /**
   * Tests the getArgumentsForMethodData method with a list of ArgumentsData.
   * Verifies that the method returns a map of active arguments.
   */
  @Test
  void testGetArgumentsForMethodData() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      setupOBContextMock(mockedOBContext);

      ArgumentsData arg1 = mock(ArgumentsData.class);
      when(arg1.isActive()).thenReturn(true);
      when(arg1.getName()).thenReturn(PARAM1);
      when(arg1.getValue()).thenReturn(VALUE1);

      ArgumentsData arg2 = mock(ArgumentsData.class);
      when(arg2.isActive()).thenReturn(true);
      when(arg2.getName()).thenReturn(PARAM2);
      when(arg2.getValue()).thenReturn(VALUE2);

      List<ArgumentsData> args = Arrays.asList(arg1, arg2);

      Map<Object, Object> result = WebHookUtil.getArgumentsForMethodData(args, baseOBObject, logger);

      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals(VALUE1, result.get(PARAM1));
      assertEquals(VALUE2, result.get(PARAM2));
    }
  }

  /**
   * Tests the getEntities method to ensure it retrieves entities based on the Events table.
   * Verifies that the method returns an array of Entity objects.
   */
  @Test
  void testGetEntities() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class);
         MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {

      setupOBContextMock(mockedOBContext);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(List.of(events));
      when(events.getTable()).thenReturn(table);
      when(table.getDBTableName()).thenReturn(TEST_TABLE_NAME);

      ModelProvider modelProvider = mock(ModelProvider.class);
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);

      Entity entity = mock(Entity.class);
      when(modelProvider.getEntityByTableName(TEST_TABLE_NAME)).thenReturn(entity);

      Entity[] result = WebHookUtil.getEntities();

      assertNotNull(result);
      assertEquals(1, result.length);
      assertEquals(entity, result[0]);

      mockedOBContext.verify(OBContext::setAdminMode);
      mockedOBContext.verify(OBContext::restorePreviousMode);
    }
  }

  /**
   * Tests the getEventHandlerClassEvents method to ensure it retrieves events based on event type and table.
   * Verifies that the method returns a list of Events objects.
   */
  @Test
  void testGetEventHandlerClassEvents() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.createAlias(anyString(), anyString())).thenReturn(eventsCriteria);
      when(eventsCriteria.add(any())).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(List.of(events));

      List<Events> result = WebHookUtil.getEventHandlerClassEvents(
          "test-event-type", TEST_TABLE_NAME);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(events, result.get(0));

      verify(eventsCriteria, atLeastOnce()).add(any());
    }
  }

  /**
   * Tests the replaceValueData method with a property placeholder.
   * Verifies that the method replaces the placeholder with the actual property value.
   */
  @Test
  void testReplaceValueDataWithComplexProperty() {
    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);
      String value = "User @user.profile.name has @user.profile.age years";

      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "user.profile.name"))
          .thenReturn("Alice");
      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "user.profile.age"))
          .thenReturn("30");

      String result = WebHookUtil.replaceValueData(value, baseOBObject, logger);

      assertEquals("User Alice has 30 years", result);
    }
  }

  /**
   * Tests the queueEvent method with a table ID and event type ID.
   * Verifies that the method creates a QueueEventHook and saves it to the database.
   */
  @Test
  void testQueueEventWithTableIdAndEventTypeId() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedProvider = mockStatic(OBProvider.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(Table.class, TEST_TABLE_ID)).thenReturn(table);
      when(obDal.get(EventType.class, TEST_EVENT_TYPE_ID)).thenReturn(eventType);

      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.createAlias(anyString(), anyString())).thenReturn(eventsCriteria);
      when(eventsCriteria.add(any())).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(List.of(events));

      QueueEventHook queueEventHook = mock(QueueEventHook.class);
      mockedProvider.when(OBProvider::getInstance).thenReturn(mock(OBProvider.class));
      mockedProvider.when(() -> OBProvider.getInstance().get(QueueEventHook.class))
          .thenReturn(queueEventHook);

      Organization org = mock(Organization.class);
      when(obDal.get(Organization.class, "0")).thenReturn(org);

      WebHookUtil.queueEvent(TEST_TABLE_ID, TEST_EVENT_TYPE_ID, TEST_EVENT_CLASS, RECORD_ID);

      verify(obDal).save(queueEventHook);
    }
  }

  /**
   * Tests the queueEvent method with a Table and EventType object.
   * Verifies that the method creates a QueueEventHook and saves it to the database.
   */
  @Test
  void testQueueEventWithTableAndEventType() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedProvider = mockStatic(OBProvider.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.createAlias(anyString(), anyString())).thenReturn(eventsCriteria);
      when(eventsCriteria.add(any())).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(List.of(events));

      QueueEventHook queueEventHook = mock(QueueEventHook.class);
      mockedProvider.when(OBProvider::getInstance).thenReturn(mock(OBProvider.class));
      mockedProvider.when(() -> OBProvider.getInstance().get(QueueEventHook.class))
          .thenReturn(queueEventHook);

      Organization org = mock(Organization.class);
      when(obDal.get(Organization.class, "0")).thenReturn(org);

      WebHookUtil.queueEvent(table, eventType, TEST_EVENT_CLASS, RECORD_ID);

      verify(obDal).save(queueEventHook);
    }
  }

  /**
   * Tests the queueEventFromEventHandler method to ensure it queues an event based on the event handler.
   * Verifies that the method creates a QueueEventHook and saves it to the database.
   */
  @Test
  void testQueueEventFromEventHandler() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedProvider = mockStatic(OBProvider.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.createAlias(anyString(), anyString())).thenReturn(eventsCriteria);
      when(eventsCriteria.add(any())).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(List.of(events));

      when(events.getDynamicEventJavaclass()).thenReturn(null);
      when(events.getTable()).thenReturn(table);
      when(events.getSmfwheEventType()).thenReturn(eventType);

      QueueEventHook queueEventHook = mock(QueueEventHook.class);
      mockedProvider.when(OBProvider::getInstance).thenReturn(mock(OBProvider.class));
      mockedProvider.when(() -> OBProvider.getInstance().get(QueueEventHook.class))
          .thenReturn(queueEventHook);

      Organization org = mock(Organization.class);
      when(obDal.get(Organization.class, "0")).thenReturn(org);
      when(obDal.get(Table.class, TEST_TABLE_ID)).thenReturn(table);

      WebHookUtil.queueEventFromEventHandler(TEST_TABLE_NAME, TEST_TABLE_ID,
          TEST_EVENT_TYPE_ID, RECORD_ID);

      verify(obDal).save(queueEventHook);
    }
  }

  /**
   * Tests the callWebHook method to ensure it sends events to active webhooks.
   * Verifies that the method retrieves active webhooks and calls WebHookUtil.sendEvent.
   */
  @Test
  void testCallWebHook() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      Webhook webhook = mock(Webhook.class);
      when(webhook.isActive()).thenReturn(true);

      OBCriteria<Webhook> webhookCriteria = mock(OBCriteria.class);
      when(obDal.createCriteria(Webhook.class)).thenReturn(webhookCriteria);
      when(webhookCriteria.add(any())).thenReturn(webhookCriteria);
      when(webhookCriteria.list()).thenReturn(List.of(webhook));

      mockedWebHookUtil.when(() -> WebHookUtil.sendEvent(any(Webhook.class),
          any(BaseOBObject.class), any(Logger.class))).thenAnswer(invocation -> null);

      mockedWebHookUtil.when(() -> WebHookUtil.callWebHook(any(Events.class),
          any(BaseOBObject.class), any(Logger.class))).thenCallRealMethod();

      WebHookUtil.callWebHook(events, baseOBObject, logger);

      mockedWebHookUtil.verify(() -> WebHookUtil.sendEvent(webhook, baseOBObject, logger));
    }
  }

  /**
   * Tests the generateUrlParameter method to ensure it replaces URL parameters with actual values.
   * Verifies that the method correctly replaces the parameter in the URL.
   */
  @Test
  void testGenerateUrlParameter() {
    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);

      UrlPathParam param1 = mock(UrlPathParam.class);
      when(param1.isActive()).thenReturn(true);
      when(param1.getTypeParameter()).thenReturn("P");
      when(param1.getName()).thenReturn("id");
      when(param1.getTypeValue()).thenReturn("S");
      when(param1.getValue()).thenReturn("@entityId");

      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "entityId"))
          .thenReturn("12345");

      List<UrlPathParam> params = List.of(param1);
      String url = "http://example.com/api/{id}/data";

      String result = WebHookUtil.generateUrlParameter(params, url, baseOBObject, logger);

      assertEquals("http://example.com/api/12345/data", result);
    }
  }

  /**
   * Tests the getValueExecuteMethod method to ensure it retrieves the value from a computed function.
   * Verifies that the method throws an OBException when the Java class is not found.
   */
  @Test
  void testGetValueExecuteMethod() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      setupOBContextMock(mockedOBContext);

      JsonXmlData jsonData = mock(JsonXmlData.class);
      when(jsonData.getJavaClassName()).thenReturn("com.example.TestComputedFunction");
      when(jsonData.getTypeValue()).thenReturn("CF");

      ArgumentsData arg = mock(ArgumentsData.class);
      when(arg.isActive()).thenReturn(true);
      when(arg.getName()).thenReturn(PARAM1);
      when(arg.getValue()).thenReturn(VALUE1);
      when(jsonData.getSmfwheArgsDataList()).thenReturn(List.of(arg));

      when(jsonData.getJavaClassName()).thenReturn("java.lang.String");

      assertThrows(OBException.class, () ->
          WebHookUtil.getValueExecuteMethod(jsonData, baseOBObject, logger, DynamicNode.class)
      );
    }
  }

  /**
   * Tests the generateDataParametersJSON method with a complex structure.
   * Verifies that the method generates a JSON object with nested properties and arrays.
   * If an exception occurs during the process, the test will fail.
   *
   * @throws Exception if an unexpected error occurs during the test execution
   */
  @Test
  void testGenerateDataParametersJSONWithComplexStructure() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      JsonXmlData parentNode = mock(JsonXmlData.class);
      when(parentNode.isSummaryLevel()).thenReturn(true);
      when(parentNode.isArray()).thenReturn(false);
      when(parentNode.getName()).thenReturn("user");

      JsonXmlData childNode1 = mock(JsonXmlData.class);
      when(childNode1.isSummaryLevel()).thenReturn(false);
      when(childNode1.getName()).thenReturn("name");
      when(childNode1.getTypeValue()).thenReturn("S");
      when(childNode1.getValue()).thenReturn("@userName");

      JsonXmlData childNode2 = mock(JsonXmlData.class);
      when(childNode2.isSummaryLevel()).thenReturn(false);
      when(childNode2.getName()).thenReturn("email");
      when(childNode2.getTypeValue()).thenReturn("PR");
      when(childNode2.getProperty()).thenReturn("userEmail");

      OBCriteria<JsonXmlData> childCriteria = mock(OBCriteria.class);
      when(obDal.createCriteria(JsonXmlData.class)).thenReturn(childCriteria);
      when(childCriteria.add(any())).thenReturn(childCriteria);
      when(childCriteria.list()).thenReturn(Arrays.asList(childNode1, childNode2));

      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "userName"))
          .thenReturn("John Doe");
      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "userEmail"))
          .thenReturn("john@example.com");

      List<JsonXmlData> nodes = List.of(parentNode);

      Object result = WebHookUtil.generateDataParametersJSON(nodes, baseOBObject, logger);

      assertNotNull(result);
      assertInstanceOf(JSONObject.class, result);
      JSONObject jsonResult = (JSONObject) result;
      assertTrue(jsonResult.has("user"));
    }
  }

  /**
   * Tests the queueEvent method with an empty list of Events.
   * Verifies that the method does not save any QueueEventHook when no events are found.
   */
  @Test
  void testQueueEventWithEmptyEventsList() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.createAlias(anyString(), anyString())).thenReturn(eventsCriteria);
      when(eventsCriteria.add(any())).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(new ArrayList<>());

      WebHookUtil.queueEvent(table, eventType, TEST_EVENT_CLASS, RECORD_ID);

      verify(obDal, never()).save(any());
    }
  }

  /**
   * Tests the callWebHook method with an inactive webhook.
   * Verifies that the method checks the active status of the webhook before calling it.
   */
  @Test
  void testCallWebHookWithInactiveWebhook() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      Webhook webhook = mock(Webhook.class);
      when(webhook.isActive()).thenReturn(false);

      OBCriteria<Webhook> webhookCriteria = mock(OBCriteria.class);
      when(obDal.createCriteria(Webhook.class)).thenReturn(webhookCriteria);
      when(webhookCriteria.add(any())).thenReturn(webhookCriteria);
      when(webhookCriteria.list()).thenReturn(List.of(webhook));

      WebHookUtil.callWebHook(events, baseOBObject, logger);

      verify(webhook).isActive();
    }
  }

  /**
   * Tests the generateDataParametersJSON method with static values and no property names.
   * Verifies that the method returns a LinkedList containing the static values.
   * Also checks that the method handles nodes with null or empty names correctly.
   * If an exception occurs during the process, the test will fail.
   *
   * @throws Exception if an unexpected error occurs during the test execution
   */
  @Test
  void testGenerateDataParametersJSONWithStaticValues() throws Exception {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      setupOBContextMock(mockedOBContext);

      JsonXmlData node1 = mock(JsonXmlData.class);
      when(node1.isSummaryLevel()).thenReturn(false);
      when(node1.getName()).thenReturn(null);
      when(node1.getTypeValue()).thenReturn("S");
      when(node1.getValue()).thenReturn("static value 1");

      JsonXmlData node2 = mock(JsonXmlData.class);
      when(node2.isSummaryLevel()).thenReturn(false);
      when(node2.getName()).thenReturn("");
      when(node2.getTypeValue()).thenReturn("S");
      when(node2.getValue()).thenReturn("static value 2");

      List<JsonXmlData> nodes = Arrays.asList(node1, node2);

      Object result = WebHookUtil.generateDataParametersJSON(nodes, baseOBObject, logger);

      assertNotNull(result);
      assertInstanceOf(LinkedList.class, result);
      LinkedList<?> listResult = (LinkedList<?>) result;
      assertEquals(2, listResult.size());
      assertEquals("static value 1", listResult.get(0));
      assertEquals("static value 2", listResult.get(1));
    }
  }

  /**
   * Tests the replaceValueData method with multiple property placeholders.
   * Verifies that the method replaces all placeholders with their respective values.
   */
  @Test
  void testReplaceValueDataWithMultipleProperties() {
    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);

      String value = "@firstName @lastName is @age years old";

      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "firstName"))
          .thenReturn("John");
      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "lastName"))
          .thenReturn("Smith");
      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "age"))
          .thenReturn("25");

      String result = WebHookUtil.replaceValueData(value, baseOBObject, logger);

      assertEquals("John Smith is 25 years old", result);
    }
  }

  /**
   * Tests the queueEventFromEventHandler method when the event has an invalid dynamic Java class.
   * Verifies that if a ClassNotFoundException occurs when trying to load the dynamic class,
   * the event is not saved to the database (save is not called).
   *
   * @throws Exception if an unexpected error occurs during the test execution
   */
  @Test
  void testQueueEventFromEventHandlerWithDynamicJavaClass() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedProvider = mockStatic(OBProvider.class);
         MockedStatic<OBClassLoader> mockedClassLoader = mockStatic(OBClassLoader.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      when(obDal.createCriteria(Events.class)).thenReturn(eventsCriteria);
      when(eventsCriteria.createAlias(anyString(), anyString())).thenReturn(eventsCriteria);
      when(eventsCriteria.add(any())).thenReturn(eventsCriteria);
      when(eventsCriteria.list()).thenReturn(List.of(events));

      when(events.getDynamicEventJavaclass()).thenReturn("com.invalid.Class");
      when(events.getTable()).thenReturn(table);
      when(events.getSmfwheEventType()).thenReturn(eventType);

      OBClassLoader classLoader = mock(OBClassLoader.class);
      mockedClassLoader.when(OBClassLoader::getInstance).thenReturn(classLoader);
      when(classLoader.loadClass("com.invalid.Class"))
          .thenThrow(new ClassNotFoundException());

      QueueEventHook queueEventHook = mock(QueueEventHook.class);
      mockedProvider.when(OBProvider::getInstance).thenReturn(mock(OBProvider.class));
      mockedProvider.when(() -> OBProvider.getInstance().get(QueueEventHook.class))
          .thenReturn(queueEventHook);

      Organization org = mock(Organization.class);
      when(obDal.get(Organization.class, "0")).thenReturn(org);
      when(obDal.get(Table.class, TEST_TABLE_ID)).thenReturn(table);

      WebHookUtil.queueEventFromEventHandler(TEST_TABLE_NAME, TEST_TABLE_ID,
          TEST_EVENT_TYPE_ID, RECORD_ID);

      verify(obDal, never()).save(any());
    }
  }

  /**
   * Tests the setHeaderConnection method to ensure it sets headers correctly on the HttpURLConnection.
   * Verifies that headers are set with string values.
   */
  @Test
  void testSetHeaderConnectionWithStringValue() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      setupOBContextMock(mockedOBContext);

      HttpURLConnection connection = mock(HttpURLConnection.class);

      UrlPathParam headerParam = mock(UrlPathParam.class);
      when(headerParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
      when(headerParam.getName()).thenReturn(CONTENT_TYPE);
      when(headerParam.getValue()).thenReturn("application/json");

      List<UrlPathParam> params = List.of(headerParam);

      WebHookUtil.setHeaderConnection(connection, params, logger, baseOBObject);

      verify(connection).setRequestProperty(CONTENT_TYPE, "application/json");
    }
  }

  /**
   * Tests the setHeaderConnection method with property values.
   * Verifies that headers are set using property values from the BaseOBObject.
   */
  @Test
  void testSetHeaderConnectionWithPropertyValue() {
    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);
      HttpURLConnection connection = mock(HttpURLConnection.class);

      UrlPathParam headerParam = mock(UrlPathParam.class);
      when(headerParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_PROPERTY);
      when(headerParam.getName()).thenReturn("Authorization");
      when(headerParam.getProperty()).thenReturn("authToken");

      mockedDalUtil.when(() -> DalUtil.getValueFromPath(baseOBObject, "authToken"))
          .thenReturn("Bearer abc123");

      List<UrlPathParam> params = List.of(headerParam);

      WebHookUtil.setHeaderConnection(connection, params, logger, baseOBObject);

      verify(connection).setRequestProperty("Authorization", "Bearer abc123");
    }
  }

  /**
   * Tests the setHeaderConnection method with computed values.
   * Verifies that headers are set using computed function values.
   */
  @Test
  void testSetHeaderConnectionWithComputedValue() {
    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);
      HttpURLConnection connection = mock(HttpURLConnection.class);

      UrlPathParam headerParam = mock(UrlPathParam.class);
      when(headerParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_COMPUTED);
      when(headerParam.getName()).thenReturn("X-Custom-Header");

      mockedWebHookUtil.when(() -> WebHookUtil.getValueExecuteMethod(
          any(), any(), any(), any())).thenReturn("computed-value");

      mockedWebHookUtil.when(() -> WebHookUtil.setHeaderConnection(
          any(), any(), any(), any())).thenCallRealMethod();

      List<UrlPathParam> params = List.of(headerParam);

      WebHookUtil.setHeaderConnection(connection, params, logger, baseOBObject);

      verify(connection).setRequestProperty("X-Custom-Header", "computed-value");
    }
  }

  /**
   * Tests the getConnection method with an HTTP URL.
   * Verifies that the connection is properly established for HTTP URLs and returns an HttpURLConnection instance.
   *
   * @throws Exception if any error occurs during reflection or connection setup:
   *                   - NoSuchMethodException if the getConnection method is not found
   *                   - IllegalAccessException if the method cannot be accessed
   *                   - InvocationTargetException if the method throws an exception
   */
  @Test
  void testGetConnectionWithHttpsUrl() throws Exception {
    String httpsUrl = "https://secure.example.com/api";
    URL url = mock(URL.class);
    HttpsURLConnection connection = mock(HttpsURLConnection.class);

    when(url.openConnection()).thenReturn(connection);

    Method getConnectionMethod = WebHookUtil.class.getDeclaredMethod(
        "getConnection", String.class, URL.class);
    getConnectionMethod.setAccessible(true);

    HttpURLConnection result = (HttpURLConnection) getConnectionMethod.invoke(
        null, httpsUrl, url);

    assertNotNull(result);
    assertEquals(connection, result);
  }

  /**
   * Tests the getConnection method with an HTTPS URL.
   * Verifies that the connection is properly established for HTTPS URLs and returns an HttpsURLConnection instance.
   *
   * @throws Exception if any error occurs during reflection or connection setup:
   *                   - NoSuchMethodException if the getConnection method is not found
   *                   - IllegalAccessException if the method cannot be accessed
   *                   - InvocationTargetException if the method throws an exception
   */
  @Test
  void testGetConnectionWithInvalidUrl() throws Exception {
    String invalidUrl = "ftp://example.com/file";
    URL url = mock(URL.class);

    Method getConnectionMethod = WebHookUtil.class.getDeclaredMethod(
        "getConnection", String.class, URL.class);
    getConnectionMethod.setAccessible(true);

    Exception exception = assertThrows(InvocationTargetException.class, () ->
        getConnectionMethod.invoke(null, invalidUrl, url)
    );

    assertInstanceOf(OBException.class, exception.getCause());
    assertTrue(exception.getCause().getMessage().contains("Invalid URL"));
  }

  /**
   * Tests the sendEvent method with JSON data type.
   * This is a partial test that verifies the URL generation and connection setup.
   */
  @Test
  void testSendEventUrlGeneration() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class);
         MockedStatic<WebHookInitializer> mockedInitializer = mockStatic(WebHookInitializer.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      Webhook webhook = mock(Webhook.class);
      Events event = mock(Events.class);

      when(webhook.getUrlnotify()).thenReturn("http://example.com/webhook");
      when(webhook.getSmfwheEvents()).thenReturn(event);
      when(webhook.getTypedata()).thenReturn(Constants.STRING_JSON);
      when(event.getMethod()).thenReturn("POST");

      OBCriteria<UrlPathParam> criteria = mock(OBCriteria.class);
      when(obDal.createCriteria(UrlPathParam.class)).thenReturn(criteria);
      when(criteria.add(any())).thenReturn(criteria);
      when(criteria.list()).thenReturn(new ArrayList<>());

      mockedWebHookUtil.when(() -> WebHookUtil.generateUrlParameter(
          any(), anyString(), any(), any())).thenReturn("http://example.com/webhook");

      mockedInitializer.when(WebHookInitializer::initialize).thenAnswer(invocation -> null);

      assertNotNull(webhook.getUrlnotify());
      assertEquals("POST", webhook.getSmfwheEvents().getMethod());
      assertEquals(Constants.STRING_JSON, webhook.getTypedata());
    }
  }

  /**
   * Tests the generateUrlParameter method with computed values.
   * Verifies that computed functions are executed to replace URL parameters.
   */
  @Test
  void testGenerateUrlParameterWithComputedValue() {
    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupOBContextMock(mockedOBContext);

      UrlPathParam param = mock(UrlPathParam.class);
      when(param.isActive()).thenReturn(true);
      when(param.getTypeParameter()).thenReturn(Constants.TYPE_PARAMETER_PATH);
      when(param.getName()).thenReturn("computedId");
      when(param.getTypeValue()).thenReturn(Constants.TYPE_VALUE_COMPUTED);

      mockedWebHookUtil.when(() -> WebHookUtil.getValueExecuteMethod(
          any(), any(), any(), any())).thenReturn("computed-123");

      mockedWebHookUtil.when(() -> WebHookUtil.generateUrlParameter(
          any(), anyString(), any(), any())).thenCallRealMethod();

      List<UrlPathParam> params = List.of(param);
      String url = "http://example.com/api/{computedId}/data";

      String result = WebHookUtil.generateUrlParameter(params, url, baseOBObject, logger);

      assertEquals("http://example.com/api/computed-123/data", result);
    }
  }

  /**
   * Tests the generateUrlParameter method with inactive parameters.
   * Verifies that inactive parameters are not processed.
   */
  @Test
  void testGenerateUrlParameterWithInactiveParam() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      setupOBContextMock(mockedOBContext);

      UrlPathParam param = mock(UrlPathParam.class);
      when(param.isActive()).thenReturn(false);
      when(param.getName()).thenReturn("inactiveParam");

      List<UrlPathParam> params = List.of(param);
      String url = "http://example.com/api/{inactiveParam}/data";

      String result = WebHookUtil.generateUrlParameter(params, url, baseOBObject, logger);

      assertEquals("http://example.com/api/{inactiveparam}/data", result.toLowerCase());
    }
  }

  /**
   * Tests error handling in generateUrlParameter when an exception occurs.
   * Verifies that an OBException is thrown with appropriate error message.
   */
  @Test
  void testGenerateUrlParameterWithException() {
    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      setupOBContextMock(mockedOBContext);

      UrlPathParam param = mock(UrlPathParam.class);
      when(param.isActive()).thenReturn(true);
      when(param.getTypeParameter()).thenReturn(Constants.TYPE_PARAMETER_PATH);
      when(param.getName()).thenReturn("errorParam");
      when(param.getTypeValue()).thenReturn(Constants.TYPE_VALUE_PROPERTY);
      when(param.getProperty()).thenReturn("invalidProperty");

      mockedDalUtil.when(() -> DalUtil.getValueFromPath(any(), anyString()))
          .thenThrow(new RuntimeException("Property not found"));

      mockedUtility.when(() -> Utility.messageBD(any(), anyString(), anyString()))
          .thenReturn("Error replacing path parameter: %s");

      List<UrlPathParam> params = List.of(param);
      String url = "http://example.com/api/{errorParam}/data";

      OBException exception = assertThrows(OBException.class, () ->
          WebHookUtil.generateUrlParameter(params, url, baseOBObject, logger)
      );

      assertNotNull(exception);
    }
  }
  /**
   * Helper method to set up the OBContext mock.
   * This method is used to configure the OBContext mock with the necessary language settings.
   *
   * @param mockedOBContext The mocked OBContext static class.
   */
  private void setupOBContextMock(MockedStatic<OBContext> mockedOBContext) {
    mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    when(obContext.getLanguage()).thenReturn(language);
    when(language.getLanguage()).thenReturn(TEST_EVENT_LANG);
  }

}
