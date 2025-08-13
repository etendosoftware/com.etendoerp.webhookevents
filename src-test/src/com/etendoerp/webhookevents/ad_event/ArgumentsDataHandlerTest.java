package com.etendoerp.webhookevents.ad_event;

import static com.etendoerp.webhookevents.WebhookTestConstants.INVALID_PROPERTY_ERROR_MESSAGE;
import static com.etendoerp.webhookevents.WebhookTestConstants.INVALID_PROPERTY_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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

import com.etendoerp.webhookevents.data.ArgumentsData;
import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.JsonXmlData;
import com.etendoerp.webhookevents.data.Webhook;

/**
 * Test class for ArgumentsDataHandler.
 * It tests the onSave and onUpdate methods with various scenarios,
 * including valid and invalid inputs, and checks the behavior of the valid method.
 */
public class ArgumentsDataHandlerTest extends OBBaseTest {

  private ArgumentsDataHandler handler;
  private EntityNewEvent newEvent;
  private EntityUpdateEvent updateEvent;
  private ArgumentsData argumentsData;
  private JsonXmlData jsonXmlData;
  private Webhook webhook;
  private Events events;
  private Entity entity;
  private Property property;
  private ModelProvider modelProvider;

  /**
   * Sets up the test environment before each test execution.
   * Initializes the handler and mocks all required dependencies.
   *
   * @throws Exception if the parent setUp method or any mock initialization fails.
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();

    handler = new ArgumentsDataHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return true;
      }
    };

    newEvent = mock(EntityNewEvent.class);
    updateEvent = mock(EntityUpdateEvent.class);
    argumentsData = mock(ArgumentsData.class);
    jsonXmlData = mock(JsonXmlData.class);
    webhook = mock(Webhook.class);
    events = mock(Events.class);
    entity = mock(Entity.class);
    property = mock(Property.class);
    modelProvider = mock(ModelProvider.class);
  }

  /**
   * Tests the onSave method with a valid input.
   * It checks that no exception is thrown when the value is a simple string without properties.
   */
  @Test
  public void testOnSaveValidInputSimpleValue() {
    setupArgumentsDataMockChain();
    when(newEvent.getTargetInstance()).thenReturn(argumentsData);
    when(argumentsData.getValue()).thenReturn("simple value");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onSave method with a valid property reference.
   * It mocks the DalUtil to return a valid property when requested.
   */
  @Test
  public void testOnSaveWithValidProperty() {
    setupArgumentsDataMockChain();
    when(newEvent.getTargetInstance()).thenReturn(argumentsData);
    when(argumentsData.getValue()).thenReturn("value @validProperty");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onSave method with an invalid property reference.
   * It mocks the DalUtil to return null when the property is requested.
   * An OBException is expected to be thrown.
   */
  @Test
  public void testOnSaveWithInvalidProperty() {
    setupArgumentsDataMockChain();
    when(newEvent.getTargetInstance()).thenReturn(argumentsData);
    when(argumentsData.getValue()).thenReturn(INVALID_PROPERTY_VALUE);

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn(INVALID_PROPERTY_ERROR_MESSAGE);

      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onUpdate method with a valid input.
   * It checks that no exception is thrown when the value is a simple string without properties.
   */
  @Test
  public void testOnUpdateValidInput() {
    setupArgumentsDataMockChain();
    when(updateEvent.getTargetInstance()).thenReturn(argumentsData);
    when(argumentsData.getValue()).thenReturn("simple value");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }
  /**
   * Tests the onUpdate method with a valid property reference.
   * It mocks the DalUtil to return a valid property when requested.
   */
  @Test
  public void testOnUpdateWithInvalidProperty() {
    setupArgumentsDataMockChain();
    when(updateEvent.getTargetInstance()).thenReturn(argumentsData);
    when(argumentsData.getValue()).thenReturn(INVALID_PROPERTY_VALUE);

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn(INVALID_PROPERTY_ERROR_MESSAGE);

      assertThrows(OBException.class, () -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Tests the valid method with a simple value.
   * It checks that no exception is thrown when the value is a simple string without properties.
   */
  @Test
  public void testValidMethodWithSimpleValue() {
    when(argumentsData.getValue()).thenReturn("simple value without properties");

    assertDoesNotThrow(() -> handler.valid(entity, argumentsData));
  }

  /**
   * Tests the valid method with a valid property reference.
   * It mocks the DalUtil to return a valid property when requested.
   */
  @Test
  public void testValidMethodWithPropertyReferences() {

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      when(argumentsData.getValue()).thenReturn("value @validProperty simple");
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(entity, argumentsData));

      when(argumentsData.getValue()).thenReturn(INVALID_PROPERTY_VALUE);
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      try (MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {
        mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
            .thenReturn(INVALID_PROPERTY_ERROR_MESSAGE);

        assertThrows(OBException.class, () -> handler.valid(entity, argumentsData));
      }
    }
  }

  /**
   * Tests the valid method with an empty value.
   * It checks that no exception is thrown when the value is null or empty.
   */
  @Test
  public void testOnSaveWhenEventNotValid() {
    ArgumentsDataHandler handlerWithInvalidEvent = new ArgumentsDataHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onSave(newEvent));
  }

  /**
   * Tests the onUpdate method with an empty value.
   * It checks that no exception is thrown when the value is null or empty.
   */
  @Test
  public void testOnUpdateWhenEventNotValid() {
    ArgumentsDataHandler handlerWithInvalidEvent = new ArgumentsDataHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onUpdate(updateEvent));
  }

  /**
   * Tests the getObservedEntities method.
   * It checks that the method returns the correct entity name.
   */
  @Test
  public void testGetObservedEntities() {
    Entity[] entities = handler.getObservedEntities();

    assertEquals(1, entities.length);
    assertEquals(ArgumentsData.ENTITY_NAME, entities[0].getName());
  }

  /**
   * Sets up the mock chain for ArgumentsData navigation.
   * Now using the correct Events class.
   */
  private void setupArgumentsDataMockChain() {
    Table table = mock(Table.class);

    when(argumentsData.getSmfwheJsonData()).thenReturn(jsonXmlData);
    when(jsonXmlData.getSmfwheWebhook()).thenReturn(webhook);
    when(webhook.getSmfwheEvents()).thenReturn(events);
    when(events.getTable()).thenReturn(table);
    when(table.getDBTableName()).thenReturn("test_table");
  }
}
