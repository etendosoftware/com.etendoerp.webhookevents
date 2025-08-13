package com.etendoerp.webhookevents.ad_event;

import static com.etendoerp.webhookevents.WebhookTestConstants.INVALID_PROPERTY_ERROR_MESSAGE_ALT;
import static com.etendoerp.webhookevents.WebhookTestConstants.SIMPLE_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.test.base.OBBaseTest;

import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.JsonXmlData;
import com.etendoerp.webhookevents.data.Webhook;
import com.etendoerp.webhookevents.webhook_util.Constants;

/**
 * Test class for JsonXmlDataHandler.
 * This class uses Mockito to mock dependencies and verify behavior.
 */
public class JsonXmlDataHandlerTest extends OBBaseTest {

  private JsonXmlDataHandler handler;
  private EntityNewEvent newEvent;
  private EntityUpdateEvent updateEvent;
  private JsonXmlData jsonXmlData;
  private Webhook webhook;
  private Events events;
  private Entity entity;
  private Property property;
  private ModelProvider modelProvider;

  /**
   * Sets up the test environment before each test.
   * This method initializes the handler and mocks required for the tests.
   *
   * @throws Exception
   *     if there is an error during setup
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();

    handler = new JsonXmlDataHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return true;
      }
    };

    newEvent = mock(EntityNewEvent.class);
    updateEvent = mock(EntityUpdateEvent.class);
    jsonXmlData = mock(JsonXmlData.class);
    webhook = mock(Webhook.class);
    events = mock(Events.class);
    entity = mock(Entity.class);
    property = mock(Property.class);
    modelProvider = mock(ModelProvider.class);
  }

  /**
   * Test for onSave with valid input.
   * This should not throw an exception since the input is valid.
   */
  @Test
  public void testOnSaveValidInputWithSummaryLevelTrue() {
    setupJsonXmlDataMockChain();
    when(newEvent.getTargetInstance()).thenReturn(jsonXmlData);
    when(jsonXmlData.isSummaryLevel()).thenReturn(true);

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Test for onSave with valid input.
   * This should not throw an exception since the input is valid.
   */
  @Test
  public void testOnSaveValidInputWithSummaryLevelFalse() {
    setupJsonXmlDataMockChain();
    when(newEvent.getTargetInstance()).thenReturn(jsonXmlData);
    when(jsonXmlData.isSummaryLevel()).thenReturn(false); // Should perform validation
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn(SIMPLE_VALUE);

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Test for onUpdate with valid input.
   * This should not throw an exception since the input is valid.
   */
  @Test
  public void testOnUpdateValidInput() {
    setupJsonXmlDataMockChain();
    when(updateEvent.getTargetInstance()).thenReturn(jsonXmlData);
    when(jsonXmlData.isSummaryLevel()).thenReturn(false);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn(SIMPLE_VALUE);

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Test for valid method with a string type and a null value.
   * This should not throw an exception since null is a valid value for a string type.
   */
  @Test
  public void testValidMethodWithStringTypeAndValidProperty() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn("value @validProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, jsonXmlData));
    }
  }

  /**
   * Test for valid method with a string type and a null value.
   * This should not throw an exception since null is a valid value for a string type.
   */
  @Test
  public void testValidMethodWithStringTypeAndInvalidProperty() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn("value @invalidProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn(INVALID_PROPERTY_ERROR_MESSAGE_ALT);

      assertThrows(OBException.class, () -> handler.valid(testEntity, jsonXmlData));
    }
  }

  /**
   * Test for valid method with a string type and a null value.
   * This should not throw an exception since null is a valid value for a string type.
   */
  @Test
  public void testValidMethodWithStringTypeAndNullValue() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn(null);

    assertDoesNotThrow(() -> handler.valid(testEntity, jsonXmlData));
  }

  /**
   * Test for valid method with a property type and a valid property.
   * This should not throw an exception if the property is valid.
   */
  @Test
  public void testValidMethodWithPropertyTypeAndValidProperty() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_PROPERTY);
    when(jsonXmlData.getProperty()).thenReturn("validProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, jsonXmlData));
    }
  }

  /**
   * Test for valid method with a property type and an invalid property.
   * This should throw an OBException if the property is not found.
   */
  @Test
  public void testValidMethodWithPropertyTypeAndInvalidProperty() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_PROPERTY);
    when(jsonXmlData.getProperty()).thenReturn("invalidProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn(INVALID_PROPERTY_ERROR_MESSAGE_ALT);

      assertThrows(OBException.class, () -> handler.valid(testEntity, jsonXmlData));
    }
  }

  /**
   * Test for valid method with a dynamic node type and a valid class.
   * This should not throw an exception if the class implements the required interfaces.
   */
  @Test
  public void testValidMethodWithDynamicNodeTypeAndInvalidClass() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_DYNAMIC_NODE);
    when(jsonXmlData.getJavaClassName()).thenReturn("com.nonexistent.InvalidClass");

    assertThrows(OBException.class, () -> handler.valid(testEntity, jsonXmlData));
  }

  /**
   * Test for onSave when the event is not valid.
   * This should not throw an exception since the handler is designed to handle invalid events gracefully.
   */
  @Test
  public void testOnSaveWhenEventNotValid() {
    JsonXmlDataHandler handlerWithInvalidEvent = new JsonXmlDataHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onSave(newEvent));
  }

  /**
   * Test for onUpdate when the event is not valid.
   * This should not throw an exception since the handler is designed to handle invalid events gracefully.
   */
  @Test
  public void testOnUpdateWhenEventNotValid() {
    JsonXmlDataHandler handlerWithInvalidEvent = new JsonXmlDataHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onUpdate(updateEvent));
  }

  /**
   * Test for getObservedEntities method.
   * This should return an array containing the JsonXmlData entity.
   */
  @Test
  public void testGetObservedEntities() {
    Entity[] entities = handler.getObservedEntities();

    assertEquals(1, entities.length);
    assertEquals(JsonXmlData.ENTITY_NAME, entities[0].getName());
  }

  /**
   * Test for valid method with a string type and multiple properties.
   * This should not throw an exception if all properties are valid.
   */
  @Test
  public void testValidMethodWithStringTypeAndSimpleValue() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn("simple value without properties");

    assertDoesNotThrow(() -> handler.valid(testEntity, jsonXmlData));
  }

  /**
   * Test for valid method with a string type and multiple properties.
   * This should not throw an exception if all properties are valid.
   */
  @Test
  public void testValidMethodWithStringTypeAndMultipleProperties() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn("value @property1 and @property2");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, jsonXmlData));
    }
  }

  /**
   * Test for onSave with an invalid property in the string type.
   * This should throw an OBException when the property is not found.
   */
  @Test
  public void testOnSaveWithInvalidPropertyInStringType() {
    setupJsonXmlDataMockChain();
    when(newEvent.getTargetInstance()).thenReturn(jsonXmlData);
    when(jsonXmlData.isSummaryLevel()).thenReturn(false);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn("value @invalidProperty");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn(INVALID_PROPERTY_ERROR_MESSAGE_ALT);

      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }

  /**
   * Test for valid method with a dynamic node type and an empty class name.
   * This should throw an OBException since the class name is required for dynamic nodes.
   */
  @Test
  public void testValidMethodWithNullClassName() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_DYNAMIC_NODE);
    when(jsonXmlData.getJavaClassName()).thenReturn(null);

    assertThrows(OBException.class, () -> handler.valid(testEntity, jsonXmlData));
  }

  /**
   * Test for valid method with an empty class name.
   * This should throw an OBException since the class name is required for dynamic nodes.
   */
  @Test
  public void testValidMethodWithEmptyClassName() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_DYNAMIC_NODE);
    when(jsonXmlData.getJavaClassName()).thenReturn("");

    assertThrows(OBException.class, () -> handler.valid(testEntity, jsonXmlData));
  }

  /**
   * Test for valid method with an unknown type value.
   * This should not throw an exception since the handler is designed to handle unknown types gracefully.
   */
  @Test
  public void testValidMethodWithUnknownTypeValue() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn("UNKNOWN_TYPE");

    assertDoesNotThrow(() -> handler.valid(testEntity, jsonXmlData));
  }

  /**
   * Test for onUpdate method with summary level set to null.
   * This should not throw an exception since BooleanUtils.isFalse(null) returns true.
   */
  @Test
  public void testOnUpdateWithSummaryLevelNull() {
    setupJsonXmlDataMockChain();
    when(updateEvent.getTargetInstance()).thenReturn(jsonXmlData);
    when(jsonXmlData.isSummaryLevel()).thenReturn(null); // BooleanUtils.isFalse(null) returns true
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn(SIMPLE_VALUE);

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Sets up the mock chain for JsonXmlData navigation.
   * The chain is: jsonXmlData -> webhook -> events -> table
   */
  private void setupJsonXmlDataMockChain() {
    Table table = mock(Table.class);

    when(jsonXmlData.getSmfwheWebhook()).thenReturn(webhook);
    when(webhook.getSmfwheEvents()).thenReturn(events);
    when(events.getTable()).thenReturn(table);
    when(table.getDBTableName()).thenReturn("test_table");
  }

  /**
   * Test direct access to getMessage method using reflection.
   * This test verifies the behavior of the private getMessage method by accessing it directly.
   *
   * @throws Exception
   *     if there is an error accessing the method through reflection
   */
  @Test
  public void testGetMessageMethodDirectly() throws Exception {
    Entity testEntity = mock(Entity.class);
    JsonXmlData testData = mock(JsonXmlData.class);
    when(testData.getValue()).thenReturn("test @validProperty message");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      Method getMessageMethod = JsonXmlDataHandler.class.getDeclaredMethod("getMessage", Entity.class,
          JsonXmlData.class);
      getMessageMethod.setAccessible(true);

      assertDoesNotThrow(() -> getMessageMethod.invoke(null, testEntity, testData));
    }
  }

  /**
   * Test for valid method with a string type and a property at the start or end of the value.
   * This should not throw an exception if the property is valid.
   */
  @Test
  public void testValidMethodWithStringTypeAndPropertyAtStart() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn("@property1 some value");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, jsonXmlData));
    }
  }

  /**
   * Test for valid method with a string type and a property at the end of the value.
   * This should not throw an exception if the property is valid.
   */
  @Test
  public void testValidMethodWithStringTypeAndPropertyAtEnd() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn("some value @property1");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, jsonXmlData));
    }
  }

  /**
   * Test for valid method with a string type and a mix of valid and invalid properties.
   * This should throw an OBException if any property is invalid.
   */
  @Test
  public void testValidMethodWithStringTypeAndMixedValidInvalidProperties() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(jsonXmlData.getValue()).thenReturn("@validProperty and @invalidProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), eq("validProperty")))
          .thenReturn(property);
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), eq("invalidProperty")))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn(INVALID_PROPERTY_ERROR_MESSAGE_ALT);

      assertThrows(OBException.class, () -> handler.valid(testEntity, jsonXmlData));
    }
  }

  /**
   * Test for onUpdate method when ModelProvider throws an exception.
   * This simulates a scenario where the ModelProvider cannot provide the entity.
   */
  @Test
  public void testOnSaveWithModelProviderException() {
    setupJsonXmlDataMockChain();
    when(newEvent.getTargetInstance()).thenReturn(jsonXmlData);
    when(jsonXmlData.isSummaryLevel()).thenReturn(false);

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenThrow(new RuntimeException("ModelProvider error"));

      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }

  /**
   * Test for onUpdate method when ModelProvider throws an exception.
   * This should result in an OBException being thrown.
   */
  @Test
  public void testOnUpdateWithModelProviderException() {
    setupJsonXmlDataMockChain();
    when(updateEvent.getTargetInstance()).thenReturn(jsonXmlData);
    when(jsonXmlData.isSummaryLevel()).thenReturn(false);

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenThrow(new RuntimeException("ModelProvider error"));

      assertThrows(OBException.class, () -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Test for valid method with a dynamic node type and no interfaces.
   * This should throw an OBException since the class does not implement any interfaces.
   */
  @Test
  public void testValidMethodWithDynamicNodeAndNoInterfaces() {
    Entity testEntity = mock(Entity.class);
    when(jsonXmlData.getTypeValue()).thenReturn(Constants.TYPE_VALUE_DYNAMIC_NODE);
    when(jsonXmlData.getJavaClassName()).thenReturn(TestNoInterfaceClass.class.getName());

    assertThrows(OBException.class, () -> handler.valid(testEntity, jsonXmlData));
  }

  /**
   * A test class that does not implement any interfaces.
   * This is used to test the behavior of the valid method when a dynamic node type
   * is provided with a class that has no interfaces.
   */
  public static class TestNoInterfaceClass {
    public TestNoInterfaceClass() {
      // Default constructor but no interfaces
    }
  }
}

