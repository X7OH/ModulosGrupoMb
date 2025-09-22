package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.sales.SalesRegion;

public class ChangeSalesRegion extends SimpleCallout {
  // private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strNewAddress = info.getStringParameter("inpcSalesregionId", null);

    SalesRegion objSalesRegion = OBDal.getInstance().get(SalesRegion.class, strNewAddress);

    info.addResult("inpadOrgRegionId", objSalesRegion.getOrganization().getId());

  }
}
