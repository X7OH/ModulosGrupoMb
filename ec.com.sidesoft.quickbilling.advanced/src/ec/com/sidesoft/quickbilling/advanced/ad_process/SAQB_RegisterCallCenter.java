package ec.com.sidesoft.quickbilling.advanced.ad_process;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalBaseProcess;

import ec.com.sidesoft.delivery.sales.channel.ad_helpers.DeliveryHelpers;
import ec.com.sidesoft.integration.picker.SspkriPickerconfig;
import ec.com.sidesoft.integration.picker.ad_process.pickerMain;
import ec.com.sidesoft.quickbilling.advanced.SaqbOrder;
import ec.com.sidesoft.smartdelivery.ad_process.SmartDeliveryAPI;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.service.db.DalConnectionProvider;

public class SAQB_RegisterCallCenter extends DalBaseProcess {

  private static final Logger log4j = Logger.getLogger(SAQB_RegisterCallCenter.class);
  
  protected void doExecute(ProcessBundle bundle) throws Exception {

    final String strSaqbOrderID = (String) bundle.getParams().get("Saqb_Order_ID");
    final ConnectionProvider conn = bundle.getConnection();
    final VariablesSecureApp vars = bundle.getContext().toVars();

    try {

      OBContext.setAdminMode(true);
    	
      SaqbOrder saqbOrder = OBDal.getInstance().get(SaqbOrder.class, strSaqbOrderID);
      
      String ORDERValue = "NA";
      
      if (saqbOrder.getSaqbOrdertype() == null) {
    	  ORDERValue = ORDERValue;
      } else {
    	  ORDERValue = saqbOrder.getSaqbOrdertype(); 
      }
      
      if(ORDERValue.equals("NA") || (ORDERValue.equals("pus") || 
    		  (ORDERValue.equals("pup") && saqbOrder.isSaqbApprovetrx()))
    		  ) {
    	  
    	  if (ORDERValue.equals("pus") || ORDERValue.equals("pup")) {
          	
          	String dsc = (saqbOrder.getDescription() == null) ? "" : saqbOrder.getDescription();
        	  	saqbOrder.setDescription(dsc + " - Retirar en Local");
          	
          }
    	  
          registerCallCenter(saqbOrder, conn, vars);
          OBDal.getInstance().save(saqbOrder);
          OBDal.getInstance().flush();

          String documnetNo = getOrder(strSaqbOrderID.trim());
          
          final OBError msg = new OBError();

          msg.setType("Success");
          msg.setTitle(Utility.messageBD(conn, "Success", bundle.getContext().getLanguage()));
          msg.setMessage(Utility.messageBD(conn, "Pedido procesado. Nº. de pedido: " + documnetNo.trim(), bundle.getContext().getLanguage()));
          bundle.setResult(msg);

          OBCriteria<Order> orderQuery = OBDal.getInstance().createCriteria(Order.class);
          orderQuery.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, documnetNo.trim()));
          orderQuery.add(Restrictions.eq(Order.PROPERTY_ACTIVE, true));
          orderQuery.setMaxResults(1);

          if (orderQuery.list() != null && orderQuery.list().size() > 0) {
            
            String orderId = orderQuery.list().get(0).getId();
            ///
            
            Order ordOB = OBDal.getInstance().get(Order.class, orderId);
            
            DeliveryHelpers DeliveryHelper = new DeliveryHelpers(); 
            
            // ENVIO DE LA INFORMACION A SMARTDELIVERY      
            pickerMain pkrProcess = new pickerMain();
            SspkriPickerconfig pkrConfig = new SspkriPickerconfig();
            try {
                // Tu lógica aquí
          	  pkrConfig = OBDal.getInstance().get(SspkriPickerconfig.class, pkrProcess.getPkrConfig());
            }catch (Exception e) {
    			// TODO: handle exception
            	msg.setMessage(e.getMessage());
    		}
            
            OBDal.getInstance().refresh(ordOB);
            
            
            if (ORDERValue.equals("pus") || ORDERValue.equals("pup")) {
            	
            	if(ORDERValue.equals("pup")) {
            		int minutosAntes = 15;
                	Date fechaActual = new Date();
                	Date fechaPickup = saqbOrder.getSaqbDatetimepickup();

                	// Restamos X minutos a la fechaPickup
                	Calendar cal = Calendar.getInstance();
                	cal.setTime(fechaPickup);
                	cal.add(Calendar.MINUTE, -minutosAntes);
                	Date fechaLimite = cal.getTime();

                	// Verificamos si fechaActual está después de la fechaLimite y antes de la fechaPickup
//                	if (fechaActual.after(fechaLimite) && fechaActual.before(fechaPickup)) {
                	if (fechaActual.after(fechaLimite)) {
                	    System.out.println("La fecha actual está dentro del rango de los " + minutosAntes + " minutos previos a la fecha de pickup.");
                	} else {
                	    System.out.println("La fecha actual NO está dentro de los " + minutosAntes + " minutos previos.");
                	}
            	}
            	
            	ordOB.setSsmrdrIslocaldelivery(true);
            	
            }else {
            	
            	if(DeliveryHelper.getDelivery(ordOB.getOrganization().getId(), "CLC").equals("PCKR")) {

                	  String PickerId = ordOB.getOrganization().getSspkriPickerid();
                	  
                	  //Pre-Checkout
                	  String preCheckoutURL = pkrConfig.getUrlprecheckout();
                	  JSONObject jsonPreCheckout = pkrProcess.buildJsonPreCheckout(ordOB, PickerId);
                	  
                	  boolean preCheckOut = pkrProcess.SendPreCheckout(preCheckoutURL, jsonPreCheckout, 
                			  PickerId, ordOB); 

                	  //Booking
                	  String preBookingURL = pkrConfig.getUrlbooking();
                	  JSONObject jsonBooking = pkrProcess.buildJsonBooking(ordOB, PickerId);
                	  
                	  String tst = pkrProcess.SendBooking(preBookingURL, jsonBooking, 
                			  PickerId, ordOB, preCheckOut);
                  }else {
                  	// ENVIO DE LA INFORMACION A SMARTDELIVERY
                      SmartDeliveryAPI smartDelivery = new SmartDeliveryAPI();
                      JSONObject result = new JSONObject();
                      result.put("orderIdOB", orderId);
                      smartDelivery.consumeSmartClientWS(result);      
                  }
            	
            }
            
            ///
            
            OBDal.getInstance().save(ordOB);
            OBDal.getInstance().flush();
          }
    	  
      }else if (ORDERValue.equals("pup") && !saqbOrder.isSaqbApprovetrx()) {
    	  saqbOrder.setSaqbApprovetrx(true);
    	  
    	  final OBError msg = new OBError();
          msg.setType("Success");
          msg.setMessage("Orden Programada Registrada");
          msg.setTitle(Utility.messageBD(conn, "Success", bundle.getContext().getLanguage()));
          System.out.println(msg);
          bundle.setResult(msg);
    	  
      }

      OBDal.getInstance().save(saqbOrder);
      OBDal.getInstance().flush();
      
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Error al Registar el pedido Call Center", e);
      final OBError msg = new OBError();
      msg.setType("Error");
      if (e instanceof org.hibernate.exception.GenericJDBCException) {
        msg.setMessage(((org.hibernate.exception.GenericJDBCException) e).getSQLException()
            .getNextException().getMessage());
      } else if (e instanceof org.hibernate.exception.ConstraintViolationException) {
        msg.setMessage(((org.hibernate.exception.ConstraintViolationException) e).getSQLException()
            .getNextException().getMessage());
      } else {
        msg.setMessage(e.getMessage());
      }
      msg.setTitle(Utility.messageBD(conn, "Error", bundle.getContext().getLanguage()));
      System.out.println(msg);
      bundle.setResult(msg);
    } finally {
        OBContext.restorePreviousMode();
    }

  }
  
  private void registerCallCenter(SaqbOrder saqbOrder, ConnectionProvider conn,
      VariablesSecureApp vars) throws Exception {
    try {
      OBContext.setAdminMode(true);

      org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get(
          org.openbravo.model.ad.ui.Process.class, "E5F0DC8AF3794BD6AEAEE03ECC2F0BD0");

      final ProcessInstance pInstance = CallProcess.getInstance().call(process,
          saqbOrder.getId(), null);      

      if (pInstance.getResult() == 0) {
        // error processing
        OBError myMessage = Utility.getProcessInstanceMessage(conn, vars,
            PInstanceProcessData.select(new DalConnectionProvider(), pInstance.getId()));
        throw new OBException(myMessage.getMessage());
      }
      
    } finally {
      OBContext.restorePreviousMode();
    }
  }  
  
  private static String getOrder(String saqb_order_id) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    try {

      String strSql = "SELECT documentno \n" + "FROM saqb_order \n" 
          + "WHERE saqb_order_id = '" + saqb_order_id + "'";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("documentno");
      }

      return strResult;

    } catch (Exception e) {
      throw new OBException("Error al consultar numero documento del pedido. " + e.getMessage());
    }

  }  
  

}
