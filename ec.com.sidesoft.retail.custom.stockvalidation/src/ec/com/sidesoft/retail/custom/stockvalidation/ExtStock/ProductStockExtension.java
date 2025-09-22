package ec.com.sidesoft.retail.custom.stockvalidation.ExtStock;

import java.util.Arrays;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.retail.posterminal.master.Product;

@Qualifier(Product.productPropertyExtension)
public class ProductStockExtension extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    return Arrays.asList(new HQLProperty("coalesce((select pr.billOfMaterials from Product as pr"
        + " where pr=product),'false')", "isboom"));
  }
}
