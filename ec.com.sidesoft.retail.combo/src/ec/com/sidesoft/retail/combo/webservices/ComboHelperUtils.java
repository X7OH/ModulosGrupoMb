package ec.com.sidesoft.retail.combo.webservices;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jfree.util.Log;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationTree;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.retail.discounts.combo.ComboProductFamily;

import ec.com.sidesoft.quickbilling.advanced.SaqbCallcenterinvoiceConf;
import ec.com.sidesoft.quickbilling.advanced.SaqbOrder;
import ec.com.sidesoft.quickbilling.advanced.SaqbOrderline;

public class ComboHelperUtils {

  public static BigDecimal getStock(String productId, String callCenterOrderId) {

    BigDecimal stock = BigDecimal.ZERO;

    Product product = OBDal.getInstance().get(Product.class, productId);

    SaqbOrder SaqbOrder = OBDal.getInstance().get(SaqbOrder.class, callCenterOrderId);

    OBCriteria<SaqbCallcenterinvoiceConf> callCenterInvConfigurationCriteria = OBDal.getInstance()
        .createCriteria(SaqbCallcenterinvoiceConf.class);
    callCenterInvConfigurationCriteria.add(Restrictions.eq(
        SaqbCallcenterinvoiceConf.PROPERTY_ORGANIZATION, SaqbOrder.getOrgRegion()));

    SaqbCallcenterinvoiceConf callCenterInvConfiguration = callCenterInvConfigurationCriteria
        .list().get(0);

    Warehouse warehouse = callCenterInvConfiguration.getWarehouse();

    OBCriteria<StorageDetail> storageDetailCriteria = OBDal.getInstance().createCriteria(
        StorageDetail.class);

    storageDetailCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, product));
    storageDetailCriteria.add(Restrictions.in(StorageDetail.PROPERTY_STORAGEBIN,
        warehouse.getLocatorList()));
    List<StorageDetail> storageDetailList = storageDetailCriteria.list();

    for (StorageDetail storageDetail : storageDetailList) {
      stock = stock.add(storageDetail.getQuantityOnHand());
    }

    return stock;
  }

  public static String getProductCategory(String productId) {
    Product product = OBDal.getInstance().get(Product.class, productId);
    return product.getProductCategory().getIdentifier();
  }

  public static BigDecimal getProductGrossPrice(String productId, String callCenterOrderId,
      TaxRate tax) {
    SaqbOrder saqbOrder = OBDal.getInstance().get(SaqbOrder.class, callCenterOrderId);
    Product product = OBDal.getInstance().get(Product.class, productId);

    PriceList priceListToUse = getPriceListFromCallCenterConfiguration(saqbOrder.getOrgRegion());

    List<ProductPrice> productPriceList = product.getPricingProductPriceList();
    if (productPriceList.size() > 0) {

      for (ProductPrice productPrice : productPriceList) {
        if (productPrice.getPriceListVersion().getPriceList().getId()
            .equals(priceListToUse.getId())) {
          return productPrice.getStandardPrice();
        }
      }
      return BigDecimal.ZERO;
    } else {
      return BigDecimal.ZERO;
    }

  }

  public static BigDecimal getProductPrice(String productId, String callCenterOrderId, TaxRate tax) {

    SaqbOrder saqbOrder = OBDal.getInstance().get(SaqbOrder.class, callCenterOrderId);
    Product product = OBDal.getInstance().get(Product.class, productId);

    PriceList priceListToUse = getPriceListFromCallCenterConfiguration(saqbOrder.getOrgRegion());

    List<ProductPrice> productPriceList = product.getPricingProductPriceList();
    if (productPriceList.size() > 0) {

      for (ProductPrice productPrice : productPriceList) {
        if (productPrice.getPriceListVersion().getPriceList().getId()
            .equals(priceListToUse.getId())) {
          if (priceListToUse.isPriceIncludesTax()) {
            // return productPrice.getStandardPrice().divide(
            // tax.getRate().divide(new BigDecimal(100)).add(new BigDecimal(1)),
            BigDecimal taxBaseAmt = productPrice.getStandardPrice().multiply(
                tax.getRate().divide(new BigDecimal(100)));

            taxBaseAmt = taxBaseAmt.setScale(priceListToUse.getCurrency().getPricePrecision()
                .intValue(), BigDecimal.ROUND_HALF_UP);
            BigDecimal netAmount = FinancialUtils.calculateNetAmtFromGross(tax.getId(),
                productPrice.getStandardPrice(), priceListToUse.getCurrency()
                    .getStandardPrecision().intValue(), taxBaseAmt);

            BigDecimal priceActual = netAmount.divide(new BigDecimal(1), priceListToUse
                .getCurrency().getPricePrecision().intValue(), RoundingMode.HALF_UP);
            return priceActual;
          } else {
            return productPrice.getStandardPrice();
          }
        }
      }
      return BigDecimal.ZERO;
    } else {
      return BigDecimal.ZERO;
    }
  }

  public static boolean isIncludetax(String productId, String callCenterOrderId) {

    SaqbOrder saqbOrder = OBDal.getInstance().get(SaqbOrder.class, callCenterOrderId);
    Product product = OBDal.getInstance().get(Product.class, productId);

    PriceList priceListToUse = getPriceListFromCallCenterConfiguration(saqbOrder.getOrgRegion());

    List<ProductPrice> productPriceList = product.getPricingProductPriceList();
    if (productPriceList.size() > 0) {

      for (ProductPrice productPrice : productPriceList) {
        if (productPrice.getPriceListVersion().getPriceList().getId()
            .equals(priceListToUse.getId())) {
          return priceListToUse.isPriceIncludesTax();
        }
      }

    }
    return true;
  }

  public static JSONObject getProductPrice2(String productId, String callCenterOrderId,
      TaxRate tax, BigDecimal orderedQuantity) throws JSONException {
    JSONObject result = new JSONObject();
    SaqbOrder saqbOrder = OBDal.getInstance().get(SaqbOrder.class, callCenterOrderId);
    Product product = OBDal.getInstance().get(Product.class, productId);

    PriceList priceListToUse = getPriceListFromCallCenterConfiguration(saqbOrder.getOrgRegion());

    List<ProductPrice> productPriceList = product.getPricingProductPriceList();
    if (productPriceList.size() > 0) {

      for (ProductPrice productPrice : productPriceList) {
        if (productPrice.getPriceListVersion().getPriceList().getId()
            .equals(priceListToUse.getId())) {
          BigDecimal bdOrderedQuantityInt = new BigDecimal(orderedQuantity.toBigInteger());
          result.put("newqty", (bdOrderedQuantityInt == null ? BigDecimal.ZERO
              : bdOrderedQuantityInt));
          if (priceListToUse.isPriceIncludesTax()) {

            // TOTAL
            BigDecimal bdTotalLines = productPrice
                .getStandardPrice()
                .multiply(bdOrderedQuantityInt)
                .setScale(priceListToUse.getCurrency().getStandardPrecision().intValue(),
                    RoundingMode.HALF_UP);
            result.put("total", (bdTotalLines == null ? BigDecimal.ZERO : bdTotalLines));

            // SUBTOTAL

            BigDecimal bdSubtotal = bdTotalLines.divide(
                new BigDecimal(1).add(tax.getRate().divide(new BigDecimal(100))), priceListToUse
                    .getCurrency().getStandardPrecision().intValue(), RoundingMode.HALF_UP);

            result.put("subtotal", (bdSubtotal == null ? BigDecimal.ZERO : bdSubtotal));

            // PRECIO SIN IMPUESTOS
            BigDecimal bdPrecioSinImpuestos = bdSubtotal.divide(bdOrderedQuantityInt,
                priceListToUse.getCurrency().getPricePrecision().intValue(), RoundingMode.HALF_UP);
            result.put("pricewithouttax", (bdPrecioSinImpuestos == null ? BigDecimal.ZERO
                : bdPrecioSinImpuestos));
            result.put("pricewithtax", productPrice.getStandardPrice());

            return result;

          } else {

            // SUBTOTAL
            BigDecimal bdSubtotal = bdOrderedQuantityInt.multiply(productPrice.getStandardPrice())
                .setScale(priceListToUse.getCurrency().getStandardPrecision().intValue(),
                    RoundingMode.HALF_UP);
            result.put("subtotal", (bdSubtotal == null ? BigDecimal.ZERO : bdSubtotal));

            // TOTAL
            BigDecimal bdTotalLines = bdSubtotal.multiply(
                new BigDecimal(1).add(tax.getRate().divide(new BigDecimal(100))).setScale(
                    priceListToUse.getCurrency().getStandardPrecision().intValue(),
                    RoundingMode.HALF_UP)).setScale(
                priceListToUse.getCurrency().getStandardPrecision().intValue(),
                RoundingMode.HALF_UP);
            result.put("total", (bdTotalLines == null ? BigDecimal.ZERO : bdTotalLines));

            // PRECIO CON IMPUESTOS
            BigDecimal bdpricewithtax = bdTotalLines.divide(bdOrderedQuantityInt, priceListToUse
                .getCurrency().getPricePrecision().intValue(), RoundingMode.HALF_UP);

            /*
             * BigDecimal bdpricewithtax = bdOrderedQuantityInt.multiply(new BigDecimal(1).add(
             * tax.getRate().divide(new BigDecimal(100))).setScale(
             * priceListToUse.getCurrency().getStandardPrecision().intValue(),
             * RoundingMode.HALF_UP));
             */
            result.put("pricewithtax", (bdpricewithtax == null ? BigDecimal.ZERO : bdpricewithtax));
            result.put("pricewithouttax", productPrice.getStandardPrice());

            return result;
          }
        }
      }
      result.put("newqty", BigDecimal.ZERO);
      result.put("pricewithouttax", BigDecimal.ZERO);
      result.put("pricewithtax", BigDecimal.ZERO);
      result.put("subtotal", BigDecimal.ZERO);
      result.put("total", BigDecimal.ZERO);
    } else {
      result.put("newqty", BigDecimal.ZERO);
      result.put("pricewithouttax", BigDecimal.ZERO);
      result.put("pricewithtax", BigDecimal.ZERO);
      result.put("subtotal", BigDecimal.ZERO);
      result.put("total", BigDecimal.ZERO);
    }
    return result;
  }

  private static PriceList getPriceListFromCallCenterConfiguration(Organization orgRegion) {
    SaqbCallcenterinvoiceConf saqbCallCenterConnf = null;

    try {
      OBContext.setAdminMode(false);

      OBCriteria<SaqbCallcenterinvoiceConf> saqbCallCenterConnfCriteria = OBDal.getInstance()
          .createCriteria(SaqbCallcenterinvoiceConf.class);
      saqbCallCenterConnfCriteria.add(Restrictions.eq(
          SaqbCallcenterinvoiceConf.PROPERTY_ORGANIZATION, orgRegion));
      saqbCallCenterConnfCriteria.setMaxResults(1);
      saqbCallCenterConnf = (SaqbCallcenterinvoiceConf) saqbCallCenterConnfCriteria.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
    return saqbCallCenterConnf.getPriceList();
  }

  private static PriceList getPriceListFromOrg(Organization orgRegion) {

    if (orgRegion.getObretcoPricelist() != null) {
      return orgRegion.getObretcoPricelist();
    } else {
      return getPriceListFromOrg(getParentOfOrg(orgRegion));
    }
  }

  private static Organization getParentOfOrg(Organization orgRegion) {

    OrganizationTree orgTree;
    try {
      OBContext.setAdminMode(false);
      OBCriteria<OrganizationTree> orgTreeCriteria = OBDal.getInstance().createCriteria(
          OrganizationTree.class);
      orgTreeCriteria.add(Restrictions.eq(OrganizationTree.PROPERTY_ORGANIZATION, orgRegion));
      orgTreeCriteria.add(Restrictions.gt(OrganizationTree.PROPERTY_LEVELNO, 1L));
      orgTreeCriteria.addOrder(Order.asc(OrganizationTree.PROPERTY_LEVELNO));
      orgTreeCriteria.setMaxResults(1);
      orgTree = (OrganizationTree) orgTreeCriteria.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
    return orgTree.getParentOrganization();
  }

  public static TaxRate getProductTax(String productId) {
    Product product = OBDal.getInstance().get(Product.class, productId);
    return product.getTaxCategory().getFinancialMgmtTaxRateList().get(0);
  }

  public static Product getProduct(String productId) {
    return OBDal.getInstance().get(Product.class, productId);
  }

  public static SaqbOrder getSaqbOrder(String callCenterOrderId) {
    return OBDal.getInstance().get(SaqbOrder.class, callCenterOrderId);
  }

  public static void deleteLineFromOrder(String selecteLineId, SaqbOrder callCenterOrder) {

    if (selecteLineId != null && !selecteLineId.equals("") && !selecteLineId.equals("null")) {
      SaqbOrderline saqbOrderline = OBDal.getInstance().get(SaqbOrderline.class, selecteLineId);
      List<SaqbOrderline> saqbOrderlines = new ArrayList<SaqbOrderline>();

      if (saqbOrderline.getCombo() != null) {
        saqbOrderlines.addAll(getLinesWithSameCombo(callCenterOrder, saqbOrderline.getCombo()));
      } else {
        saqbOrderlines.add(saqbOrderline);
      }

      for (SaqbOrderline orderline : saqbOrderlines) {
        callCenterOrder.getSaqbOrderlineList().remove(orderline);
        OBDal.getInstance().remove(orderline);
      }
    }
    OBDal.getInstance().flush();
  }

  private static ArrayList<SaqbOrderline> getLinesWithSameCombo(SaqbOrder saqbOrder,
      PriceAdjustment combo) {

    ArrayList<SaqbOrderline> lines = new ArrayList<SaqbOrderline>();

    OBCriteria<SaqbOrderline> saqbOrderlineCriteria = OBDal.getInstance().createCriteria(
        SaqbOrderline.class);
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_SAQBORDER, saqbOrder));
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_COMBO, combo));

    List<SaqbOrderline> saqbOrderlines = saqbOrderlineCriteria.list();

    if (saqbOrderlines.size() > 0) {
      for (SaqbOrderline saqbOrderline : saqbOrderlines) {
        lines.add(saqbOrderline);
      }
    }
    return lines;
  }

  public static Long getNextLineNo(List<SaqbOrderline> saqbOrderlineList) {
    Long lineNo = 10L;

    for (SaqbOrderline saqbOrderline : saqbOrderlineList) {
      if (saqbOrderline.getLineNo() >= lineNo) {
        lineNo = saqbOrderline.getLineNo() + 10L;
      }
    }

    return lineNo;
  }

  public static PriceAdjustment getComboFromFamily(Iterator keys) {
    while (keys.hasNext()) {
      String key = (String) keys.next();
      ComboProductFamily family = OBDal.getInstance().get(ComboProductFamily.class, key);
      return family.getPriceAdjustment();
    }
    return null;
  }

  public static void createLine(SaqbOrder saqbOrder, PriceAdjustment combo, int comboQty,
      JSONObject product, BigDecimal alreadyComboQty1) {

    Product productInstance = null;
    String strDescription = "";
    try {
      productInstance = OBDal.getInstance().get(Product.class, product.getString("id"));

      TaxRate taxRate = ComboHelperUtils.getProductTax(product.getString("id"));
      SaqbOrderline saqbOrderline = getLineWithSameProductAndCombo(saqbOrder, product, combo);

      int alreadyComboQty = Integer.parseInt(alreadyComboQty1.toString());
      // comboQty = bdcomboQty.add(alreadyComboQty);
      comboQty += alreadyComboQty;
      BigDecimal bdcomboQty = new BigDecimal(comboQty);
      strDescription = (product.optString("description") != null ? product.optString("description")
          : "");
      if (saqbOrderline == null) {
        saqbOrderline = new SaqbOrderline();

        saqbOrderline.setLineNo(ComboHelperUtils.getNextLineNo(saqbOrder.getSaqbOrderlineList()));

        saqbOrderline.setProduct(productInstance);
        saqbOrderline.setTax(taxRate);

        saqbOrderline.setSubtotal(taxRate
            .getRate()
            .divide(new BigDecimal(100))
            .add(new BigDecimal(1))
            .multiply(BigDecimal.valueOf(product.getInt("insertedQty")))
            .multiply(
                ComboHelperUtils.getProductPrice(product.getString("id"), saqbOrder.getId(),
                    ComboHelperUtils.getProductTax(product.getString("id")))));

        saqbOrderline.setDescription(strDescription);
        saqbOrderline
            .setStock(ComboHelperUtils.getStock(product.getString("id"), saqbOrder.getId()));
        saqbOrderline.setSaqbOrder(saqbOrder);
        saqbOrderline.setNetUnitPrice(ComboHelperUtils.getProductPrice(product.getString("id"),
            saqbOrder.getId(), ComboHelperUtils.getProductTax(product.getString("id"))));
        saqbOrderline.setCombo(combo);

        saqbOrderline.setComboQty(bdcomboQty);
        saqbOrderline.setOrderedQuantity(BigDecimal.valueOf(product.getInt("insertedQty")));

        saqbOrderline.setLineNetAmount(saqbOrderline.getOrderedQuantity().multiply(
            ComboHelperUtils.getProductPrice(product.getString("id"), saqbOrder.getId(),
                ComboHelperUtils.getProductTax(product.getString("id")))));
        saqbOrderline.setOrganization(saqbOrder.getOrgRegion());
        saqbOrderline.setDefault(false);
      } else {
        saqbOrderline.setTax(taxRate);
        saqbOrderline.setSaqbOrder(saqbOrder);
        saqbOrderline.setDescription(strDescription);

        saqbOrderline.setSubtotal(taxRate
            .getRate()
            .divide(new BigDecimal(100))
            .add(new BigDecimal(1))
            .multiply(BigDecimal.valueOf(product.getInt("insertedQty")))
            .multiply(
                ComboHelperUtils.getProductPrice(product.getString("id"), saqbOrder.getId(),
                    ComboHelperUtils.getProductTax(product.getString("id")))));

        saqbOrderline
            .setStock(ComboHelperUtils.getStock(product.getString("id"), saqbOrder.getId()));
        saqbOrderline.setOrderedQuantity(saqbOrderline.getOrderedQuantity().add(
            BigDecimal.valueOf(product.getInt("insertedQty"))));

        saqbOrderline.setComboQty(saqbOrderline.getComboQty().add(bdcomboQty));
        saqbOrderline.setLineNetAmount(saqbOrderline.getOrderedQuantity().multiply(
            saqbOrderline.getNetUnitPrice()));
        saqbOrderline.setOrganization(saqbOrder.getOrgRegion());
        saqbOrderline.setDefault(false);
      }
      OBDal.getInstance().save(saqbOrderline);
    } catch (JSONException e) {
      Log.error(e.getMessage());
      e.printStackTrace();
    }

    ComboHelperUtils.updateLines(saqbOrder, combo, comboQty, strDescription);

  }

  public static void createLine2(SaqbOrder saqbOrder, PriceAdjustment combo, int comboQty,
      JSONObject product, BigDecimal alreadyComboQty1, Long numberline) {

    Product productInstance = null;
    String strDescription = "";
    try {
      productInstance = OBDal.getInstance().get(Product.class, product.getString("id"));

      TaxRate taxRate = ComboHelperUtils.getProductTax(product.getString("id"));
      SaqbOrderline saqbOrderline = getLineWithSameProductAndCombo(saqbOrder, product, combo);

      int alreadyComboQty = Integer.parseInt(alreadyComboQty1.toString());
      // comboQty = bdcomboQty.add(alreadyComboQty);
      comboQty += alreadyComboQty;
      BigDecimal bdcomboQty = new BigDecimal(comboQty);
      strDescription = (product.optString("description") == null
          || product.optString("description").equals("null") ? "" : product
          .optString("description"));
      if (saqbOrderline == null) {
        saqbOrderline = new SaqbOrderline();

        saqbOrderline.setLineNo(numberline);
        saqbOrderline.setProduct(productInstance);
        saqbOrderline.setTax(taxRate);

        saqbOrderline.setDescription(strDescription);
        saqbOrderline
            .setStock(ComboHelperUtils.getStock(product.getString("id"), saqbOrder.getId()));
        saqbOrderline.setSaqbOrder(saqbOrder);

        saqbOrderline.setCombo(combo);

        saqbOrderline.setComboQty(bdcomboQty);
        saqbOrderline.setOrderedQuantity(BigDecimal.valueOf(product.getInt("insertedQty")));

        JSONObject result = ComboHelperUtils.getProductPrice2(product.getString("id"),
            saqbOrder.getId(), taxRate, BigDecimal.valueOf(product.getInt("insertedQty")));

        saqbOrderline
            .setNetUnitPrice((new BigDecimal(result.getString("pricewithouttax")) == null ? BigDecimal.ZERO
                : new BigDecimal(result.getString("pricewithouttax"))));
        saqbOrderline
            .setGrossUnitPrice((new BigDecimal(result.getString("pricewithtax")) == null ? BigDecimal.ZERO
                : new BigDecimal(result.getString("pricewithtax"))));
        saqbOrderline
            .setSubtotal((new BigDecimal(result.getString("subtotal")) == null ? BigDecimal.ZERO
                : new BigDecimal(result.getString("subtotal"))));
        saqbOrderline
            .setLineNetAmount((new BigDecimal(result.getString("total")) == null ? BigDecimal.ZERO
                : new BigDecimal(result.getString("total"))));

        saqbOrderline.setTaxinclude(ComboHelperUtils.isIncludetax(product.getString("id"),
            saqbOrder.getId()));

        saqbOrderline.setOrganization(saqbOrder.getOrgRegion());
        saqbOrderline.setDefault(false);
      } else {
        saqbOrderline.setTax(taxRate);
        saqbOrderline.setSaqbOrder(saqbOrder);
        saqbOrderline.setDescription(strDescription);

        saqbOrderline
            .setStock(ComboHelperUtils.getStock(product.getString("id"), saqbOrder.getId()));
        saqbOrderline.setOrderedQuantity(saqbOrderline.getOrderedQuantity().add(
            BigDecimal.valueOf(product.getInt("insertedQty"))));

        saqbOrderline.setComboQty(saqbOrderline.getComboQty().add(bdcomboQty));

        JSONObject result = ComboHelperUtils.getProductPrice2(
            product.getString("id"),
            saqbOrder.getId(),
            taxRate,
            saqbOrderline.getOrderedQuantity());

        saqbOrderline
            .setNetUnitPrice((new BigDecimal(result.getString("pricewithouttax")) == null ? BigDecimal.ZERO
                : new BigDecimal(result.getString("pricewithouttax"))));
        saqbOrderline
            .setGrossUnitPrice((new BigDecimal(result.getString("pricewithtax")) == null ? BigDecimal.ZERO
                : new BigDecimal(result.getString("pricewithtax"))));
        saqbOrderline
            .setSubtotal((new BigDecimal(result.getString("subtotal")) == null ? BigDecimal.ZERO
                : new BigDecimal(result.getString("subtotal"))));

        saqbOrderline
            .setLineNetAmount((new BigDecimal(result.getString("total")) == null ? BigDecimal.ZERO
                : new BigDecimal(result.getString("total"))));

        saqbOrderline.setTaxinclude(ComboHelperUtils.isIncludetax(product.getString("id"),
            saqbOrder.getId()));

        saqbOrderline.setOrganization(saqbOrder.getOrgRegion());
        saqbOrderline.setDefault(false);
      }
      OBDal.getInstance().save(saqbOrderline);
    } catch (JSONException e) {
      Log.error(e.getMessage());
      e.printStackTrace();
    }

    ComboHelperUtils.updateLines(saqbOrder, combo, comboQty, strDescription);

  }

  public static BigDecimal getComboQty(SaqbOrder saqbOrder, PriceAdjustment combo) {

    OBCriteria<SaqbOrderline> saqbOrderlineCriteria = OBDal.getInstance().createCriteria(
        SaqbOrderline.class);
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_SAQBORDER, saqbOrder));
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_COMBO, combo));

    List<SaqbOrderline> saqbOrderlines = saqbOrderlineCriteria.list();

    if (saqbOrderlines.size() > 0) {
      return saqbOrderlines.get(0).getComboQty();
    }
    return new BigDecimal(0);
  }

  private static SaqbOrderline getLineWithSameProductAndCombo(SaqbOrder saqbOrder,
      JSONObject product, PriceAdjustment combo) {

    Product productInstance = null;
    try {
      productInstance = OBDal.getInstance().get(Product.class, product.getString("id"));
    } catch (JSONException e) {
      Log.error(e.getMessage());
      e.printStackTrace();
    }

    OBCriteria<SaqbOrderline> saqbOrderlineCriteria = OBDal.getInstance().createCriteria(
        SaqbOrderline.class);
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_SAQBORDER, saqbOrder));
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_COMBO, combo));
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_PRODUCT, productInstance));
    return (SaqbOrderline) saqbOrderlineCriteria.uniqueResult();
  }

  public static void updateLines(SaqbOrder saqbOrder, PriceAdjustment combo, int comboQty,
      String strDescription) {
    OBCriteria<SaqbOrderline> saqbOrderlineCriteria = OBDal.getInstance().createCriteria(
        SaqbOrderline.class);
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_SAQBORDER, saqbOrder));
    saqbOrderlineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_COMBO, combo));

    List<SaqbOrderline> saqbOrderlines = saqbOrderlineCriteria.list();

    if (saqbOrderlines.size() > 0) {
      for (SaqbOrderline saqbOrderline : saqbOrderlines) {

        BigDecimal bdcomboQty = new BigDecimal(comboQty);
        saqbOrderline.setComboQty(bdcomboQty);
        saqbOrderline.setDescription(strDescription);
      }
    }
    // OBDal.getInstance().flush();
  }
}
