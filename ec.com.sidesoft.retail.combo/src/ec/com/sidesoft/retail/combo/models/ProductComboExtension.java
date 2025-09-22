package ec.com.sidesoft.retail.combo.models;

import java.util.Arrays;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.retail.discounts.combo.master.Product;

@Qualifier(Product.productComboPropertyExtension)
public class ProductComboExtension extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    return Arrays.asList(new HQLProperty("cp.ssrcmExtraSuplement", "ssrcmExtraSuplement"));
  }
}