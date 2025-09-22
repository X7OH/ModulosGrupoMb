package ec.com.sidesoft.retail.combo.models;

import java.util.Arrays;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.retail.posterminal.master.Product;

@Qualifier(Product.productDiscPropertyExtension)
public class ProductDiscExtension extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    return Arrays.asList(new HQLProperty("'false'", "sSRCMHideFromWebPOS"),
        new HQLProperty(
            "(case when p.ssrcmProCategory.id != null  then p.ssrcmProCategory.id else p.discountType.id end )",
            "productComboCategory", 999));
  }
}
