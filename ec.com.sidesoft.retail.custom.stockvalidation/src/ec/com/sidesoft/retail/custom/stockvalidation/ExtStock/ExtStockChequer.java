package ec.com.sidesoft.retail.custom.stockvalidation.ExtStock;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.retail.stockvalidation.StockChecker;

public class ExtStockChequer extends StockChecker {

  private static final Logger log = Logger.getLogger(ExtStockChequer.class);

  public JSONObject exec(JSONObject jsonData) throws JSONException, ServletException {

    boolean allowSell = false;
    BigDecimal unitsFound = new BigDecimal(0);
    OBContext.setAdminMode(true);
    try {
      String orgId;
      JSONObject jsonOrderLine, jsonProduct;
      orgId = jsonData.getString("organization");

      jsonOrderLine = jsonData.getJSONObject("orderLine");
      BigDecimal qtyToBuy;
      qtyToBuy = new BigDecimal(jsonOrderLine.getString("qty"));
      jsonProduct = jsonOrderLine.getJSONObject("product");
      String StrBillOfMaterials = jsonProduct.getString("isboom");
      if (!StrBillOfMaterials.equals("true")) {
        String hqlQuery = "select sum(ms.quantityOnHand) as qtyonhand "
            + "from MaterialMgmtStorageDetail ms " + "where ms.storageBin.warehouse.id in ("
            + "SELECT ow.warehouse.id " + "FROM OrganizationWarehouse as ow " + "WHERE "
            + "ow.organization.id = '" + orgId + "') " + "and ms.product.id = '"
            + jsonProduct.getString("id") + "'";

        final Session session = OBDal.getInstance().getSession();
        final Query query = session.createQuery(hqlQuery);

        if (query.uniqueResult() != null) {
          unitsFound = new BigDecimal(query.uniqueResult().toString());
        } else {
          unitsFound = BigDecimal.ZERO;
        }

        if (unitsFound.compareTo(qtyToBuy) >= 0) {
          allowSell = true;
        }
      } else {
        allowSell = true;
        // unitsFound = qtyToBuy;
      }
    } catch (Exception e) {
      throw new OBException();
    } finally {
      OBContext.restorePreviousMode();
    }

    JSONObject preFinalResult = new JSONObject();
    preFinalResult.put("allowSell", allowSell);
    if (allowSell == false) {
      preFinalResult.put("qty", unitsFound);
    }

    JSONObject finalResult = new JSONObject();
    finalResult.put("data", preFinalResult);
    finalResult.put("status", 0);
    return finalResult;
  }
}
