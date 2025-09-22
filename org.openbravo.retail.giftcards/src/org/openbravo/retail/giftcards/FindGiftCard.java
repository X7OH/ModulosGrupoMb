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
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.JsonConstants;

public class FindGiftCard extends AbstractSimpleGiftCardProcess {

  @Override
  protected JSONObject execute(JSONObject json) {
    GiftCardModel model = new GiftCardModel();
    final JSONObject result = new JSONObject();
    try {
      try {
        final GiftCardInst giftcard = model.findGiftCard(json.getString("giftcard"),
            json.optString("giftcardtype", null));

        if (giftcard != null && "BasedOnGLItem".equals(giftcard.getType())
            && giftcard.getCategory().isOnlyOrg()) {
          Organization org = OBContext.getOBContext().getCurrentOrganization();
          if (!giftcard.getOrganization().getId().equals(org.getId())) {
            throw new Exception("GCNV_ErrorGiftCardNotExists:" + giftcard.getSearchKey());
          }
        }
        final DataToJsonConverterExt toJsonConverter = OBProvider.getInstance().get(
            DataToJsonConverterExt.class);

        final JSONObject jsongiftcard = toJsonConverter.toJsonObject(giftcard,
            DataResolvingMode.FULL);

        if (giftcard.getPayment() != null) {
          jsongiftcard.put("currency", giftcard.getPayment().getAccount().getCurrency().getId());
        }

        result.put(JsonConstants.RESPONSE_DATA, jsongiftcard);
        result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      } catch (Exception e) {
        result.put(JsonConstants.RESPONSE_ERROR, e.getMessage());
        result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
      }
    } catch (JSONException je) {
      // Ignore
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

  @Override
  protected boolean executeFirstInCentral(JSONObject json) throws JSONException {
    return true;
  }

  @Override
  protected boolean executeInOneServer(JSONObject json) throws JSONException {
    return true;
  }

}
