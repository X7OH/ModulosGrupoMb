/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore.process;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyTerminalActionHandler extends CopyBaseActionHandler {
  private static final Logger log = LoggerFactory.getLogger(CopyTerminalActionHandler.class);

  @Override
  protected void executeProcess(CopyStoreProcess copyStore, JSONObject jsonContent) {
    String terminalId = null;
    try {
      terminalId = jsonContent.getString("Obpos_Applications_ID");
    } catch (JSONException ignore) {
      log.error("There was an error getting params for content:{}", jsonContent, ignore);
    }
    if (copyStore.validateCopyTerminal()) {
      copyStore.executeCopyTerminal(terminalId);
    }
  }
}
