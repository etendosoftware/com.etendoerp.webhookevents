package com.etendoerp.webhookevents.ad_event;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.model.Entity;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.test.base.OBBaseTest;

import com.etendoerp.webhookevents.webhook_util.Constants;
import com.etendoerp.webhookevents.webhook_util.WebHookUtil;

/**
 * Test class for GenericEventHook, which handles entity persistence events and queues webhooks.
 * This class tests the onSave, onUpdate, and onDelete methods for successful and exceptional cases,
 * as well as the behavior when events are not valid.
 */
public class GenericEventHookTest extends OBBaseTest {

  private GenericEventHook handler;
  private EntityNewEvent newEvent;
  private EntityUpdateEvent updateEvent;
  private EntityDeleteEvent deleteEvent;
  private BaseOBObject baseOBObject;
  private Entity entity;

  
  @Before
  public void setUp() throws Exception {
    super.setUp();

    handler = new GenericEventHook() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return true;
      }
    };

    newEvent = mock(EntityNewEvent.class);
    updateEvent = mock(EntityUpdateEvent.class);
    deleteEvent = mock(EntityDeleteEvent.class);
    baseOBObject = mock(BaseOBObject.class);
    entity = mock(Entity.class);
  }

  /**
   * Tests the onSave method of GenericEventHook.
   * It verifies that the method correctly queues a web hook event for a new entity.
   */
  @Test
  public void testOnSaveSuccessful() {
    when(newEvent.getTargetInstance()).thenReturn(baseOBObject);
    when(baseOBObject.getEntity()).thenReturn(entity);
    when(entity.getTableName()).thenReturn("test_table");
    when(entity.getTableId()).thenReturn("table_id_123");
    when(baseOBObject.get("id")).thenReturn("record_id_456");

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      assertDoesNotThrow(() -> handler.onSave(newEvent));

      mockedWebHookUtil.verify(() -> WebHookUtil.queueEventFromEventHandler(
          eq("test_table"),
          eq("table_id_123"),
          eq(Constants.CREATE),
          eq("record_id_456")
      ), times(1));
    }
  }

  /**
   * Tests the onUpdate method of GenericEventHook.
   * It verifies that the method correctly queues a web hook event for an updated entity.
   */
  @Test
  public void testOnUpdateSuccessful() {
    when(updateEvent.getTargetInstance()).thenReturn(baseOBObject);
    when(baseOBObject.getEntity()).thenReturn(entity);
    when(entity.getTableName()).thenReturn("test_table");
    when(entity.getTableId()).thenReturn("table_id_123");
    when(baseOBObject.get("id")).thenReturn("record_id_456");

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));

      mockedWebHookUtil.verify(() -> WebHookUtil.queueEventFromEventHandler(
          eq("test_table"),
          eq("table_id_123"),
          eq(Constants.UPDATE),
          eq("record_id_456")
      ), times(1));
    }
  }

  /**
   * Tests the onDelete method of GenericEventHook.
   * It verifies that the method correctly queues a web hook event for a deleted entity.
   */
  @Test
  public void testOnDeleteSuccessful() {
    when(deleteEvent.getTargetInstance()).thenReturn(baseOBObject);
    when(baseOBObject.getEntity()).thenReturn(entity);
    when(entity.getTableName()).thenReturn("test_table");
    when(entity.getTableId()).thenReturn("table_id_123");
    when(baseOBObject.get("id")).thenReturn("record_id_456");

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      assertDoesNotThrow(() -> handler.onDelete(deleteEvent));

      mockedWebHookUtil.verify(() -> WebHookUtil.queueEventFromEventHandler(
          eq("test_table"),
          eq("table_id_123"),
          eq(Constants.DELETE),
          eq("record_id_456")
      ), times(1));
    }
  }

  /**
   * Tests the onSave method of GenericEventHook when an exception occurs during web hook processing.
   * It verifies that the method handles the exception gracefully without throwing it.
   */
  @Test
  public void testOnSaveWithException() {
    when(newEvent.getTargetInstance()).thenReturn(baseOBObject);
    when(baseOBObject.getEntity()).thenReturn(entity);
    when(entity.getTableName()).thenReturn("test_table");
    when(entity.getTableId()).thenReturn("table_id_123");
    when(baseOBObject.get("id")).thenReturn("record_id_456");

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      mockedWebHookUtil.when(() -> WebHookUtil.queueEventFromEventHandler(anyString(), anyString(), anyString(), anyString()))
          .thenThrow(new RuntimeException("WebHook processing failed"));

      assertDoesNotThrow(() -> handler.onSave(newEvent));

      mockedWebHookUtil.verify(() -> WebHookUtil.queueEventFromEventHandler(
          eq("test_table"),
          eq("table_id_123"),
          eq(Constants.CREATE),
          eq("record_id_456")
      ), times(1));
    }
  }

  /**
   * Tests the onUpdate method of GenericEventHook when an exception occurs during web hook processing.
   * It verifies that the method handles the exception gracefully without throwing it.
   */
  @Test
  public void testOnUpdateWithException() {
    when(updateEvent.getTargetInstance()).thenReturn(baseOBObject);
    when(baseOBObject.getEntity()).thenReturn(entity);
    when(entity.getTableName()).thenReturn("test_table");
    when(entity.getTableId()).thenReturn("table_id_123");
    when(baseOBObject.get("id")).thenReturn("record_id_456");

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      mockedWebHookUtil.when(() -> WebHookUtil.queueEventFromEventHandler(anyString(), anyString(), anyString(), anyString()))
          .thenThrow(new RuntimeException("WebHook processing failed"));

      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));

      mockedWebHookUtil.verify(() -> WebHookUtil.queueEventFromEventHandler(
          eq("test_table"),
          eq("table_id_123"),
          eq(Constants.UPDATE),
          eq("record_id_456")
      ), times(1));
    }
  }

  /**
   * Tests the onDelete method of GenericEventHook when an exception occurs during web hook processing.
   * It verifies that the method handles the exception gracefully without throwing it.
   */
  @Test
  public void testOnDeleteWithException() {
    when(deleteEvent.getTargetInstance()).thenReturn(baseOBObject);
    when(baseOBObject.getEntity()).thenReturn(entity);
    when(entity.getTableName()).thenReturn("test_table");
    when(entity.getTableId()).thenReturn("table_id_123");
    when(baseOBObject.get("id")).thenReturn("record_id_456");

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      mockedWebHookUtil.when(() -> WebHookUtil.queueEventFromEventHandler(anyString(), anyString(), anyString(), anyString()))
          .thenThrow(new RuntimeException("WebHook processing failed"));

      assertDoesNotThrow(() -> handler.onDelete(deleteEvent));

      mockedWebHookUtil.verify(() -> WebHookUtil.queueEventFromEventHandler(
          eq("test_table"),
          eq("table_id_123"),
          eq(Constants.DELETE),
          eq("record_id_456")
      ), times(1));
    }
  }

  /**
   * Tests the onSave method of GenericEventHook when the event is not valid.
   * It verifies that the method does not queue a web hook event and does not throw an exception.
   */
  @Test
  public void testOnSaveWhenEventNotValid() {
    GenericEventHook handlerWithInvalidEvent = new GenericEventHook() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      assertDoesNotThrow(() -> handlerWithInvalidEvent.onSave(newEvent));

      mockedWebHookUtil.verifyNoInteractions();
    }
  }

  /**
   * Tests the onUpdate method of GenericEventHook when the event is not valid.
   * It verifies that the method does not queue a web hook event and does not throw an exception.
   */
  @Test
  public void testOnUpdateWhenEventNotValid() {
    GenericEventHook handlerWithInvalidEvent = new GenericEventHook() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      assertDoesNotThrow(() -> handlerWithInvalidEvent.onUpdate(updateEvent));

      mockedWebHookUtil.verifyNoInteractions();
    }
  }

  /**
   * Tests the onDelete method of GenericEventHook when the event is not valid.
   * It verifies that the method does not queue a web hook event and does not throw an exception.
   */
  @Test
  public void testOnDeleteWhenEventNotValid() {
    GenericEventHook handlerWithInvalidEvent = new GenericEventHook() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      assertDoesNotThrow(() -> handlerWithInvalidEvent.onDelete(deleteEvent));

      mockedWebHookUtil.verifyNoInteractions();
    }
  }

  /**
   * Tests the getObservedEntities method of GenericEventHook.
   * It verifies that the method returns the correct entities observed by the handler.
   */
  @Test
  public void testGetObservedEntities() {
    Entity[] mockEntities = { mock(Entity.class), mock(Entity.class) };

    try (MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class)) {
      mockedWebHookUtil.when(WebHookUtil::getEntities).thenReturn(mockEntities);

      Entity[] entities = handler.getObservedEntities();

      assertEquals(mockEntities, entities);
      assertEquals(2, entities.length);

      mockedWebHookUtil.verify(WebHookUtil::getEntities, times(1));
    }
  }

}
