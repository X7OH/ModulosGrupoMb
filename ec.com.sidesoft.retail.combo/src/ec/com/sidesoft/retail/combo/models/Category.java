package ec.com.sidesoft.retail.combo.models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.retail.config.OBRETCOProductList;
import org.openbravo.retail.posterminal.POSUtils;
import org.openbravo.retail.posterminal.ProcessHQLQuery;
import org.openbravo.retail.posterminal.master.CategoryTree;

public class Category extends ProcessHQLQuery {
  public static final String productCategoryPropertyExtension = "OBPOS_ProductCategoryExtension";
  public static final Logger log = Logger.getLogger(Category.class);

  @Inject
  @Any
  @Qualifier(productCategoryPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected Map<String, Object> getParameterValues(JSONObject jsonsent) throws JSONException {
    try {
      OBContext.setAdminMode(true);
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      String orgId = OBContext.getOBContext().getCurrentOrganization().getId();
      final OBRETCOProductList productList = POSUtils.getProductListByOrgId(orgId);
      boolean isRemote = false;
      try {
        OBContext.setAdminMode(false);
        isRemote = "Y".equals(Preferences.getPreferenceValue("OBPOS_remote.product", true,
            OBContext.getOBContext().getCurrentClient(),
            OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
            OBContext.getOBContext().getRole(), null));
      } catch (PropertyException e) {
        log.error("Error getting preference OBPOS_remote.product " + e.getMessage(), e);
      } finally {
        OBContext.restorePreviousMode();
      }

      Map<String, Object> paramValues = new HashMap<String, Object>();
      paramValues.put("productListId", productList.getId());
      if (isRemote) {
        paramValues.put("productCategoryTableId", CategoryTree.productCategoryTableId);
      }
      if (!isRemote) {
        final Date terminalDate = OBMOBCUtils.calculateServerDate(
            jsonsent.getJSONObject("parameters").getString("terminalTime"), jsonsent
                .getJSONObject("parameters").getJSONObject("terminalTimeOffset").getLong("value"));

        final PriceListVersion priceListVersion = POSUtils.getPriceListVersionByOrgId(orgId,
            terminalDate);
        paramValues.put("priceListVersionId", priceListVersion.getId());
      }
      if (OBContext.hasTranslationInstalled()) {
        paramValues.put("languageId", OBContext.getOBContext().getLanguage().getId());
      }
      Calendar now = Calendar.getInstance();
      paramValues.put("endingDate", now.getTime());
      paramValues.put("startingDate", now.getTime());
      paramValues.put("clientId", clientId);
      paramValues.put("orgId", orgId);
      return paramValues;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {

    List<String> hqlQueries = new ArrayList<String>();

    HQLPropertyList regularProductsCategoriesHQLProperties = ModelExtensionUtils
        .getPropertyExtensions(extensions);

    boolean isRemote = false;
    final String clientId = OBContext.getOBContext().getCurrentClient().getId();
    try {
      OBContext.setAdminMode(false);
      isRemote = "Y".equals(Preferences.getPreferenceValue("OBPOS_remote.product", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), null));
    } catch (PropertyException e) {
      log.error("Error getting preference OBPOS_remote.product " + e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }

    Long lastUpdated;

    if (jsonsent != null) {
      lastUpdated = jsonsent.has("lastUpdated") && !jsonsent.get("lastUpdated").equals("undefined")
          && !jsonsent.get("lastUpdated").equals("null") ? jsonsent.getLong("lastUpdated") : null;
    } else {
      lastUpdated = null;
    }

    String fullRefreshCondition = lastUpdated == null ? "and pCat.active = true " : "";

    if (isRemote) {
      hqlQueries.add("select" + regularProductsCategoriesHQLProperties.getHqlSelect() //
          + "from OBRETCO_Productcategory aCat left outer join aCat.productCategory as pCat left outer join pCat.image as img"
          + " where ( aCat.obretcoProductlist.id = :productListId ) "
          + " and  aCat.$incrementalUpdateCriteria and aCat.active = true and aCat.$naturalOrgCriteria and aCat.$readableSimpleClientCriteria  "
          + " order by pCat.name, pCat.id");
      hqlQueries.add("select" + regularProductsCategoriesHQLProperties.getHqlSelect() //
          + "from ADTreeNode tn, ProductCategory pCat left outer join pCat.image as img "
          + "where tn.$incrementalUpdateCriteria and pCat.active = true and tn.$naturalOrgCriteria and tn.$readableSimpleClientCriteria "
          + " and tn.node = pCat.id and tn.tree.table.id = :productCategoryTableId "
          + " and pCat.summaryLevel = 'Y'"
          + " and not exists (select pc.id from OBRETCO_Productcategory pc where tn.node = pc.productCategory.id) "
          + "order by tn.sequenceNumber, tn.id");

    } else {
      hqlQueries.add("select" + regularProductsCategoriesHQLProperties.getHqlSelect() //
          + "from ProductCategory as pCat left outer join pCat.image as img  " + " where (exists("
          + "from OBRETCO_Prol_Product pli, " + "PricingProductPrice ppp, "
          + "PricingPriceListVersion pplv "
          + "WHERE pCat=pli.product.productCategory and (pli.obretcoProductlist.id = :productListId ) "
          + "AND (pplv.id= :priceListVersionId) AND (" + "ppp.priceListVersion.id = pplv.id"
          + ") AND (" + "pli.product.id = ppp.product.id" + ") AND ("
          + "pli.product.active = true)) " + "OR (pCat.summaryLevel = 'Y' " + fullRefreshCondition
          + "AND pCat.$naturalOrgCriteria AND "
          + "pCat.$readableSimpleClientCriteria)) AND pCat.$incrementalUpdateCriteria "
          + "order by pCat.name, pCat.id");
    }
    String promoNameTrl;
    if (OBContext.hasTranslationInstalled()) {
      promoNameTrl = "coalesce ((select t.commercialName from PromotionTypeTrl t where t.discountPromotionType=pt and t.language.id= :languageId), pt.commercialName)";
    } else {
      promoNameTrl = "pt.commercialName";
    }

    String whereClause = "p.client.id = '" + clientId + "' "
        + "and p.startingDate <= :startingDate "
        + "and (p.endingDate is null or p.endingDate >= :endingDate) "
        // assortment products
        + "and ((p.includedProducts = 'N' and not exists (select 1 "
        + "      from PricingAdjustmentProduct pap where pap.active = true and "
        + "      pap.priceAdjustment = p and pap.product.sale = true "
        + "      and pap.product not in (select ppl.product.id from OBRETCO_Prol_Product ppl "
        + "      where ppl.obretcoProductlist.id = :productListId and ppl.active = true))) "
        + " or (p.includedProducts = 'Y' and not exists (select 1 "
        + "      from PricingAdjustmentProduct pap, OBRETCO_Prol_Product ppl "
        + "      where pap.active = true and pap.priceAdjustment = p "
        + "      and pap.product.id = ppl.product.id "
        + "      and ppl.obretcoProductlist.id = :productListId))) "
        // organization
        + "and ((p.includedOrganizations='Y' " + "  and not exists (select 1 "
        + "         from PricingAdjustmentOrganization o" + "        where active = true"
        + "          and o.priceAdjustment = p" + "          and o.organization.id = :orgId )) "
        + "   or (p.includedOrganizations='N' " + "  and  exists (select 1 "
        + "         from PricingAdjustmentOrganization o" + "        where active = true"
        + "          and o.priceAdjustment = p" + "          and o.organization.id = :orgId ))) ";

    // Discounts marked as category
    hqlQueries.add("select pt.id as id, " + promoNameTrl + " as searchKey, " + promoNameTrl
        + " as name, " + promoNameTrl + " as _identifier, "
        + "(select bindaryData from ADImage ai where ai = pt.obposImage) as img, "
        + "(case when (count(p.name) > 0 and exists (select 1 from PricingAdjustment p "
        + "where p.discountType = pt and p.active = true and " + whereClause + ")) "
        + "then true else false end) as active, "
        + "'N' as realCategory, (case when (pt.id = '71895FA82C9645949CB752564FB1389D' or pt.id = '7899A7A4204749AD92881133C4EE7A57') then true else false end) as summaryLevel " //
        + "from PromotionType pt inner join pt.pricingAdjustmentList p "
        + "where pt.active = true and pt.obposIsCategory = true "//
        + "and pt.$readableSimpleClientCriteria "//
        + "and (p.$incrementalUpdateCriteria) " //
        + "and " + whereClause//
        + "group by pt.id, pt.commercialName, pt.obposImage");

    // // // Combo categories
    //
    // String fullRefreshCondition2 = lastUpdated == null ? "and pCat2.active = true " : "";
    // hqlQueries.add("select " + regularProductsCategoriesHQLProperties.getHqlSelect() //
    // + "from PricingAdjustment pa left join pa.ssrcmProCategory as pCat "
    // + "left outer join pCat.image as img where pa.ssrcmProCategory is not null and pa.active =
    // true "
    // + "and pa.discountType.id in (select pt.id "
    // + "from PromotionType pt inner join pt.pricingAdjustmentList p "
    // + "where pt.active = true and pt.obposIsCategory = true "//
    // + "and pt.$readableSimpleClientCriteria "//
    // + "and (p.$incrementalUpdateCriteria) " //
    // + "and " + whereClause//
    // + "group by pt.id) and pCat.id not in " + "( select pCat2.id " //
    // + "from ProductCategory as pCat2 where (exists(" + "from OBRETCO_Prol_Product pli, "
    // + "PricingProductPrice ppp, " + "PricingPriceListVersion pplv "
    // + "WHERE pCat2=pli.product.productCategory and (pli.obretcoProductlist.id = :productListId )
    // "
    // + "AND (pplv.id= :priceListVersionId) AND (" + "ppp.priceListVersion.id = pplv.id"
    // + ") AND (" + "pli.product.id = ppp.product.id" + ") AND (" + "pli.product.active = true)) "
    // + "OR (pCat2.summaryLevel = 'Y' " + fullRefreshCondition2
    // + "AND pCat2.$naturalOrgCriteria AND "
    // + "pCat2.$readableSimpleClientCriteria)) AND pCat2.$incrementalUpdateCriteria)");

    return hqlQueries;
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }
}