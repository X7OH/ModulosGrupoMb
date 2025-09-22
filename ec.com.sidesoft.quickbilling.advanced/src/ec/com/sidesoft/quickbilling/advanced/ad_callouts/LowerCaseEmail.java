package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class LowerCaseEmail extends SimpleCallout {
  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strEmail = info.getStringParameter("inpemail", null);
    // String strUOMProduct = info.getStringParameter("inpmProductUomId", IsIDFilter.instance);
    info.addResult("inpemail", strEmail.toLowerCase());
  }
}
