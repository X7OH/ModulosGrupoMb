/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.modulescript;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * Sets the mobile server key columns of those module servers that do not define it. The value will
 * be taken from the server's name, after replacing the spaces with underscore
 * 
 * @author AugustoMauch
 */
public class UpdateEmptyMobileServerKey extends ModuleScript {

  private static final String MOBILE_CORE_MODULE_ID = "08943B85ADF64E708797A753E5B6AAEE";

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      UpdateEmptyMobileServerKeyData[] mobileServersWithEmptyKey = UpdateEmptyMobileServerKeyData
          .select(cp);
      for (UpdateEmptyMobileServerKeyData mobileServerWithEmptyKey : mobileServersWithEmptyKey) {
        String mobileServerKey = createServerKeyFromServerName(mobileServerWithEmptyKey.name);
        UpdateEmptyMobileServerKeyData.updateMobileServerKey(cp, mobileServerKey,
            mobileServerWithEmptyKey.obmobcServerDefinitionId);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }

  private String createServerKeyFromServerName(String serverName) {
    // replace the spaces with underscore and lower case it
    return serverName.replaceAll(" ", "_").toLowerCase();
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    // The module script needs to be executed only when updating from a version from a version
    // between RR15Q3 (included) and RR16Q2 (not included). In those versions the mobile server key
    // was not mandatory
    return new ModuleScriptExecutionLimits(MOBILE_CORE_MODULE_ID, new OpenbravoVersion(1, 0, 2000),
        new OpenbravoVersion(1, 0, 2600));
  }

  public static void main(String[] args) {

    // This method is provided for testing purposes.

    UpdateEmptyMobileServerKey t = new UpdateEmptyMobileServerKey();
    t.execute();
  }
}
