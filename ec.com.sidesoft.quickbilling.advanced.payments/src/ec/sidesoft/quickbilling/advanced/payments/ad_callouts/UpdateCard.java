package ec.com.sidesoft.quickbilling.advanced.payments.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;

import ec.com.sidesoft.quickbilling.advanced.SaqbCard;

public class UpdateCard extends SimpleCallout {
  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strPaymentMethod = info.getStringParameter("inpfinPaymentmethodId", null);
    String strCardId = info.getStringParameter("inpsaqbCardId", null);

    FIN_PaymentMethod objPaymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class,
        strPaymentMethod);

    if (objPaymentMethod != null) {

      SaqbCard objCard = null;
      if (strCardId != null && !strCardId.equals("") && !strCardId.equals("null")) {
        objCard = OBDal.getInstance().get(SaqbCard.class, strCardId);
      }

      if (objPaymentMethod.isSaqbCallCenter()
          && objPaymentMethod.getSaqbTypeCallCenter().equals("TAR") && objCard != null) {

        info.addResult("inpscaiCardBrandId", (objCard.getScaiCardBrand() == null ? "" : objCard
            .getScaiCardBrand().getId()));
        info.addResult("inpcardtype", (objCard.getCardType() == null ? "" : objCard.getCardType()));
        info.addResult("inpcardno", objCard.getIdentifier());
        info.addResult("inpexpirationdate", objCard.getExpirationdate());
        // info.addResult("inpsecuritycode", objCard.getSecuritycode());
        info.addResult("inpcardpropietary", objCard.getCardPropietary());
        info.addResult("inpsaqbFinancialEntityId", (objCard.getSaqbFinancialEntity() == null ? ""
            : objCard.getSaqbFinancialEntity().getId()));
        info.addResult("inpnewcard", false);

      }
    }

  }
}
