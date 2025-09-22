package ec.com.sidesoft.retail.delivery.options;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.retail.posterminal.JSONProcessSimple;
import org.openbravo.service.db.DalConnectionProvider;

public class StockChecker extends JSONProcessSimple {
  public static final Logger log = Logger.getLogger(StockChecker.class);

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    
    JSONObject result = new JSONObject();
    JSONObject preFinalResult = new JSONObject();
    OBContext.setAdminMode(true);
    try {

      JSONArray products = jsonsent.getJSONArray("products");
      String orgId = jsonsent.getString("organization");
      Integer contador = 0;
      String productsNames = "";
      
      for (int i = 0; i < products.length(); i++) {
        
        Double stock;
        JSONObject info = products.getJSONObject(i);
        String id = info.getString("id");
        Double qty = Double.valueOf(info.getString("qty"));
        stock = verifyStock(id,orgId);
         if(qty > stock) {
           contador++;
           String name = getNameProduct(id);
           productsNames = productsNames + name + ", ";
         }
         
      }   
      
      productsNames = productsNames.substring(0, productsNames.length() - 2);
      productsNames = productsNames + " ";
      
      if(contador > 0) {
        preFinalResult.put("allowSell", false);
        preFinalResult.put("products", productsNames);
      }else {
        preFinalResult.put("allowSell", true);
      }

      result.put("data", preFinalResult);
      result.put("status", 0);
    
  } catch (Exception e) {
    throw new OBException("Error en Verificar Stock: ", e);
  } finally {
    OBContext.restorePreviousMode();
  }
    
    return result;
  }
  
  
  private static Double verifyStock(String id, String orgid) {

    String strResult = "";
    Double count;
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {

      String strSql = "SELECT COALESCE(SUM(QtyOnHand), 0) as qtyonhand \n"
          + "  FROM M_STORAGE_DETAIL s \n"
          + "  WHERE M_Product_ID = '"+id+"' \n"
          + "  AND EXISTS (SELECT * FROM M_LOCATOR l  WHERE s.M_Locator_ID=l.M_Locator_ID  AND l.M_Warehouse_ID IN (SELECT ow.m_warehouse_id  FROM AD_Org_Warehouse as ow WHERE ow.ad_org_id= '"+orgid+"') );";
      
      PreparedStatement st = null;
      
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("qtyonhand");
      }

      count = Double.valueOf(strResult);

      return count;

    } catch (Exception e) {
      throw new OBException("Error al consultar canton de la Organizacion. " + e.getMessage());
    }

  }  

  private static String getNameProduct(String id) {

    String strResult = "";
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {

      String strSql = "SELECT name as name FROM m_product WHERE M_Product_ID = '"+id+"';";
      
      PreparedStatement st = null;
      
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("name");
      }

      return strResult;

    } catch (Exception e) {
      throw new OBException("Error al consultar nombre del producto. " + e.getMessage());
    }

  }  


  
}
