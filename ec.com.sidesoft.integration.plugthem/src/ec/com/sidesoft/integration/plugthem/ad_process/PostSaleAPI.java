package ec.com.sidesoft.integration.plugthem.ad_process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;

import ec.com.sidesoft.integration.plugthem.SsplugPostSaleLogs;
import ec.com.sidesoft.integration.plugthem.SsplugPostSaleSettings;

public class PostSaleAPI {

	private static final Logger log4j = Logger.getLogger(PostSaleAPI.class);

	public static void consumeServiceWS(Invoice objInvoice,
			SsplugPostSaleSettings objSettings) throws JSONException {

		log4j.debug("Estructurando JSON.");
		JSONObject jsonInvoice = new JSONObject();

		jsonInvoice.put("BrandId", objSettings.getBrandid());
		jsonInvoice.put("SiteId", objInvoice.getOrganization().getSearchKey());
		jsonInvoice.put("SiteName", objInvoice.getOrganization().getName());
		jsonInvoice.put("GroupId", objSettings.getGroupid());

		String strEmpId = "", strEmpName = "";
		if (objInvoice.getCreatedBy().getBusinessPartner() == null) {
			strEmpId = objSettings.getBpartner().getTaxID();
			strEmpName = objSettings.getBpartner().getName();
		} else {
			strEmpId = objInvoice.getCreatedBy().getBusinessPartner()
					.getTaxID();
			strEmpName = objInvoice.getCreatedBy().getBusinessPartner()
					.getName();
		}
		jsonInvoice.put("EmpId", strEmpId);

		jsonInvoice.put("EmpName", strEmpName);
		jsonInvoice.put("CustomerDoc", objInvoice.getBusinessPartner()
				.getTaxID());
		jsonInvoice.put("CustomerName", objInvoice.getBusinessPartner()
				.getName());

		String strPhone = "";
		if (objInvoice.getPartnerAddress().getPhone() == null) {
			strPhone = objSettings.getDefaultPhone();
		} else {
			strPhone = objInvoice.getPartnerAddress().getPhone();
		}
		jsonInvoice.put("CustomerMobile", strPhone);

		jsonInvoice.put("CustomerEmail", objInvoice.getBusinessPartner()
				.getEEIEmail());

		Map objCustom = new LinkedHashMap(4);
		SimpleDateFormat ecFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		objCustom.put("fecha_factura",
				ecFormat.format(objInvoice.getInvoiceDate()));
		objCustom.put("factura", objInvoice.getDocumentNo());
		objCustom.put("personalizacion_cliente", objInvoice
				.getBusinessPartner().getName());
		objCustom.put("personalizacion_asesor", strEmpName);

		jsonInvoice.put("custom", objCustom);

		String input = jsonInvoice.toString();
		log4j.debug("JSON a enviar: " + input);

		try {

			URL url = new URL(objSettings.getServiceUrl());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();

			String strFinalOutput = null,strOutput = null;
			if (conn.getResponseCode() != 200) {
				InputStream is = conn.getErrorStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				int counter = 0;
				while ((strOutput = br.readLine()) != null) {
					counter++;
					log4j.debug("Respuesta: "
							+ (strOutput == null ? "" : strOutput));
					if (strOutput.startsWith("{")==false && counter == 1) {
						strFinalOutput ="Error en la respuesta del servicio." ;
						break;
					}else{
						strFinalOutput = strFinalOutput+strOutput;
					}
				}
				if (counter==0){
					strFinalOutput ="Sin respuesta del servicio" ;
				}
				saveLogs2(input, strFinalOutput, "E", objInvoice, objSettings);
			} else {
				InputStreamReader in = new InputStreamReader(
						conn.getInputStream());
				BufferedReader br = new BufferedReader(in);
				while ((strFinalOutput = br.readLine()) != null) {
					log4j.debug("Respuesta: "
							+ (strFinalOutput == null ? "" : strFinalOutput));
				}
				objInvoice.setSsplugSurveySended(true);
				OBDal.getInstance().save(objInvoice);
				log4j.debug("Factura actualizada.");
			}
			conn.disconnect();

		} catch (MalformedURLException e) {
			throw new  OBException(e.getMessage()); 
		} catch (IOException e) {
			
			throw new  OBException(e.getMessage()); 
		}

	}

	private static void saveLogs2(String sendedJSON, String answerJSON,
			String status, Invoice objInvoice,
			SsplugPostSaleSettings objSettings) {

		log4j.debug("Guardando log.");
		SsplugPostSaleLogs objLog = OBProvider.getInstance().get(
				SsplugPostSaleLogs.class);

		objLog.setSendedJson(sendedJSON);
		objLog.setAnswerJson(answerJSON);
		objLog.setStatus(status);
		objLog.setDocumentno(objInvoice.getDocumentNo());

		OBDal.getInstance().save(objLog);
		log4j.debug("Log guardado.");
	}
}
