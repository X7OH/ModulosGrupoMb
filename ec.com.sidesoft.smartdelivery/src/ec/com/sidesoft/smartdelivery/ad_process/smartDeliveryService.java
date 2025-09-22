package ec.com.sidesoft.smartdelivery.ad_process;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import ec.com.sidesoft.smartdelivery.SSMRDRNotAllowedPayment;

public class smartDeliveryService extends DalBaseProcess {

  protected void doExecute(ProcessBundle bundle) throws Exception {

    OBError message = new OBError();

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = bundle.getConnection();
    
    try {
      message = new OBError();
      String orderId = (String) bundle.getParams().get("C_Order_ID");
      Order order = OBDal.getInstance().get(Order.class, orderId);
      
      FIN_PaymentMethod metodo = order.getPaymentMethod();
      String origen = order.getSscmbSalesOrigin().trim();     
      
      if(origen.equals("WEB") || origen.equals("CLC") || origen.equals("CHATBOT")) {
       
        OBCriteria<SSMRDRNotAllowedPayment> paymnetMethod = OBDal.getInstance()
        .createCriteria(SSMRDRNotAllowedPayment.class);
        paymnetMethod.add(Restrictions.eq(SSMRDRNotAllowedPayment.PROPERTY_ACTIVE, true));
        paymnetMethod.add(Restrictions.eq(SSMRDRNotAllowedPayment.PROPERTY_FINPAYMENTMETHOD, metodo));
        paymnetMethod.addOrderBy(SSMRDRNotAllowedPayment.PROPERTY_CREATIONDATE, false);
        paymnetMethod.setMaxResults(1);

        // SI EL METODO DE PAGO ES UBER O GLOVO NO SE ENVIA A SMARTDELIVERY
        if(paymnetMethod.list() != null && paymnetMethod.list().size() > 0) {
            saveOrderCheck(order.getId());
            message.setTitle(Utility.messageBD(conn, "Info", language));
            message.setType("Info");
            message.setMessage(Utility.messageBD(conn, "SSMRDR_PaymentMethod", language));        
        }else {
            // ENVIO DE LA INFORMACION A SMARTDELIVERY
            SmartDeliveryAPI smartDelivery = new SmartDeliveryAPI();
            JSONObject result = new JSONObject();
            result.put("orderIdOB", order.getId());
            smartDelivery.consumeSmartClientWS(result);
        }       

      }else {
        saveOrderCheck(order.getId());
        message.setTitle(Utility.messageBD(conn, "Info", language));
        message.setType("Info");
        message.setMessage(Utility.messageBD(conn, "SSMRDR_SalesOrigin", language));          
      }      
      
    } catch (Exception e) {
      message.setTitle(Utility.messageBD(conn, "Error", language));
      message.setType("Error");
      message.setMessage(e.getMessage());
    } finally {
      bundle.setResult(message);
    }

  }
  
  private void saveOrderCheck(String orderId) {

    Order order = OBDal.getInstance().get(Order.class, orderId);
    order.setSsmrdrSmartdeliveryCheck(true);
    OBDal.getInstance().save(order);
    OBDal.getInstance().flush();

  }  
}
