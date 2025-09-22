package ec.com.sidesoft.quickbilling.advanced.ad_process;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.quickbilling.advanced.SaqbOrder;

public class SAQB_ApproveTansaction extends DalBaseProcess {

	  private static final Logger log4j = Logger.getLogger(SAQB_ApproveTansaction.class);
	  
	  protected void doExecute(ProcessBundle bundle) throws Exception {

	    final String strSaqbOrderID = (String) bundle.getParams().get("Saqb_Order_ID");
	    final ConnectionProvider conn = bundle.getConnection();
	    final VariablesSecureApp vars = bundle.getContext().toVars();

	    try {

	      OBContext.setAdminMode(true);
	    	
	      SaqbOrder saqbOrder = OBDal.getInstance().get(SaqbOrder.class, strSaqbOrderID);

	      ApproveTransaction(saqbOrder, conn, vars);
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
	  
	  private void ApproveTransaction(SaqbOrder saqbOrder, ConnectionProvider conn,
	      VariablesSecureApp vars) throws Exception {
	    try {
	      OBContext.setAdminMode(true);

	      org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get(
	          org.openbravo.model.ad.ui.Process.class, "698431ED3A79418E93361A4D5DF709C5");

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

	}
