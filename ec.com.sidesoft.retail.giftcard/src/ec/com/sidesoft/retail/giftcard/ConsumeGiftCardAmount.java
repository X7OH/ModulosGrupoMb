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
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

public class ConsumeGiftCardAmount extends AbstractSimpleGiftCardProcess {

  @Override
  public JSONObject execute(JSONObject jsonsent) {

    GiftCardModel model = new GiftCardModel();
    GiftCardInst giftcard = null;
    JSONObject result = new JSONObject();
    
    try {
      if (jsonsent.has("paymentToReverse") && jsonsent.getString("paymentToReverse") != null) {
        final FIN_Payment paymentToReverse = OBDal.getInstance().get(FIN_Payment.class,
            jsonsent.getString("paymentToReverse"));
        final OBCriteria<GiftCardTrans> obc = OBDal.getInstance().createCriteria(
            GiftCardTrans.class);
        obc.add(Restrictions.eq(GiftCardTrans.PROPERTY_PAYMENT, paymentToReverse));
        obc.setMaxResults(1);

        final List<GiftCardTrans> listt = obc.list();
        if (listt == null || listt.size() != 1) {
          throw new Exception("SRGC_ErrorGiftCardNotExists:" + jsonsent.get("paymentToReverse"));
        }

        giftcard = listt.get(0).getGcnvGiftcardInst();
      } else {
        String giftcardid = jsonsent.getString("giftcard");
        final OBCriteria<GiftCardInst> obCriteria = OBDal.getInstance().createCriteria(
            GiftCardInst.class);
        obCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_SEARCHKEY, giftcardid));
        obCriteria.setMaxResults(1);

        final List<GiftCardInst> listt = obCriteria.list();

        if (listt == null || listt.size() != 1) {
          // Gift Card With ID %0 not found.
          throw new Exception("SRGC_ErrorGiftCardNotExists:" + giftcardid);
        }
        giftcard = listt.get(0);
      }

      Boolean isReturn = jsonsent.getBoolean("isReturn");
      BigDecimal amount = new BigDecimal(
          (jsonsent.getString("amount") != null) ? jsonsent.getString("amount") : "0");
      BigDecimal realamount = amount;
      
      if (!isReturn && amount.compareTo(BigDecimal.ZERO) > 0) {
        if (giftcard.getCurrentamount().compareTo(amount) < 0) {
          realamount = giftcard.getCurrentamount();
        }
      }

      String newTrxId;
      try {
        newTrxId = jsonsent.getJSONObject("_result").getJSONObject("data")
            .getJSONObject("transaction").getString("id");
      } catch (Exception e) {
        newTrxId = SequenceIdData.getUUID();
      }

        
      String transactionId = jsonsent.has("transaction") ? jsonsent.getString("transaction") : null;
      GiftCardTrans transaction = model.consumeAmountGiftCard(newTrxId, giftcard, new Date(), OBDal
          .getInstance().get(Order.class, jsonsent.optString("order")),
          OBDal.getInstance().get(OrderLine.class, jsonsent.optString("orderline")), amount,
          isReturn, transactionId == null || "null".equals(transactionId) ? null : transactionId,
          jsonsent.has("hasPaymentMethod") ? jsonsent.getBoolean("hasPaymentMethod") : false);

      final DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(
          DataToJsonConverter.class);
      final DataToJsonConverterExt toJsonConverterExt = OBProvider.getInstance().get(
          DataToJsonConverterExt.class);

      if (transaction.getGcnvGiftcardInst().getType().equals("BasedOnProductGiftCard")
          || transaction.getGcnvGiftcardInst().getType().equals("BasedOnVoucher")) {
        result.put("product", toJsonConverter.toJsonObject(transaction.getGcnvGiftcardInst()
            .getProduct(), DataResolvingMode.FULL));
      } else if (!"BasedOnCreditNote".equals(transaction.getGcnvGiftcardInst().getType())) {
        result.put(
            "paymentMethod",
            toJsonConverter.toJsonObject(transaction.getGcnvGiftcardInst().getCategory()
                .getPaymentMethod(), DataResolvingMode.FULL));
      }
      result.put("currentamt", transaction.getGcnvGiftcardInst().getCurrentamount());
      result.put("realamnt", realamount);
      result.put("transaction",
          toJsonConverterExt.toJsonObject(transaction, DataResolvingMode.FULL));
      result.put("giftcard", giftcard);
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
