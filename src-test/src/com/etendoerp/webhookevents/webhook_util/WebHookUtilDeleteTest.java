package com.etendoerp.webhookevents.webhook_util;

import static com.etendoerp.webhookevents.WebhookTestConstants.RECORD_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Language;

import com.etendoerp.webhookevents.data.Events;
import com.etendoerp.webhookevents.data.Webhook;

/**
 * Unit tests for the On Delete delivery methods of {@link WebHookUtil}: the notification sent by
 * {@link WebHookUtil#sendDeleteNotification} and the webhook iteration of
 * {@link WebHookUtil#callWebHookForDelete}.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class WebHookUtilDeleteTest {

  private static final String WEBHOOK_URL = "http://example.com/delete";
  private static final String UNREACHABLE_URL = "http://invalid url with spaces";
  private static final String POST_METHOD = "POST";

  /**
   * Forces the initialization of {@link WebHookUtil} with a usable OBContext, so that mocking other
   * classes cannot poison it for the rest of the JVM.
   *
   * @throws Exception if the class cannot be loaded
   */
  @BeforeAll
  static void loadWebHookUtilWithContext() throws Exception {
    try (MockedStatic<OBContext> mockedContext = mockStatic(OBContext.class)) {
      OBContext context = mock(OBContext.class);
      Language language = mock(Language.class);
      mockedContext.when(OBContext::getOBContext).thenReturn(context);
      when(context.getLanguage()).thenReturn(language);
      when(language.getLanguage()).thenReturn("en_US");
      Class.forName(WebHookUtil.class.getName());
    }
  }

  /**
   * The delete notification must write the minimal payload built from the data captured in the
   * queue (the deleted record id and the delete event type) and set the event's HTTP method.
   *
   * @throws Exception if the notification fails
   */
  @Test
  void testSendDeleteNotificationWritesMinimalDeletePayload() throws Exception {
    Webhook webhook = mock(Webhook.class);
    Events event = mock(Events.class);
    when(webhook.getUrlnotify()).thenReturn(WEBHOOK_URL);
    when(webhook.getSmfwheEvents()).thenReturn(event);
    when(event.getMethod()).thenReturn(POST_METHOD);

    HttpURLConnection connection = mock(HttpURLConnection.class);
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    when(connection.getOutputStream()).thenReturn(body);
    when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(connection.getInputStream()).thenReturn(
        new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)));

    WebHookUtil.sendDeleteNotification(webhook, connection, RECORD_ID, mock(Logger.class));

    verify(connection).setRequestMethod(POST_METHOD);
    String payload = body.toString(StandardCharsets.UTF_8);
    assertTrue(payload.contains("\"id\":\"" + RECORD_ID + "\""));
    assertTrue(payload.contains(Constants.DELETE));
  }

  /**
   * callWebHookForDelete must ignore inactive webhooks, so no request is attempted for them.
   */
  @Test
  void testCallWebHookForDeleteSkipsInactiveWebhook() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      OBDal obDal = mock(OBDal.class);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      stubWebhooks(obDal, buildWebhook(false, UNREACHABLE_URL));

      assertDoesNotThrow(() -> WebHookUtil.callWebHookForDelete(mock(Events.class), RECORD_ID,
          mock(Logger.class)));
    }
  }

  /**
   * callWebHookForDelete must wrap a delivery failure in an OBException, so the caller can keep the
   * queue entry instead of discarding it silently.
   */
  @Test
  void testCallWebHookForDeleteWrapsFailureInOBException() {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      OBDal obDal = mock(OBDal.class);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      stubWebhooks(obDal, buildWebhook(true, UNREACHABLE_URL));

      Events event = mock(Events.class);
      Logger logger = mock(Logger.class);
      assertThrows(OBException.class,
          () -> WebHookUtil.callWebHookForDelete(event, RECORD_ID, logger));
    }
  }

  private Webhook buildWebhook(boolean active, String url) {
    Webhook webhook = mock(Webhook.class);
    when(webhook.isActive()).thenReturn(active);
    when(webhook.getUrlnotify()).thenReturn(url);
    return webhook;
  }

  private void stubWebhooks(OBDal obDal, Webhook webhook) {
    OBCriteria<Webhook> webhookCriteria = mock(OBCriteria.class);
    when(obDal.createCriteria(Webhook.class)).thenReturn(webhookCriteria);
    when(webhookCriteria.add(any())).thenReturn(webhookCriteria);
    when(webhookCriteria.list()).thenReturn(List.of(webhook));
  }
}
