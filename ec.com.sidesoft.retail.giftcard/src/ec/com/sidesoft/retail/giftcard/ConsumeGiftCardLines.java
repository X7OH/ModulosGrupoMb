/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package ec.com.sidesoft.retail.giftcard;

import java.math.BigDecimal;
import java.util.Date;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

public class ConsumeGiftCardLines extends AbstractSimpleGiftCardProcess {

  @Override
  public JSONObject execute(JSONObject jsonsent) {

    JSONArray lines;
    JSONArray results = new JSONArray();
    JSONObject returnresult = new JSONObject();

    try {
      lines = jsonsent.getJSONArray("lines");
      
      String giftcard = jsonsent.getString("giftcard");
      Boolean isReturn = false;
      if (jsonsent.has("isReturn") && !jsonsent.isNull("isReturn")) {
        isReturn = jsonsent.getBoolean("isReturn");
      }
      Date date = new Date();
      Order order = OBDal.getInstance().get(Order.class, jsonsent.optString("order"));
      OrderLine orderline = OBDal.getInstance().get(OrderLine.class,
          jsonsent.optString("orderline"));

      final DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(
          DataToJsonConverter.class);
      final DataToJsonConverterExt toJsonConverterExt = OBProvider.getInstance().get(
          DataToJsonConverterExt.class);

      GiftCardModel model = new GiftCardModel();
      for (int i = 0; i < lines.length(); i++) {
        JSONObject line = lines.getJSONObject(i);

        try {
          BigDecimal amt = new BigDecimal(line.getString("quantity"));

          String transactionId = line.has("transactionId") ? line.getString("transactionId")
              : SequenceIdData.getUUID();

          GiftCardTrans transaction = model.consumeProductGiftCard(transactionId, giftcard, date,
              order, orderline, OBDal.getInstance().get(Product.class, line.getString("product")),
              amt, isReturn);

          line.put("transactionId", transaction.getId());

          JSONObject result = new JSONObject();
          result.put("product", toJsonConverter.toJsonObject(transaction.getGcnvGiftcardInst()
              .getProduct(), DataResolvingMode.FULL));
          result.put("transaction",
              toJsonConverterExt.toJsonObject(transaction, DataResolvingMode.FULL));

          if (amt.compareTo(BigDecimal.ZERO) < 0 && !isReturn)
            result.put("price", "-" + line.get("price"));
          else
            result.put("price", line.get("price"));

          results.put(result);
        } catch (GiftCardCreditException e) {
          // continue
        }
      }
      returnresult.put("data", results);
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    }
    return returnresult;
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
