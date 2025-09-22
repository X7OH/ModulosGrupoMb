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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.mobile.core.process.JSONPropertyToEntity;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardSummary;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.ProductSummary;
import ec.com.sidesoft.retail.giftcard.process.GiftCardGLItemUtils;
import org.openbravo.retail.posterminal.OBPOSAppCashup;
import org.openbravo.retail.posterminal.OBPOSAppPayment;

public class GiftCardModel {

  public GiftCardTrans cancelGiftCardTransaction(String transid) throws Exception {

    GiftCardTrans transaction = OBDal.getInstance().get(GiftCardTrans.class, transid);

    if (transaction == null) {
      // Gift Card Transaction With ID %0 not found.
      throw new Exception("SRGC_ErrorGiftCardTransactionNotExists:" + transid);
    }

    GiftCardInst giftcard = transaction.getGcnvGiftcardInst();

    String type = giftcard.getProduct().getGcnvGiftcardtype();
    
    if ("G".equals(type)) {

      BigDecimal currentAmount = giftcard.getCurrentamount().add(transaction.getAmount());

      giftcard.getGCNVGiftCardTransList().remove(transaction);
      // OBDal.getInstance().remove(transaction);

      if (giftcard.getGCNVGiftCardTransList().size() == 0) {
        // Is new so we put the amount of the transaction cancelled
        giftcard.setCurrentamount(giftcard.getAmount());
      } else {
        // Is partially consumed so we put the initial balance as current amount
        giftcard.setCurrentamount(currentAmount);
      }
    } else if ("V".equals(type)) {

      if (transaction.isCancelled()) {

        // First remove the transaction with the amount...
        giftcard.getGCNVGiftCardTransList().remove(transaction);

        // We must remove all transactions that cancel the gift voucher
        // Create first a copy of all transactions to be removed.
        ArrayList<GiftCardTrans> tlist = new ArrayList<GiftCardTrans>();
        for (GiftCardTrans t : giftcard.getGCNVGiftCardTransList()) {
          if (t.isCancelled()) {
            tlist.add(t);
          }
        }

        for (GiftCardTrans t : tlist) {
          removeVoucherTransaction(giftcard, t);
        }
      } else {
        removeVoucherTransaction(giftcard, transaction);
      }
    } else {
      throw new Exception("SRGC_ErrorGiftCardInvalid:" + giftcard.getSearchKey());
    }

    giftcard.setAlertStatus(giftcard.getGCNVGiftCardTransList().size() == 0 ? "N" : "P");
    giftcard.setCancelled(false);

    OBDal.getInstance().save(giftcard);
    OBDal.getInstance().flush();

    return transaction;
  }

  private void removeVoucherTransaction(GiftCardInst giftcard, GiftCardTrans transaction)
      throws Exception {

    // find summary
    GiftCardSummary giftcardsummary = null;
    for (GiftCardSummary s : giftcard.getGCNVGiftCardSummaryList()) {
      if (s.getProduct().getId().equals(transaction.getProduct().getId())) {
        giftcardsummary = s;
        break;
      }
    }

    if (giftcardsummary == null) {
      throw new Exception("SRGC_ErrorGiftCardDoesNotContainProduct:" + giftcard.getSearchKey()
          + ":" + transaction.getProduct().getIdentifier());
    }

    BigDecimal currentQuantity = giftcardsummary.getCurrentquantity()
        .add(transaction.getQuantity());
    giftcardsummary.setCurrentquantity(currentQuantity);

    OBDal.getInstance().save(giftcardsummary);
    giftcard.getGCNVGiftCardTransList().remove(transaction);
    // OBDal.getInstance().remove(transaction);
  }

  public void revertGiftCardTrans(String transid) throws Exception {
    try {
      OBContext.setAdminMode(false);

      GiftCardTrans trans = OBDal.getInstance().get(GiftCardTrans.class, transid);
      if (trans != null) {
        GiftCardInst giftcard = trans.getGcnvGiftcardInst();
        giftcard.setCurrentamount(giftcard.getCurrentamount().add(trans.getAmount()));
        giftcard.setAlertStatus(giftcard.getCurrentamount().compareTo(BigDecimal.ZERO) == 0 ? "C"
            : giftcard.getCurrentamount().compareTo(giftcard.getAmount()) == 0 ? "N" : "P");

        giftcard.setGiftCardCertificateStatus(giftcard.getCurrentamount()
            .compareTo(BigDecimal.ZERO) == 0 ? "U" : giftcard.getCurrentamount().compareTo(
            giftcard.getAmount()) == 0 ? "C" : "PU");

        OBDal.getInstance().remove(trans);
        OBDal.getInstance().save(giftcard);
        OBDal.getInstance().flush();
      } else {
        throw new Exception("SRGC_ErrorGiftCardTransactionNotExists:" + transid);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public GiftCardTrans consumeProductGiftCard(String transactionId, String giftcardid,
      Date orderDate, Order order, OrderLine orderLine, Product product, BigDecimal quantity,
      Boolean isReturn) throws Exception {

    final OBCriteria<GiftCardInst> obCriteria = OBDal.getInstance().createCriteria(
        GiftCardInst.class);
    obCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_SEARCHKEY, giftcardid));
    obCriteria.setMaxResults(1);

    final List<GiftCardInst> listt = obCriteria.list();

    if (listt == null || listt.size() != 1) {
      throw new Exception("SRGC_ErrorGiftCardNotExists:" + giftcardid); // Gift Card With ID %0 not
                                                                        // found.
    }

    GiftCardInst giftcard = listt.get(0);

    if (!"V".equals(giftcard.getProduct().getGcnvGiftcardtype())) {
      throw new Exception("GCNV_ErrorGiftCardNotVoucher:" + giftcardid);
    }

    if ("C".equals(giftcard.getAlertStatus())) {
      throw new Exception("GCNV_ErrorGiftVoucherClosed:" + giftcardid);
    }

    if (BigDecimal.ZERO.compareTo(quantity) == 0) {
      throw new GiftCardCreditException("GCNV_ErrorGiftCardZeroQuantity");
    }

    // find summary
    GiftCardSummary giftcardsummary = null;
    boolean emptygiftcard = true;
    for (GiftCardSummary s : giftcard.getGCNVGiftCardSummaryList()) {
      if (s.getProduct().getId().equals(product.getId())) {
        giftcardsummary = s;
      } else {
        emptygiftcard = emptygiftcard && s.getCurrentquantity().compareTo(BigDecimal.ZERO) == 0;
      }
    }

    if (giftcardsummary == null
        || (giftcardsummary.getCurrentquantity().compareTo(BigDecimal.ZERO) <= 0 && BigDecimal.ZERO
            .compareTo(quantity) < 0)) {
      throw new GiftCardCreditException("GCNV_ErrorGiftCardNoProductCredit:" + giftcardid + ":"
          + product.getIdentifier());
    }

    BigDecimal realquantity = quantity;
    if (giftcardsummary.getCurrentquantity().compareTo(quantity) < 0) {
      realquantity = giftcardsummary.getCurrentquantity();
    }
    // if return add
    BigDecimal currentQuantity = BigDecimal.ZERO;
    currentQuantity = giftcardsummary.getCurrentquantity().subtract(realquantity);

    giftcardsummary.setCurrentquantity(currentQuantity);
    giftcard.setAlertStatus(emptygiftcard && currentQuantity.compareTo(BigDecimal.ZERO) == 0 ? "C"
        : "P");

    GiftCardTrans trans = OBProvider.getInstance().get(GiftCardTrans.class);
    trans.setOrganization(OBContext.getOBContext().getCurrentOrganization());
    trans.setId(transactionId);
    trans.setNewOBObject(true);
    trans.setActive(true);
    trans.setOrderDate(orderDate);
    trans.setSalesOrder(order);
    trans.setSalesOrderLine(orderLine);
    trans.setProduct(giftcardsummary.getProduct());

    // if return this transaction should be negative
    if (isReturn) {
      trans.setQuantity(realquantity.multiply(new BigDecimal(-1)));
    } else {
      trans.setQuantity(realquantity);
    }
    trans.setGcnvGiftcardInst(giftcard);

    // Save and return
    OBDal.getInstance().save(giftcard);
    OBDal.getInstance().save(giftcardsummary);
    OBDal.getInstance().save(trans);
    OBDal.getInstance().flush();

    return trans;
  }

  public GiftCardTrans consumeProductGiftCard(String giftcardid, Date orderDate, Order order,
      OrderLine orderLine, Product product, BigDecimal quantity, Boolean isReturn) throws Exception {
    return consumeProductGiftCard(SequenceIdData.getUUID(), giftcardid, orderDate, order,
        orderLine, product, quantity, isReturn);
  }

  public GiftCardTrans consumeAmountGiftCard(String giftcardid, Date orderDate, Order order,
      OrderLine orderLine, BigDecimal amount, Boolean isReturn, String transactionId,
      boolean hasPaymentMethod) throws Exception {

    final OBCriteria<GiftCardInst> obCriteria = OBDal.getInstance().createCriteria(
        GiftCardInst.class);
    obCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_SEARCHKEY, giftcardid));
    obCriteria.setMaxResults(1);

    final List<GiftCardInst> listt = obCriteria.list();

    if (listt == null || listt.size() != 1) {
      // Gift Card With ID %0 not found.
      throw new Exception("SRGC_ErrorGiftCardNotExists:" + giftcardid);
    }
    return consumeAmountGiftCard(listt.get(0), orderDate, order, orderLine, amount, isReturn,
        transactionId, hasPaymentMethod);
  }

  public GiftCardTrans consumeAmountGiftCard(String newTrxId, GiftCardInst giftcard,
      Date orderDate, Order order, OrderLine orderLine, BigDecimal amount, Boolean isReturn,
      String transactionId, boolean hasPaymentMethod) throws Exception {

    if (giftcard.getType().equals("BasedOnVoucher")) {
      throw new Exception("GCNV_ErrorGiftCardNotGift:" + giftcard.getSearchKey());
    } else if ("BasedOnGLItem".equals(giftcard.getType())) {
      if ("BasedOnGLItem".equals(giftcard.getType())
          && (giftcard.getCategory() == null ? false : giftcard.getCategory().isOnlyOrg())) {
        Organization org = OBContext.getOBContext().getCurrentOrganization();
        if (!giftcard.getOrganization().getId().equals(org.getId())) {
          throw new Exception("SRGC_ErrorGiftCardNotExists:" + giftcard.getSearchKey());
        }
      }
    }
    if ((giftcard.getType().equals("BasedOnCreditNote") || giftcard.getType().equals(
        "BasedOnGLItem"))
        && !hasPaymentMethod) {
      throw new Exception("GCNV_ErrorGiftCardCanNotUsed:" + giftcard.getSearchKey());
    }
    if ("C".equals(giftcard.getAlertStatus()) && amount.compareTo(BigDecimal.ZERO) > 0) {
      throw new Exception("GCNV_ErrorGiftCardClosed:" + giftcard.getSearchKey());
    }

    BigDecimal realamount = amount;

    if (!isReturn && amount.compareTo(BigDecimal.ZERO) > 0) {
      if (giftcard.getCurrentamount().compareTo(amount) < 0) {
        realamount = giftcard.getCurrentamount();
      }
    }

    BigDecimal currentAmount = giftcard.getCurrentamount().subtract(
        (isReturn ? new BigDecimal("-1").multiply(realamount) : realamount));

    giftcard.setCurrentamount(currentAmount);
    giftcard.setAlertStatus(currentAmount.compareTo(BigDecimal.ZERO) == 0 ? "C" : "P");
    if ("BasedOnGLItem".equals(giftcard.getType())) {
      giftcard.setGiftCardCertificateStatus(currentAmount.compareTo(BigDecimal.ZERO) == 0 ? "U"
          : "PU");
    } else if (giftcard.getType().equals("BasedOnCreditNote")) {
      giftcard.setGiftCardCertificateStatus(currentAmount.compareTo(BigDecimal.ZERO) == 0 ? "U"
          : "PU");
    }

    // Make amount as negative if the receipt type is return
    if (isReturn && realamount.compareTo(BigDecimal.ZERO) > 0) {
      realamount = realamount.negate();
    }

    GiftCardTrans trans;
    if (transactionId != null) {
      trans = OBDal.getInstance().get(GiftCardTrans.class, transactionId);
      trans.setAmount(realamount.add(trans.getAmount()));
    } else {
      trans = OBProvider.getInstance().get(GiftCardTrans.class);
      trans.setId(newTrxId);
      trans.setNewOBObject(true);
      trans.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      trans.setActive(true);
      trans.setOrderDate(orderDate);
      trans.setAmount(realamount);
      trans.setGcnvGiftcardInst(giftcard);
      if (giftcard.getType().equals("BasedOnProductGiftCard")
          || giftcard.getType().equals("BasedOnVoucher")) {
        trans.setSalesOrder(order);
        trans.setSalesOrderLine(orderLine);
      }
    }

    // Save and return
    OBDal.getInstance().save(giftcard);
    OBDal.getInstance().save(trans);
    OBDal.getInstance().flush();

    return trans;
  }

  public GiftCardTrans consumeAmountGiftCard(GiftCardInst giftcard, Date orderDate, Order order,
      OrderLine orderLine, BigDecimal amount, Boolean isReturn, String transactionId,
      boolean hasPaymentMethod) throws Exception {
    return consumeAmountGiftCard(SequenceIdData.getUUID(), giftcard, orderDate, order, orderLine,
        amount, isReturn, transactionId, hasPaymentMethod);
  }

  public GiftCardTrans cancelGiftCard(String transactionId, String giftcardid, Date orderDate,
      Order order, OrderLine orderLine) throws Exception {

    final OBCriteria<GiftCardInst> obCriteria = OBDal.getInstance().createCriteria(
        GiftCardInst.class);
    obCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_SEARCHKEY, giftcardid));
    obCriteria.setMaxResults(1);

    final List<GiftCardInst> listt = obCriteria.list();

    if (listt == null || listt.size() != 1) {
      // Gift Card With ID %0 not found.
      throw new Exception("SRGC_ErrorGiftCardNotExists:" + giftcardid);
    }

    GiftCardInst giftcard = listt.get(0);

    if ("C".equals(giftcard.getAlertStatus())) {
      throw new Exception("GCNV_ErrorGiftCardClosed:" + giftcardid);
    }

    BigDecimal tamount;
    if (giftcard.getSalesOrderLine() == null) {
      throw new Exception("GCNV_ErrorGiftCardNoTransactionAmount:" + giftcardid);
    } else {
      if (giftcard.getSalesOrder().getPriceList().isPriceIncludesTax()) {
        tamount = giftcard.getSalesOrderLine().getLineGrossAmount();
      } else {
        tamount = giftcard.getSalesOrderLine().getLineNetAmount();
      }
      if (tamount == null || tamount.equals(BigDecimal.ZERO)) {
        throw new Exception("GCNV_ErrorGiftCardNoTransactionAmount:" + giftcardid);
      }
    }

    GiftCardTrans returnedtrans = null;

    // Cancel Gift Card
    if ("G".equals(giftcard.getProduct().getGcnvGiftcardtype())) {
      // Gift card

      if ("P".equals(giftcard.getAlertStatus())
          && !giftcard.getProduct().isGcnvAllowpartialreturn()) {
        throw new Exception("GCNV_ErrorCannotCancelCardPartial:" + giftcardid);
      }

      // If new we return the transaction amount, the amount the card was sold
      // If partial we return the current amount
      BigDecimal amount = "N".equals(giftcard.getAlertStatus()) ? tamount : giftcard
          .getCurrentamount();

      giftcard.setCurrentamount(BigDecimal.ZERO);

      // Create a cancel transaction with amount
      GiftCardTrans trans = OBProvider.getInstance().get(GiftCardTrans.class);
      trans.setId(transactionId);
      trans.setNewOBObject(true);
      trans.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      trans.setActive(true);
      trans.setOrderDate(orderDate);
      trans.setSalesOrder(order);
      trans.setSalesOrderLine(orderLine);
      trans.setAmount(amount);
      trans.setCancelled(true);
      trans.setGcnvGiftcardInst(giftcard);
      OBDal.getInstance().save(trans);

      returnedtrans = trans;

    } else if ("V".equals(giftcard.getProduct().getGcnvGiftcardtype())) {
      // Gift voucher

      if ("P".equals(giftcard.getAlertStatus())) {
        throw new Exception("GCNV_ErrorCannotCancelVoucherPartial:" + giftcardid);
      }

      for (GiftCardSummary s : giftcard.getGCNVGiftCardSummaryList()) {
        BigDecimal qty = s.getCurrentquantity();

        s.setCurrentquantity(BigDecimal.ZERO);
        OBDal.getInstance().save(s);

        // Create a cancel transaction with qty
        GiftCardTrans trans = OBProvider.getInstance().get(GiftCardTrans.class);
        trans.setOrganization(OBContext.getOBContext().getCurrentOrganization());
        trans.setActive(true);
        trans.setOrderDate(orderDate);
        trans.setSalesOrder(order);
        trans.setSalesOrderLine(orderLine);
        trans.setQuantity(qty);
        trans.setProduct(s.getProduct());
        trans.setCancelled(true);
        trans.setGcnvGiftcardInst(giftcard);
        OBDal.getInstance().save(trans);
      }

      // And now create a transaction with the amount returned...
      GiftCardTrans trans = OBProvider.getInstance().get(GiftCardTrans.class);
      trans.setId(transactionId);
      trans.setNewOBObject(true);
      trans.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      trans.setActive(true);
      trans.setOrderDate(orderDate);
      trans.setSalesOrder(order);
      trans.setSalesOrderLine(orderLine);
      trans.setAmount(tamount);
      trans.setCancelled(true);
      trans.setGcnvGiftcardInst(giftcard);
      OBDal.getInstance().save(trans);

      returnedtrans = trans; // It will be returned the transaction with the amount.

    } else {
      throw new Exception("GCNV_ErrorGiftCardInvalid:" + giftcard.getSearchKey());
    }
    giftcard.setAlertStatus("C");
    giftcard.setCancelled(true);

    // Save and return
    OBDal.getInstance().save(giftcard);
    OBDal.getInstance().flush();

    return returnedtrans; // TODO: Return the right object.
  }

  public GiftCardTrans cancelGiftCard(String giftcardid, Date orderDate, Order order,
      OrderLine orderLine) throws Exception {
    return cancelGiftCard(SequenceIdData.getUUID(), giftcardid, orderDate, order, orderLine);
  }

  public GiftCardTrans populateGiftCardTransaction(String transactionid, Order order,
      OrderLine orderLine) {
    return populateGiftCardTransaction(transactionid, order, orderLine, null);
  }

  public GiftCardTrans populateGiftCardTransaction(String transactionid, Order order,
      OrderLine orderLine, String paymentId) {
    
    GiftCardTrans trans = OBDal.getInstance().get(GiftCardTrans.class, transactionid);
    FIN_Payment payment = null;

    if (paymentId != null) {
      payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
    }
    
    if (payment != null && trans.getAmount().compareTo(payment.getAmount()) != 0) {
      trans.setAmount(trans.getAmount().subtract(payment.getAmount()));
      GiftCardTrans newTrans = (GiftCardTrans) DalUtil.copy(trans);
      newTrans.setAmount(payment.getAmount());
      newTrans.setPayment(payment);
      newTrans.setSalesOrder(order);
      OBDal.getInstance().save(newTrans);
    } else {
      if (payment != null) {
        trans.setPayment(payment);
      }
      trans.setSalesOrder(order);
      trans.setSalesOrderLine(orderLine);
    }

    OBDal.getInstance().save(trans);
    OBDal.getInstance().flush();

    return trans;
  }

  public GiftCardTrans populateGiftCardTransactionReturn(String transactionid, Order order,
      OrderLine orderLine, String paymentId) throws Exception {

    FIN_Payment paymentOrigin = null;
    if (transactionid != null) {
      paymentOrigin = OBDal.getInstance().get(FIN_Payment.class, transactionid);
    }
    
    final OBCriteria<GiftCardTrans> obCriteria = OBDal.getInstance().createCriteria(
        GiftCardTrans.class);
    obCriteria.add(Restrictions.eq(GiftCardTrans.PROPERTY_PAYMENT, paymentOrigin));
    obCriteria.setMaxResults(1);
    
    final List<GiftCardTrans> listt = obCriteria.list();

    if (listt == null || listt.size() != 1) {
      // Gift Card With ID %0 not found.
      throw new Exception("error");
    }

    GiftCardTrans trans = listt.get(0);    
    
    FIN_Payment payment = null;

    if (paymentId != null) {
      payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
    }    
    
    if (payment != null && trans.getAmount().compareTo(payment.getAmount()) != 0) {
      BigDecimal amount = trans.getGcnvGiftcardInst().getCurrentamount().add(payment.getAmount().abs());
      trans.getGcnvGiftcardInst().setCurrentamount(amount);
      GiftCardTrans newTrans = (GiftCardTrans) DalUtil.copy(trans);
      newTrans.setAmount(payment.getAmount());
      newTrans.setPayment(payment);
      newTrans.setSalesOrder(order);
      OBDal.getInstance().save(newTrans);
    } else {
      if (payment != null) {
        trans.setPayment(payment);
      }
      trans.setSalesOrder(order);
      trans.setSalesOrderLine(orderLine);
    }

    OBDal.getInstance().save(trans);
    OBDal.getInstance().flush();

    return trans;
  }  
  
  public GiftCardInst findGiftCard(String giftcardid, String giftcardtype) throws Exception {

    final OBCriteria<GiftCardInst> obCriteria = OBDal.getInstance().createCriteria(
        GiftCardInst.class);
    obCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_SEARCHKEY, giftcardid));
    obCriteria.setMaxResults(1);

    final List<GiftCardInst> listt = obCriteria.list();

    if (listt == null || listt.size() != 1) {
      // Gift Card With ID %0 not found.
      throw new Exception("SRGC_ErrorGiftCardNotExists:" + giftcardid);
    }

    GiftCardInst giftcard = listt.get(0);

    // Check gift card type
    if (giftcardtype != null) {
      if ("BasedOnGLItem".equals(giftcard.getType())) {
        if ("V".equals(giftcardtype)) {
          throw new Exception("GCNV_ErrorGiftCardNotVoucher:" + giftcardid);
        }
        if (!"G".equals(giftcardtype)) {
          throw new Exception("GCNV_ErrorGiftCardInvalid:" + giftcard.getSearchKey());
        }
      } else if (giftcard.getType().equals("BasedOnCreditNote")) {
        if ("V".equals(giftcardtype)) {
          throw new Exception("GCNV_ErrorCreditNoteInvalid:" + giftcard.getSearchKey());
        }
      } else {
        if (!giftcardtype.equals(giftcard.getProduct().getGcnvGiftcardtype())) {
          if ("G".equals(giftcardtype)) {
            throw new Exception("GCNV_ErrorGiftCardNotGift:" + giftcardid);
          }
          if ("V".equals(giftcardtype)) {
            throw new Exception("GCNV_ErrorGiftCardNotVoucher:" + giftcardid);
          }
          throw new Exception("GCNV_ErrorGiftCardInvalid:" + giftcard.getSearchKey());
        }
      }
    }

    return giftcard;
  }

  public GiftCardInst createGiftCard(String giftcardid, Product product, BusinessPartner bp,
      Date orderDate, Order order, OrderLine orderLine, BigDecimal orderLineAmount)
      throws Exception {
    return createGiftCard(giftcardid, product, bp, orderDate, order, orderLine, orderLineAmount,
        null);
  }

  public GiftCardInst createGiftCard(String giftcardid, Product product, BusinessPartner bp,
      Date orderDate, Order order, OrderLine orderLine, BigDecimal orderLineAmount,
      JSONObject giftCardProperties) throws Exception {

    if (product == null) {
      // do not create a gift card
      return null;
    } else if ("G".equals(product.getGcnvGiftcardtype())) {
      // create a gift card
    } else if ("V".equals(product.getGcnvGiftcardtype())) {
      // create a gift voucher
    } else {
      // do not create a gift card
      return null;
    }

    OBContext.setAdminMode(false);
    GiftCardInst giftcard = null;
    OrganizationStructureProvider osp = new OrganizationStructureProvider();
    try {

      giftcard = OBProvider.getInstance().get(GiftCardInst.class);
      giftcard.setOrganization(osp.getLegalEntity(order.getOrganization()));
      giftcard.setActive(true);

      giftcard.setSearchKey(giftcardid == null ? UUID.randomUUID().toString().replace("-", "")
          .toUpperCase() : giftcardid);
      giftcard.setProduct(product);
      giftcard.setBusinessPartner(bp);
      giftcard.setObgcneGCOwner(bp);
      giftcard.setOrderDate(orderDate);
      giftcard.setSalesOrder(order);
      giftcard.setSalesOrderLine(orderLine);
      giftcard.setAlertStatus("N");

      if ("G".equals(product.getGcnvGiftcardtype())) {
        BigDecimal amount = (product.getGcnvAmount() == null || product.getGcnvAmount().intValue() == 0) ? orderLineAmount
            : product.getGcnvAmount();
        giftcard.setAmount(amount);
        giftcard.setCurrentamount(amount);
        giftcard.setType("BasedOnProductGiftCard");
      } else if ("V".equals(product.getGcnvGiftcardtype())) {
        giftcard.setType("BasedOnVoucher");
        for (ProductSummary productSumm : product.getGCNVProductSummaryList()) {
          GiftCardSummary gcSumm = OBProvider.getInstance().get(GiftCardSummary.class);
          giftcard.getGCNVGiftCardSummaryList().add(gcSumm);
          gcSumm.setGcnvGiftcardInst(giftcard);
          gcSumm.setOrganization(OBContext.getOBContext().getCurrentOrganization());
          gcSumm.setActive(true);
          gcSumm.setProduct(productSumm.getIncproduct());
          gcSumm.setQuantity(productSumm.getQuantity());
          gcSumm.setCurrentquantity(productSumm.getQuantity());
        }
      }

      if (giftCardProperties != null) {
        JSONPropertyToEntity.fillBobFromJSON(giftcard.getEntity(), giftcard, giftCardProperties);
      }

      // Save and return
      OBDal.getInstance().save(giftcard);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
    return giftcard;
  }

  @Deprecated
  public GiftCardInst createCreditNote(Organization org, String giftcardid, BusinessPartner bp,
      Date orderDate, Order order, OrderLine orderLine, BigDecimal paymentAmount,
      OBPOSAppPayment appPaymentObj) throws Exception {
    return createCreditNote(org, giftcardid, bp, orderDate, order, orderLine, paymentAmount,
        getPaymentforOrder(order, appPaymentObj));
  }

  public GiftCardInst createCreditNote(Organization org, String giftcardid, BusinessPartner bp,
      Date orderDate, Order order, OrderLine orderLine, BigDecimal paymentAmount,
      FIN_Payment origPayment) throws Exception {

    OBContext.setAdminMode(false);

    GiftCardInst giftcard = null;
    OrganizationStructureProvider osp = new OrganizationStructureProvider();
    try {
      BigDecimal totalCreditNote = BigDecimal.ZERO, totalAmount = paymentAmount.abs();
      Organization organization = order.getOrganization();

      giftcard = OBProvider.getInstance().get(GiftCardInst.class);
      giftcard.setOrganization(osp.getLegalEntity(organization));
      giftcard.setActive(true);
      giftcard.setBusinessPartner(bp);
      giftcard.setOrderDate(orderDate);
      giftcard.setType("BasedOnCreditNote");
      giftcard.setSalesOrder(order);
      giftcard.setReturnCOrder(order);
      giftcard.setSalesOrderLine(orderLine);
      giftcard.setAlertStatus("N");

      String defDocNo = (giftcardid == null ? UUID.randomUUID().toString().replace("-", "")
          .toUpperCase() : giftcardid);
      giftcard.setSearchKey(defDocNo);

      if (bp.isGcnvUniquecreditnote() && org.getObretcoCBpartner().getId() != bp.getId()) {

        final OBCriteria<GiftCardInst> giftCardInstCriteria = OBDal.getInstance().createCriteria(
            GiftCardInst.class);
        giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_BUSINESSPARTNER, bp));
        giftCardInstCriteria.add(Restrictions.ne(GiftCardInst.PROPERTY_ALERTSTATUS,
            GiftCardGLItemUtils.STATUS_CLOSED));
        giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_ISCANCELLED, false));
        giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_TYPE, "BasedOnCreditNote"));
        final List<GiftCardInst> giftCardInstList = giftCardInstCriteria.list();

        long maxTime = 0;
        for (GiftCardInst giftCardInst : giftCardInstList) {
          if (giftCardInst.getCreationDate().getTime() > maxTime) {
            defDocNo = giftCardInst.getSearchKey().split("\\*C")[0];
            maxTime = giftCardInst.getCreationDate().getTime();
          }
        }
        giftcard.setSearchKey(defDocNo);
        String cancelledGiftCards = "";
        final int start = defDocNo.length() + 3;
        int maxCancelled = 0;
        final String sqlString = "SELECT COALESCE(max(to_number(substr(value, :start))), 0) "
            + "FROM gcnv_giftcard_inst WHERE c_bpartner_id = :bpartner AND status = 'C' "
            + "AND value LIKE :documentNo";
        final Session session = OBDal.getInstance().getSession();
        final Query query = session.createSQLQuery(sqlString);
        query.setParameter("start", start);
        query.setParameter("bpartner", bp.getId());
        query.setParameter("documentNo", defDocNo + "%");
        final String result = (String) query.uniqueResult().toString();
        if (result != null) {
          maxCancelled = Integer.parseInt(result);
        }
        for (GiftCardInst giftCardInst : giftCardInstList) {
          final String cancelledAmount = giftCardInst.getCurrentamount().toString();
          totalCreditNote = totalCreditNote.add(giftCardInst.getCurrentamount());
          String oBPOSAppCashupId = order.getObposAppCashup();
          OBPOSAppCashup oBPOSAppCashup = OBDal.getInstance().get(OBPOSAppCashup.class,
              oBPOSAppCashupId);
          GiftCardGLItemUtils.close(giftCardInst, oBPOSAppCashup, null);
          giftCardInst.setCancelGiftCard(true);

          maxCancelled++;
          giftCardInst.setSearchKey(defDocNo + "*C" + maxCancelled);

          cancelledGiftCards += " " + giftCardInst.getSearchKey() + " (" + cancelledAmount + "),";
          OBDal.getInstance().save(giftCardInst);
        }

        totalAmount = totalAmount.add(totalCreditNote);
        if (cancelledGiftCards.length() > 1) {
          cancelledGiftCards = cancelledGiftCards.substring(1, cancelledGiftCards.length() - 1);
        }
        String[] args = { cancelledGiftCards };
        origPayment.setDescription(origPayment.getDescription()
            + OBMessageUtils.getI18NMessage("GCNV_CancelledGiftCards", args));
      }

      giftcard.setPayment(origPayment);
      giftcard.setAmount(totalAmount);
      giftcard.setCurrentamount(totalAmount);

      // Save and return
      OBDal.getInstance().save(giftcard);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
    return giftcard;
  }

  private FIN_Payment getPaymentforOrder(Order order, OBPOSAppPayment appPaymentObj) {
    OBCriteria<FIN_PaymentSchedule> paymentSchCri = OBDal.getInstance().createCriteria(
        FIN_PaymentSchedule.class);
    paymentSchCri.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_ORDER, order));
    paymentSchCri.setMaxResults(1);
    FIN_PaymentSchedule paymentSchObj = paymentSchCri.list().size() > 0 ? paymentSchCri.list().get(
        0) : null;
    if (paymentSchObj != null) {
      OBCriteria<FIN_PaymentScheduleDetail> paymentSchDetailCri = OBDal.getInstance()
          .createCriteria(FIN_PaymentScheduleDetail.class);
      paymentSchDetailCri.add(Restrictions.eq(
          FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE, paymentSchObj));
      paymentSchCri.setMaxResults(1);
      List<FIN_PaymentScheduleDetail> paymentSchDetailList = paymentSchDetailCri.list();
      for (FIN_PaymentScheduleDetail paymentSchDetailObj : paymentSchDetailList) {
        if (paymentSchDetailObj.getPaymentDetails() != null
            && appPaymentObj
                .getFinancialAccount()
                .getId()
                .equals(
                    paymentSchDetailObj.getPaymentDetails().getFinPayment().getAccount().getId())) {
          return paymentSchDetailObj.getPaymentDetails().getFinPayment();
        }

      }
    } else {
      return null;
    }
    return null;
  }

}
