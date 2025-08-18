package com.etendoerp.webhookevents.ad_event;

import static com.etendoerp.webhookevents.WebhookTestConstants.INVALID_PROPERTY_ERROR_MESSAGE;
import static com.etendoerp.webhookevents.WebhookTestConstants.INVALID_PROPERTY_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import com.etendoerp.webhookevents.data.Arguments;
import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.UrlPathParam;
import com.etendoerp.webhookevents.data.Webhook;

/**
 * Test class for ArgumentsHandler.
 * This class tests the functionality of the ArgumentsHandler class, which processes
 * events related to the Arguments entity.
 */
public class ArgumentsHandlerTest extends OBBaseTest {

  private ArgumentsHandler handler;
  private EntityNewEvent newEvent;
  private EntityUpdateEvent updateEvent;
  private Arguments arguments;
  private UrlPathParam urlPathParam;
  private Webhook webhook;
  private Events events;
  private Entity entity;
  private Property property;
  private ModelProvider modelProvider;

  /**
   * Sets up the test environment before each test execution.
   * Initializes mocks and the ArgumentsHandler instance.
   *
   * @throws Exception if the setup fails
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();

    handler = new ArgumentsHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return true;
      }
    };

    newEvent = mock(EntityNewEvent.class);
    updateEvent = mock(EntityUpdateEvent.class);
    arguments = mock(Arguments.class);
    urlPathParam = mock(UrlPathParam.class);
    webhook = mock(Webhook.class);
    events = mock(Events.class);
    entity = mock(Entity.class);
    property = mock(Property.class);
    modelProvider = mock(ModelProvider.class);
  }

  /**
   * Test case for onSave method with valid input.
   * It should pass without exceptions when the input is a simple value.
   */
  @Test
  public void testOnSaveValidInputSimpleValue() {
    setupArgumentsMockChain();
    when(newEvent.getTargetInstance()).thenReturn(arguments);
    when(arguments.getValueParameter()).thenReturn("simple value");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Test case for onSave method with a valid property reference.
   * It should pass without exceptions when the property exists.
   */
  @Test
  public void testOnSaveWithValidProperty() {
    setupArgumentsMockChain();
    when(newEvent.getTargetInstance()).thenReturn(arguments);
    when(arguments.getValueParameter()).thenReturn("value @validProperty");

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
   * Test case for onSave method with an invalid property reference.
   * It should throw an OBException when the property does not exist.
   */
  @Test
  public void testOnSaveWithInvalidProperty() {
    setupArgumentsMockChain();
    when(newEvent.getTargetInstance()).thenReturn(arguments);
    when(arguments.getValueParameter()).thenReturn(INVALID_PROPERTY_VALUE);

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
   * Test case for onUpdate method with valid input.
   * It should pass without exceptions when the input is a simple value.
   */
  @Test
  public void testOnUpdateValidInput() {
    setupArgumentsMockChain();
    when(updateEvent.getTargetInstance()).thenReturn(arguments);
    when(arguments.getValueParameter()).thenReturn("simple value");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Test case for onUpdate method with a valid property reference.
   * It should pass without exceptions when the property exists.
   */
  @Test
  public void testOnUpdateWithInvalidProperty() {
    setupArgumentsMockChain();
    when(updateEvent.getTargetInstance()).thenReturn(arguments);
    when(arguments.getValueParameter()).thenReturn(INVALID_PROPERTY_VALUE);

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
   * Test case for onUpdate method with a valid property reference.
   * It should pass without exceptions when the property exists.
   */
  @Test
  public void testValidMethodWithSimpleValue() {
    when(arguments.getValueParameter()).thenReturn("simple value without properties");
    Entity testEntity = mock(Entity.class);

    assertDoesNotThrow(() -> handler.valid(testEntity, arguments));
  }

  /**
   * Test case for valid method with a valid property reference.
   * It should pass without exceptions when the property exists.
   */
  @Test
  public void testValidMethodWithPropertyReferences() {
    Entity testEntity = mock(Entity.class);

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      when(arguments.getValueParameter()).thenReturn("value @validProperty simple");
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, arguments));

      when(arguments.getValueParameter()).thenReturn(INVALID_PROPERTY_VALUE);
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      try (MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {
        mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
            .thenReturn(INVALID_PROPERTY_ERROR_MESSAGE);

        assertThrows(OBException.class, () -> handler.valid(testEntity, arguments));
      }
    }
  }

  /**
   * Test case for valid method with a property that does not exist.
   * It should throw an OBException when the property does not exist.
   */
  @Test
  public void testOnSaveWhenEventNotValid() {
    ArgumentsHandler handlerWithInvalidEvent = new ArgumentsHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onSave(newEvent));
  }

  /**
   * Test case for onUpdate method with an event that is not valid.
   * It should not throw any exceptions when the event is not valid.
   */
  @Test
  public void testOnUpdateWhenEventNotValid() {
    ArgumentsHandler handlerWithInvalidEvent = new ArgumentsHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onUpdate(updateEvent));
  }

  /**
   * Test case for getObservedEntities method.
   * It should return the Arguments entity.
   */
  @Test
  public void testGetObservedEntities() {
    Entity[] entities = handler.getObservedEntities();

    assertEquals(1, entities.length);
    assertEquals(Arguments.ENTITY_NAME, entities[0].getName());
  }

  /**
   * Test case for valid method with multiple property references.
   * It should pass without exceptions when all properties exist.
   */
  @Test
  public void testValidMethodWithMultiplePropertyReferences() {
    Entity testEntity = mock(Entity.class);

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      when(arguments.getValueParameter()).thenReturn("value @property1 and @property2 text");
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, arguments));
    }
  }

  /**
   * Test case for valid method with a mix of valid and invalid property references.
   * It should throw an OBException when any property does not exist.
   */
  @Test
  public void testValidMethodWithMixedValidAndInvalidProperties() {
    Entity testEntity = mock(Entity.class);

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), eq("validProperty")))
          .thenReturn(property);
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), eq("invalidProperty")))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Error in name the property: 'invalidProperty'");

      when(arguments.getValueParameter()).thenReturn("value @validProperty and @invalidProperty");

      assertThrows(OBException.class, () -> handler.valid(testEntity, arguments));
    }
  }

  /**
   * Sets up the mock chain for Arguments navigation.
   * The chain is: arguments -> urlPathParam -> webhook -> events -> table
   */
  private void setupArgumentsMockChain() {
    Table table = mock(Table.class);

    when(arguments.getSmfwheUrlpathparam()).thenReturn(urlPathParam);
    when(urlPathParam.getSmfwheWebhook()).thenReturn(webhook);
    when(webhook.getSmfwheEvents()).thenReturn(events);
    when(events.getTable()).thenReturn(table);
    when(table.getDBTableName()).thenReturn("test_table");
  }
}
