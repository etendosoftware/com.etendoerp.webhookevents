package com.etendoerp.webhookevents;

/**
 * This class contains constants used for testing webhook events.
 * It includes various strings that represent error messages, event names,
 * table names, record IDs, and other relevant identifiers.
 */
public class WebhookTestConstants {
  public static final String INVALID_PROPERTY_VALUE = "value @nonExistentProperty";
  public static final String INVALID_PROPERTY_ERROR_MESSAGE = "Error in name the property: 'nonExistentProperty'";

  public static final String TEST_EVENT_NAME = "Test Event";
  public static final String TEST_EVENT_LANG = "en_US";
  public static final String TEST_EVENT_ALREADY_EXISTS_MESSAGE = "Event 'Test Event' already exists";
  public static final String CURRENT_EVENT_ID = "current-event-id";

  public static final String TEST_TABLE_NAME = "test_table";
  public static final String TEST_TABLE_ID = "table_id_123";
  public static final String TEST_RECORD_ID = "record_id_456";

  public static final String WEBHOOK_PROCESSING_FAILED_MESSAGE = "WebHook processing failed";
  public static final String SIMPLE_VALUE = "simple value";
  public static final String INVALID_PROPERTY_ERROR_MESSAGE_ALT = "Error in name the property: 'invalidProperty'";

  public static final String TEST_FLOW_NAME = "testFlow";
  public static final String TEST_DESCRIPTION = "Test Description";
  public static final String TEST_WEBHOOK_ENDPOINT = "/webhooks/testWebhook";

  public static final String GET_DEFINED_WEBHOOK_ROLE = "getDefinedwebhookRole";
  public static final String CHECK_ROLE_SECURITY = "checkRoleSecurity";

  public static final String DEFAULT_API_PREFIX = "/api";
  public static final String API_V2_PREFIX = "/api/v2";
  public static final String LEGACY_V1_PREFIX = "/v1";
  public static final String API_V1_PREFIX = "/api/v1";

  public static final String DESCRIPTION = "description";
  public static final String PARAMS = "params";
  public static final String SUMMARY = "summary";
  public static final String REQUEST_BODY = "requestBody";
  public static final String REQUIRED = "required";
  public static final String CONTENT = "content";
  public static final String APPLICATION_JSON = "application/json";
  public static final String SCHEMA = "schema";
  public static final String USER_ID = "userId";
  public static final String STRING = "string";
  public static final String AMOUNT = "amount";
  public static final String WEBHOOK = "webhook";
  public static final String WEBHOOK_PATH = "/webhook";

  public static final String RECORD_ID = "record-id";
  public static final String VALUE2 = "value2";
  public static final String PARAM2 = "param2";
  public static final String VALUE1 = "value1";
  public static final String PARAM1 = "param1";
  public static final String ITEMS = "items";

  public static final String TEST_EVENT_TYPE_ID = "test-event-type-id";
  public static final String TEST_EVENT_CLASS = "test-event-class";
  public static final String CONTENT_TYPE = "Content-Type";

  /**
   * Private constructor to prevent instantiation of this utility class.
   * Throws UnsupportedOperationException if called.
   */
  private WebhookTestConstants() {
    throw new UnsupportedOperationException("This is a utility class and should not be instantiated");
  }

}
