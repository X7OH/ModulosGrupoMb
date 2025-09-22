package ec.com.sidesoft.integration.picker.ad_WebHooks;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import ec.com.sidesoft.integration.picker.SspkriPickerlogs;
import ec.com.sidesoft.integration.picker.ad_process.pickerMain;

@WebServlet("/ec.com.sidesoft.integration.picker.ad_WebHooks.UpdateWebHook")
public class UpdateWebHook extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(UpdateWebHook.class);

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		StringBuilder body = new StringBuilder();
		pickerMain pkrProcess = new pickerMain();
		OBContext.setAdminMode();

		// Leer el cuerpo de la solicitud
		try (BufferedReader reader = request.getReader()) {
			String line;
			while ((line = reader.readLine()) != null) {
				body.append(line);
			}
		}

		logger.info("Received WebHook JSON: " + body.toString());

		try {
			// Convertir el cuerpo de la solicitud en un objeto JSON
			JSONObject jsonRequest = new JSONObject(body.toString());

			// Validar campos obligatorios
			if (!jsonRequest.has("type") || !jsonRequest.has("bookingID") || !jsonRequest.has("currentStatus")) {
				throw new JSONException("Faltan campos obligatorios en el JSON: type, bookingID o currentStatus");
			}

			// Extraer valores del JSON
			String bookingID = jsonRequest.getString("bookingID");
			String statusText = jsonRequest.optString("statusText", ""); // Campo opcional

			// Responder al cliente con éxito
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");

			SspkriPickerlogs pkrLog = OBDal.getInstance().get(SspkriPickerlogs.class, pkrProcess.getPkrLog(bookingID));
			pkrLog.setBookingstate(statusText);
			OBDal.getInstance().save(pkrLog);
			OBDal.getInstance().flush();

		} catch (JSONException e) {
			// Manejar errores de JSON
			logger.error("Error procesando el JSON recibido: ", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json");
//            PrintWriter out = response.getWriter();
//            out.print("{\"error\": \"JSON inválido: " + e.getMessage() + "\"}");
//            out.flush();
		} catch (Exception e) {
			// Manejar otros errores
			logger.error("Error procesando el WebHook: ", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("application/json");
//            PrintWriter out = response.getWriter();
//            out.print("{\"error\": \"Error interno: " + e.getMessage() + "\"}");
//            out.flush();
		} finally {
			OBContext.restorePreviousMode();
		}
	}

}
