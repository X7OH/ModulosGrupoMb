/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards.hooks;

import java.math.BigDecimal;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.retail.giftcards.GiftCardModel;

public class GiftCardHookUtils {

  private static final String PAYMENT_TYPE_GIFTCARD = "OBPOS_payment.giftcard";
  private static final String PAYMENT_TYPE_CREDITNOTE = "GCNV_payment.creditnote";

  public static void updateGCPaymentTransaction(Order order, JSONObject jsonorder,
      FIN_PaymentSchedule finPaymentSchedule) throws Exception {

    BusinessPartner bp = order.getBusinessPartner();
    Organization org = OBDal.getInstance().get(Organization.class,
        jsonorder.getString("organization"));

    // Update transaction for Gift Card Payments
    JSONArray payments = jsonorder.getJSONArray("payments");
    GiftCardModel gc = new GiftCardModel();
    for (int i = 0; i < payments.length(); i++) {
      final JSONObject payment = payments.getJSONObject(i);
      final boolean isPrePayment = payment.optBoolean("isPrePayment", false);
      if (!isPrePayment) {
        String giftCardTransaction = payment.optString("transaction", null);
        String[] kind = payment.getString("kind").split(":");
        if (PAYMENT_TYPE_GIFTCARD.equals(kind[0])) {
          gc.populateGiftCardTransaction(giftCardTransaction, order, null, payment.getString("id"));
        } else if (PAYMENT_TYPE_CREDITNOTE.equals(kind[0])) {
          if (giftCardTransaction == null) {
            // Is not a Credit Note Transaction (the payment doesn't have the transaction id)
            final String giftcardid = payment.optString("creditnoteId", null);
            final BigDecimal amount = BigDecimal.valueOf(payment.getDouble("amount"));
            final FIN_Payment origPayment = OBDal.getInstance().get(FIN_Payment.class,
                payment.getString("id"));
            gc.createCreditNote(org, giftcardid, bp, order.getOrderDate(), order, null, amount,
                origPayment);
          } else {
            // Is a Credit Note transaction so just fill in the order and order-line fields
            gc.populateGiftCardTransaction(giftCardTransaction, order, null,
                payment.getString("id"));
          }
        }
      }
    }

  }

}
