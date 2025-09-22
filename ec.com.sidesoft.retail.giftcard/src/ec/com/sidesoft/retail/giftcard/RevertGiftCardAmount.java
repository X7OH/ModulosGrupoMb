/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package ec.com.sidesoft.retail.giftcard;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.model.common.enterprise.Organization;

public class RevertGiftCardAmount extends AbstractSimpleGiftCardProcess {

  @Override
  public JSONObject execute(JSONObject jsonsent) {
    JSONObject result = new JSONObject();
    GiftCardModel model = new GiftCardModel();
    try {
      model.revertGiftCardTrans(jsonsent.getString("transaction"));
      result.put("success", true);
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    }
    return result;
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
