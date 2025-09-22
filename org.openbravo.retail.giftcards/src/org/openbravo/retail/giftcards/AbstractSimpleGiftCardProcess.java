/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.giftcards;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.mobile.core.servercontroller.MultiServerJSONProcess;
import org.openbravo.model.common.enterprise.Organization;

public abstract class AbstractSimpleGiftCardProcess extends MultiServerJSONProcess {

  public String getProcessPreference() {
    return "GCNV_PaymentGiftCard";
  }

  @Override
  public JSONObject exec(JSONObject jsonsent) {

    JSONObject result;
    OBContext.setAdminMode(false);
    try {
      if (getPreferenceValue(getProcessPreference())) {
        result = super.exec(jsonsent);
      } else {
        throw new OBSecurityException();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  public boolean getPreferenceValue(String p) {
    try {
      return !OBContext.getOBContext().getRole().isManual()
          || "Y".equals(Preferences.getPreferenceValue(p, true, OBContext.getOBContext()
              .getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(), OBContext
              .getOBContext().getUser(), OBContext.getOBContext().getRole(), null));
    } catch (PropertyException e) {
      return false;
    }
  }
  
  @Override
  protected String getImportEntryDataType() {
    return null;
  }

  @Override
  protected void createImportEntry(String messageId, JSONObject sentIn, JSONObject processResult,
      Organization organization) throws JSONException {
    // We don't want to create any import entry in these transactions.
  }

  @Override
  protected void createArchiveEntry(String id, JSONObject json) throws JSONException {
    // We don't want to create any import entry in these transactions.
  }
}