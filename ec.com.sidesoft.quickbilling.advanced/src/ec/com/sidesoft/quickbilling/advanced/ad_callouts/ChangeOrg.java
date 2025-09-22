package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.enterprise.Organization;

public class ChangeOrg extends SimpleCallout {
  // private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    info.addResult("inpdefaultproductscharged", false);

    String strOrgId = info.getStringParameter("inpadOrgRegionId", null);
    Organization objOrganization = OBDal.getInstance().get(Organization.class, strOrgId);

    info.addResult("inpattentionHours", objOrganization.getSaqbAttentionHours());

  }
}
