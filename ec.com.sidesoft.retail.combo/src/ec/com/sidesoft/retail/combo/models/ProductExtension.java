package ec.com.sidesoft.retail.combo.models;

import java.util.Arrays;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.retail.posterminal.master.Product;

@Qualifier(Product.productPropertyExtension)
public class ProductExtension extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    return Arrays.asList(new HQLProperty("product.sSRCMHideFromWebPOS", "sSRCMHideFromWebPOS"),
        new HQLProperty("product.productCategory.id", "productComboCategory"));
  }
}
