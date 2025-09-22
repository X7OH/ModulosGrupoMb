package ec.com.sidesoft.integration.pedidosya.ad_process;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.SequenceIdData;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.integration.pedidosya.webservices.util.ResponseWS;

public class ProcessJustoLogs {

	private final Logger logger = Logger.getLogger(ProcessJustoLogs.class);
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private ConnectionProvider conn;
	
	public String registerLog(String data) throws ServletException {
		String id = null;
		try {
			OBContext.setAdminMode(true);
			logger.info("Begin Justo Log Register");
			conn = new DalConnectionProvider(false);
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//			process();
			SsipyJustoLogData registerLog = new SsipyJustoLogData();
			String cliID = registerLog.getField("ad_client_id");
			id = SequenceIdData.getUUID();
			SsipyJustoLogData.insert(conn, id, data, "El Registro se encuentra en proceso");
			SsipyJustoLogData.ok(conn, "El Registro se inserto con exito", id);
						
		} catch (final Exception e) {
			SsipyJustoLogData.error(conn, "El Registro tuvo errores en la inserccion", id);
			throw new OBException("Error: ", e);
		} finally {
			try {
//				conn.getConnection().close();
//				conn.destroy();
			} catch (Exception e) {
				throw new OBException("Error: ", e);
			}
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
			OBContext.restorePreviousMode();
			logger.info("Finish Justo Log Register");
		}
		return id;
	}
}
