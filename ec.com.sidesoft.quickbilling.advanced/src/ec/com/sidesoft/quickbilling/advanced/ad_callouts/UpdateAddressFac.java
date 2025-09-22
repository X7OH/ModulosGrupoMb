package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.Location;

public class UpdateAddressFac extends SimpleCallout {
  
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strNewAddress = info.getStringParameter("inpaddress1AliasRefFac", null);

    if (strNewAddress != null && !strNewAddress.equals("")) {
	      Location objLocation = OBDal.getInstance().get(Location.class, strNewAddress);
	      info.addResult("inpphoneFac", objLocation.getPhone());
	   //   info.addResult("inpaddress1Fac", objLocation.getSaqbReference());
	      info.addResult("inpcBpartnerLocationFacId", objLocation.getId());
	      info.addResult("inpaddress1AliasFac", objLocation.getSaqbAlias());
	      info.addResult("inpaddressCompleteFac", objLocation.getName());
	      info.addResult("inpnewAddressFac", false);
    }else{
        info.addResult("inpphoneFac", null);
      //  info.addResult("inpaddress1Fac", null);
        info.addResult("inpcBpartnerLocationIdFac", "");
        info.addResult("inpaddress1AliasFac", null);
        info.addResult("inpaddressCompleteFac", null);
    }

  }
}
