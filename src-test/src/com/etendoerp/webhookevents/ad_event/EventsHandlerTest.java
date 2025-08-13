package com.etendoerp.webhookevents.ad_event;

import static com.etendoerp.webhookevents.WebhookTestConstants.CURRENT_EVENT_ID;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_EVENT_ALREADY_EXISTS_MESSAGE;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_EVENT_LANG;
import static com.etendoerp.webhookevents.WebhookTestConstants.TEST_EVENT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
   * @throws Exception
   *     if the parent setUp method fails or mock initialization encounters an error
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    super.setUp();
    initializeMocks();
    setupBasicMockBehavior();
  }

  /**
   * Initializes all the necessary mocks for the EventsHandler tests.
   * This includes mocks for EntityNewEvent, EntityUpdateEvent, Events, EventType, Table,
   * OBDal, OBCriteria, OBContext, and Language.
   */
  private void initializeMocks() {
    handler = createValidEventsHandler();
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
   * Sets up the basic mock behavior for the EventsHandler tests.
   * This includes setting up the target instance, event type, table, and other necessary properties.
   */
  private void setupBasicMockBehavior() {
    when(newEvent.getTargetInstance()).thenReturn(events);
    when(updateEvent.getTargetInstance()).thenReturn(events);
    when(events.getSmfwheEventType()).thenReturn(eventType);
    when(events.getTable()).thenReturn(table);
    when(events.getId()).thenReturn(CURRENT_EVENT_ID);
    when(events.getIdentifier()).thenReturn(TEST_EVENT_NAME);

    when(obDal.createCriteria(Events.class)).thenReturn(obCriteria);
    when(obCriteria.add(any())).thenReturn(obCriteria);

    when(obContext.getLanguage()).thenReturn(language);
    when(language.getLanguage()).thenReturn(TEST_EVENT_LANG);
  }

  /**
   * Creates a valid EventsHandler that always returns true for event validity.
   * This is used to test the behavior of the handler when events are valid.
   *
   * @return an instance of EventsHandler with overridden isValidEvent method
   */
  private EventsHandler createValidEventsHandler() {
    return new EventsHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return true;
      }
    };
  }

  /**
   * Creates an invalid EventsHandler that always returns false for event validity.
   * This is used to test the behavior of the handler when events are not valid.
   *
   * @return an instance of EventsHandler with overridden isValidEvent method
   */
  private EventsHandler createInvalidEventsHandler() {
    return new EventsHandler() {
      @Override
      protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
        return false;
      }
    };
  }

  /**
   * Sets up the mocked static methods for a scenario where duplicate events are expected.
   * This includes mocking OBDal, Utility, and OBContext to return predefined values.
   *
   * @param mockedOBDal
   *     the mocked OBDal instance
   * @param mockedUtility
   *     the mocked Utility instance
   * @param mockedOBContext
   *     the mocked OBContext instance
   */
  private void setupMockedStaticsForDuplicateScenario(MockedStatic<OBDal> mockedOBDal,
      MockedStatic<Utility> mockedUtility,
      MockedStatic<OBContext> mockedOBContext) {
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
        .thenReturn(TEST_EVENT_ALREADY_EXISTS_MESSAGE);
  }

  /**
   * Mocks the OBCriteria to return a predefined list of results.
   * This is used to simulate the behavior of the database when checking for existing events.
   *
   * @param results
   *     the list of Events that should be returned by the criteria
   */
  private void mockCriteriaWithResults(List<Events> results) {
    when(obCriteria.list()).thenReturn(results);
  }

  /**
   * Executes the provided test action with mocked static methods for a scenario where unique events
   * are expected.
   *
   * @param testAction
   *     the action to be executed, typically a save or update operation on an event
   */
  private void executeWithUniqueEventMocks(Runnable testAction) {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      mockCriteriaWithResults(new ArrayList<>());
      testAction.run();
    }
  }

  /**
   * Executes the provided test action with mocked static methods for a scenario where duplicate events
   * are expected.
   *
   * @param testAction
   *     the action to be executed, typically a save or update operation on an event
   * @param existingEvents
   *     a list of existing events that should trigger the duplicate check
   */
  private void executeWithDuplicateEventMocks(Runnable testAction, List<Events> existingEvents) {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupMockedStaticsForDuplicateScenario(mockedOBDal, mockedUtility, mockedOBContext);
      mockCriteriaWithResults(existingEvents);
      testAction.run();
    }
  }

  /**
   * Tests the onSave method of EventsHandler when an event is saved with a unique identifier.
   * It checks if no exception is thrown during the save operation.
   */
  @Test
  public void testOnSaveWithUniqueEvent() {
    executeWithUniqueEventMocks(() ->
        assertDoesNotThrow(() -> handler.onSave(newEvent))
    );
  }

  /**
   * Tests the onSave method of EventsHandler when an event is saved with a duplicate identifier.
   * It checks if an OBException is thrown indicating that the event already exists.
   */
  @Test
  public void testOnSaveWithDuplicateEvent() {
    executeWithDuplicateEventMocks(() ->
            assertThrows(OBException.class, () -> handler.onSave(newEvent)),
        Collections.singletonList(existingEvent)
    );
  }

  /**
   * Tests the onUpdate method of EventsHandler when an event is updated with a unique identifier.
   * It checks if no exception is thrown during the update operation.
   */
  @Test
  public void testOnUpdateWithUniqueEvent() {
    executeWithUniqueEventMocks(() ->
        assertDoesNotThrow(() -> handler.onUpdate(updateEvent))
    );
  }

  /**
   * Tests the onUpdate method of EventsHandler when an event is updated with a duplicate identifier.
   * It checks if an OBException is thrown indicating that the event already exists.
   */
  @Test
  public void testOnUpdateWithDuplicateEvent() {
    executeWithDuplicateEventMocks(() ->
            assertThrows(OBException.class, () -> handler.onUpdate(updateEvent)),
        Collections.singletonList(existingEvent)
    );
  }

  /**
   * Tests the onUpdate method of EventsHandler when multiple duplicate events are found.
   * It checks if an OBException is thrown indicating that the event already exists.
   */
  @Test
  public void testOnUpdateWithMultipleDuplicateEvents() {
    executeWithDuplicateEventMocks(() ->
            assertThrows(OBException.class, () -> handler.onUpdate(updateEvent)),
        Arrays.asList(existingEvent, mock(Events.class))
    );
  }

  /**
   * Tests the onSave method of EventsHandler when an event is saved with an empty identifier.
   * It checks if an OBException is thrown indicating the event already exists.
   */
  @Test
  public void testOnSaveWhenEventNotValid() {
    EventsHandler invalidHandler = createInvalidEventsHandler();
    assertDoesNotThrow(() -> invalidHandler.onSave(newEvent));
  }

  /**
   * Tests the onUpdate method of EventsHandler when an event is updated with an empty identifier.
   * It checks if an OBException is thrown indicating the event already exists.
   */
  @Test
  public void testOnUpdateWhenEventNotValid() {
    EventsHandler invalidHandler = createInvalidEventsHandler();
    assertDoesNotThrow(() -> invalidHandler.onUpdate(updateEvent));
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
    when(events.getSmfwheEventType()).thenReturn(null);
    executeWithUniqueEventMocks(() ->
        assertDoesNotThrow(() -> handler.onSave(newEvent))
    );
  }

  /**
   * Tests the onSave method of EventsHandler when the event's table is null.
   * It checks if no exception is thrown during the save operation.
   */
  @Test
  public void testOnSaveWithNullTable() {
    when(events.getTable()).thenReturn(null);
    executeWithUniqueEventMocks(() ->
        assertDoesNotThrow(() -> handler.onSave(newEvent))
    );
  }

  /**
   * Tests the onSave method when the event identifier is empty.
   * It should throw an OBException indicating that the event already exists.
   */
  @Test
  public void testOnSaveWithEmptyIdentifier() {
    when(events.getIdentifier()).thenReturn("");

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {

      setupMockedStaticsForDuplicateScenario(mockedOBDal, mockedUtility, mockedOBContext);
      mockedUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
          .thenReturn("Event '' already exists");
      mockCriteriaWithResults(Collections.singletonList(existingEvent));

      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }
}
