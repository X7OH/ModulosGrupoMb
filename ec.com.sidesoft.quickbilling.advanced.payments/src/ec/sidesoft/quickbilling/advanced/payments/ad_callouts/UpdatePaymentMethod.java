package ec.com.sidesoft.quickbilling.advanced.payments.ad_callouts;

import java.math.BigDecimal;
import java.util.List;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;

import ec.com.sidesoft.quickbilling.advanced.SaqbCard;
import ec.com.sidesoft.quickbilling.advanced.SaqbOrder;
import ec.com.sidesoft.quickbilling.advanced.SaqbOrderline;
import ec.com.sidesoft.quickbilling.advanced.payments.SaqbpPaymentMethods;

public class UpdatePaymentMethod extends SimpleCallout {

  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strPaymentMethod = info.getStringParameter("inpfinPaymentmethodId", null);
    String strInpSaqbOrderId = info.getStringParameter("inpsaqbOrderId", null);
    SaqbOrder objSaqbOrder = OBDal.getInstance().get(SaqbOrder.class, strInpSaqbOrderId);

    String strSaqbpPaymentId = info.getStringParameter("inpsaqbpPaymentMethodsId", null);
    strSaqbpPaymentId = (strSaqbpPaymentId==""?null:strSaqbpPaymentId);

    if(strSaqbpPaymentId == null){ 
	    // NUEVA CANTIDAD RESTADA DEL TOTAL
	    OBCriteria<SaqbpPaymentMethods> objSaqbpPaymentMethods = OBDal.getInstance().createCriteria(
	        SaqbpPaymentMethods.class);
	    objSaqbpPaymentMethods.add(Restrictions.eq("saqbOrder", objSaqbOrder));
	
	    List<SaqbpPaymentMethods> lstSaqbpPaymentMethods = objSaqbpPaymentMethods.list();
	    BigDecimal bdtotalpayments = new BigDecimal(0);
	    for (SaqbpPaymentMethods lstSaqbpPaymentMethods2 : lstSaqbpPaymentMethods) {
	      bdtotalpayments = bdtotalpayments
	          .add(lstSaqbpPaymentMethods2.getAmount() == null ? new BigDecimal(0)
	              : lstSaqbpPaymentMethods2.getAmount());
	    }
	
	    BigDecimal bdTotal = (objSaqbOrder.getGrandTotalAmount() == null ? BigDecimal.ZERO
	        : objSaqbOrder.getGrandTotalAmount()).subtract(bdtotalpayments);
	
	    info.addResult("inpamount", (bdTotal.compareTo(BigDecimal.ZERO) == -1 ? BigDecimal.ZERO
	        : bdTotal));
   }
    

    // ACTUALIZA TIPO DE MÉTODO DE PAGO
    FIN_PaymentMethod objPaymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class,
        strPaymentMethod);

    if (objPaymentMethod != null) {

      info.addResult("inptypeCallCenter", objPaymentMethod.getSaqbTypeCallCenter());
    }
    // ACTUALIZA SI EL TERCERO NO TIENE TARJETAS
    if (objPaymentMethod.getSaqbTypeCallCenter().equals("TAR")) {

      OBCriteria<SaqbCard> objSaqbCard = OBDal.getInstance().createCriteria(SaqbCard.class);
      objSaqbCard.add(Restrictions.eq("businessPartner", objSaqbOrder.getBusinessPartner()));

      if (objSaqbCard.list().size() == 0) {
        info.addResult("inpnewcard", true);
      }

    }

  }
}
