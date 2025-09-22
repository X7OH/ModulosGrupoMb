package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class NewAddressFac extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strNewAddress = info.getStringParameter("inpnewAddressFac", null);

    if (strNewAddress.equals("Y")) {
      info.addResult("inpaddress1AliasRefFac", "");
      info.addResult("inpphoneFac", null);
      //info.addResult("inpaddress1Fac", null);
      info.addResult("inpcBpartnerLocationFacId", "");
      info.addResult("inpaddress1AliasFac", null);
      info.addResult("inpaddressCompleteFac", null);
      
    }
  }
}
