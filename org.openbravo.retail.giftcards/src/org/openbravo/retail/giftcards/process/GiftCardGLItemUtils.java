/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards.process;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import org.openbravo.retail.posterminal.OBPOSAppCashup;
import org.openbravo.retail.posterminal.OBPOSAppPayment;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.retail.posterminal.OBPOSPaymentcashupEvents;
import org.openbravo.service.db.DalConnectionProvider;

public class GiftCardGLItemUtils {
  private static final Logger log4j = Logger.getLogger(GiftCardGLItemUtils.class);
  public static final String STATUS_CLOSED = "C";

  public static GiftCardTrans close(String gftCardId, String certificateStatus) {
    return close(OBDal.getInstance().get(GiftCardInst.class, gftCardId), null, certificateStatus);
  }

  public static GiftCardTrans close(GiftCardInst giftCardInst, OBPOSAppCashup cashup,
      String certificateStatus) {
    return close(giftCardInst, cashup, null, certificateStatus);
  }

  public static GiftCardTrans close(GiftCardInst giftCardInst, OBPOSAppCashup cashup,
      OBPOSPaymentcashupEvents paymentcashupEvent, String certificateStatus) {

    if ("C".equals(giftCardInst.getAlertStatus())) {
      // Giftcard already closed
      return getLastCancelTransaction(giftCardInst);
    }

    GiftCardTrans trans = OBProvider.getInstance().get(GiftCardTrans.class);

    try {
      long init = System.currentTimeMillis();
      log4j.info("Closing Gift Card Instance: " + giftCardInst.getIdentifier());
      Organization organization = giftCardInst.getPayment().getOrganization();
      Date date = OBMOBCUtils.stripTime(new Date());
      BigDecimal amount = giftCardInst.getCurrentamount();
      // Make a payment
      FIN_Payment payment = new AdvPaymentMngtDao().getNewPayment(true, organization, FIN_Utility
          .getDocumentType(organization, AcctServer.DOCTYPE_ARReceipt), (String) null, giftCardInst
          .getPayment().getBusinessPartner(), giftCardInst.getPayment().getPaymentMethod(),
          giftCardInst.getPayment().getAccount(), amount.toString(), date, (String) null,
          giftCardInst.getPayment().getAccount().getCurrency(), (BigDecimal) null,
          (BigDecimal) null);
      log4j.info("Time to save payment header: " + (System.currentTimeMillis() - init));
      GLItem glitem = null;
      if ("BasedOnCreditNote".equals(giftCardInst.getType())) {
        String posTerminalId = giftCardInst.getSalesOrder().getObposApplications().getId();
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
                posTerminalId);
            throw new OBException(OBMessageUtils.getI18NMessage("GCNV_NoGLItemForReimburse",
                new String[] { OBMessageUtils.messageBD("GCNV_LblCreditNote"),
                    terminal.getObposTerminaltype().getName() }));
          } else
            glitem = appPayObj.getPaymentMethod().getGcnvReimburseGlitem();
        }
      } else {
        glitem = giftCardInst.getCategory().getGLItem();
      }

      // Flush to prevent security errors on the following code.
      OBDal.getInstance().flush();

      if (payment != null) {
        long init2 = System.currentTimeMillis();
        if ("BasedOnCreditNote".equals(giftCardInst.getType())) {
          FIN_AddPayment.saveGLItem(payment, amount, glitem, giftCardInst.getPayment()
              .getBusinessPartner(), null, null, null, null, null, null, null, null);
        } else {
          FIN_AddPayment.saveGLItem(payment, amount, glitem, giftCardInst.getPayment()
              .getBusinessPartner(), null, null, null, null, null, null, null, null);
        }
        log4j.info("Time to save payment GL Item: " + (System.currentTimeMillis() - init2));
        long init3 = System.currentTimeMillis();
        FIN_AddPayment.processPayment(RequestContext.get().getVariablesSecureApp(),
            new DalConnectionProvider(true), "D", payment);
        log4j.info("Time to process payment: " + (System.currentTimeMillis() - init3));
      }
      long init4 = System.currentTimeMillis();
      if (cashup != null) {
        FIN_FinaccTransaction transaction = payment.getFINFinaccTransactionList().get(0);
        transaction.setObposAppCashup(cashup);
        OBDal.getInstance().save(transaction);
        if (paymentcashupEvent != null) {
          paymentcashupEvent.setRelatedTransaction(transaction);
          OBDal.getInstance().save(paymentcashupEvent);
        }
      }
      // Create a cancel transaction with amount
      trans.setOrganization(organization);
      trans.setActive(true);
      trans.setOrderDate(date);
      trans.setPayment(payment);
      trans.setAmount(amount);
      trans.setCancelled(true);
      trans.setGcnvGiftcardInst(giftCardInst);
      OBDal.getInstance().save(trans);
      giftCardInst.setCurrentamount(BigDecimal.ZERO);
      giftCardInst.setAlertStatus(STATUS_CLOSED);
      giftCardInst.setGiftCardCertificateStatus(certificateStatus);
      OBDal.getInstance().save(giftCardInst);
      log4j.info("Time to save gift card transaction: " + (System.currentTimeMillis() - init4));
      log4j.info("Total Time to close a gift card: " + (System.currentTimeMillis() - init));
    } catch (Exception e) {
      if (giftCardInst != null) {
        throw new OBException(OBMessageUtils.getI18NMessage("GCNV_ExceptionCloseGiftcard",
            new String[] { giftCardInst.getSearchKey() }), e);
      }
    }
    return trans;

  }

  private static GiftCardTrans getLastCancelTransaction(GiftCardInst giftCardInst) {
    OBCriteria<GiftCardTrans> obc = OBDal.getInstance().createCriteria(GiftCardTrans.class);
    obc.add(Restrictions.eq(GiftCardTrans.PROPERTY_GCNVGIFTCARDINST, giftCardInst));
    obc.add(Restrictions.eq(GiftCardTrans.PROPERTY_ISCANCELLED, true));
    obc.addOrderBy(GiftCardTrans.PROPERTY_CREATIONDATE, false);
    obc.setMaxResults(1);

    return (GiftCardTrans) obc.uniqueResult();
  }
}
