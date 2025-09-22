package ec.com.sidesoft.integration.pedidosya.ad_process;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
//import java.util.Base64;
import org.apache.commons.codec.binary.Base64;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.FormatUtilities;

import ec.com.sidesoft.integration.pedidosya.SsipyJustoconfig;
import ec.com.sidesoft.integration.pedidosya.webservices.webhooks.ProcessWebHook;

public class CallProcessOrderWS {

    private final Logger logger = Logger.getLogger(ProcessWebHook.class);

    public String getOrderProcessResponse(String jdata) throws Exception {
        JSONObject JSONobj = new JSONObject(jdata);
        SsipyJustoconfig justolog = null;

        
        justolog = getJustoConfig(JSONobj.getString("origin"));
        
        if (justolog == null) {
            throw new OBException("No configuration found");
        }

        String apiToken = justolog.getSsipyApikey();
        String urlop = justolog.getSsipyOrderurl();
        int responseCode;

        if (apiToken != null) {
            try {
                URL url = new URL(urlop);
                HttpURLConnection conec = (HttpURLConnection) url.openConnection();
                conec.setRequestMethod("POST");
                conec.setRequestProperty("Content-Type", "application/json; utf-8");
                conec.setRequestProperty("Accept", "application/json");

                // Configurar Basic Auth
                String username = justolog.getSsipyUsername();
                String prepass = justolog.getSsipyPassword();
	            String password = FormatUtilities.encryptDecrypt(prepass, false);
                String auth = username + ":" + password;
//                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                String encodedAuth = Base64.encodeBase64String(auth.getBytes());
                conec.setRequestProperty("Authorization", "Basic " + encodedAuth);

                conec.setDoOutput(true);

                // Enviar el JSON
                try (OutputStream os = conec.getOutputStream()) {
                    byte[] input = JSONobj.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Leer la respuesta
                responseCode = conec.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (Scanner scanner = new Scanner(conec.getInputStream(), "UTF-8")) {
                        String response = scanner.useDelimiter("\\A").next();
                        JSONObject jsonResponse = new JSONObject(response);
                        return jsonResponse.toString();
                    }
                } else {
                    throw new OBException("Error receiving the response: " + responseCode);
                }

            } catch (Exception e) {
                logger.error("Error in API call: " + e.getMessage(), e);
                throw new OBException("Error in API call", e);
            }
        } else {
            throw new OBException("API token is missing");
        }
    }
    
    public SsipyJustoconfig getJustoConfig(String brand) throws Exception {
		PreparedStatement st = null;
		ResultSet rs = null;
		SsipyJustoconfig justolog = null;
		ConnectionProvider connec = new DalConnectionProvider(false);
		String confId = null;

		try {
			String sql = " SELECT max(ssipy_justoconfig_id) as jusconfigid FROM ssipy_justoconfig"
					+ " WHERE isactive = 'Y'"
					+ " AND ssipy_brand = ?;";

			st = connec.getPreparedStatement(sql);
			st.setString(1, brand);
			rs = st.executeQuery();
			
			if (rs.next()) {
                confId = rs.getString("jusconfigid");
            }
			
		} catch (Exception e) {
			String message = e.getMessage();
		} finally {
			if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
		}
		
		if (confId != null) {
            justolog = OBDal.getInstance().get(SsipyJustoconfig.class, confId);
        }
		//
		return justolog;
	}
}
