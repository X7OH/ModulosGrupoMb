package ec.com.sidesoft.integration.peya.webservices.ad_callouts;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import org.openbravo.retail.discounts.combo.ComboProductFamily;
import org.openbravo.retail.discounts.combo.ComboProduct;

import org.openbravo.erpCommon.utility.OBMessageUtils;

public class SDELVR_UniqueFamilyProductValidate extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strObcomboProductid = info.getStringParameter("inpobcomboProductId", null);
    String strGlovo = info.getStringParameter("inpemSglovoDefault", null);
    String strObcomboFamilyid = info.getStringParameter("inpobcomboFamilyId", null);
    ComboProductFamily ObcomboFamilyID = OBDal.getInstance().get(ComboProductFamily.class, strObcomboFamilyid);
    
    OBCriteria<ComboProduct> fmCrt = OBDal.getInstance().createCriteria(ComboProduct.class);
    fmCrt.add(Restrictions.eq(ComboProduct.PROPERTY_SGLOVODEFAULT, true));
    fmCrt.add(Restrictions.eq(ComboProduct.PROPERTY_OBCOMBOFAMILY, ObcomboFamilyID));
    fmCrt.add(Restrictions.ne(ComboProduct.PROPERTY_ID, strObcomboProductid));
    
    if(fmCrt.count() == 0 && strGlovo.equals("N")) {
        info.addResult("WARNING", OBMessageUtils.parseTranslation(
                OBMessageUtils.messageBD("Sdelvr_Has_Main_Family")));    	
    }
  }

}
