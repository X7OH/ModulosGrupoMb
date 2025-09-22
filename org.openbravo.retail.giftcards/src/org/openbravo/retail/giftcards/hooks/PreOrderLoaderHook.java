/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards.hooks;

import java.math.BigDecimal;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.retail.giftcards.GiftCardModel;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.process.GiftCardGLItemUtils;
import org.openbravo.retail.posterminal.OBPOSAppCashup;
import org.openbravo.retail.posterminal.OrderLoaderPreProcessHook;

@ApplicationScoped
public class PreOrderLoaderHook implements OrderLoaderPreProcessHook {

  @Override
  public void exec(JSONObject jsonorder) throws Exception {
    JSONArray payments = jsonorder.getJSONArray("payments");
    for (int i = 0; i < payments.length(); i++) {
      JSONObject payment = payments.getJSONObject(i);
      if ("OBPOS_payment.giftcard".equals(payment.getString("kind")) && payment.has("paymentData")) {
        String giftcardNumber = payment.getJSONObject("paymentData").getString("card");
        GiftCardModel model = new GiftCardModel();
        GiftCardInst giftCardInst = model.findGiftCard(giftcardNumber, "G");
        if (giftCardInst != null) {
          if (giftCardInst.getCategory().isUseOneTime()
              && giftCardInst.getCurrentamount().compareTo(BigDecimal.ZERO) == 1) {
            OBPOSAppCashup cashup = OBDal.getInstance().get(OBPOSAppCashup.class,
                jsonorder.getString("obposAppCashup"));
            GiftCardGLItemUtils.close(giftCardInst, cashup, "U");
          }
        }
      }
    }
  }

}
