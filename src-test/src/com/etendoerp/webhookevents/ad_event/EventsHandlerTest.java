package com.etendoerp.webhookevents.ad_event;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Language;
import org.openbravo.test.base.OBBaseTest;

import com.etendoerp.webhookevents.data.EventType;
import com.etendoerp.webhookevents.data.Events;

/**
 * Test class for EventsHandler, which handles saving and updating of Events.
 * It checks for unique constraints and proper handling of duplicate events.
 */
public class EventsHandlerTest extends OBBaseTest {

  private EventsHandler handler;
  private EntityNewEvent newEvent;
  private EntityUpdateEvent updateEvent;
  private Events events;
  private Events existingEvent;
  private EventType eventType;
  private Table table;
  private OBDal obDal;
  private OBCriteria<Events> obCriteria;
  private OBContext obContext;
  private Language language;

  /**
   * Sets up the test environment before each test execution.
   * Initializes all required mocks and the EventsHandler instance.
   *
   * @throws Exception if the parent setUp method fails or mock initialization encounters an error
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    super.setUp();

    handler = new EventsHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return true;
      }
    };

    newEvent = mock(EntityNewEvent.class);
    updateEvent = mock(EntityUpdateEvent.class);
    events = mock(Events.class);
    existingEvent = mock(Events.class);
    eventType = mock(EventType.class);
    table = mock(Table.class);
    obDal = mock(OBDal.class);
    obCriteria = mock(OBCriteria.class);
    obContext = mock(OBContext.class);
    language = mock(Language.class);
  }

  /**
   * Tests the onSave method of EventsHandler when a new event is saved.
   * It checks if the event is unique and does not throw an exception.
   */
  @Test
  public void testOnSaveWithUniqueEvent() {
    when(newEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(eventType);
    when(events.getTable()).thenReturn(table);

    List<Events> emptyList = new ArrayList<>();

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(emptyList); // No existing events

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onSave method of EventsHandler when a duplicate event is saved.
   * It checks if an OBException is thrown indicating the event already exists.
   */
  @Test
  public void testOnSaveWithDuplicateEvent() {
    when(newEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(eventType);
    when(events.getTable()).thenReturn(table);
    when(events.getIdentifier()).thenReturn("Test Event");

    List<Events> existingEventsList = new ArrayList<>();
    existingEventsList.add(existingEvent);

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(existingEventsList); // Existing event found

      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
      when(obContext.getLanguage()).thenReturn(language);
      when(language.getLanguage()).thenReturn("en_US");

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Event 'Test Event' already exists");

      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onUpdate method of EventsHandler when an existing event is updated.
   * It checks if the event is unique and does not throw an exception.
   */
  @Test
  public void testOnUpdateWithUniqueEvent() {
    when(updateEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(eventType);
    when(events.getTable()).thenReturn(table);
    when(events.getId()).thenReturn("current-event-id");

    List<Events> emptyList = new ArrayList<>();

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(emptyList); // No other events with same type+table

      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }
/**
   * Tests the onUpdate method of EventsHandler when a duplicate event is updated.
   * It checks if an OBException is thrown indicating the event already exists.
   */
  @Test
  public void testOnUpdateWithDuplicateEvent() {
    when(updateEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(eventType);
    when(events.getTable()).thenReturn(table);
    when(events.getId()).thenReturn("current-event-id");
    when(events.getIdentifier()).thenReturn("Test Event");

    List<Events> existingEventsList = new ArrayList<>();
    existingEventsList.add(existingEvent);

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(existingEventsList); // Another event found with same type+table

      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
      when(obContext.getLanguage()).thenReturn(language);
      when(language.getLanguage()).thenReturn("en_US");

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Event 'Test Event' already exists");

      assertThrows(OBException.class, () -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Tests the onUpdate method of EventsHandler when an event is updated with an empty identifier.
   * It checks if an OBException is thrown indicating the event already exists.
   */
  @Test
  public void testOnSaveWhenEventNotValid() {
    EventsHandler handlerWithInvalidEvent = new EventsHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onSave(newEvent));
  }

  /**
   * Tests the onUpdate method of EventsHandler when an event is updated with an empty identifier.
   * It checks if an OBException is thrown indicating the event already exists.
   */
  @Test
  public void testOnUpdateWhenEventNotValid() {
    EventsHandler handlerWithInvalidEvent = new EventsHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };

    assertDoesNotThrow(() -> handlerWithInvalidEvent.onUpdate(updateEvent));
  }

  /**
   * Tests the getObservedEntities method of EventsHandler.
   * It checks if the correct entity is being observed.
   */
  @Test
  public void testGetObservedEntities() {
    Entity[] entities = handler.getObservedEntities();

    assertEquals(1, entities.length);
    assertEquals(Events.ENTITY_NAME, entities[0].getName());
  }

  /**
   * Tests the getObservedEntity method of EventsHandler.
   * It checks if the correct entity is being observed.
   */
  @Test
  public void testOnSaveWithNullEventType() {
    when(newEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(null);
    when(events.getTable()).thenReturn(table);

    List<Events> emptyList = new ArrayList<>();

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(emptyList);

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onSave method of EventsHandler when the event's table is null.
   * It checks if no exception is thrown during the save operation.
   */
  @Test
  public void testOnSaveWithNullTable() {
    when(newEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(eventType);
    when(events.getTable()).thenReturn(null);

    List<Events> emptyList = new ArrayList<>();

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(emptyList);

      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Tests the onUpdate method of EventsHandler when the event's table is null.
   * It checks if no exception is thrown during the update operation.
   */
  @Test
  public void testOnUpdateWithMultipleDuplicateEvents() {
    // Given
    when(updateEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(eventType);
    when(events.getTable()).thenReturn(table);
    when(events.getId()).thenReturn("current-event-id");
    when(events.getIdentifier()).thenReturn("Test Event");

    List<Events> existingEventsList = new ArrayList<>();
    existingEventsList.add(existingEvent);
    existingEventsList.add(mock(Events.class));

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(existingEventsList);

      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
      when(obContext.getLanguage()).thenReturn(language);
      when(language.getLanguage()).thenReturn("en_US");

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Event 'Test Event' already exists");

      assertThrows(OBException.class, () -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Tests the onSave method of EventsHandler when the event's identifier is an empty string.
   * It checks if an OBException is thrown indicating the event already exists.
   */
  @Test
  public void testOnSaveWithEmptyIdentifier() {
    when(newEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(eventType);
    when(events.getTable()).thenReturn(table);
    when(events.getIdentifier()).thenReturn("");

    List<Events> existingEventsList = new ArrayList<>();
    existingEventsList.add(existingEvent);

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(existingEventsList);

      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
      when(obContext.getLanguage()).thenReturn(language);
      when(language.getLanguage()).thenReturn("en_US");

      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Event '' already exists");

      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }
}
