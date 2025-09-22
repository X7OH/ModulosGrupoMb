package ec.com.sidesoft.integration.pedidosya.webservices.webhooks;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;


import ec.com.sidesoft.integration.pedidosya.ad_process.ProcessJustoLogs;
import ec.com.sidesoft.integration.pedidosya.webservices.util.SignatureVerifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/ec.com.sidesoft.integration.pedidosya.webservices.webhooks.WebHookController")
public class WebHookController extends HttpServlet {

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		VariablesSecureApp vars = new VariablesSecureApp(request);
        StringBuilder body = new StringBuilder();
        
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        ProcessWebHook processdata = new ProcessWebHook();
        String newJson = new String("");
        String rev = new String("");
        
        
        try {
			
        	newJson = processdata.buildJSON(body.toString());
        	JSONObject nJson = new JSONObject(newJson);
        	
        	nJson.put("origin", "MF");
        	newJson = nJson.toString();
        	
        	try {
                rev = processdata.getNotifyResponse(newJson, "MF");
                JSONObject nRev = new JSONObject(rev);
                
                if(nRev.get("status").equals("ERROR")) {
                	throw new OBException("Error: " + nRev.get("message"));
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error processing the request: " + e.getMessage());
                return;
            }

            // Successfully processed the webhook
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            out.print("{\"message\": \"Data recibida\"}");
            out.flush();
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Failed to build JSON from data: " + e.getMessage());
        }
        
        
        
    }
	
	
	
}
