/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package ec.com.sidesoft.retail.giftcard;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import org.openbravo.retail.giftcards.process.GiftCardGLItemUtils;
import org.openbravo.service.json.DataResolvingMode;

public class CloseGiftCard extends AbstractSimpleGiftCardProcess {

  @Override
  public JSONObject execute(JSONObject jsonsent) {

    JSONObject result = new JSONObject();

    try {

      final String GIFTCARD_REIMBURSED = "R";
      final String giftcardID = jsonsent.getString("giftcard");

      GiftCardTrans trans = GiftCardGLItemUtils.close(giftcardID, GIFTCARD_REIMBURSED);

      final DataToJsonConverterExt toJsonConverterExt = OBProvider.getInstance().get(
          DataToJsonConverterExt.class);
      result.put("transaction", toJsonConverterExt.toJsonObject(trans, DataResolvingMode.FULL));

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