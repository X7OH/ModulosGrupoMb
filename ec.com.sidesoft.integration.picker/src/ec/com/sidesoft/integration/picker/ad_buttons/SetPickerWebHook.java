package ec.com.sidesoft.integration.picker.ad_buttons;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

import ec.com.sidesoft.integration.picker.SspkriPickerWebhook;
import ec.com.sidesoft.integration.picker.SspkriPickerconfig;

public class SetPickerWebHook extends DalBaseProcess {
	private final Logger logger = Logger.getLogger(SetPickerWebHook.class);

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub
		OBError msg = new OBError();

		OBContext.setAdminMode(true);
		
		final String recordId = (String) bundle.getParams().get("Sspkri_Pickerwebhook_ID");
		
		try {
			SspkriPickerWebhook WebHooks = OBDal.getInstance().get(SspkriPickerWebhook.class, recordId);
			
			JSONObject json = buildJsonPayload(WebHooks.getTagtype(), WebHooks.getWebhookurl());
			
			SspkriPickerconfig PickerConfig = OBDal.getInstance().get(SspkriPickerconfig.class, getPkrConfig());
			
			msg = SendWHConfigs(PickerConfig.getUrlwebhook(), json, WebHooks.getStoreid());
			
		} catch (Exception e) {
			OBDal.getInstance().rollbackAndClose();
			logger.error("Exception found in Sbc_Reactivate: ", e);
			Throwable throwable = DbUtility.getUnderlyingSQLException(e);
			String message = OBMessageUtils.translateError(throwable.getMessage()).getMessage();
			msg.setTitle(OBMessageUtils.messageBD("Error"));
			msg.setType("Error");
			msg.setMessage(message);
		}finally {
			OBContext.setAdminMode(false);
			bundle.setResult(msg);
		}
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
	
	public OBError SendWHConfigs(String apiUrl, JSONObject json, String tokenKey) {
        // URL del servicio
		HttpURLConnection connection = null;

		try {	
			// Crear conexión
			URL url = new URL(apiUrl);
			connection = (HttpURLConnection) url.openConnection();

            // Configurar método y encabezados
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Language", "en");
            connection.setRequestProperty("Authorization", "Bearer "+tokenKey);
            connection.setDoOutput(true);

         // Enviar el JSON
         			try (OutputStream os = connection.getOutputStream()) {
         				byte[] input = json.toString().getBytes("utf-8");
         				os.write(input, 0, input.length);
         			}

            // Leer la respuesta
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            OBError message = new OBError();

            if (responseCode == HttpURLConnection.HTTP_OK) {
            	// Crear un mensaje satisfactorio
                message.setType("Success"); // Tipo de mensaje: Success, Warning, Error
                message.setTitle(OBMessageUtils.messageBD("Success")); // Título del mensaje
                message.setMessage("Webhook registrado correctamente."); // Mensaje personalizado
            } else {
            	// Manejo de errores
            	message.setType("Error");
                message.setTitle(OBMessageUtils.messageBD("Error"));
                message.setMessage("Ha ocurrido un error al registrar el WebHook");
                
            }
            
            return message;

        } catch (Exception e) {
        	OBError message = new OBError();
        	message.setType("Error");
            message.setTitle(OBMessageUtils.messageBD("Error"));
            message.setMessage("Ha ocurrido un error: " + e.getMessage());
            e.printStackTrace();
            
            return message;
        }
    }
	
	public JSONObject buildJsonPayload(String Type, String WebHook) {
		JSONObject jsonPayload = new JSONObject();

		try {
			
			jsonPayload.put("type", Type); // Puedes reemplazar con																								// datos dinámicos
			jsonPayload.put("url", WebHook); // Puedes reemplazar con
																								// datos dinámicos
		} catch (Exception e) {
			throw new OBException("Error building AddWebHook JSON: " + e.getMessage());
		}

		return jsonPayload;
	}

}
