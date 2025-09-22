package ec.com.sidesoft.integration.picker.ad_process;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.businesspartner.Location;
//import org.openbravo.model.common.geography.Location;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.integration.delivery.SDELVRLogs;
import ec.com.sidesoft.integration.picker.SspkriPickerconfig;
import ec.com.sidesoft.integration.picker.SspkriPickerlogs;

public class pickerMain extends HttpSecureAppServlet {

	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(pickerMain.class);

	public JSONObject buildJsonPreCheckout(Order order, String apiKey) {

		JSONObject jsonPayload = new JSONObject();
		OBDal.getInstance().refresh(order);

		try {
			String[] parts = order.getSaqbLongitudeLatitude().split(",\\s*");

			String lo = parts[1];
			String lat = parts[0];
			BigDecimal longi = new BigDecimal(lo);
			BigDecimal lati = new BigDecimal(lat);
			jsonPayload.put("longitude", longi); // Puedes reemplazar con // datos dinámicos
			jsonPayload.put("latitude", lati); // Puedes reemplazar con
												// datos dinámicos
			jsonPayload.put("paymentMethod", order.getPaymentMethod().getSspkriPickervalue());
			jsonPayload.put("carName", "BIKE");
		} catch (Exception e) {
			throw new OBException("Error building JSON Precheckout: " + e.getMessage());
		}

		return jsonPayload;
	}

	public JSONObject buildJsonBooking(Order order, String apiKey) {
		JSONObject jsonPayload = new JSONObject();
		OBDal.getInstance().refresh(order);

		try {
			OBContext.setAdminMode(true);
			Location loc = order.getInvoiceAddress();
			OBDal.getInstance().refresh(loc);

			String[] parts = order.getSaqbLongitudeLatitude().split(",\\s*");
			String lo = parts[1];
			String lat = parts[0];

			String FullName = "Consumidor Final";

			if (order.getSaqbContactdelivery() == null || order.getSaqbContactdelivery().isEmpty()) {
				FullName = order.getBusinessPartner().getName();
			} else {
				FullName = order.getSaqbContactdelivery();
			}

			String[] names = FullName.split("\\s+");
			String name1 = "";
			String name2 = "";

			if (names.length == 1) {
				name1 = names[0];
				name2 = names[0]; // Replícalo en ambos
			} else {
				name1 = names[0];
				name2 = names[1];
			}

			String customerMobile = (order.getSaqbContactnumber() != null && !order.getSaqbContactnumber().equals(".") && !order.getSaqbContactnumber().isEmpty())
					? order.getSaqbContactnumber()
					: (loc.getPhone() != null && !loc.getPhone().isEmpty()) ? loc.getPhone() : "0999999999";

			BigDecimal longi = new BigDecimal(lo);
			BigDecimal lati = new BigDecimal(lat);
			jsonPayload.put("longitude", longi); // Puedes reemplazar con // datos dinámicos
			jsonPayload.put("latitude", lati); // Puedes reemplazar con
												// datos dinámicos
			String city = "Quito";
			String cityClient = loc.getLocationAddress().getCityName();
			String cityOrg = order.getOrganization().getOrganizationInformationList().get(0).getSorgiCanton().getName();
			
			if(cityClient != null && !cityClient.trim().isEmpty()) {
				city = cityClient; 
			}else if(cityOrg != null && !cityOrg.trim().isEmpty()){
				city = cityOrg; 
			}
			
			String state = "Pichincha";
			String stateClient = loc.getLocationAddress().getRegionName();
			String stateOrg = order.getOrganization().getOrganizationInformationList().get(0).getSorgiRegion().getName();
			
			if(cityClient != null && !cityClient.trim().isEmpty()) {
				state = stateClient; 
			}else if(stateOrg != null && !stateOrg.trim().isEmpty()){
				state = stateOrg; 
			}
			
			jsonPayload.put("city", city);
			jsonPayload.put("state", state);

			jsonPayload.put("address", order.getSwsocHomeaddress());
			jsonPayload.put("customerName", name1); // Ejemplo, puedes extraer de order
			jsonPayload.put("customerLastName", name2);
			jsonPayload.put("customerEmail", order.getBusinessPartner().getEEIEmail());
			jsonPayload.put("customerMobile", getLastNineDigits(customerMobile));
			jsonPayload.put("customerCountryCode", "+593");
			jsonPayload.put("sendTrackingLink", true);
			jsonPayload.put("paymentMethod", order.getPaymentMethod().getSspkriPickervalue());
			jsonPayload.put("orderAmount", order.getGrandTotalAmount()); // Ejemplo, puedes extraer el monto de order
			jsonPayload.put("externalBookingId", order.getDocumentNo()); // Ejemplo de ID externo
			String referencia = extraerCampo(order.getDescription(), "\\*REF:\\s*(.+)");
			jsonPayload.put("reference", referencia);
			jsonPayload.put("carName", "BIKE");
			jsonPayload.put("businessDeliveryFee", 0);
			jsonPayload.put("onlyMyFleet", false);
			jsonPayload.put("productValue", order.getSummedLineAmount());
		} catch (Exception e) {
			throw new OBException("Error building JSON payload: " + e.getMessage());
		} finally {
			OBContext.restorePreviousMode();
		}

		return jsonPayload;
	}

	public boolean SendPreCheckout(String apiUrl, JSONObject jsonPayload, String apiKey, Order Ord) {
		HttpURLConnection connection = null;
		OBContext.setAdminMode();
		boolean preCheckOut = false;
//		SspkriPickerlogs pkrLogs = OBProvider.getInstance().get(SspkriPickerlogs.class);
		OBDal.getInstance().refresh(Ord);

		JSONObject jsonResponse = new JSONObject();

		try {
			int responseCode = 500;

			// Crear conexión
			URL url = new URL(apiUrl);
			connection = (HttpURLConnection) url.openConnection();

			// Configurar método y cabeceras
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + apiKey);
			connection.setDoOutput(true);

			// Enviar el JSON
			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonPayload.toString().getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			// Leer la respuesta
			responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				try (Scanner scanner = new Scanner(
						new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

//				try (Scanner scanner = new Scanner(connection.getInputStream(), "UTF-8")) {
					String responseInt = scanner.useDelimiter("\\A").next();
					jsonResponse = new JSONObject(responseInt); // Esto asume que la respuesta es un JSON
					preCheckOut = true;
				}

			} else {
				try (Scanner scanner = new Scanner(connection.getErrorStream(), "UTF-8")) {
					String errorResponse = scanner.useDelimiter("\\A").next();
					logger.error("Error Response: " + errorResponse);
					jsonResponse.put("status", "ERROR");
					jsonResponse.put("message", "Error al recibir el Token. Código de respuesta: " + responseCode);
					jsonResponse.put("errorDetails", errorResponse);
					preCheckOut = false;
				}
			}

		} catch (Exception e) {
			throw new OBException("Error consuming service: " + e.getMessage());
		} finally {
			OBContext.restorePreviousMode();
		}

		return preCheckOut;
	}

	public String SendBooking(String apiUrl, JSONObject jsonPayload, String apiKey, Order Ord, boolean preCheckOut) {
		HttpURLConnection connection = null;
		OBContext.setAdminMode();
		SspkriPickerlogs pkrLogs = OBProvider.getInstance().get(SspkriPickerlogs.class);
		OBDal.getInstance().refresh(Ord);
		JSONObject jsonResponse = new JSONObject();
		JSONObject priceBreakUp = new JSONObject();
		JSONObject mainJsonResponse = new JSONObject();
		String response = "";

		try {

			// Configurar conexión HTTP
			URL url = new URL(apiUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + apiKey);
			connection.setDoOutput(true);

			// Enviar JSON
			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonPayload.toString().getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			// Leer respuesta
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				try (Scanner scanner = new Scanner(
						new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
//				try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
					String responseInt = scanner.useDelimiter("\\A").next();
					mainJsonResponse = new JSONObject(responseInt); // Esto asume que la respuesta es un JSON
					jsonResponse = mainJsonResponse.getJSONObject("data");
					String id = jsonResponse.getString("_id");
					priceBreakUp = jsonResponse.getJSONObject("priceBreakUp");
					double baseFare = priceBreakUp.getDouble("baseFare");
					double perKmCharge = priceBreakUp.getDouble("perKmCharge");
					logger.info("Response: " + responseInt);
					pkrLogs.setResponselog(responseInt);
					pkrLogs.setPickerprice(new BigDecimal(baseFare + perKmCharge));
					pkrLogs.setPickerid(id);
					response = responseInt;
				}
			} else {
				try (Scanner scanner = new Scanner(
						new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
//				try (Scanner scanner = new Scanner(connection.getErrorStream(), StandardCharsets.UTF_8)) {
					String errorResponse = scanner.useDelimiter("\\A").next();
					logger.error("Error Response: " + errorResponse);
					pkrLogs.setResponselog("Error: " + errorResponse);
				}
			}

			// Guardar log en Openbravo
//			pkrLogs.setId(SequenceIdData.getUUID());
			pkrLogs.setClient(Ord.getClient());
			pkrLogs.setOrganization(Ord.getOrganization());
			pkrLogs.setBookingstate("ON_HOLD");
			pkrLogs.setActive(true);
			pkrLogs.setCreatedBy(Ord.getCreatedBy());
			pkrLogs.setCreationDate(Ord.getCreationDate());
			pkrLogs.setUpdated(Ord.getUpdated());
			pkrLogs.setUpdatedBy(Ord.getUpdatedBy());
			pkrLogs.setSalesOrder(Ord);
			pkrLogs.setProcessed(preCheckOut);
			pkrLogs.setJsonlog(jsonPayload.toString());

			// Validar campos obligatorios
			if (pkrLogs.getClient() == null || pkrLogs.getOrganization() == null || pkrLogs.getSalesOrder() == null) {
				throw new OBException("SspkriLogs no está completamente inicializado.");
			}

			// Guardar y confirmar transacción
			OBDal.getInstance().save(pkrLogs);
			OBDal.getInstance().flush();

		} catch (Exception e) {
			throw new OBException("Error consuming service: " + e.getMessage(), e);
		} finally {
			OBContext.restorePreviousMode();
		}

		return response;
	}

	public String getLastNineDigits(String input) {
		if (input == null || input.length() < 9) {
			throw new IllegalArgumentException("La cadena debe tener al menos 9 caracteres.");
		}
		return input.substring(input.length() - 9);
	}

	public String getPkrConfig() {

		// Configuración de conexión a la base de datos
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
		String pkrConfigId = null;

		try {
			String sql = " select max(sspkri_pickerconfig_id) as pkrConfId from sspkri_pickerconfig;";

			st = conn.getPreparedStatement(sql);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				pkrConfigId = rs.getString("pkrConfId");
			}
			rs.close();
			st.close();
		} catch (Exception e) {
			String message = e.getMessage();
		} finally {
			try {
				conn.releasePreparedStatement(st);
				conn.destroy();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}

		return pkrConfigId;
	}

	public static String extraerCampo(String texto, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            return null; // o "" si prefieres una cadena vacía
        }
    }
	
	public String getPkrLog(String pickerId) {

		// Configuración de conexión a la base de datos
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
		String pkrLogId = null;

		try {
			String sql = " select sspkri_pickerlogs_id as logId from sspkri_pickerlogs where pickerid = ?;";

			st = conn.getPreparedStatement(sql);
			st.setString(1, pickerId);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				pkrLogId = rs.getString("logId");
			}
			rs.close();
			st.close();
		} catch (Exception e) {
			String message = e.getMessage();
		} finally {
			try {
				conn.releasePreparedStatement(st);
				conn.destroy();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}

		return pkrLogId;
	}

}
