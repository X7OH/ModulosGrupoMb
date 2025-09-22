package ec.com.sidesoft.integration.peya.ad_process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;

//import java.time.ZoneOffset;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import ec.com.sidesoft.integration.delivery.SDELVRLogs;
import ec.com.sidesoft.integration.peya.speyaConfig;

public class Speya_CancellOrder extends DalBaseProcess {
	private final Logger logger = Logger.getLogger(Speya_CancellOrder.class);

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		ConnectionProvider conn = new DalConnectionProvider(false);
		OBError msg = new OBError();
		OBContext.setAdminMode(true);
		final String strLogID = (String) bundle.getParams().get("Sdelvr_Logs_ID");
		final SDELVRLogs Log = (SDELVRLogs) OBDal.getInstance().getProxy(SDELVRLogs.ENTITY_NAME, strLogID);
		String UrlOrderReject = Log.getSpeyaUrlreject();

		try {
			String AuthToken = obtenerToken();
			String OrderRespons = actualizarEstadoOrden(Log, AuthToken, UrlOrderReject);
			
			if(OrderRespons.equals("OK")){
				msg.setTitle(Utility.messageBD(conn, "Success", language));
	            msg.setType("Success");
	            msg.setMessage(OrderRespons + " - Cancelado exitosamente");
			}else {
				msg.setTitle(Utility.messageBD(conn, "Error", language));
	            msg.setType("Error");
	            msg.setMessage(OrderRespons + " - Error al consumir el proceso");
			}
			

		} catch (Exception e) {
			// TODO: handle exception
            msg.setTitle(Utility.messageBD(conn, "Error", language));
            msg.setType("Error");
            msg.setMessage(e.getMessage());
			throw new OBException("Error");
		} finally {
			bundle.setResult(msg);
			OBContext.setAdminMode(false);
		}

	}

	public String obtenerToken() {

		speyaConfig PeyaConfig = null;

		try {
			OBContext.setAdminMode(true);
			OBCriteria<speyaConfig> PeyaConfigCrt = OBDal.getInstance().createCriteria(speyaConfig.class);
			PeyaConfigCrt.add(Restrictions.eq(speyaConfig.PROPERTY_ACTIVE, true));
			PeyaConfigCrt.setMaxResults(1);

			PeyaConfig = (speyaConfig) PeyaConfigCrt.uniqueResult();

			String url = PeyaConfig.getPyloginUrl();
			String username = PeyaConfig.getPyloginUsername();
			String password = PeyaConfig.getPyloginPassword();
			String grantType = "client_credentials";

			URL endpoint = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setDoOutput(true);

			// Datos del cuerpo de la solicitud
			String data = String.format("username=%s&password=%s&grant_type=%s", username, password, grantType);

			// Enviar datos
			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = data.getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			// Leer respuesta
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
			StringBuilder response = new StringBuilder();
			String responseLine;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}

			// Parsear JSON y obtener token
			JSONObject jsonResponse = new JSONObject(response.toString());
			String TokenRP = "";
			TokenRP = jsonResponse.getString("access_token"); // asumiendo que el token viene en "access_token"

//			PeyaConfig.setPyloginToken(TokenRP);
//			OBDal.getInstance().save(PeyaConfig);
//			OBDal.getInstance().flush();

			return TokenRP;

		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		} finally {
			OBContext.restorePreviousMode();
		}
	}

	public String actualizarEstadoOrden(SDELVRLogs Log,
		String token, String UrlOrderReject) {
		OBDal.getInstance().refresh(Log);
		String Reason = Log.getSpeyaCancellreasons().getReasonidentifiers();
		String message = Log.getSpeyaReasontocancell();
		Reason = (Reason == null) ? "cancelado" : Reason;
//		speyaConfig PeyaConfig = null;

		try {
			OBContext.setAdminMode(true);
			OBCriteria<speyaConfig> PeyaConfigCrt = OBDal.getInstance().createCriteria(speyaConfig.class);
			PeyaConfigCrt.add(Restrictions.eq(speyaConfig.PROPERTY_ACTIVE, true));
			PeyaConfigCrt.setMaxResults(1);
//			PeyaConfig = (speyaConfig) PeyaConfigCrt.uniqueResult();

			String url = UrlOrderReject;

			URL endpoint = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + token);
			connection.setDoOutput(true);

			// Crear el cuerpo de la solicitud en formato JSON
			JSONObject jsonBody = new JSONObject();
			DateTime now = new DateTime(DateTimeZone.forOffsetHours(5));
			DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
			String formattedDate = formatter.print(now);
//	            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
//	            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.ofHours(5)); // Establece la zona horaria con el desplazamiento +05:00
//	            String formattedDate = now.format(formatter);

			jsonBody.put("message", message);
			jsonBody.put("reason", Reason);
			jsonBody.put("status", "order_rejected");

			// Enviar el cuerpo de la solicitud
			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonBody.toString().getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			int ResponseCode = 0;

			ResponseCode = connection.getResponseCode();

			// Retornar la respuesta
			if (ResponseCode == HttpURLConnection.HTTP_OK) {

				// Leer la respuesta
				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
				StringBuilder response = new StringBuilder();
				String responseLine;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
				
				Log.setSpeyaListstates("rejected");
				OBDal.getInstance().save(Log);
				return "OK";
			} else {
				return "ERROR";
			}

		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		} finally {
//			PeyaConfig.setPyaceptorderToken(token);
//			OBDal.getInstance().save(PeyaConfig);
			OBContext.restorePreviousMode();
		}
	}

}
