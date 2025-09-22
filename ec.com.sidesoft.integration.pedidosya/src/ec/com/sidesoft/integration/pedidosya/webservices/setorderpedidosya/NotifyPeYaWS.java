package ec.com.sidesoft.integration.pedidosya.webservices.setorderpedidosya;

import java.io.InputStreamReader;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ec.com.sidesoft.integration.pedidosya.SsipyJustologs;
import ec.com.sidesoft.integration.pedidosya.ad_process.CallProcessOrderWS;
import ec.com.sidesoft.integration.pedidosya.ad_process.ProcessJustoLogs;
import ec.com.sidesoft.integration.pedidosya.webservices.util.ResponseWS;

public class NotifyPeYaWS implements WebService {

	ProcessJustoLogs JustoLog = new ProcessJustoLogs();
	private static final Logger logger = Logger.getLogger(NotifyPeYaWS.class);
	private static final long serialVersionUID = 1L;
	private static final ConnectionProvider connectionProvider = new DalConnectionProvider(false);
	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {

			JsonElement element = new JsonParser().parse(new InputStreamReader(request.getInputStream()));
			JsonObject dataPedido = element.getAsJsonObject();
			System.out.println("DATA:" + dataPedido);
			ResponseWS responseWS = insertSalesOrder(dataPedido);
			CallProcessOrderWS ProOrdWS = new CallProcessOrderWS();
			String orderProcess = ProOrdWS.getOrderProcessResponse(dataPedido.toString());
			
			final String json = getResponse(responseWS);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			
			if ("OK".equals(responseWS.getStatus())) {
	            response.setStatus(HttpServletResponse.SC_OK); // 200 OK
	        } else {
	            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
	        }
			
			final Writer w = response.getWriter();
			w.write(json);
			w.close();
		}catch (Exception e) {
	        logger.error("Error al procesar la solicitud: " + e.getMessage(), e);
	        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 en caso de excepción
	        response.setContentType("application/json");
	        response.setCharacterEncoding("UTF-8");
	        final Writer w = response.getWriter();
	        w.write("{\"status\":\"ERROR\",\"message\":\"Error al procesar la solicitud\"}");
	        w.close();
	    }

		
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public ResponseWS insertSalesOrder(JsonObject datapedido) throws Exception {
		
		ResponseWS responseWS = new ResponseWS();
		
		String ok = JustoLog.registerLog(datapedido.toString());
		if(ok != null && !ok.isEmpty()) {
			responseWS.setDocumentNo("Pedido procesado con exito");
		    responseWS.setStatus("OK");
		    responseWS.setMessage("Insertado");
		}else {
			responseWS.setDocumentNo("Error al procesado el pedido");
		    responseWS.setStatus("ERROR");
		    responseWS.setMessage("Error");
		}
		return responseWS;
	}
	
	private String getResponse(ResponseWS response) {
	    Gson gson = new Gson();
	    String json = gson.toJson(response);
	    return json;
	}
}
