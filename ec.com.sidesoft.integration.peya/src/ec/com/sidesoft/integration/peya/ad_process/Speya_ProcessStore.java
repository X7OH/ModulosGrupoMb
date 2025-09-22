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
import ec.com.sidesoft.integration.peya.SpeyaOpenCloseStores;

public class Speya_ProcessStore extends DalBaseProcess {
	private final Logger logger = Logger.getLogger(Speya_ProcessStore.class);

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		ConnectionProvider conn = new DalConnectionProvider(false);
		OBError msg = new OBError();
		OBContext.setAdminMode(true);
		final String strOCStoresID = (String) bundle.getParams().get("Speya_Openclosestores_ID");
		final SpeyaOpenCloseStores Log = (SpeyaOpenCloseStores) OBDal.getInstance()
				.getProxy(SpeyaOpenCloseStores.ENTITY_NAME, strOCStoresID);
		String mssg = "";
		String chainCode = null;
		String posVendorId = null;
		JSONObject jsonResponse = new JSONObject();

		try {

			String State = Log.getSpeyaStateslist();
			if (State.isEmpty() || State == null) {
				mssg = "No se ha seleccionado una accion.";
			}

			speyaConfig PeyaConfig = null;

			try {
				OBCriteria<speyaConfig> PeyaConfigCrt = OBDal.getInstance().createCriteria(speyaConfig.class);
				PeyaConfigCrt.add(Restrictions.eq(speyaConfig.PROPERTY_ACTIVE, true));
				PeyaConfigCrt.setMaxResults(1);

				PeyaConfig = (speyaConfig) PeyaConfigCrt.uniqueResult();
			} catch (Exception e) {
				msg.setTitle(Utility.messageBD(conn, "Error", language));
				msg.setType("Error");
				msg.setMessage("Error al obtener el Token - " + e.getMessage());
			}

			// obtenemos y formateamos la url
			String UpdateStoreUrl = PeyaConfig.getPyupdatestoreUrl();

			chainCode = Log.getSpeyaChainid();
			posVendorId = Log.getSpeyaVendorid();
			String NewUpdateStoreUrl = replacePlaceholders(UpdateStoreUrl, chainCode, posVendorId);

//			Configuramos el json segun la accion que necesitemos
			if (State.equals("OPEN")) {

				jsonResponse.put("availabilityState", State);
				jsonResponse.put("platformKey", "PY");
				jsonResponse.put("platformRestaurantId", Log.getSpeyaRestaurantid());

				mssg = "Establecimiento Abierto al Publico.";

			} else if (State.equals("CLOSED_UNTIL")) {

				int closingTime = Log.getSpeyaClosingtime().intValue();

				if (closingTime < 5) {
					throw new OBException("El valor de los minutos debe ser mayor o igual a 5");
				}

				jsonResponse.put("availabilityState", State);
				jsonResponse.put("closedReason", Log.getSpeyaClosinglist());
				jsonResponse.put("closingMinutes", Log.getSpeyaClosingtime());
				jsonResponse.put("platformKey", "PY");
				jsonResponse.put("platformRestaurantId", Log.getSpeyaRestaurantid());

				mssg = "Establecimiento cerrado durante " + closingTime + " mins.";

			} else if (State.equals("CLOSED_TODAY")) {

				jsonResponse.put("availabilityState", State);
				jsonResponse.put("closedReason", Log.getSpeyaClosinglist());
				jsonResponse.put("platformKey", "PY");
				jsonResponse.put("platformRestaurantId", Log.getSpeyaRestaurantid());
				mssg = "Establecimiento cerrado durante TODO el dia.";

			}

			String AuthToken = obtenerToken();
			String OrderRespons = actualizarEstadoOrden(jsonResponse, AuthToken, NewUpdateStoreUrl);

			if (OrderRespons.equals("OK")) {
				msg.setTitle(Utility.messageBD(conn, "Success", language));
				msg.setType("Success");
				msg.setMessage(OrderRespons + " - " + mssg);
			} else {
				msg.setTitle(Utility.messageBD(conn, "Error", language));
				msg.setType("Error");
				msg.setMessage(OrderRespons + " - Error al consumir el proceso");
			}

		} catch (Exception e) {
			// TODO: handle exception
			msg.setTitle(Utility.messageBD(conn, "Error", language));
			msg.setType("Error");
			msg.setMessage(e.getMessage() + " - " + mssg);
		} finally {
			bundle.setResult(msg);
			OBContext.setAdminMode(false);
		}

	}

	public String replacePlaceholders(String url, String chainCode, String posVendorId) {
		if (url == null || chainCode == null || posVendorId == null) {
			throw new IllegalArgumentException("Los parámetros no pueden ser nulos");
		}
		return url.replace("{chainCode}", chainCode).replace("{posVendorId}", posVendorId);
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

			PeyaConfig.setPyupdatestoreToken(TokenRP);
			OBDal.getInstance().save(PeyaConfig);
			OBDal.getInstance().flush();

			return TokenRP;

		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		} finally {
			OBContext.restorePreviousMode();
		}
	}

	public String actualizarEstadoOrden(JSONObject Log, String token, String UrlOrderReject) {
//		OBDal.getInstance().refresh(Log);
		speyaConfig PeyaConfig = null;

		try {
			OBContext.setAdminMode(true);
			OBCriteria<speyaConfig> PeyaConfigCrt = OBDal.getInstance().createCriteria(speyaConfig.class);
			PeyaConfigCrt.add(Restrictions.eq(speyaConfig.PROPERTY_ACTIVE, true));
			PeyaConfigCrt.setMaxResults(1);
			PeyaConfig = (speyaConfig) PeyaConfigCrt.uniqueResult();

			String url = UrlOrderReject;

			URL endpoint = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + token);
			connection.setDoOutput(true);

			// Crear el cuerpo de la solicitud en formato JSON
			JSONObject jsonBody = new JSONObject();

			jsonBody = Log;

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

				return "OK";
			} else {
				return "ERROR";
			}

		} catch (Exception e) {
			OBDal.getInstance().rollbackAndClose();
			e.printStackTrace();
			return e.getMessage();
		} finally {
			PeyaConfig.setPyaceptorderToken(token);
			OBDal.getInstance().save(PeyaConfig);
			OBContext.restorePreviousMode();
		}
	}

}
