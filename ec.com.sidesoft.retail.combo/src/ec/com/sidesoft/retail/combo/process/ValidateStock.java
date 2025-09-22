package ec.com.sidesoft.retail.combo.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductBOM;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

public class ValidateStock {

  ArrayList<MaterialTransaction> materialTransactionList = new ArrayList<MaterialTransaction>();

  public Product validateStock(Order order) throws OBException {

    OBContext.setAdminMode(true);

    try {
      Organization organization = order.getOrganization();

      if (!organization.isReady()) {
        throw new OBException(OBMessageUtils.messageBD("OrgHeaderNotReady"));
      }
      if (!organization.getOrganizationType().isTransactionsAllowed()) {
        throw new OBException(OBMessageUtils.messageBD("OrgHeaderNotTransAllowed"));
      }

      Date date = order.getOrderDate();

      List<OrderLine> lines = order.getOrderLineList();

      Product objProductWithoutStock = null;

      for (OrderLine line : lines) {

        objProductWithoutStock = null;
        Product product = line.getProduct();
        BigDecimal qty = line.getOrderedQuantity();
        if (product.isBillOfMaterials() && product.isObbomAutogeneratebom() && qty.signum() == 1) {
          objProductWithoutStock = validateProductBOMRecurrency(product, qty, organization, date,
              order);
          if (objProductWithoutStock != null) {
            return objProductWithoutStock;
          }

        } else if (!product.isBillOfMaterials() && !product.isObbomAutogeneratebom()
            && product.getProductType().equals("I") && product.isStocked() && qty.signum() == 1) {
          BigDecimal stock = getStockByWarehouse(null, product, order.getWarehouse(), false);
          if (stock.compareTo(qty) == -1) {
            return product;
          }
        }
      }

      return null;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private Product validateProductBOMRecurrency(Product product, BigDecimal qty,
      Organization organization, Date date, Order order) throws OBException {
    Product objProducto = null;
    for (ProductBOM productBOM : product.getProductBOMList()) {
      Product bomProduct = productBOM.getBOMProduct();
      if (!bomProduct.getProductType().equals("I") || !bomProduct.isStocked()) {
        continue;
      }
      BigDecimal stock = getStockByWarehouse(productBOM, null, order.getWarehouse(), true);
      BigDecimal totalQty = qty.multiply(productBOM.getBOMQuantity());
      if (stock.compareTo(totalQty) == -1) {
        if (bomProduct.isBillOfMaterials() && bomProduct.isObbomAutogeneratebom()) {

          objProducto = validateProductBOMRecurrency(productBOM.getBOMProduct(),
              totalQty.subtract(stock), organization, date, order);
          if (objProducto != null) {
            return objProducto;
          }

        } else {

          return bomProduct;

        }
      }
    }
    return null;
  }

  private BigDecimal getStockByWarehouse(ProductBOM productBOM, Product product,
      Warehouse warehouse, boolean isBOM) {

    StringBuffer selectSD = new StringBuffer();
    selectSD.append(" select sum(sd." + StorageDetail.PROPERTY_QUANTITYONHAND + " - sd."
        + StorageDetail.PROPERTY_RESERVEDQTY + ") as stock");
    selectSD.append(" from " + StorageDetail.ENTITY_NAME + " as sd");
    selectSD.append(" where sd." + StorageDetail.PROPERTY_PRODUCT + ".id = :product");
    selectSD.append(" and sd." + StorageDetail.PROPERTY_STORAGEBIN + "."
        + Locator.PROPERTY_WAREHOUSE + ".id = :warehouse");

    Query stockQrySD = OBDal.getInstance().getSession().createQuery(selectSD.toString());
    if (isBOM) {
      stockQrySD.setParameter("product", productBOM.getBOMProduct().getId());
    } else {
      stockQrySD.setParameter("product", product.getId());
    }
    stockQrySD.setParameter("warehouse", warehouse.getId());

    BigDecimal stock = (BigDecimal) stockQrySD.uniqueResult();

    stock = (stock == null ? BigDecimal.ZERO : stock);
    return stock;

  }

}
