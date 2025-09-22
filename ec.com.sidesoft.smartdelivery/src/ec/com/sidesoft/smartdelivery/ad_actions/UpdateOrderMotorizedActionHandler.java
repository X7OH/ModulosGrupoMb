package ec.com.sidesoft.smartdelivery.ad_actions;

import java.sql.PreparedStatement;
import java.util.Map;

import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.service.db.DalConnectionProvider;

public class UpdateOrderMotorizedActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    OBContext.setAdminMode(true);
    JSONObject response = new JSONObject();

    try {

      final JSONObject jsonData = new JSONObject(data);
      final String orderId = jsonData.getString("orderId");

      final Order order = OBDal.getInstance().get(Order.class,
          orderId);
      
      final BusinessPartner motorized = OBDal.getInstance().get(BusinessPartner.class,
          order.getSsmrdrMotorized().getId());

      updateOrder(order,motorized);

      response.put("status", "OK");
    } catch (Exception e) {
      System.out.println("UpdateOrderMotorizedActionHandler: " + e.getMessage());
      try {
        response.put("status", "ERROR");
        response.put("message", e.getMessage());
      } catch (Exception e2) {
      }
    }
    OBContext.setAdminMode(false);
    return response;
  }
  
  private void updateOrder(Order order, BusinessPartner motorized){
    
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = "";
      strSql = "UPDATE c_order SET EM_Ssmrdr_Motorizedname = '"+ motorized.getName() +"', "
          + " EM_Saqb_Assigned_Motorized = '"+ order.getSsmrdrMotorized().getId()+"', "
          + " em_ssmrdr_processed_date = NOW(),"
          + " em_saqb_deliverystatus = 'DOT', "
          + " em_saqb_allocation_hour = to_char(now(), 'HH24:MI:SS')"          
          + " WHERE c_order_id = '"+ order.getId() +"';";  
      
    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = conn.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();
    } catch (Exception ignore) {
      System.out.println(ignore.getMessage());
      ignore.printStackTrace();
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {
      }
    }
  }   




}
