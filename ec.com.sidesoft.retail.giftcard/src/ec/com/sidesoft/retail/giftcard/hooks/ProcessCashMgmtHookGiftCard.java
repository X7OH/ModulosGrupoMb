/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.retail.giftcard.hooks;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import ec.com.sidesoft.retail.giftcard.process.GiftCardGLItemUtils;
import org.openbravo.retail.posterminal.OBPOSAppCashup;
import org.openbravo.retail.posterminal.OBPOSAppPayment;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.retail.posterminal.OBPOSPaymentcashupEvents;
import org.openbravo.retail.posterminal.ProcessCashMgmtHook;

@ApplicationScoped
public class ProcessCashMgmtHookGiftCard implements ProcessCashMgmtHook {

  @Override
  public void exec(JSONObject jsonsent, String type, OBPOSAppPayment paymentMethod,
      OBPOSAppCashup cashup, OBPOSPaymentcashupEvents paymentcashupEvent, BigDecimal amount,
      BigDecimal origAmount) throws Exception {

    if (type.equals("GCNV_reimbursed")) {
      GiftCardInst giftCardInst = OBDal.getInstance().get(GiftCardInst.class,
          jsonsent.getString("gcnvGiftCardId"));
      if (giftCardInst != null) {
        // - Register cash out transaction (reimburse)
        // - Clears Gift Card remaining amount in financial account and closes gift card
        String description = jsonsent.getString("description");
        GLItem glitem = null;
        if ("BasedOnCreditNote".equals(giftCardInst.getType())) {
          OBCriteria<OBPOSAppPayment> appPaymentCri = OBDal.getInstance().createCriteria(
              OBPOSAppPayment.class);
          appPaymentCri.add(Restrictions.eq(OBPOSAppPayment.PROPERTY_OBPOSAPPLICATIONS + ".id",
              jsonsent.getString("posTerminal")));
          appPaymentCri.add(Restrictions.eq(OBPOSAppPayment.PROPERTY_SEARCHKEY,
              "GCNV_payment.creditnote"));
          appPaymentCri.setMaxResults(1);
          List<OBPOSAppPayment> appPaymentList = appPaymentCri.list();
          if (appPaymentList.size() > 0) {
            OBPOSAppPayment appPayObj = appPaymentList.get(0);
            if (appPayObj.getPaymentMethod().getGcnvReimburseGlitem() == null) {
              OBPOSApplications terminal = OBDal.getInstance().get(OBPOSApplications.class,
                  jsonsent.getString("posTerminal"));
              throw new OBException(OBMessageUtils.getI18NMessage("GCNV_NoGLItemForReimburse",
                  new String[] { OBMessageUtils.messageBD("GCNV_LblCreditNote"),
                      terminal.getObposTerminaltype().getName() }));
            } else
              glitem = appPayObj.getPaymentMethod().getGcnvReimburseGlitem();
          }
        } else {
          glitem = giftCardInst.getCategory().getGLItem();
        }

        FIN_FinancialAccount cashFinancialAccount = paymentMethod.getFinancialAccount();

        FIN_FinaccTransaction cashOutTransaction = OBProvider.getInstance().get(
            FIN_FinaccTransaction.class);
        cashOutTransaction.setCurrency(cashFinancialAccount.getCurrency());
        cashOutTransaction.setObposAppCashup(cashup);
        cashOutTransaction.setAccount(cashFinancialAccount);
        cashOutTransaction
            .setLineNo(TransactionsDao.getTransactionMaxLineNo(cashFinancialAccount) + 10);
        cashOutTransaction.setGLItem(glitem);
        cashOutTransaction.setPaymentAmount(amount);
        cashOutTransaction.setDepositAmount(BigDecimal.ZERO);
        cashFinancialAccount.setCurrentBalance(cashFinancialAccount.getCurrentBalance().subtract(
            amount));
        cashOutTransaction.setProcessed(true);
        cashOutTransaction.setTransactionType("BPW");
        cashOutTransaction.setDescription(description);
        cashOutTransaction.setDateAcct(new Date());
        cashOutTransaction.setTransactionDate(new Date());
        cashOutTransaction.setStatus("RDNC");
        OBDal.getInstance().save(cashOutTransaction);

        paymentcashupEvent.setFINFinaccTransaction(cashOutTransaction);
        OBDal.getInstance().save(paymentcashupEvent);

        // Close GiftCard
        if (jsonsent.has("transactionId")) {
          GiftCardTrans trans = OBDal.getInstance().get(GiftCardTrans.class,
              jsonsent.getString("transactionId"));
          FIN_FinaccTransaction transaction = trans.getPayment().getFINFinaccTransactionList()
              .get(0);
          transaction.setObposAppCashup(cashup);
          OBDal.getInstance().save(transaction);
          paymentcashupEvent.setRelatedTransaction(transaction);
          OBDal.getInstance().save(paymentcashupEvent);
        } else {
          GiftCardGLItemUtils.close(giftCardInst, cashup, paymentcashupEvent, "R");
        }
      }
    }
  }
}
