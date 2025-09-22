/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards;

import java.math.BigDecimal;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.process.GiftCardGLItemUtils;
import org.openbravo.retail.posterminal.OBPOSAppPayment;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.retail.posterminal.OrderLoaderPreProcessPaymentHook;

@ApplicationScoped
public class OrderLoaderPreProcessPaymentHookExtension extends OrderLoaderPreProcessPaymentHook {

  @Override
  public void exec(JSONObject jsonorder, Order order, JSONObject jsonpayment, FIN_Payment payment)
      throws Exception {

    OBContext.setAdminMode(false);
    try {
      payment.getPaymentMethod();
      Organization org = OBDal.getInstance().get(Organization.class,
          jsonorder.getString("organization"));

      if (org.getObretcoCBpartner().getId() != order.getBusinessPartner().getId()
          && order.getBusinessPartner().isGcnvUniquecreditnote()
          && jsonpayment.get("kind").equals("GCNV_payment.creditnote")
          && payment.getAmount().compareTo(BigDecimal.ZERO) < 0) {

        BigDecimal totalCreditNote = BigDecimal.ZERO;

        OBCriteria<GiftCardInst> giftCardInstCriteria = OBDal.getInstance().createCriteria(
            GiftCardInst.class);
        giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_BUSINESSPARTNER,
            order.getBusinessPartner()));
        giftCardInstCriteria.add(Restrictions.ne(GiftCardInst.PROPERTY_ALERTSTATUS,
            GiftCardGLItemUtils.STATUS_CLOSED));
        giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_ISCANCELLED, false));
        giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_TYPE, "BasedOnCreditNote"));
        for (GiftCardInst giftCardInst : giftCardInstCriteria.list()) {
          totalCreditNote = totalCreditNote.add(giftCardInst.getCurrentamount());
          giftCardInst.setCancelGiftCard(true);
          OBDal.getInstance().save(giftCardInst);
        }
        OBDal.getInstance().flush();

        if (totalCreditNote.compareTo(BigDecimal.ZERO) != 0) {
          GLItem glitem = null;
          String posTerminalId = order.getObposApplications().getId();
          OBCriteria<OBPOSAppPayment> appPaymentCri = OBDal.getInstance().createCriteria(
              OBPOSAppPayment.class);
          appPaymentCri.add(Restrictions.eq(OBPOSAppPayment.PROPERTY_OBPOSAPPLICATIONS + ".id",
              posTerminalId));
          appPaymentCri.add(Restrictions.eq(OBPOSAppPayment.PROPERTY_SEARCHKEY,
              "GCNV_payment.creditnote"));
          appPaymentCri.setMaxResults(1);
          List<OBPOSAppPayment> appPaymentList = appPaymentCri.list();
          if (appPaymentList.size() > 0) {
            OBPOSAppPayment appPayObj = appPaymentList.get(0);
            if (appPayObj.getPaymentMethod().getGcnvReimburseGlitem() == null) {
              OBPOSApplications terminal = OBDal.getInstance().get(OBPOSApplications.class,
                  jsonorder.getString("posTerminal"));
              throw new OBException(OBMessageUtils.getI18NMessage("GCNV_NoGLItemForReimburse",
                  new String[] { OBMessageUtils.messageBD("GCNV_LblCreditNote"),
                      terminal.getObposTerminaltype().getName() }));
            } else
              glitem = appPayObj.getPaymentMethod().getGcnvReimburseGlitem();
          }

          BigDecimal paymentAmount = payment.getAmount().add(totalCreditNote.negate());

          FIN_AddPayment.saveGLItem(payment, totalCreditNote.negate(), glitem,
              payment.getBusinessPartner(), null, null, null, null, null, null, null, null);
          payment.setAmount(paymentAmount);
          payment.setFinancialTransactionAmount(paymentAmount);
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}