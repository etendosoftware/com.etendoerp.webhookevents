package com.etendoerp.webhookevents.ad_process;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Language;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

import com.etendoerp.webhookevents.data.EventType;
import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.QueueEventHook;
import com.etendoerp.webhookevents.webhook_util.Constants;
import com.etendoerp.webhookevents.webhook_util.WebHookUtil;

/**
 * Unit tests for {@link DequeueEventsFromQueue}, focused on the handling of the queue entries
 * whose affected record no longer exists (On Delete events) and on the behaviour when one entry
 * cannot be processed.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DequeueEventsFromQueueTest {

  private static final String ENTITY_NAME = "BusinessPartnerLocation";
  private static final String TABLE_NAME = "C_BPartner_Location";
  private static final String RECORD_ID = "record-id-1";
  private static final String SECOND_RECORD_ID = "record-id-2";
  private static final String SEND_WEBHOOK_MESSAGE = "Send webhook for this record: '%s'";
  private static final String ERROR_QUEUE_ENTRY_MESSAGE = "Error processing queue entry";

  private DequeueEventsFromQueue process;
  private ProcessBundle bundle;
  private ProcessLogger processLogger;

  /**
   * Forces the initialization of the production classes whose static fields read the OBContext
   * language, while a stubbed OBContext is available. Without this, the first test that mocks
   * {@link WebHookUtil} (or another class) before stubbing the OBContext would fail to initialize
   * it and poison the class for the whole JVM.
   *
   * @throws Exception if a class cannot be loaded
   */
  @BeforeAll
  static void loadProductionClassesWithContext() throws Exception {
    try (MockedStatic<OBContext> mockedContext = mockStatic(OBContext.class)) {
      stubOBContext(mockedContext);
      Class.forName(WebHookUtil.class.getName());
      Class.forName(Utility.class.getName());
      Class.forName(DequeueEventsFromQueue.class.getName());
    }
  }

  /**
   * Stubs the mocked static OBContext with a usable language, so that any production class that is
   * initialized while the mock is active can read it.
   *
   * @param mockedContext the mocked static OBContext
   */
  private static void stubOBContext(MockedStatic<OBContext> mockedContext) {
    OBContext context = mock(OBContext.class);
    Language language = mock(Language.class);
    mockedContext.when(OBContext::getOBContext).thenReturn(context);
    when(context.getLanguage()).thenReturn(language);
    when(language.getLanguage()).thenReturn("en_US");
  }

  @BeforeEach
  void setUp() {
    process = new DequeueEventsFromQueue();
    bundle = mock(ProcessBundle.class);
    processLogger = mock(ProcessLogger.class);
    when(bundle.getLogger()).thenReturn(processLogger);
  }

  /**
   * An On Delete event whose record is gone must still deliver the webhook using the record id
   * captured in the queue, log the confirmation and remove the processed entry.
   *
   * @throws Exception if the process execution fails
   */
  @Test
  void deleteEventWithMissingRecordDeliversWebhookAndLogs() throws Exception {
    Events event = buildEvent(Constants.DELETE);
    QueueEventHook entry = buildEntry("queue-1", event, RECORD_ID);

    OBDal obDal = mock(OBDal.class);
    OBCriteria<QueueEventHook> queueCriteria = mock(OBCriteria.class);
    ScrollableResults scroller = mock(ScrollableResults.class);
    OBQuery<BaseOBObject> query = mock(OBQuery.class);

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedContext = mockStatic(OBContext.class)) {
      stubOBContext(mockedContext);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      mockQueue(obDal, queueCriteria, scroller, entry);
      mockEntityResolution(mockedModelProvider);
      when(obDal.createQuery(anyString(), anyString())).thenReturn(query);
      when(query.list()).thenReturn(new ArrayList<>());
      mockedUtility.when(() -> Utility.messageBD(any(), anyString(), anyString()))
          .thenReturn(SEND_WEBHOOK_MESSAGE);

      process.doExecute(bundle);

      mockedWebHookUtil.verify(() -> WebHookUtil.callWebHookForDelete(eq(event), eq(RECORD_ID),
          any(Logger.class)));
      verify(obDal).remove(entry);
      verify(processLogger).logln(contains("Send webhook for this record"));
    }
  }

  /**
   * A non-delete event whose record is unexpectedly missing must log the error and keep the entry
   * in the queue instead of silently removing it.
   *
   * @throws Exception if the process execution fails
   */
  @Test
  void missingRecordForNonDeleteEventLogsErrorAndKeepsEntry() throws Exception {
    Events event = buildEvent(Constants.UPDATE);
    QueueEventHook entry = buildEntry("queue-1", event, RECORD_ID);

    OBDal obDal = mock(OBDal.class);
    OBCriteria<QueueEventHook> queueCriteria = mock(OBCriteria.class);
    ScrollableResults scroller = mock(ScrollableResults.class);
    OBQuery<BaseOBObject> query = mock(OBQuery.class);

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class);
         MockedStatic<OBContext> mockedContext = mockStatic(OBContext.class)) {
      stubOBContext(mockedContext);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      mockQueue(obDal, queueCriteria, scroller, entry);
      mockEntityResolution(mockedModelProvider);
      when(obDal.createQuery(anyString(), anyString())).thenReturn(query);
      when(query.list()).thenReturn(new ArrayList<>());

      assertDoesNotThrow(() -> process.doExecute(bundle));

      verify(processLogger).logln(contains(ERROR_QUEUE_ENTRY_MESSAGE));
      verify(obDal, never()).remove(entry);
      verify(processLogger, never()).logln(contains("Send webhook for this record"));
    }
  }

  /**
   * When one entry cannot be processed the process must log the error, keep that entry and
   * continue with the remaining entries.
   *
   * @throws Exception if the process execution fails
   */
  @Test
  void batchContinuesAfterOneFailingEntry() throws Exception {
    Events failingEvent = buildEvent(Constants.UPDATE);
    QueueEventHook failingEntry = buildEntry("queue-1", failingEvent, RECORD_ID);
    Events validEvent = buildEvent(Constants.UPDATE);
    QueueEventHook validEntry = buildEntry("queue-2", validEvent, SECOND_RECORD_ID);

    OBDal obDal = mock(OBDal.class);
    OBCriteria<QueueEventHook> queueCriteria = mock(OBCriteria.class);
    ScrollableResults scroller = mock(ScrollableResults.class);
    OBQuery<BaseOBObject> failingQuery = mock(OBQuery.class);
    OBQuery<BaseOBObject> validQuery = mock(OBQuery.class);

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedContext = mockStatic(OBContext.class)) {
      stubOBContext(mockedContext);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      mockQueue(obDal, queueCriteria, scroller, failingEntry, validEntry);
      mockEntityResolution(mockedModelProvider);
      when(obDal.createQuery(anyString(), anyString())).thenReturn(failingQuery, validQuery);
      when(failingQuery.list()).thenReturn(new ArrayList<>());
      BaseOBObject entity = mock(BaseOBObject.class);
      when(entity.getEntityName()).thenReturn(ENTITY_NAME);
      when(entity.getIdentifier()).thenReturn("Address 1");
      when(validQuery.list()).thenReturn(List.of(entity));
      mockedUtility.when(() -> Utility.messageBD(any(), anyString(), anyString()))
          .thenReturn(SEND_WEBHOOK_MESSAGE);

      process.doExecute(bundle);

      verify(obDal, never()).remove(failingEntry);
      verify(obDal).remove(validEntry);
      verify(processLogger).logln(contains(ERROR_QUEUE_ENTRY_MESSAGE));
      verify(processLogger).logln(contains("Send webhook for this record"));
    }
  }

  /**
   * Control case: a non-delete event whose record still exists keeps the original behaviour, it
   * delivers the webhook from the entity, logs the confirmation and removes the entry.
   *
   * @throws Exception if the process execution fails
   */
  @Test
  void existingRecordStillCallsWebHookAndRemovesEntry() throws Exception {
    Events event = buildEvent(Constants.CREATE);
    QueueEventHook entry = buildEntry("queue-1", event, RECORD_ID);
    BaseOBObject entity = mock(BaseOBObject.class);

    OBDal obDal = mock(OBDal.class);
    OBCriteria<QueueEventHook> queueCriteria = mock(OBCriteria.class);
    ScrollableResults scroller = mock(ScrollableResults.class);
    OBQuery<BaseOBObject> query = mock(OBQuery.class);

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<ModelProvider> mockedModelProvider = mockStatic(ModelProvider.class);
         MockedStatic<WebHookUtil> mockedWebHookUtil = mockStatic(WebHookUtil.class);
         MockedStatic<Utility> mockedUtility = mockStatic(Utility.class);
         MockedStatic<OBContext> mockedContext = mockStatic(OBContext.class)) {
      stubOBContext(mockedContext);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      mockQueue(obDal, queueCriteria, scroller, entry);
      mockEntityResolution(mockedModelProvider);
      when(obDal.createQuery(anyString(), anyString())).thenReturn(query);
      when(query.list()).thenReturn(List.of(entity));
      when(entity.getEntityName()).thenReturn(ENTITY_NAME);
      when(entity.getIdentifier()).thenReturn("Address 1");
      mockedUtility.when(() -> Utility.messageBD(any(), anyString(), anyString()))
          .thenReturn(SEND_WEBHOOK_MESSAGE);

      process.doExecute(bundle);

      mockedWebHookUtil.verify(
          () -> WebHookUtil.callWebHook(eq(event), eq(entity), any(Logger.class)));
      verify(obDal).remove(entry);
      verify(processLogger).logln(contains("Send webhook for this record"));
    }
  }

  private void mockQueue(OBDal obDal, OBCriteria<QueueEventHook> queueCriteria,
      ScrollableResults scroller, QueueEventHook... entries) {
    when(obDal.createCriteria(QueueEventHook.class)).thenReturn(queueCriteria);
    when(queueCriteria.scroll()).thenReturn(scroller);

    Boolean[] nextReturn = new Boolean[entries.length + 1];
    Object[][] getReturn = new Object[entries.length][];
    for (int i = 0; i < entries.length; i++) {
      nextReturn[i] = true;
      getReturn[i] = new Object[] { entries[i] };
    }
    nextReturn[entries.length] = false;
    when(scroller.next()).thenReturn(nextReturn[0],
        Arrays.copyOfRange(nextReturn, 1, nextReturn.length));
    when(scroller.get()).thenReturn(getReturn[0],
        Arrays.copyOfRange(getReturn, 1, getReturn.length));
  }

  private void mockEntityResolution(MockedStatic<ModelProvider> mockedModelProvider) {
    ModelProvider modelProvider = mock(ModelProvider.class);
    Entity entity = mock(Entity.class);
    mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
    when(modelProvider.getEntityByTableName(TABLE_NAME)).thenReturn(entity);
    when(entity.getName()).thenReturn(ENTITY_NAME);
  }

  private QueueEventHook buildEntry(String id, Events event, String recordId) {
    QueueEventHook entry = mock(QueueEventHook.class);
    when(entry.getId()).thenReturn(id);
    when(entry.getSmfwheEvents()).thenReturn(event);
    when(entry.getRecord()).thenReturn(recordId);
    Table table = mock(Table.class);
    when(table.getDBTableName()).thenReturn(TABLE_NAME);
    when(entry.getTable()).thenReturn(table);
    return entry;
  }

  private Events buildEvent(String eventTypeId) {
    Events event = mock(Events.class);
    EventType eventType = mock(EventType.class);
    when(eventType.getId()).thenReturn(eventTypeId);
    when(event.getSmfwheEventType()).thenReturn(eventType);
    when(event.getHQLWhereClause()).thenReturn(null);
    when(event.getName()).thenReturn("Test Event");
    return event;
  }
}
