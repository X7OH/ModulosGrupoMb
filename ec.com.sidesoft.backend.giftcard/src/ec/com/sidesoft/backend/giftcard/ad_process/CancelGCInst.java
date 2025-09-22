package ec.com.sidesoft.backend.giftcard.ad_process;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardSummary;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardTrans;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

public class CancelGCInst extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    OBError myMessage = new OBError();
    final String giftcardInstId = (String) bundle.getParams().get("Gcnv_Giftcard_Inst_ID");
    GiftCardInst terminal = OBDal.getInstance().get(GiftCardInst.class, giftcardInstId);
    
    Product product = terminal.getProduct();
    BigDecimal amountGC = null;
    
    if (terminal.getAmount() == null || terminal.getAmount().intValue() == 0) {
      
      if (product.getGcnvAmount() == null || product.getGcnvAmount().intValue() == 0) {
        myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
            "BKGC_GenerateGC_Error", OBContext.getOBContext().getLanguage().getLanguage())));
        myMessage.setType("Error");
        myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
            OBContext.getOBContext().getLanguage().getLanguage()));
        bundle.setResult(myMessage);
        return;
        
      } else {
        amountGC = product.getGcnvAmount();
      }
      
    } else {
      amountGC = terminal.getAmount();
    }

    try {
      
      // SE CANCELA LA GIFTCARD
      cancelGiftCard(terminal.getId(), terminal.getCreationDate(), null, null, amountGC);
      
      myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
          "BKGC_CancelGC_Exito", OBContext.getOBContext().getLanguage().getLanguage())));
      myMessage.setType("Success");
      myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Success",
          OBContext.getOBContext().getLanguage().getLanguage()));
      bundle.setResult(myMessage);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
          "BKGC_CancelGC_Error2", OBContext.getOBContext().getLanguage().getLanguage())));
      myMessage.setType("Error");
      myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
          OBContext.getOBContext().getLanguage().getLanguage()));
      bundle.setResult(myMessage);
    }

  }

  public void cancelGiftCard(String giftcardid, Date orderDate, Order order, OrderLine orderLine,
      BigDecimal tamount) throws Exception {

    final OBCriteria<GiftCardInst> obCriteria = OBDal.getInstance()
        .createCriteria(GiftCardInst.class);
    obCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_ID, giftcardid));
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

    // Cancel Gift Card
    if ("G".equals(giftcard.getProduct().getGcnvGiftcardtype())) {
      // Gift card

      // If new we return the transaction amount, the amount the card was sold
      // If partial we return the current amount
      BigDecimal amount = "N".equals(giftcard.getAlertStatus()) ? tamount
          : giftcard.getCurrentamount();

      giftcard.setCurrentamount(BigDecimal.ZERO);

      // Create a cancel transaction with amount
      GiftCardTrans trans = OBProvider.getInstance().get(GiftCardTrans.class);
      trans.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      trans.setActive(true);
      trans.setOrderDate(orderDate);
      trans.setSalesOrder(order);
      trans.setSalesOrderLine(orderLine);
      trans.setAmount(amount);
      trans.setCancelled(true);
      trans.setGcnvGiftcardInst(giftcard);
      OBDal.getInstance().save(trans);

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
      trans.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      trans.setActive(true);
      trans.setOrderDate(orderDate);
      trans.setSalesOrder(order);
      trans.setSalesOrderLine(orderLine);
      trans.setAmount(tamount);
      trans.setCancelled(true);
      trans.setGcnvGiftcardInst(giftcard);
      OBDal.getInstance().save(trans);

    } else {
      throw new Exception("GCNV_ErrorGiftCardInvalid:" + giftcard.getSearchKey());
    }
    giftcard.setAlertStatus("C");
    giftcard.setCancelled(true);

    // Save and return
    OBDal.getInstance().save(giftcard);
    OBDal.getInstance().flush();
  }

}