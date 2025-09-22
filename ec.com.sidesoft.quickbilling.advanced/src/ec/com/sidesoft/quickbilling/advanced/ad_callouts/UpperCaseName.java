package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class UpperCaseName extends SimpleCallout {
  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strPartnerName = info.getStringParameter("inpnamePartner", null);
    info.addResult("inpnamePartner", strPartnerName.toUpperCase());

  }
}
