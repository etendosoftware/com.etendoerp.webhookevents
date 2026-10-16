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
import org.junit.jupiter.api.AfterEach;
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
  private static final String QUEUE_ONE = "queue-1";
  private static final String QUEUE_TWO = "queue-2";
  private static final String IDENTIFIER = "Address 1";
  private static final String SEND_WEBHOOK_MESSAGE = "Send webhook for this record: '%s'";
  private static final String SEND_WEBHOOK_LOG = "Send webhook for this record";
  private static final String ERROR_QUEUE_ENTRY_MESSAGE = "Error processing queue entry";

  private DequeueEventsFromQueue process;
  private ProcessBundle bundle;
  private ProcessLogger processLogger;
  private OBDal obDal;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<ModelProvider> mockedModelProvider;
  private MockedStatic<WebHookUtil> mockedWebHookUtil;
  private MockedStatic<Utility> mockedUtility;
  private MockedStatic<OBContext> mockedContext;

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

  @BeforeEach
  void setUp() {
    process = new DequeueEventsFromQueue();
    bundle = mock(ProcessBundle.class);
    processLogger = mock(ProcessLogger.class);
    when(bundle.getLogger()).thenReturn(processLogger);

    obDal = mock(OBDal.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedModelProvider = mockStatic(ModelProvider.class);
    mockedWebHookUtil = mockStatic(WebHookUtil.class);
    mockedUtility = mockStatic(Utility.class);
    mockedContext = mockStatic(OBContext.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    stubOBContext(mockedContext);
    mockEntityResolution();
    mockedUtility.when(() -> Utility.messageBD(any(), anyString(), anyString()))
        .thenReturn(SEND_WEBHOOK_MESSAGE);
  }

  @AfterEach
  void tearDown() {
    mockedContext.close();
    mockedUtility.close();
    mockedWebHookUtil.close();
    mockedModelProvider.close();
    mockedOBDal.close();
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
    QueueEventHook entry = buildEntry(QUEUE_ONE, event, RECORD_ID);
    stubQueue(entry);
    stubEmptyQuery();

    process.doExecute(bundle);

    mockedWebHookUtil.verify(
        () -> WebHookUtil.callWebHookForDelete(eq(event), eq(RECORD_ID), any(Logger.class)));
    verify(obDal).remove(entry);
    verify(processLogger).logln(contains(SEND_WEBHOOK_LOG));
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
    QueueEventHook entry = buildEntry(QUEUE_ONE, event, RECORD_ID);
    stubQueue(entry);
    stubEmptyQuery();

    assertDoesNotThrow(() -> process.doExecute(bundle));

    verify(processLogger).logln(contains(ERROR_QUEUE_ENTRY_MESSAGE));
    verify(obDal, never()).remove(entry);
    verify(processLogger, never()).logln(contains(SEND_WEBHOOK_LOG));
  }

  /**
   * When one entry cannot be processed the process must log the error, keep that entry and
   * continue with the remaining entries.
   *
   * @throws Exception if the process execution fails
   */
  @Test
  void batchContinuesAfterOneFailingEntry() throws Exception {
    QueueEventHook failingEntry = buildEntry(QUEUE_ONE, buildEvent(Constants.UPDATE), RECORD_ID);
    QueueEventHook validEntry = buildEntry(QUEUE_TWO, buildEvent(Constants.UPDATE),
        SECOND_RECORD_ID);
    stubQueue(failingEntry, validEntry);

    OBQuery<BaseOBObject> failingQuery = mock(OBQuery.class);
    OBQuery<BaseOBObject> validQuery = mock(OBQuery.class);
    when(obDal.createQuery(anyString(), anyString())).thenReturn(failingQuery, validQuery);
    when(failingQuery.list()).thenReturn(new ArrayList<>());
    BaseOBObject entity = buildEntity();
    when(validQuery.list()).thenReturn(List.of(entity));

    process.doExecute(bundle);

    verify(obDal, never()).remove(failingEntry);
    verify(obDal).remove(validEntry);
    verify(processLogger).logln(contains(ERROR_QUEUE_ENTRY_MESSAGE));
    verify(processLogger).logln(contains(SEND_WEBHOOK_LOG));
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
    QueueEventHook entry = buildEntry(QUEUE_ONE, event, RECORD_ID);
    stubQueue(entry);
    BaseOBObject entity = buildEntity();
    when(mockQuery().list()).thenReturn(List.of(entity));

    process.doExecute(bundle);

    mockedWebHookUtil.verify(
        () -> WebHookUtil.callWebHook(eq(event), eq(entity), any(Logger.class)));
    verify(obDal).remove(entry);
    verify(processLogger).logln(contains(SEND_WEBHOOK_LOG));
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

  private void mockEntityResolution() {
    ModelProvider modelProvider = mock(ModelProvider.class);
    Entity entity = mock(Entity.class);
    mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
    when(modelProvider.getEntityByTableName(TABLE_NAME)).thenReturn(entity);
    when(entity.getName()).thenReturn(ENTITY_NAME);
  }

  private void stubQueue(QueueEventHook... entries) {
    OBCriteria<QueueEventHook> queueCriteria = mock(OBCriteria.class);
    ScrollableResults scroller = mock(ScrollableResults.class);
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

  private OBQuery<BaseOBObject> mockQuery() {
    OBQuery<BaseOBObject> query = mock(OBQuery.class);
    when(obDal.createQuery(anyString(), anyString())).thenReturn(query);
    return query;
  }

  private void stubEmptyQuery() {
    when(mockQuery().list()).thenReturn(new ArrayList<>());
  }

  private BaseOBObject buildEntity() {
    BaseOBObject entity = mock(BaseOBObject.class);
    when(entity.getEntityName()).thenReturn(ENTITY_NAME);
    when(entity.getIdentifier()).thenReturn(IDENTIFIER);
    return entity;
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
