package org.openbravo.retail.stockvalidation;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.retail.posterminal.JSONProcessSimple;
import org.openbravo.retail.posterminal.utility.AttributesUtils;

public class StockChecker extends JSONProcessSimple {
  @Override
  public JSONObject exec(JSONObject jsonData) throws JSONException, ServletException {
    boolean allowSell = false;
    BigDecimal unitsFound = new BigDecimal(0);
    String overissueStoreBin = "";
    BigDecimal overissueQty = BigDecimal.ZERO;
    String validatedAttrSetInstanceDescription = "";
    OBContext.setAdminMode(true);
    try {
      String orgId;
      JSONObject jsonOrderLine, jsonProduct;
      orgId = jsonData.getString("organization");
      BigDecimal qtyToBuy;
      qtyToBuy = new BigDecimal(jsonData.getString("qty"));
      String attributeValue = null;

      jsonOrderLine = jsonData.getJSONObject("orderLine");
      jsonProduct = jsonOrderLine.getJSONObject("product");
      String productId = jsonProduct.getString("id");
      String StrBillOfMaterials = jsonProduct.getString("isboom");
      if (!StrBillOfMaterials.equals("true")) {
      if (OBMOBCUtils.isJsonObjectPropertyStringPresentNotNullAndNotEmptyString(jsonOrderLine,
          "attSetInstanceDesc")) {
        attributeValue = jsonOrderLine.getString("attSetInstanceDesc");
      } else if (OBMOBCUtils.isJsonObjectPropertyStringPresentNotNullAndNotEmptyString(
          jsonOrderLine, "attributeValue")) {
        attributeValue = jsonOrderLine.getString("attributeValue");
      }

      if (attributeValue != null) {
        validatedAttrSetInstanceDescription = AttributesUtils
            .generateValidAttSetInstanceDescription(attributeValue, productId);
      }
      String hqlQuery = "select sum(ms.quantityOnHand-ms.reservedQty) as qtyonhand "
          + "from MaterialMgmtStorageDetail ms " + "where ms.storageBin.warehouse.id in ("
          + "SELECT ow.warehouse.id " + "FROM OrganizationWarehouse as ow " + "WHERE "
          + "ow.organization.id = :orgId) " + "and ms.product.id = :productId "
          + "and ms.quantityOnHand > 0 and ms.quantityOnHand > ms.reservedQty";
      if (attributeValue != null && !attributeValue.isEmpty())
        if (attributeValue != null && !attributeValue.isEmpty())
          hqlQuery += " and ms.attributeSetValue.description = :attSetInstanceDesc";

      final Session session = OBDal.getInstance().getSession();
      final Query query = session.createQuery(hqlQuery);
      query.setString("orgId", orgId);
      query.setString("productId", productId);
      if (attributeValue != null && !attributeValue.isEmpty())
        query.setString("attSetInstanceDesc", validatedAttrSetInstanceDescription);

      if (query.uniqueResult() != null) {
        unitsFound = new BigDecimal(query.uniqueResult().toString());
      } else {
        unitsFound = BigDecimal.ZERO;
      }

      if (unitsFound.compareTo(qtyToBuy) >= 0) {
        allowSell = true;
      } else {
        String hqlQueryStatus = "select ms.storageBin.id as overissueStoreBin "
            + "from MaterialMgmtStorageDetail ms "
            + "where ms.storageBin.warehouse.id in ("
            + "SELECT ow.warehouse.id "
            + "FROM OrganizationWarehouse as ow "
            + "WHERE "
            + "ow.organization.id = :orgId) "
            + "and ms.product.id = :productId "
            + "and ms.storageBin.inventoryStatus.overissue = 'Y' order by ms.storageBin.relativePriority";
        final Session sessionStatus = OBDal.getInstance().getSession();
        final Query queryStatus = sessionStatus.createQuery(hqlQueryStatus);
        queryStatus.setString("orgId", orgId);
        queryStatus.setString("productId", productId);
        queryStatus.setMaxResults(1);

        if (queryStatus.uniqueResult() != null) {
          overissueStoreBin = (String) queryStatus.uniqueResult();
          overissueQty = qtyToBuy.subtract(unitsFound);
        } else {
          overissueStoreBin = "";
          if (StringUtils.isEmpty(overissueStoreBin)) {
            String hqlQueryAnyOverissueBin = "select loc.id as overissueStoreBin "
                + "from Locator loc " //
                + "where loc.warehouse.id in (" //
                + "  SELECT ow.warehouse.id " //
                + "  FROM OrganizationWarehouse as ow " //
                + "  WHERE ow.organization.id = :orgId" //
                + "  ) " //
                + "AND loc.inventoryStatus.overissue = 'Y' " //
                + "ORDER BY loc.relativePriority ASC";
            final Query queryAnyOverissueBin = OBDal.getInstance().getSession()
                .createQuery(hqlQueryAnyOverissueBin);
            queryAnyOverissueBin.setString("orgId", orgId);
            queryAnyOverissueBin.setMaxResults(1);
            if (queryAnyOverissueBin.uniqueResult() != null) {
              overissueStoreBin = (String) queryAnyOverissueBin.uniqueResult();
              overissueQty = BigDecimal.ZERO;
            } else {
              overissueStoreBin = "";
            }
          }
        }
      }
      } else {
          allowSell = true;
          // unitsFound = qtyToBuy;
        }
    } catch (Exception e) {
      throw new OBException(e.getMessage(), true);
    } finally {
      OBContext.restorePreviousMode();
    }

    JSONObject preFinalResult = new JSONObject();
    preFinalResult.put("allowSell", allowSell);
    if (allowSell == false) {
      preFinalResult.put("qty", unitsFound);
      if (!overissueStoreBin.isEmpty()) {
        preFinalResult.put("allowNegativeStock", true);
        preFinalResult.put("overissueStoreBin", overissueStoreBin);
        preFinalResult.put("overissueQty", overissueQty);
      } else {
        preFinalResult.put("allowNegativeStock", false);
      }
    }

    JSONObject finalResult = new JSONObject();
    finalResult.put("data", preFinalResult);
    finalResult.put("status", 0);
    return finalResult;
  }
}