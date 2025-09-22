/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.giftcards;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftcardReason;
import org.openbravo.retail.posterminal.OBPOSAppCashup;
import org.openbravo.retail.posterminal.OBPOSAppPayment;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.service.db.DalConnectionProvider;

public class GiftCardCertificate extends AbstractSimpleGiftCardProcess {

  @Override
  public JSONObject execute(JSONObject jsonData) {

    JSONObject result = new JSONObject();
    JSONObject finalResult = new JSONObject();
    JSONObject model;
    int status = 0;
    long sequence;

    try {
      OBContext.setAdminMode(false);
      model = jsonData.getJSONObject("model");
      String giftcardCategory = model.getString("giftcardCategory");
      String posId = jsonData.getString("pos");
      GiftcardReason giftcardReason = OBDal.getInstance().get(GiftcardReason.class,
          giftcardCategory);
      OBPOSApplications application = OBDal.getInstance().get(OBPOSApplications.class, posId);
      BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class,
          jsonData.getString("businessPartner"));
      final OBPOSAppCashup cashup = OBDal.getInstance().get(OBPOSAppCashup.class,
          jsonData.getString("cashupId"));
      if (giftcardReason != null && application != null && businessPartner != null) {
        String prefix = application.getGcnvGcseqPrefix();
        if (prefix == null) {
          prefix = application.getOrderdocnoPrefix();
        }
        String suffix = giftcardReason.getSequencesuffix() != null ? giftcardReason
            .getSequencesuffix() : "";
        sequence = application.getGcnvGcseqLastnum() == null ? 1l : application
            .getGcnvGcseqLastnum() + 1l;

        String searchKey = prefix + String.format("%07d", sequence) + suffix;
        JSONObject check = CheckDuplicityOfID.checkCardId(searchKey, null);
        if ((Boolean) check.get("used")) {
          throw new JSONException(searchKey + " "
              + OBMessageUtils.getI18NMessage("GCNV_DuplicatedID", null));
        }
        // Get PaymentMethod and FinancialAccount
        String paymentMethodId = giftcardReason.getPaymentMethod().getId();
        List<OBPOSAppPayment> appPayments = application.getOBPOSAppPaymentList();
        OBPOSAppPayment appPayment = null;
        for (OBPOSAppPayment payment : appPayments) {
          if (payment.getPaymentMethod().getPaymentMethod().getId().equals(paymentMethodId)) {
            appPayment = payment;
            break;
          }
        }

        // Flush to prevent security errors on the following code.
        OBDal.getInstance().flush();

        if (appPayment != null) {
          Organization org = OBContext.getOBContext().getCurrentOrganization();
          Date date = OBDateUtils.getDate(model.getString("giftcardCreationDate"));
          // TODO: Review currency. Before was BP but I took the one from financial account.
          Currency currency = appPayment.getFinancialAccount().getCurrency();

          // Make a payment
          FIN_Payment payment = new AdvPaymentMngtDao().getNewPayment(false, org,
              FIN_Utility.getDocumentType(org, AcctServer.DOCTYPE_APPayment), (String) null,
              businessPartner, giftcardReason.getPaymentMethod(), appPayment.getFinancialAccount(),
              model.getString("giftcardAmount"), date, (String) null, currency, null, null);

          if (payment != null) {
            FIN_AddPayment.saveGLItem(payment, new BigDecimal(model.getString("giftcardAmount")),
                giftcardReason.getGLItem(), businessPartner, null, null, null, null, null, null,
                null, null);
            FIN_AddPayment.processPayment(RequestContext.get().getVariablesSecureApp(),
                new DalConnectionProvider(true), "D", payment);

            // retrieve the transactions of this payment and set the cashupId to those transactions
            if (cashup != null) {
              String paymentId = payment.getId();
              OBDal.getInstance().getSession().evict(payment);
              payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
              final List<FIN_FinaccTransaction> transactions = payment
                  .getFINFinaccTransactionList();
              for (FIN_FinaccTransaction transaction : transactions) {
                transaction.setObposAppCashup(cashup);
              }
            }

            if (!giftcardReason.isOnlyOrg()) {
              org = new OrganizationStructureProvider().getLegalEntity(org);
            }
            // Create Gift Card Instance
            GiftCardInst giftCardInst = OBProvider.getInstance().get(GiftCardInst.class);
            giftCardInst.setOrganization(org);
            giftCardInst.setGLItem(giftcardReason.getGLItem());
            giftCardInst.setCategory(giftcardReason);
            giftCardInst.setPayment(payment);
            giftCardInst.setType("BasedOnGLItem");
            giftCardInst.setOrderDate(date);
            Date expirationDate = OBDateUtils.getDate(model.getString("giftcardExpirationDate"));
            giftCardInst.setObgcneExpirationdate(expirationDate);
            giftCardInst.setAmount(payment.getAmount());
            giftCardInst.setCurrentamount(payment.getAmount());
            giftCardInst.setSearchKey(searchKey);
            giftCardInst.setCancelled(false);
            giftCardInst.setAlertStatus("N");
            giftCardInst.setGiftCardCertificateStatus("C");
            if (giftcardReason.isHasOwner()) {
              BusinessPartner owner = OBDal.getInstance().get(BusinessPartner.class,
                  model.getString("giftcardCBpartner"));
              giftCardInst.setBusinessPartner(owner);
              giftCardInst.setObgcneGCOwner(owner);
            } else {
              giftCardInst.setBusinessPartner(businessPartner);
            }
            OBDal.getInstance().save(giftCardInst);
            result.put("cardNumber", searchKey);
          } else {
            throw new JSONException(OBMessageUtils.getI18NMessage("GCNV_ErrorCertificatePayment",
                null));
          }
        } else {
          throw new JSONException(OBMessageUtils.getI18NMessage(
              "GCNV_ErrorCertificatePaymentMethod", null));
        }
      } else {
        throw new JSONException(OBMessageUtils.getI18NMessage("GCNV_ErrorCertificateInvalidParams",
            null));
      }

      if (application.getGcnvGcseqLastnum() == null || sequence > application.getGcnvGcseqLastnum()) {
        application.setGcnvGcseqLastnum(sequence);
        OBDal.getInstance().save(application);
      }

      finalResult.put("data", result);
      finalResult.put("status", status);
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return finalResult;

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
