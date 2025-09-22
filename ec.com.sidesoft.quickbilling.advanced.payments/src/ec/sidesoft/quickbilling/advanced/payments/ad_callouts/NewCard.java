package ec.com.sidesoft.quickbilling.advanced.payments.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class NewCard extends SimpleCallout {
  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strNewCard = info.getStringParameter("inpnewcard", null);

    if (strNewCard.equals("Y")) {
      info.addResult("inpscaiCardBrandId", "");
      info.addResult("inpcardtype", "");
      info.addResult("inpcardno", null);
      info.addResult("inpexpirationdate", null);
      info.addResult("inpsecuritycode", null);
      info.addResult("inpcardpropietary", null);
      info.addResult("inpsaqbFinancialEntityId", "");
    }
  }
}
