package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.Location;

public class UpdateAddress extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strNewAddress = info.getStringParameter("inpaddress1AliasRef", null);

    if (strNewAddress != null && !strNewAddress.equals("")) {
      Location objLocation = OBDal.getInstance().get(Location.class, strNewAddress);
      info.addResult("inpphone", objLocation.getPhone());
      info.addResult("inpaddress1", objLocation.getSaqbReference());
      info.addResult("inpcBpartnerLocationId", objLocation.getId());
      info.addResult("inpaddress1Alias", objLocation.getSaqbAlias());
      info.addResult("inpaddressComplete", objLocation.getName());
      if (objLocation.getSalesRegion() != null) {
        info.addResult("inpcSalesregionId", objLocation.getSalesRegion().getId());
      }
      info.addResult("inpnewAddress", false);

    }else{
        info.addResult("inpphone", null);
        info.addResult("inpaddress1", null);
        info.addResult("inpcBpartnerLocationId", "");
        info.addResult("inpaddress1Alias", null);
        info.addResult("inpaddressComplete", null);
        info.addResult("inpcSalesregionId", "");
    }

  }
}
