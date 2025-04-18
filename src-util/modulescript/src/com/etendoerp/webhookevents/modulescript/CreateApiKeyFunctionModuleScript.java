package com.etendoerp.webhookevents.modulescript;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

public class CreateApiKeyFunctionModuleScript extends ModuleScript {
  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute() {
    log4j.debug("Creating SMFWHE_APIKEY database function");

    ConnectionProvider connectionProvider = getConnectionProvider();
    StringBuilder functionCreationQuery = new StringBuilder();
    PreparedStatement preparedStatement = null;

    try {
      if (StringUtils.equalsIgnoreCase("ORACLE", connectionProvider.getRDBMS())) {
        log4j.debug("ORACLE database detected");

        functionCreationQuery.append("CREATE OR REPLACE FUNCTION SMFWHE_APIKEY RETURN VARCHAR2 IS \n");
        functionCreationQuery.append("    v_random1 VARCHAR2(32); \n");
        functionCreationQuery.append("    v_random2 VARCHAR2(32); \n");
        functionCreationQuery.append("BEGIN \n");

        // Generate random values and calculate MD5 hash
        functionCreationQuery.append("    SELECT LOWER(RAWTOHEX(DBMS_OBFUSCATION_TOOLKIT.md5(input_string => TO_CHAR(DBMS_RANDOM.VALUE)))) \n");
        functionCreationQuery.append("    INTO v_random1 FROM dual; \n\n");
        functionCreationQuery.append("    SELECT LOWER(RAWTOHEX(DBMS_OBFUSCATION_TOOLKIT.md5(input_string => TO_CHAR(DBMS_RANDOM.VALUE)))) \n");
        functionCreationQuery.append("    INTO v_random2 FROM dual; \n\n");

        // Concatenate the MD5 values to form a 64 characters long string
        functionCreationQuery.append("    RETURN v_random1 || v_random2; \n");
        functionCreationQuery.append("END SMFWHE_APIKEY;");

        preparedStatement = connectionProvider.getPreparedStatement(functionCreationQuery.toString());
        preparedStatement.executeUpdate();
      }
    } catch (Exception ex) {
      handleError(ex);
    } finally {
      if (preparedStatement != null) {
        try {
          preparedStatement.close();
        } catch (Exception e) {
          log4j.error("Error closing PreparedStatement", e);
        }
      }
    }
  }
}
