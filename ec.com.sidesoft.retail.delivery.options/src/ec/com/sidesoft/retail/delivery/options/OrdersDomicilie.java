package ec.com.sidesoft.retail.delivery.options;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.retail.posterminal.JSONProcessSimple;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;

public class OrdersDomicilie extends JSONProcessSimple {
  public static final Logger log = Logger.getLogger(OrdersDomicilie.class);

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {

    JSONObject result = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      String posid = jsonsent.getString("posid");

      Integer count = countOrder(posid);

      result.put(JsonConstants.RESPONSE_DATA, count);
      result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);

  } catch (Exception e) {
    throw new OBException("Error in PaidReceips: ", e);
  } finally {
    OBContext.restorePreviousMode();
  }

    return result;
  }


  private static Integer countOrder(String posid) {

    String strResult = "";
    Integer count = 0;
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {

      String strSql = "select count(ord.c_order_id) as contador \n"
          + "  from c_order as ord \n"
          + "  where ord.em_obpos_isdeleted = 'N' \n"
          + "  and ord.em_obpos_applications_id is not null \n"
          + "  and ord.docstatus <> 'CJ' \n"
          + "  and ord.docstatus <> 'CA' \n"
          + "  and (ord.docstatus <> 'CL' or ord.iscancelled = 'Y') \n"
          + "  and (ord.em_sscmb_sales_origin = 'WEB' or ord.em_sscmb_sales_origin = 'CLC' or ord.em_sscmb_sales_origin = 'CHATBOT' or ord.em_sscmb_sales_origin = 'SGLOVO_GLV' or ord.em_sscmb_sales_origin = 'SUBER_UBR' or ord.em_sscmb_sales_origin = 'SRAPPI_RPP' or ord.em_sscmb_sales_origin = 'SPEYA_PEYA') \n"
          + "  and ord.em_obpos_app_cashup_id is null \n"
          + "  and ord.em_obpos_islayaway = 'Y' \n"
          + "  and to_char(ord.created, 'YYYY-MM-dd') = to_char(now(), 'YYYY-MM-dd') \n"
          + "  and ord.em_obpos_applications_id = '"+posid+"' ;";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("contador");
      }

      count = Integer.parseInt(strResult);

      return count;

    } catch (Exception e) {
      throw new OBException("Error al consultar canton de la Organizacion. " + e.getMessage());
    }

  }


}