package ec.com.sidesoft.integration.delivery.ad_callouts;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import org.openbravo.retail.discounts.combo.ComboProductFamily;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;

import org.openbravo.erpCommon.utility.OBMessageUtils;

public class SDELVR_UniqueFamilyValidate extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strObcomboFamilyid = info.getStringParameter("inpobcomboFamilyId", null);
    String strDeliveryPrice = info.getStringParameter("inpemSdelvrDeliprice", null);
    String strmOfferid = info.getStringParameter("inpmOfferId", null);
    PriceAdjustment offerID = OBDal.getInstance().get(PriceAdjustment.class, strmOfferid);
    
    OBCriteria<ComboProductFamily> fmCrt = OBDal.getInstance().createCriteria(ComboProductFamily.class);
    fmCrt.add(Restrictions.eq(ComboProductFamily.PROPERTY_SDELVRDELIPRICE, true));
    fmCrt.add(Restrictions.eq(ComboProductFamily.PROPERTY_PRICEADJUSTMENT, offerID));
    fmCrt.add(Restrictions.ne(ComboProductFamily.PROPERTY_ID, strObcomboFamilyid));
    
    if(fmCrt.count() == 0 && strDeliveryPrice.equals("N")) {
        info.addResult("WARNING", OBMessageUtils.parseTranslation(
                OBMessageUtils.messageBD("Sdelvr_MainFamilyPrice")));    	
    }
  }

}
