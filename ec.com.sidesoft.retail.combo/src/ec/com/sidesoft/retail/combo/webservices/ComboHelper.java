package ec.com.sidesoft.retail.combo.webservices;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.retail.config.OBRETCOProlProduct;
import org.openbravo.retail.discounts.combo.ComboProduct;
import org.openbravo.retail.discounts.combo.ComboProductFamily;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.quickbilling.advanced.SaqbCallcenterinvoiceConf;
import ec.com.sidesoft.quickbilling.advanced.SaqbDefaultproduct;
import ec.com.sidesoft.quickbilling.advanced.SaqbOrder;
import ec.com.sidesoft.quickbilling.advanced.SaqbOrderline;

public class ComboHelper extends BaseActionHandler {
  private static Logger log = Logger.getLogger(ComboHelper.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    String action = (String) parameters.get("action");

    try {
      switch (action) {
      case "getProductPriceAndStock":
        return getProductPriceAndStock((String) parameters.get("productId"),
            (String) parameters.get("saqbOrderID"));
      case "insertDefaultProduct":
        return insertDefaultProduct((String) parameters.get("saqbOrderID"));
      case "insertProductLine":
        return insertProductLine2((String) parameters.get("productId"),
            (String) parameters.get("saqbOrderID"),
            (BigDecimal) new BigDecimal((String) parameters.get("orderedQuantity")),
            (String) parameters.get("description"), (String) parameters.get("orderLineId"),
            (BigDecimal) new BigDecimal((String) parameters.get("grossUnitPrice")),
            (BigDecimal) new BigDecimal((String) parameters.get("netUnitPrice")),
            (BigDecimal) new BigDecimal((String) parameters.get("subtotal")),
            (BigDecimal) new BigDecimal((String) parameters.get("lineNetAmount")),
            (String) parameters.get("istaxinclude"));

      case "insertComboLine":
        return insertComboLine((String) parameters.get("comboConf"),
            (String) parameters.get("saqbOrderID"), (String) parameters.get("comboLines"));
      case "deleteLine":
        return deleteLine((String) parameters.get("selecteLineId"),
            (String) parameters.get("saqbOrderID"));
      case "getProductList":
        return getProductList((String) parameters.get("saqbOrderID"));
      case "getComboList":
        return getComboList((String) parameters.get("saqbOrderID"));
      case "getDiscountDefinition":
        return getDiscountDefinition((String) parameters.get("productId"),
            (String) parameters.get("saqbOrderID"));
      case "getPrices":
        return getPrices((String) parameters.get("productId"),
            (String) parameters.get("saqbOrderID"),
            (BigDecimal) new BigDecimal((String) parameters.get("orderedQuantity")));
      }

    } catch (JSONException e) {
      log.error(e.getMessage());
      e.printStackTrace();
    }
    return new JSONObject();
  }

  private JSONObject getDiscountDefinition(String disountId, String saqbOrderID) {

    JSONObject result = new JSONObject();

    OBCriteria<PriceAdjustment> priceAdjustmentCriteria = OBDal.getInstance().createCriteria(
        PriceAdjustment.class);
    priceAdjustmentCriteria.add(Restrictions.eq(PriceAdjustment.PROPERTY_ID, disountId));
    PriceAdjustment priceAdjustment = (PriceAdjustment) priceAdjustmentCriteria.uniqueResult();

    List<ComboProductFamily> families = priceAdjustment.getOBCOMBOFamilyList();
    
    // ORDENAR LISTA
    Collections.sort(families, new Comparator<ComboProductFamily>() {
 				@Override
 				public int compare(ComboProductFamily o1, ComboProductFamily o2) {
 					return o1.getName().compareTo(o2.getName());
 				}
 	});
    
    try {
      for (ComboProductFamily family : families) {
        if (!family.isActive()) {
          continue;
        }
        JSONArray products = new JSONArray();
        List<ComboProduct> productList = family.getOBCOMBOProductList();

        for (ComboProduct comboProduct : productList) {
          if(comboProduct.isSsrcmCallcenter()){
	          JSONObject product = new JSONObject();
	          Product productInstance = comboProduct.getProduct();
	          product.put("id", productInstance.getId());
	          product.put("product", productInstance.getId());
	          product.put("product$_identifier", productInstance.getIdentifier());
	          product.put("productCategory", productInstance.getProductCategory().getId());
	          product.put("stock", ComboHelperUtils.getStock(productInstance.getId(), saqbOrderID));
	
	          product.put(
	              "grossUnitPrice",
	              ComboHelperUtils.getProductPrice2(productInstance.getId(), saqbOrderID,
	                  ComboHelperUtils.getProductTax(productInstance.getId()), new BigDecimal(1))
	                  .getString("pricewithtax"));
	
	          product.put("orderedQuantity", 0);
	          product.put("insertedQty", 0);
	          products.put(product);
          }
        }
        JSONObject familySummary = new JSONObject();
        familySummary.put("name", family.getName());
        familySummary.put("requiredQty", family.getQuantity());
        familySummary.put("newRequiredQty", family.getQuantity());
        familySummary.put("products", products);
        familySummary.put("alreadyFilledQty", 0);

        result.put(family.getId(), familySummary);
      }
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return result;
  }

  private JSONObject getProductList(String saqbOrderID) {
    JSONObject productList = new JSONObject();

    if (saqbOrderID == null || saqbOrderID.equals("")) {
      return productList;
    }
    SaqbOrder saqbOrder = ComboHelperUtils.getSaqbOrder(saqbOrderID);

    // COMPROBACIÓN CONFIGURACIÓN ACTIVA
    OBCriteria<SaqbCallcenterinvoiceConf> objSaqbcallconf = OBDal.getInstance().createCriteria(
        SaqbCallcenterinvoiceConf.class);
    objSaqbcallconf.add(Restrictions.eq(OBRETCOProlProduct.PROPERTY_ORGANIZATION,
        saqbOrder.getOrgRegion()));
    objSaqbcallconf.add(Restrictions.eq(OBRETCOProlProduct.PROPERTY_ACTIVE, true));

    if (objSaqbcallconf.list().size() == 0) {
      throw new OBException(
          "No existe configuración activa para la organización seleccionada en la cabecera.");
    }

    if (saqbOrder.getOrgRegion().getObretcoProductlist() == null) {
      throw new OBException("No existe un surtido(lista de productos) en la organización.");
    }

    if (saqbOrder.getOrgRegion().getObretcoProductlist() == null) {
      throw new OBException("No existe un surtido(lista de productos) en la organización.");
    }

    OBCriteria<OBRETCOProlProduct> objOBRETCOProlProduct = OBDal.getInstance().createCriteria(
        OBRETCOProlProduct.class);
    objOBRETCOProlProduct.add(Restrictions.eq(OBRETCOProlProduct.PROPERTY_OBRETCOPRODUCTLIST,
        saqbOrder.getOrgRegion().getObretcoProductlist()));
    objOBRETCOProlProduct.add(Restrictions.eq(OBRETCOProlProduct.PROPERTY_ACTIVE, true));
    objOBRETCOProlProduct.addOrderBy(OBRETCOProlProduct.PROPERTY_PRODUCT, true);

    List<OBRETCOProlProduct> lstProducts = objOBRETCOProlProduct.list();
    // ORDENAR LISTA
    Collections.sort(lstProducts, new Comparator<OBRETCOProlProduct>() {
      @Override
      public int compare(OBRETCOProlProduct o1, OBRETCOProlProduct o2) {
        return o1.getProduct().getIdentifier().compareTo(o2.getProduct().getIdentifier());
      }
    });

    if (lstProducts.size() == 0) {
      throw new OBException("No existen items en la lista de productos de la organización.");
    }

    for (OBRETCOProlProduct lstProduct : lstProducts) {
      try {
        if (lstProduct.getProduct().isSaqbHomeDelivery() && lstProduct.getProduct().isActive()) {
          productList.put(lstProduct.getProduct().getId(), lstProduct.getProduct().getIdentifier());
        }
      } catch (JSONException e) {
        log.error(e.getMessage());
        e.printStackTrace();
      }
    }

    return productList;
  }

  private JSONObject getComboList(String saqbOrderID) {
    JSONObject comboList = new JSONObject();

    if (saqbOrderID == null || saqbOrderID.equals("")) {
      return comboList;
    }
    SaqbOrder saqbOrder = ComboHelperUtils.getSaqbOrder(saqbOrderID);

    // COMPROBACIÓN CONFIGURACIÓN ACTIVA
    OBCriteria<SaqbCallcenterinvoiceConf> objSaqbcallconf = OBDal.getInstance().createCriteria(
        SaqbCallcenterinvoiceConf.class);
    objSaqbcallconf.add(Restrictions.eq(OBRETCOProlProduct.PROPERTY_ORGANIZATION,
        saqbOrder.getOrgRegion()));
    objSaqbcallconf.add(Restrictions.eq(OBRETCOProlProduct.PROPERTY_ACTIVE, true));

    if (objSaqbcallconf.list().size() == 0) {
      throw new OBException(
          "No existe configuración activa para la organización seleccionada en la cabecera.");
    }

    List<PriceAdjustment> priceAdjustmentList = getPriceAdjustmentByOrg(saqbOrder.getOrgRegion()
        .getId());

    for (PriceAdjustment priceAdjustment : priceAdjustmentList) {
      try {
        comboList.put(priceAdjustment.getId(), priceAdjustment.getPrintName());
      } catch (JSONException e) {
        log.error(e.getMessage());
        e.printStackTrace();
      }
    }

    return comboList;
  }

  private JSONObject deleteLine(String selecteLineId, String callCenterOrderId) {

    ComboHelperUtils.deleteLineFromOrder(selecteLineId,
        ComboHelperUtils.getSaqbOrder(callCenterOrderId));
    return new JSONObject();
  }

  private JSONObject insertDefaultProduct(String callCenterOrderId) throws JSONException {

    SaqbOrder saqbOrder = ComboHelperUtils.getSaqbOrder(callCenterOrderId);

    if (!saqbOrder.isDefaultproductscharged()) {
      // ELIMINAR TODOS LOS CAMPOS CON CHECK ISDEFAULT ='Y'
      OBCriteria<SaqbOrderline> objsaqbOrderLineCriteria = OBDal.getInstance().createCriteria(
          SaqbOrderline.class);
      objsaqbOrderLineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_SAQBORDER, saqbOrder));
      objsaqbOrderLineCriteria.add(Restrictions.eq(SaqbOrderline.PROPERTY_ISDEFAULT, true));

      List<SaqbOrderline> lstSaqbOrderLine = objsaqbOrderLineCriteria.list();

      if (lstSaqbOrderLine.size() > 0) {
        for (SaqbOrderline lstSaqbOrderLines : lstSaqbOrderLine) {
          saqbOrder.getSaqbOrderlineList().remove(lstSaqbOrderLines);
          OBDal.getInstance().remove(lstSaqbOrderLines);
        }
        OBDal.getInstance().flush();
      }
      // BUSCAR CONFIGURACIÓN CALL CENTER POR ORGANIZACIÓN
      OBCriteria<SaqbCallcenterinvoiceConf> ocjCallCenterConfCriteria = OBDal.getInstance()
          .createCriteria(SaqbCallcenterinvoiceConf.class);
      ocjCallCenterConfCriteria.add(Restrictions.eq(
          SaqbCallcenterinvoiceConf.PROPERTY_ORGANIZATION, saqbOrder.getOrgRegion()));

      List<SaqbCallcenterinvoiceConf> lstCallCenterConf = ocjCallCenterConfCriteria.list();

      if (lstCallCenterConf.size() < 1) {
        throw new OBException("No existe configuración para la organización seleccionada.");
      }
      // BUSCAR PRODUCTOS POR DEFECTO PARA ESA CONFIGURACIÓN
      OBCriteria<SaqbDefaultproduct> objDefaultproductCriteria = OBDal.getInstance()
          .createCriteria(SaqbDefaultproduct.class);
      objDefaultproductCriteria.add(Restrictions.eq(
          SaqbDefaultproduct.PROPERTY_SAQBCALLCENTERINVOICECONF, lstCallCenterConf.get(0)));

      List<SaqbDefaultproduct> lstDefaultproduct = objDefaultproductCriteria.list();
      // AÑADIR PRODUCTOS POR DEFECTO
      Long strNextLine = ComboHelperUtils.getNextLineNo(saqbOrder.getSaqbOrderlineList());
      for (SaqbDefaultproduct defaultproduct : lstDefaultproduct) {

        SaqbOrderline saqbOrderline = new SaqbOrderline();
        Product product = defaultproduct.getProduct();
        String productId = product.getId();
        TaxRate taxRate = ComboHelperUtils.getProductTax(product.getId());

        saqbOrderline.setLineNo(strNextLine);
        strNextLine = strNextLine + 10;
        saqbOrderline.setProduct(product);
        saqbOrderline.setTax(taxRate);

        BigDecimal bdDefaultQuantity = new BigDecimal(defaultproduct.getQuantity());

        saqbOrderline.setStock(ComboHelperUtils.getStock(product.getId(), callCenterOrderId));
        saqbOrderline.setDescription(/* description.equals("null") ? "" : description */"");
        saqbOrderline.setSaqbOrder(saqbOrder);
        JSONObject result = ComboHelperUtils.getProductPrice2(productId, callCenterOrderId,
            taxRate, bdDefaultQuantity);
        saqbOrderline.setOrderedQuantity(bdDefaultQuantity);
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
        saqbOrderline.setTaxinclude(ComboHelperUtils.isIncludetax(productId, callCenterOrderId));
        saqbOrderline.setDefault(true);
        saqbOrderline.setOrganization(saqbOrder.getOrgRegion());

        OBDal.getInstance().save(saqbOrderline);
      }
      // VALORES POR DEFECTO CARGADOS = SI
      saqbOrder.setDefaultproductscharged(true);
      OBDal.getInstance().save(saqbOrder);
    }
    return new JSONObject();

  }

  private JSONObject insertProductLine(String productId, String callCenterOrderId,
      BigDecimal orderedQuantity1, String description, String orderLineId) {
    SaqbOrderline saqbOrderline;
    SaqbOrder saqbOrder = ComboHelperUtils.getSaqbOrder(callCenterOrderId);
    Product product = ComboHelperUtils.getProduct(productId);
    TaxRate taxRate = ComboHelperUtils.getProductTax(productId);
    BigDecimal orderedQuantity = new BigDecimal(orderedQuantity1.intValue());

    if (!orderLineId.equals("null")) {
      saqbOrderline = OBDal.getInstance().get(SaqbOrderline.class, orderLineId);
    } else {
      saqbOrderline = new SaqbOrderline();
      saqbOrderline.setLineNo(ComboHelperUtils.getNextLineNo(saqbOrder.getSaqbOrderlineList()));
    }

    saqbOrderline.setProduct(product);
    saqbOrderline.setTax(taxRate);

    saqbOrderline.setSubtotal(taxRate
        .getRate()
        .divide(new BigDecimal(100))
        .add(new BigDecimal(1))
        .multiply(orderedQuantity)
        .multiply(
            ComboHelperUtils.getProductPrice(productId, callCenterOrderId,
                ComboHelperUtils.getProductTax(productId))));

    saqbOrderline.setStock(ComboHelperUtils.getStock(productId, callCenterOrderId));
    saqbOrderline.setDescription(description.equals("null") ? "" : description);
    saqbOrderline.setSaqbOrder(saqbOrder);
    saqbOrderline.setNetUnitPrice(ComboHelperUtils.getProductPrice(productId, callCenterOrderId,
        ComboHelperUtils.getProductTax(productId)));
    saqbOrderline.setGrossUnitPrice(ComboHelperUtils.getProductGrossPrice(productId,
        callCenterOrderId, ComboHelperUtils.getProductTax(productId)));
    saqbOrderline.setOrderedQuantity(orderedQuantity);
    saqbOrderline.setLineNetAmount(orderedQuantity.multiply(ComboHelperUtils.getProductPrice(
        productId, callCenterOrderId, ComboHelperUtils.getProductTax(productId))));
    saqbOrderline.setOrganization(saqbOrder.getOrgRegion());

    OBDal.getInstance().save(saqbOrderline);
    return new JSONObject();
  }

  private JSONObject insertProductLine2(String productId, String callCenterOrderId,
      BigDecimal orderedQuantity1, String description, String orderLineId,
      BigDecimal grossUnitPrice, BigDecimal netUnitPrice, BigDecimal subtotal,
      BigDecimal lineNetAmount, String isIncludetax) {
    SaqbOrderline saqbOrderline;
    SaqbOrder saqbOrder = ComboHelperUtils.getSaqbOrder(callCenterOrderId);
    Product product = ComboHelperUtils.getProduct(productId);
    TaxRate taxRate = ComboHelperUtils.getProductTax(productId);
    BigDecimal orderedQuantity = new BigDecimal(orderedQuantity1.intValue());

    if (!orderLineId.equals("null")) {
      saqbOrderline = OBDal.getInstance().get(SaqbOrderline.class, orderLineId);
    } else {
      saqbOrderline = new SaqbOrderline();
      saqbOrderline.setLineNo(ComboHelperUtils.getNextLineNo(saqbOrder.getSaqbOrderlineList()));
    }

    saqbOrderline.setProduct(product);
    saqbOrderline.setTax(taxRate);

    saqbOrderline.setStock(ComboHelperUtils.getStock(productId, callCenterOrderId));
    saqbOrderline.setDescription(description.equals("null") ? "" : description);
    saqbOrderline.setSaqbOrder(saqbOrder);
    saqbOrderline.setNetUnitPrice(netUnitPrice);
    saqbOrderline.setGrossUnitPrice(grossUnitPrice);
    saqbOrderline.setOrderedQuantity(orderedQuantity);
    saqbOrderline.setLineNetAmount(lineNetAmount);
    saqbOrderline.setSubtotal(subtotal);
    saqbOrderline.setOrganization(saqbOrder.getOrgRegion());
    saqbOrderline.setTaxinclude((isIncludetax.equals("true") ? true : false));

    OBDal.getInstance().save(saqbOrderline);
    return new JSONObject();
  }

  private JSONObject insertComboLine(String comboConf, String callCenterOrderId, String comboLines) {
    try {

      if (!comboLines.equals("null")) {
        deleteLine(comboLines, callCenterOrderId);
      }

      SaqbOrder saqbOrder = ComboHelperUtils.getSaqbOrder(callCenterOrderId);
      JSONObject comboConfigurationObject = new JSONObject(comboConf);

      Iterator keys = comboConfigurationObject.keys();

      PriceAdjustment combo = ComboHelperUtils.getComboFromFamily(keys);

      keys = comboConfigurationObject.keys();
      BigDecimal alreadyComboQty = ComboHelperUtils.getComboQty(saqbOrder, combo);

      while (keys.hasNext()) {
        String key = (String) keys.next();

        JSONObject familyContent = comboConfigurationObject.getJSONObject(key);
        JSONArray products = familyContent.getJSONArray("products");
        Long numberline = ComboHelperUtils.getNextLineNo(saqbOrder.getSaqbOrderlineList());
        int comboQty = calculateComboQty(familyContent, products);

        for (int i = 0; i < products.length(); i++) {
          if (products.getJSONObject(i).getInt("insertedQty") > 0) {
            ComboHelperUtils.createLine2(saqbOrder, combo, comboQty, products.getJSONObject(i),
                alreadyComboQty, numberline);
            numberline = numberline + 10;
          }
        }
      }

    } catch (JSONException e) {
      log.error(e.getMessage());
      e.printStackTrace();
    }
    return new JSONObject();
  }

  private int calculateComboQty(JSONObject familyContent, JSONArray products) throws JSONException {

    int totalQty = 0;
    for (int i = 0; i < products.length(); i++) {
      totalQty += products.getJSONObject(i).getInt("insertedQty");
    }

    return totalQty / familyContent.getInt("requiredQty");
  }

  private JSONObject getProductPriceAndStock(String productId, String callCenterOrderId)
      throws JSONException {
    JSONObject result = new JSONObject();

    result.put("stock", ComboHelperUtils.getStock(productId, callCenterOrderId));
    result.put("productCategory", ComboHelperUtils.getProductCategory(productId));
    TaxRate tax = ComboHelperUtils.getProductTax(productId);
    result.put("tax", tax.getId());
    result.put("netUnitPrice", BigDecimal.ZERO);
    result.put("grossUnitPrice",
        ComboHelperUtils.getProductGrossPrice(productId, callCenterOrderId, tax));
    result.put("taxIdentifier", ComboHelperUtils.getProductTax(productId).getIdentifier());
    result.put("taxRate", ComboHelperUtils.getProductTax(productId).getRate());
    result.put("istaxinclude", ComboHelperUtils.isIncludetax(productId, callCenterOrderId));

    return result;
  }

  private JSONObject getPrices(String productId, String callCenterOrderId,
      BigDecimal orderedQuantity) throws JSONException {
    JSONObject result = new JSONObject();
    TaxRate tax = ComboHelperUtils.getProductTax(productId);
    result = ComboHelperUtils.getProductPrice2(productId, callCenterOrderId, tax, orderedQuantity);

    return result;
  }

  public static List<PriceAdjustment> getPriceAdjustmentByOrg(String strOrderOrgId) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    // ID MÓDULO ec.com.sidesoft.retail.combo 4A04E9526D26474999736FE44BFF57FF
    try {
      String strSql = "SELECT * FROM (SELECT mo1.m_offer_id, mo1.name "
          + "FROM m_offer mo1 "
          + "WHERE mo1.isactive='Y' "
          + "AND mo1.ad_org_id=? "
          + "AND TO_DATE(now())>=mo1.datefrom AND (TO_DATE(now())<=mo1.dateto OR mo1.dateto IS NULL) "
          + "AND mo1.m_offer_type_id IN (SELECT ot.m_offer_type_id FROM M_Offer_Type ot WHERE ot.ad_module_id='4A04E9526D26474999736FE44BFF57FF') "
          + "UNION ALL "
          + "SELECT mo.m_offer_id,mo.name FROM m_offer mo "
          + "WHERE now()>=mo.datefrom AND (now()<=mo.dateto OR mo.dateto IS NULL) "
          + "AND mo.m_offer_type_id IN (SELECT ot.m_offer_type_id FROM M_Offer_Type ot WHERE ot.ad_module_id='4A04E9526D26474999736FE44BFF57FF') "
          + "AND mo.org_selection='Y' "
          + "AND (SELECT COUNT(*) FROM m_offer "
          + "INNER JOIN M_Offer_Organization mor ON mo.m_offer_id = mor.m_offer_id WHERE now()>=mo.datefrom AND (now()<=mo.dateto OR mo.dateto IS NULL) "
          + "AND mo.m_offer_type_id IN (SELECT ot.m_offer_type_id FROM M_Offer_Type ot WHERE ot.ad_module_id='4A04E9526D26474999736FE44BFF57FF') "
          + "AND mo.org_selection='Y' AND mor.ad_org_id =? )=0 "
          + "AND (select count(*) from ad_treenode adtn "
          + "join ad_org ao on ao.ad_org_id = adtn.parent_id "
          + "join ad_org aon on aon.ad_org_id = adtn.node_id "
          + "where "
          + "(ao.ad_org_id=? and aon.ad_org_id = mo.ad_org_id)or "
          + "(ao.ad_org_id=mo.ad_org_id and aon.ad_org_id =?)) >0 AND mo.isactive='Y'"
          + "UNION ALL "
          + "SELECT mo2.m_offer_id,mo2.name "
          + "FROM m_offer mo2 "
          + "WHERE now()>=mo2.datefrom AND (now()<=mo2.dateto OR mo2.dateto IS NULL)"
          + "AND mo2.m_offer_type_id IN (SELECT ot.m_offer_type_id FROM M_Offer_Type ot WHERE ot.ad_module_id='4A04E9526D26474999736FE44BFF57FF') "
          + "AND mo2.org_selection='N'"
          + "AND (SELECT COUNT(*) "
          + "FROM m_offer "
          + "INNER JOIN M_Offer_Organization mor ON mo2.m_offer_id = mor.m_offer_id WHERE now()>=mo2.datefrom AND (now()<=mo2.dateto OR mo2.dateto IS NULL) "
          + "AND mo2.m_offer_type_id IN (SELECT ot.m_offer_type_id FROM M_Offer_Type ot WHERE ot.ad_module_id='4A04E9526D26474999736FE44BFF57FF') "
          + "AND mo2.org_selection='N' AND mor.ad_org_id =?)>0 "
          + "AND (select count(*) from ad_treenode adtn "
          + "JOIN ad_org ao on ao.ad_org_id = adtn.parent_id "
          + "JOIN ad_org aon on aon.ad_org_id = adtn.node_id "
          + "WHERE (ao.ad_org_id=? and aon.ad_org_id = mo2.ad_org_id)or "
          + "(ao.ad_org_id=mo2.ad_org_id and aon.ad_org_id =?)) >0 "
          + "AND mo2.isactive='Y') SQ ORDER BY SQ.name ";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(strSql);
      st.setString(1, strOrderOrgId);
      st.setString(2, strOrderOrgId);
      st.setString(3, strOrderOrgId);
      st.setString(4, strOrderOrgId);
      st.setString(5, strOrderOrgId);
      st.setString(6, strOrderOrgId);
      st.setString(7, strOrderOrgId);
      ResultSet rsConsulta = st.executeQuery();

      List<PriceAdjustment> lstPriceAdjustment = new ArrayList<>();
      PriceAdjustment objPriceAdjustment = null;
      while (rsConsulta.next()) {
        objPriceAdjustment = null;
        objPriceAdjustment = OBDal.getInstance().get(PriceAdjustment.class,
            rsConsulta.getString("m_offer_id"));

        lstPriceAdjustment.add(objPriceAdjustment);
      }
      return lstPriceAdjustment;
    } catch (Exception e) {

      throw new OBException("Error al consultar lista de combos (m_offer). " + e.getMessage());
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }
}
