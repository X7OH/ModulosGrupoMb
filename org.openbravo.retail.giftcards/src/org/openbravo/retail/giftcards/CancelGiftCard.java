/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.giftcards;

import java.util.Date;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

public class CancelGiftCard extends AbstractSimpleGiftCardProcess {

  @Override
  public JSONObject execute(JSONObject jsonsent) {

    JSONObject result = new JSONObject();

    try {
      String transactionId = null;
      if (jsonsent.has("_result")) {
        JSONObject _result = jsonsent.getJSONObject("_result");
        if (_result.has("data")) {
          JSONObject data = _result.getJSONObject("data");
          if (data.has("transaction")) {
            transactionId = data.getJSONObject("transaction").getString("id");
          }
        }
      }
      if (transactionId == null) {
        transactionId = SequenceIdData.getUUID();
      }

      GiftCardModel model = new GiftCardModel();
      GiftCardTrans transaction = model.cancelGiftCard(transactionId, jsonsent
          .getString("giftcard"), new Date(),
          OBDal.getInstance().get(Order.class, jsonsent.optString("order")), OBDal.getInstance()
              .get(OrderLine.class, jsonsent.optString("orderline")));

      final DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(
          DataToJsonConverter.class);
      final DataToJsonConverterExt toJsonConverterExt = OBProvider.getInstance().get(
          DataToJsonConverterExt.class);

      result.put("product", toJsonConverter.toJsonObject(transaction.getGcnvGiftcardInst()
          .getProduct(), DataResolvingMode.FULL));
      result.put("transaction",
          toJsonConverterExt.toJsonObject(transaction, DataResolvingMode.FULL));
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
