package com.etendoerp.webhookevents;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.test.base.TestConstants;

public abstract class WebhookBaseTest extends WeldBaseTest {

  protected WebhookUtils webhookUtils;

  @Override
  protected void setTestUserContext() {
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
  }

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    ensureWebhookUtils();
  }

  protected void ensureWebhookUtils() {
    if (webhookUtils == null) {
      webhookUtils = new WebhookUtils();
    }
    webhookUtils.setupUserSystem();
  }

  @AfterEach
  public void tearDown() {
    try {
      if (webhookUtils != null) {
        webhookUtils.setupUserSystem();
        webhookUtils.deleteAll();
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      // If cleanup fails (e.g. the test left the PostgreSQL transaction in aborted state),
      // roll back instead of leaving the connection dirty for the next test.
      OBDal.getInstance().rollbackAndClose();
    }
  }
}
