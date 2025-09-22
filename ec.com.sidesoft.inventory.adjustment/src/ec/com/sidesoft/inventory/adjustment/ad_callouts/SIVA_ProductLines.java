package ec.com.sidesoft.inventory.adjustment.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.Product;

public class SIVA_ProductLines extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String UOM_ID;
    String ProductCategory_ID;
    String Product_ID = info.getStringParameter("inpmProductId", null);

    Product product_obj = OBDal.getInstance().get(Product.class, Product_ID);

    UOM_ID = product_obj.getUOM().getId();
    ProductCategory_ID = product_obj.getProductCategory().getId();

    info.addResult("inpcUomId", UOM_ID);
    info.addResult("inpmProductCategoryId", ProductCategory_ID);

  }
}
