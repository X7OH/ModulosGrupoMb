package ec.com.sidesoft.retail.combo.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.StockUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductBOM;
import org.openbravo.model.common.plm.ProductOrg;
import org.openbravo.model.materialmgmt.onhandquantity.StockProposed;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ProductionPlan;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;

public class OwnBOMProcess {

  ArrayList<MaterialTransaction> materialTransactionList = new ArrayList<MaterialTransaction>();

  public ArrayList<MaterialTransaction> createProcessBOM(Order order)
      throws OBException, JSONException {

    OBContext.setAdminMode(true);

    try {
      Organization organization = order.getOrganization();

      if (!organization.isReady()) {
        throw new OBException(OBMessageUtils.messageBD("OrgHeaderNotReady"));
      }
      if (!organization.getOrganizationType().isTransactionsAllowed()) {
        throw new OBException(OBMessageUtils.messageBD("OrgHeaderNotTransAllowed"));
      }

      Map<Product, BigDecimal> bomProducts = new HashMap<Product, BigDecimal>();

      Date date = order.getOrderDate();

      List<OrderLine> lines = order.getOrderLineList();

      for (OrderLine line : lines) {

        Product product = line.getProduct();
        BigDecimal qty = line.getOrderedQuantity();
        if (product.isBillOfMaterials() && product.isObbomAutogeneratebom() && qty.signum() == 1) {
          if (bomProducts.containsKey(product)) {
            bomProducts.put(product, bomProducts.get(product).add(qty));
          } else {
            bomProducts.put(product, qty);
          }
          createRecurrencyBOM(product, qty, organization, date, order);
        }
      }

      if (!bomProducts.isEmpty()) {
        createAndProcess(bomProducts, organization, date, order);
      }

    } finally {
      OBContext.restorePreviousMode();
    }

    return materialTransactionList;
  }

  private void createRecurrencyBOM(Product product, BigDecimal qty, Organization organization,
      Date date, Order order) throws OBException {

    Map<Product, BigDecimal> bomProducts = new HashMap<Product, BigDecimal>();

    for (ProductBOM productBOM : product.getProductBOMList()) {
      Product bomProduct = productBOM.getBOMProduct();
      if (bomProduct.isBillOfMaterials() && bomProduct.isObbomAutogeneratebom()
          && bomProduct.getProductType().equals("I") && bomProduct.isStocked()) {
        BigDecimal stock = getStock(productBOM, organization);
        BigDecimal totalQty = qty.multiply(productBOM.getBOMQuantity());
        if (stock == null || stock.compareTo(BigDecimal.ZERO) == -1) {
          stock = BigDecimal.ZERO;
        }
        if (stock.compareTo(totalQty) == -1) {
          bomProducts.put(productBOM.getBOMProduct(), totalQty.subtract(stock));
          createRecurrencyBOM(productBOM.getBOMProduct(), totalQty.subtract(stock), organization,
              date, order);
        }
      }
    }
    if (!bomProducts.isEmpty()) {
      createAndProcess(bomProducts, organization, date, order);
    }
  }

  private void createAndProcess(Map<Product, BigDecimal> bomProducts, Organization organization,
      Date date, Order order) throws OBException {
    ProductionTransaction production = createProduction(organization, date, order);
    createProductionPlan(production, bomProducts);
    processProduction(production);
  }

  private ProductionTransaction createProduction(Organization organization, Date date,
      Order order) {
    ProductionTransaction productionTransaction = OBProvider.getInstance()
        .get(ProductionTransaction.class);
    productionTransaction.setName(OBMessageUtils.messageBD("OBBOM_created"));
    productionTransaction.setOrganization(organization);
    productionTransaction.setMovementDate(date);
    productionTransaction.setObbomCOrder(order);
    productionTransaction.setSalesTransaction(true);
    productionTransaction.setRecordsCreated(true);
    OBDal.getInstance().save(productionTransaction);
    return productionTransaction;
  }

  private void createProductionPlan(ProductionTransaction productionTransaction,
      Map<Product, BigDecimal> bomProducts) throws OBException {

    List<ProductionPlan> productionPlanArray = new ArrayList<ProductionPlan>();
    long counter = 0;

    for (Entry<Product, BigDecimal> productEntry : bomProducts.entrySet()) {
      Product product = productEntry.getKey();
      BigDecimal qty = productEntry.getValue();

      Locator locator = getManufacturingLocator(product, productionTransaction.getOrganization());

      ProductionPlan productionPlan = OBProvider.getInstance().get(ProductionPlan.class);
      productionPlan.setOrganization(productionTransaction.getOrganization());
      productionPlan.setProduct(product);
      productionPlan.setProductionQuantity(qty);
      productionPlan.setStorageBin(locator);
      productionPlan.setProduction(productionTransaction);
      productionPlan.setLineNo((counter + 1) * 10);
      OBDal.getInstance().save(productionPlan);

      productionPlanArray.add(productionPlan);
      createProductionLines(productionPlan);
      counter++;
    }
    productionTransaction.getMaterialMgmtProductionPlanList().addAll(productionPlanArray);
    OBDal.getInstance().save(productionTransaction);
  }

  private void createProductionLines(ProductionPlan productionPlan) throws OBException {

    long counter = 1;
    Product product = productionPlan.getProduct();
    BigDecimal qty = productionPlan.getProductionQuantity();
    Locator locator = productionPlan.getStorageBin();
    List<ProductionLine> productionLineArray = new ArrayList<ProductionLine>();

    ProductionLine productionLine = OBProvider.getInstance().get(ProductionLine.class);
    productionLine.setOrganization(productionPlan.getOrganization());
    productionLine.setProduct(product);
    productionLine.setMovementQuantity(qty);
    productionLine.setUOM(product.getUOM());
    productionLine.setStorageBin(locator);
    productionLine.setLineNo(new Long(10));
    productionLine.setProductionPlan(productionPlan);
    productionLine.setAttributeSetValue(product.getAttributeSetValue());
    OBDal.getInstance().save(productionLine);
    productionLineArray.add(productionLine);

    List<ProductBOM> pBOMList = product.getProductBOMList();

    for (ProductBOM productBOM : pBOMList) {

      BigDecimal pendingQty = qty.multiply(productBOM.getBOMQuantity());

      ScrollableResults scrollableStock = getStockProposed(productBOM,
          productionPlan.getOrganization());
      try {
        while (pendingQty.compareTo(BigDecimal.ZERO) > 0 && scrollableStock.next()) {

          StockProposed stock = (StockProposed) scrollableStock.get(0);
          productionLine = OBProvider.getInstance().get(ProductionLine.class);
          productionLine.setOrganization(productionPlan.getOrganization());
          productionLine.setProduct(productBOM.getBOMProduct());
          if (stock.getQuantity().compareTo(pendingQty) == -1) {
            productionLine.setMovementQuantity(stock.getQuantity().negate());
            pendingQty = pendingQty.subtract(stock.getQuantity());
          } else {
            productionLine.setMovementQuantity(pendingQty.negate());
            pendingQty = BigDecimal.ZERO;
          }
          productionLine.setUOM(productBOM.getBOMProduct().getUOM());
          productionLine.setStorageBin(stock.getStorageDetail().getStorageBin());
          productionLine.setLineNo((counter + 1) * 10);
          productionLine.setProductionPlan(productionPlan);
          productionLine.setAttributeSetValue(stock.getStorageDetail().getAttributeSetValue());
          OBDal.getInstance().save(productionLine);
          productionLineArray.add(productionLine);
          counter++;
        }

      } finally {
        scrollableStock.close();
      }

      String preference = "N";
      try {
        preference = Preferences.getPreferenceValue("OBBOM_allow_negative_raw_materials", true,
            OBContext.getOBContext().getCurrentClient(), null, null, null, null);
      } catch (PropertyException e) {
        // It is not necessary to do anything
      }
      if (pendingQty.compareTo(BigDecimal.ZERO) != 0 && "Y".equals(preference)) {
        productionLine = OBProvider.getInstance().get(ProductionLine.class);
        productionLine.setOrganization(productionPlan.getOrganization());
        productionLine.setProduct(productBOM.getBOMProduct());
        productionLine.setMovementQuantity(pendingQty.negate());
        pendingQty = BigDecimal.ZERO;
        productionLine.setUOM(productBOM.getBOMProduct().getUOM());
        productionLine.setStorageBin(getBestLocator(productionPlan));
        productionLine.setLineNo((counter + 1) * 10);
        productionLine.setProductionPlan(productionPlan);
        productionLine.setAttributeSetValue(productBOM.getBOMProduct().getAttributeSetValue());
        OBDal.getInstance().save(productionLine);
        productionLineArray.add(productionLine);
        counter++;
        pendingQty = BigDecimal.ZERO;
      }

      if (pendingQty.compareTo(BigDecimal.ZERO) != 0
          && !productBOM.getBOMProduct().isBillOfMaterials()
          && !productBOM.getBOMProduct().isObbomAutogeneratebom()
          && productBOM.getBOMProduct().getProductType().equals("I")
          && productBOM.getBOMProduct().isStocked()) {
        throw new OBException(OBMessageUtils.messageBD("NotEnoughStocked") + " "
            + productBOM.getBOMProduct().getName() + " " + pendingQty);
      }

    }
    productionPlan.getManufacturingProductionLineList().addAll(productionLineArray);
    OBDal.getInstance().save(productionPlan);
  }

  private Locator getBestLocator(ProductionPlan productionPlan) {
    OrgWarehouse orgWarehouse = null;
    Locator locator = null;
    List<OrgWarehouse> warehouseList = productionPlan.getOrganization()
        .getOrganizationWarehouseList();
    if (warehouseList.isEmpty()) {
      return productionPlan.getStorageBin();
    } else {
      for (OrgWarehouse orgWare : warehouseList) {
        if (orgWarehouse == null) {
          orgWarehouse = orgWare;
        } else if (orgWare.getPriority() < orgWarehouse.getPriority())
          orgWarehouse = orgWare;
      }
    }
    List<Locator> locatorList = orgWarehouse.getWarehouse().getLocatorList();
    if (locatorList.isEmpty()) {
      return productionPlan.getStorageBin();
    } else {
      for (Locator locaIterator : locatorList) {
        if (locator == null) {
          locator = locaIterator;
        } else if (locaIterator.getRelativePriority() < locator.getRelativePriority())
          locator = locaIterator;
      }
    }
    return locator;
  }

  private void processProduction(ProductionTransaction productionTransaction) {

    for (ProductionPlan productionPlan : productionTransaction
        .getMaterialMgmtProductionPlanList()) {
      for (ProductionLine productionLine : productionPlan.getManufacturingProductionLineList()) {
        if (productionLine.getProduct().getProductType().equals("I")
            && productionLine.getProduct().isStocked()) {
          MaterialTransaction materialTransaction = OBProvider.getInstance()
              .get(MaterialTransaction.class);
          materialTransaction.setOrganization(productionLine.getOrganization());
          materialTransaction.setProductionLine(productionLine);
          materialTransaction.setStorageBin(productionLine.getStorageBin());
          materialTransaction.setMovementType("P+");
          materialTransaction.setProduct(productionLine.getProduct());
          materialTransaction.setMovementDate(productionTransaction.getMovementDate());
          materialTransaction.setMovementQuantity(productionLine.getMovementQuantity());
          materialTransaction.setUOM(productionLine.getUOM());
          materialTransaction.setOrderUOM(productionLine.getOrderUOM());
          materialTransaction.setOrderQuantity(productionLine.getOrderQuantity());
          materialTransaction.setAttributeSetValue(productionLine.getAttributeSetValue());
          OBDal.getInstance().save(materialTransaction);
          materialTransactionList.add(materialTransaction);
        }
      }
    }

    productionTransaction.setProcessed(true);
    OBDal.getInstance().save(productionTransaction);
    // addProductionList(productionTransaction.getId());
  }

  private ScrollableResults getStockProposed(ProductBOM productBOM, Organization organization)
      throws OBException {

    // The M_GetStock function is used

    String id = "";

    String processId = SequenceIdData.getUUID();
    String recordId = SequenceIdData.getUUID();
    try {
      StockUtils.getStock(processId, recordId, productBOM.getBOMQuantity(),
          productBOM.getBOMProduct().getId(), null, null, null, organization.getId(), null,
          OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(), null,
          productBOM.getBOMProduct().getUOM().getId(), null, null, null, null, null, null, "N");
      id = processId;
    } catch (Exception ex) {
      throw new OBException(
          "Error in AutoBOM when getting stock for product " + productBOM.getBOMProduct().getId(),
          ex);
    }

    OBCriteria<StockProposed> stockProposed = OBDal.getInstance()
        .createCriteria(StockProposed.class);
    stockProposed.add(Restrictions.eq(StockProposed.PROPERTY_PROCESSINSTANCE, id));
    stockProposed.addOrderBy(StockProposed.PROPERTY_PRIORITY, true);

    ScrollableResults scrollableStock = stockProposed.scroll(ScrollMode.FORWARD_ONLY);

    return scrollableStock;
  }

  private Locator getManufacturingLocator(Product product, Organization organization)
      throws OBException {

    StringBuffer select = new StringBuffer();
    select.append(" select po.storageBin as locator");
    select.append(" from " + ProductOrg.ENTITY_NAME + " as po");
    select.append(" where po." + ProductOrg.PROPERTY_PRODUCT + ".id = :product");
    select.append(" and po." + ProductOrg.PROPERTY_ORGANIZATION + ".id = :organization");

    Query locatorQry = OBDal.getInstance().getSession().createQuery(select.toString());
    locatorQry.setParameter("product", product.getId());
    locatorQry.setParameter("organization", organization.getId());
    Object locator = locatorQry.uniqueResult();

    if (locator == null) {
      throw new OBException(OBMessageUtils.messageBD("ProductionPlanLocatorNeeded"));
    }

    return (Locator) locator;
  }

  private BigDecimal getStock(ProductBOM productBOM, Organization organization) {
    List<OrgWarehouse> orderOrgWHList = organization.getOrganizationWarehouseList();

    StringBuffer selectSD = new StringBuffer();
    selectSD
        .append(" select sum(sd." + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY + ") as stock");
    selectSD.append(" from " + MaterialTransaction.ENTITY_NAME + " as sd");
    selectSD.append(" where sd." + MaterialTransaction.PROPERTY_PRODUCT + ".id = :product");
    selectSD.append(" and sd." + MaterialTransaction.PROPERTY_STORAGEBIN + "."
        + Locator.PROPERTY_WAREHOUSE + ".id in (:warehouse)");

    if (materialTransactionList.size() > 0) {
      selectSD.append(" and sd." + MaterialTransaction.PROPERTY_ID + " not in ("
          + Utility.getInStrList(materialTransactionList) + ")");
    }

    Query stockQrySD = OBDal.getInstance().getSession().createQuery(selectSD.toString());
    stockQrySD.setParameter("product", productBOM.getBOMProduct().getId());
    List<String> warehouseIds = new ArrayList<String>();
    for (OrgWarehouse orgWarehouse : orderOrgWHList) {
      warehouseIds.add(orgWarehouse.getWarehouse().getId());
    }
    stockQrySD.setParameterList("warehouse", warehouseIds);
    //BigDecimal stock = (BigDecimal) stockQrySD.uniqueResult();
    BigDecimal stock = (BigDecimal) stockQrySD.uniqueResult() == null ? BigDecimal.ZERO
        : (BigDecimal) stockQrySD.uniqueResult();

    //if (stock == null) {
    //  throw new OBException("Error in AutoBOM when getting stock for product "
    //      + productBOM.getBOMProduct().getName());
    //}

    return stock;

  }

  private BigDecimal getStockByWarehouse(ProductBOM productBOM, Warehouse warehouse) {

    StringBuffer selectSD = new StringBuffer();
    selectSD.append(" select sum(sd." + StorageDetail.PROPERTY_QUANTITYONHAND + " - sd."
        + StorageDetail.PROPERTY_RESERVEDQTY + ") as stock");
    selectSD.append(" from " + StorageDetail.ENTITY_NAME + " as sd");
    selectSD.append(" where sd." + StorageDetail.PROPERTY_PRODUCT + ".id = :product");
    selectSD.append(" and sd." + StorageDetail.PROPERTY_STORAGEBIN + "."
        + Locator.PROPERTY_WAREHOUSE + ".id = :warehouse");

    Query stockQrySD = OBDal.getInstance().getSession().createQuery(selectSD.toString());
    stockQrySD.setParameter("product", productBOM.getBOMProduct().getId());
    stockQrySD.setParameter("warehouse", warehouse.getId());

    BigDecimal stock = (BigDecimal) stockQrySD.uniqueResult();

    stock = (stock == null ? BigDecimal.ZERO : stock);
    return stock;

  }

}
