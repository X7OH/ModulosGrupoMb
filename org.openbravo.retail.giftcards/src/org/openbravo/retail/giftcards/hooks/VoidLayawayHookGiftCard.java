/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards.hooks;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.retail.giftcards.GiftCardModel;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import org.openbravo.retail.posterminal.VoidLayawayPostHook;

@ApplicationScoped
public class VoidLayawayHookGiftCard extends VoidLayawayPostHook {

  @Override
  public void exec(JSONObject jsonorder, Order order) throws Exception {

    GiftCardModel gc = new GiftCardModel();
    for (OrderLine orderline : order.getOrderLineList()) {

      List<GiftCardTrans> transactions = orderline.getGCNVGiftCardTransList();

      if (transactions.size() == 1) {
        // Cancel the associated transaction if exists
        OBContext.setAdminMode(false);
        try {
          gc.cancelGiftCardTransaction(transactions.get(0).getId());
        } finally {
          OBContext.restorePreviousMode();
        }
      }
    }

    if (jsonorder != null) {
      OBCriteria<FIN_PaymentSchedule> finPaymentScheduleList = OBDal.getInstance().createCriteria(
          FIN_PaymentSchedule.class);
      finPaymentScheduleList.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_ORDER, order));
      finPaymentScheduleList.setMaxResults(1);
      FIN_PaymentSchedule finPaymentSchedule = (FIN_PaymentSchedule) finPaymentScheduleList
          .uniqueResult();

      GiftCardHookUtils.updateGCPaymentTransaction(order, jsonorder, finPaymentSchedule);
    }
  }
}
