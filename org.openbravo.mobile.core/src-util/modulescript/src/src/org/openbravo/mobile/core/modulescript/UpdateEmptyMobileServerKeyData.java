//Sqlc generated V1.O00-1
package org.openbravo.mobile.core.modulescript;

import java.sql.*;

import org.apache.log4j.Logger;

import javax.servlet.ServletException;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import java.util.*;

class UpdateEmptyMobileServerKeyData implements FieldProvider {
  static Logger log4j = Logger.getLogger(UpdateEmptyMobileServerKeyData.class);
  private String InitRecordNumber = "0";
  public String obmobcServerDefinitionId;
  public String name;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("obmobc_server_definition_id")
        || fieldName.equals("obmobcServerDefinitionId"))
      return obmobcServerDefinitionId;
    else if (fieldName.equalsIgnoreCase("name"))
      return name;
    else {
      log4j.debug("Field does not exist: " + fieldName);
      return null;
    }
  }

  public static UpdateEmptyMobileServerKeyData[] select(ConnectionProvider connectionProvider)
      throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static UpdateEmptyMobileServerKeyData[] select(ConnectionProvider connectionProvider,
      int firstRegister, int numberRegisters) throws ServletException {
    String strSql = "";
    strSql = strSql + "        SELECT obmobc_server_definition_id, name"
        + "        FROM obmobc_server_definition " + "        WHERE mobile_server_key is null";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      long countRecord = 0;
      long countRecordSkip = 1;
      boolean continueResult = true;
      while (countRecordSkip < firstRegister && continueResult) {
        continueResult = result.next();
        countRecordSkip++;
      }
      while (continueResult && result.next()) {
        countRecord++;
        UpdateEmptyMobileServerKeyData objectUpdateEmptyMobileServerKeyData = new UpdateEmptyMobileServerKeyData();
        objectUpdateEmptyMobileServerKeyData.obmobcServerDefinitionId = UtilSql.getValue(result,
            "obmobc_server_definition_id");
        objectUpdateEmptyMobileServerKeyData.name = UtilSql.getValue(result, "name");
        objectUpdateEmptyMobileServerKeyData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectUpdateEmptyMobileServerKeyData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
          + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        log4j.error("Ignore Exception:" + ignore.getMessage(), ignore);
      }
    }
    UpdateEmptyMobileServerKeyData objectUpdateEmptyMobileServerKeyData[] = new UpdateEmptyMobileServerKeyData[vector
        .size()];
    vector.copyInto(objectUpdateEmptyMobileServerKeyData);
    return (objectUpdateEmptyMobileServerKeyData);
  }

  public static int updateMobileServerKey(ConnectionProvider connectionProvider,
      String mobileServerKey, String serverDefinitionId) throws ServletException {
    String strSql = "";
    strSql = strSql
        + "      UPDATE obmobc_server_definition SET mobile_server_key = ? WHERE obmobc_server_definition_id = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, mobileServerKey);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, serverDefinitionId);

      updateCount = st.executeUpdate();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
          + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        log4j.error("Ignore Exception:" + ignore.getMessage(), ignore);
      }
    }
    return (updateCount);
  }
}
