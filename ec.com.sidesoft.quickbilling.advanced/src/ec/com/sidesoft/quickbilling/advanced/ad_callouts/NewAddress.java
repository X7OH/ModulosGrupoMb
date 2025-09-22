package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class NewAddress extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strNewAddress = info.getStringParameter("inpnewAddress", null);

    if (strNewAddress.equals("Y")) {
      info.addResult("inpaddress1AliasRef", "");
      info.addResult("inpphone", null);
      info.addResult("inpaddress1", null);
      info.addResult("inpcBpartnerLocationId", "");
      info.addResult("inpaddress1Alias", null);
      info.addResult("inpaddressComplete", null);
      info.addResult("inpcSalesregionId", "");
    }
  }
}
