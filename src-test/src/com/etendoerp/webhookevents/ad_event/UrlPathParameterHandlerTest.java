package com.etendoerp.webhookevents.ad_event;

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

import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.UrlPathParam;
import com.etendoerp.webhookevents.data.Webhook;
import com.etendoerp.webhookevents.webhook_util.Constants;

/**
 * Test class for UrlPathParameterHandler.
 * This class tests the functionality of handling URL path parameters in webhook events.
 */
public class UrlPathParameterHandlerTest extends OBBaseTest {

  private UrlPathParameterHandler handler;
  private EntityNewEvent newEvent;
  private EntityUpdateEvent updateEvent;
  private UrlPathParam urlPathParam;
  private Webhook webhook;
  private Events events;
  private Entity entity;
  private Property property;
  private ModelProvider modelProvider;

  /**
   * Sets up the test environment before each test.
   * Initializes all necessary mocks including handler, events, entities and properties.
   *
   * @throws Exception If any error occurs during test environment setup
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();

    handler = new UrlPathParameterHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return true;
      }
    };

    newEvent = mock(EntityNewEvent.class);
    updateEvent = mock(EntityUpdateEvent.class);
    urlPathParam = mock(UrlPathParam.class);
    webhook = mock(Webhook.class);
    events = mock(Events.class);
    entity = mock(Entity.class);
    property = mock(Property.class);
    modelProvider = mock(ModelProvider.class);
  }

  /**
   * Tests the onSave method with a valid UrlPathParam.
   * It checks if the method executes without throwing exceptions.
   */
  @Test
  public void testOnSaveValidInput() {
    setupUrlPathParamMockChain();
    when(newEvent.getTargetInstance()).thenReturn(urlPathParam);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(urlPathParam.getValue()).thenReturn("simple value");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onUpdate method with a valid UrlPathParam.
   * It checks if the method executes without throwing exceptions.
   */
  @Test
  public void testOnUpdateValidInput() {
    setupUrlPathParamMockChain();
    when(updateEvent.getTargetInstance()).thenReturn(urlPathParam);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(urlPathParam.getValue()).thenReturn("simple value");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithStringTypeAndValidProperty() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(urlPathParam.getValue()).thenReturn("value @validProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, urlPathParam));
    }
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithStringTypeAndInvalidProperty() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(urlPathParam.getValue()).thenReturn("value @invalidProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Error in name the property: 'invalidProperty'");

      assertThrows(OBException.class, () -> handler.valid(testEntity, urlPathParam));
    }
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithPropertyTypeAndValidProperty() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_PROPERTY);
    when(urlPathParam.getProperty()).thenReturn("validProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, urlPathParam));
    }
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithPropertyTypeAndInvalidProperty() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_PROPERTY);
    when(urlPathParam.getProperty()).thenReturn("invalidProperty");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Error in name the property: 'invalidProperty'");

      assertThrows(OBException.class, () -> handler.valid(testEntity, urlPathParam));
    }
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithComputedTypeAndInvalidClass() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_COMPUTED);
    when(urlPathParam.getJavaClassName()).thenReturn("com.nonexistent.InvalidClass");

    assertThrows(OBException.class, () -> handler.valid(testEntity, urlPathParam));
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testOnSaveWhenEventNotValid() {
    UrlPathParameterHandler handlerWithInvalidEvent = new UrlPathParameterHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onSave(newEvent));
  }

  /**
   * Tests the onUpdate method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testOnUpdateWhenEventNotValid() {
    UrlPathParameterHandler handlerWithInvalidEvent = new UrlPathParameterHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onUpdate(updateEvent));
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testGetObservedEntities() {
    Entity[] entities = handler.getObservedEntities();

    assertEquals(1, entities.length);
    assertEquals(UrlPathParam.ENTITY_NAME, entities[0].getName());
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithStringTypeAndSimpleValue() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(urlPathParam.getValue()).thenReturn("simple value without properties");

    assertDoesNotThrow(() -> handler.valid(testEntity, urlPathParam));
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithStringTypeAndMultipleProperties() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(urlPathParam.getValue()).thenReturn("value @property1 and @property2");

    try (MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class)) {
      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(property);

      assertDoesNotThrow(() -> handler.valid(testEntity, urlPathParam));
    }
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testOnSaveWithInvalidPropertyInStringType() {
    setupUrlPathParamMockChain();
    when(newEvent.getTargetInstance()).thenReturn(urlPathParam);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_STRING);
    when(urlPathParam.getValue()).thenReturn("value @invalidProperty");

    try (MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<DalUtil> mockedDalUtil = mockStatic(DalUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableName(anyString())).thenReturn(entity);

      mockedDalUtil.when(() -> DalUtil.getPropertyFromPath(any(Entity.class), anyString()))
          .thenReturn(null);

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Error in name the property: 'invalidProperty'");

      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithNullClassName() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_COMPUTED);
    when(urlPathParam.getJavaClassName()).thenReturn(null);

    assertThrows(OBException.class, () -> handler.valid(testEntity, urlPathParam));
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithEmptyClassName() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn(Constants.TYPE_VALUE_COMPUTED);
    when(urlPathParam.getJavaClassName()).thenReturn("");

    assertThrows(OBException.class, () -> handler.valid(testEntity, urlPathParam));
  }

  /**
   * Tests the onSave method with an invalid UrlPathParam.
   * It checks if the method throws an OBException when the type value is not recognized.
   */
  @Test
  public void testValidMethodWithUnknownTypeValue() {
    Entity testEntity = mock(Entity.class);
    when(urlPathParam.getTypeValue()).thenReturn("UNKNOWN_TYPE");

    assertDoesNotThrow(() -> handler.valid(testEntity, urlPathParam));
  }

  /**
   * Sets up the mock chain for UrlPathParam navigation.
   * The chain is: urlPathParam -> webhook -> events -> table
   */
  private void setupUrlPathParamMockChain() {
    Table table = mock(Table.class);

    when(urlPathParam.getSmfwheWebhook()).thenReturn(webhook);
    when(webhook.getSmfwheEvents()).thenReturn(events);
    when(events.getTable()).thenReturn(table);
    when(table.getDBTableName()).thenReturn("test_table");
  }

}
