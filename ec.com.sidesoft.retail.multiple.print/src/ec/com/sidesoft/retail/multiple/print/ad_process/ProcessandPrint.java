package ec.com.sidesoft.retail.multiple.print.ad_process;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.BUnzip2;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.process.Parameter;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

public class ProcessandPrint extends DalBaseProcess {
	OBError message;
	static Logger log4j = Logger.getLogger(ProcessandPrint.class);

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		ConnectionProvider conn = new DalConnectionProvider(false);
		message = new OBError();
		Order order = null;
		User objUser = null;
		OBError myMessage = null;
		try {
			OBContext.setAdminMode(true);
			String strOrderID = (String) bundle.getParams().get("C_Order_ID");
			String strUserID = bundle.getContext().getUser();

			order = OBDal.getInstance().get(Order.class, strOrderID);
			objUser = OBDal.getInstance().get(User.class, strUserID);
			myMessage = Procesar(strOrderID, order, objUser, bundle);

		} catch (Exception e) {
			e.printStackTrace();
			OBDal.getInstance().rollbackAndClose();
			throw new OBException(e.getMessage());
		} finally {
			bundle.setResult(myMessage);
			OBContext.restorePreviousMode();
		}

		System.out.println("status" + order.getDocumentStatus());

		if (myMessage.getMessage().equals("")) {
			String strOrderID = (String) bundle.getParams().get("C_Order_ID");
			order = OBDal.getInstance().get(Order.class, strOrderID);
			if (order != null) {
				if (order.isSalesTransaction()) {
					if(order.getSscmbSalesOrigin()!=null && !order.getSscmbSalesOrigin().equals("ND")){ 
					if (getNewDocstatus(order).equals("CO")) {

						try {
							OBContext.setAdminMode(true);
							ClientSOAP client = new ClientSOAP();
							client.GeneratePrint(order, objUser);
						} catch (Exception e) {
							e.printStackTrace(); //
							OBDal.getInstance().rollbackAndClose();
							throw new OBException(e.getMessage());

						} finally {
							OBContext.restorePreviousMode();
						}
					}
					}
				}

			}
		}

	}

	public OBError Procesar(String strC_Order_Id, Order order, User user,
			ProcessBundle bundle) throws NoConnectionAvailableException,
			SQLException, ServletException {
		// setUserContext("100");

		// 104 C_Order_Post
		ConnectionProvider con = new DalConnectionProvider(false);
		final org.openbravo.model.ad.ui.Process process = OBDal.getInstance()
				.get(org.openbravo.model.ad.ui.Process.class, "104");
		final ProcessInstance pInstance = OBProvider.getInstance().get(
				ProcessInstance.class);
		pInstance.setProcess(process);
		pInstance.setActive(true);
		pInstance.setRecordID(strC_Order_Id);
		pInstance.setUserContact(OBContext.getOBContext().getUser());
		OBDal.getInstance().save(pInstance);
		OBDal.getInstance().flush();
		String strPInstanceId = pInstance.getId();

		if (order.getDocumentStatus().equals("CO")) {
			order.setDocumentAction("RE");
			OBDal.getInstance().save(order);
			OBDal.getInstance().flush();
			OBDal.getInstance().getSession().refresh(order);
		}

		try {

			CallableStatement cs = con.getConnection().prepareCall(
					"{call C_Order_Post (?)}");

			cs.setString(1, strPInstanceId);
			cs.execute();
			cs.close();
		} catch (Exception e) {
			throw new OBException(e.getMessage(), e);
		}

		OBDal.getInstance().flush();
		OBDal.getInstance().getSession().refresh(pInstance);
		OBDal.getInstance().commitAndClose();

		VariablesSecureApp vars = new VariablesSecureApp(user.getId(),
				(String) bundle.getParams().get("Ad_Client_Id"),
				(String) bundle.getParams().get("Ad_Org_Id"));

		Connection conn = con.getTransactionConnection();

		PInstanceProcessData[] pinstanceData = PInstanceProcessData
				.selectConnection(conn, con, strPInstanceId);
		OBError myMessage = Utility.getProcessInstanceMessage(con, vars,
				pinstanceData);

		return myMessage;
	}
	

	  public static String getNewDocstatus(Order order) {
	    String  strDocstatus = "";
	    ConnectionProvider conn = new DalConnectionProvider(false);
	    try {
	      String strSql = "SELECT docstatus  FROM  c_order WHERE c_order_id= ? ";
	      PreparedStatement st = null;
	      st = conn.getPreparedStatement(strSql);
	      st.setString(1, order.getId());
	      ResultSet rsConsulta = st.executeQuery();

	      while (rsConsulta.next()) {
	    	  strDocstatus = rsConsulta.getString("docstatus");
	      }

	      return strDocstatus;

	    } catch (Exception e) {

	      throw new OBException("Error al consultar la tabla c_order. " + e.getMessage());
	    } finally {
	      try {
	        conn.destroy();
	      } catch (Exception e) {

	      }
	    }

	  }

}
