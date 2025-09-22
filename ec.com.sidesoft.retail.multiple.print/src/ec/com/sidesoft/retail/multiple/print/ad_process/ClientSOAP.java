package ec.com.sidesoft.retail.multiple.print.ad_process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.geography.CountryTrl;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_Impresion;
import ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_ImpresionMultipleLocator;

public class ClientSOAP {
	private static final Logger logger = Logger.getLogger(ClientSOAP.class);

	public void GeneratePrint(Order order, User user) {

		ArrayList<String> lstFactura = new ArrayList<String>();

		OBContext.setAdminMode(true);
		OrganizationType objTypeOrg = null;
		objTypeOrg = OBDal.getInstance().get(OrganizationType.class, "1");

		OBCriteria<Organization> objOrganization = OBDal.getInstance()
				.createCriteria(Organization.class);
		objOrganization.add(Restrictions.eq("organizationType", objTypeOrg));
		List<Organization> lstOrganizationMatriz = objOrganization.list();
		OBContext.restorePreviousMode();

		if (lstOrganizationMatriz.size() == 0) {
			throw new OBException("No existe una organización matriz.");
		}
		if (lstOrganizationMatriz.get(0).getOrganizationInformationList()
				.size() == 0) {
			throw new OBException(
					"No existe información de la organización matriz.");
		}

		lstFactura.add("*");
		// ORGANIZACIÓN (SIMBOLO INICIAL | PARA CENTRAR)
		lstFactura.add("|"
				+ Depurador(lstOrganizationMatriz.get(0).getName().toString()));
		// DIRECCIÓN MATRIZ
		String strPais = null;
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		try {
			OBContext.setAdminMode(true);
			for (CountryTrl countryTrl : lstOrganizationMatriz.get(0)
					.getOrganizationInformationList().get(0)
					.getLocationAddress().getCountry().getCountryTrlList()) {
				if (countryTrl.getLanguage().getLanguage().equals(language)) {
					strPais = countryTrl.getName();
				}
			}
		} catch (NullPointerException e) {
			// throw new
			// OBException("La organización matriz no tiene dirección.");
		} finally {
			OBContext.restorePreviousMode();
		}

		if (strPais == null) {
			strPais = lstOrganizationMatriz.get(0)
					.getOrganizationInformationList().get(0)
					.getLocationAddress().getCountry().getName();
		}

		String strDireccionMatriz = (lstOrganizationMatriz.get(0)
				.getOrganizationInformationList().get(0).getLocationAddress()
				.getAddressLine1() == null ? "" : lstOrganizationMatriz.get(0)
				.getOrganizationInformationList().get(0).getLocationAddress()
				.getAddressLine1()
				+ " ")

				+ (lstOrganizationMatriz.get(0)
						.getOrganizationInformationList().get(0)
						.getLocationAddress().getAddressLine2() == null ? ""
						: lstOrganizationMatriz.get(0)
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getAddressLine2()
								+ " ")

				+ (lstOrganizationMatriz.get(0)
						.getOrganizationInformationList().get(0)
						.getLocationAddress().getPostalCode() == null ? ""
						: lstOrganizationMatriz.get(0)
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getPostalCode()
								+ " ")

				+ (lstOrganizationMatriz.get(0)
						.getOrganizationInformationList().get(0)
						.getLocationAddress().getCityName() == null ? ""
						: lstOrganizationMatriz.get(0)
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getCityName()
								+ " ")

				+ (lstOrganizationMatriz.get(0)
						.getOrganizationInformationList().get(0)
						.getLocationAddress().getRegion() == null ? ""
						: lstOrganizationMatriz.get(0)
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getRegion().getName()
								+ " ") + (strPais == null ? "" : strPais);

		lstFactura.add("MATRIZ: " + Depurador(strDireccionMatriz).toString());
		// DIRECCIÓN DIRECCIÓN SUCURSAL

		String strPais2 = null;
		if (order.getOrganization().getOrganizationInformationList().size() == 0) {
			throw new OBException(
					"No existe información de la organización sucursal.");
		}
		try {
			OBContext.setAdminMode(true);
			for (CountryTrl countryTrl : order.getOrganization()
					.getOrganizationInformationList().get(0)
					.getLocationAddress().getCountry().getCountryTrlList()) {
				if (countryTrl.getLanguage().getLanguage().equals(language)) {
					strPais2 = countryTrl.getName();
				}
			}
		} catch (NullPointerException e) {
			// throw new OBException("La Organización no tiene dirección.");
		} finally {
			OBContext.restorePreviousMode();
		}

		if (strPais2 == null) {
			strPais2 = order.getOrganization().getOrganizationInformationList()
					.get(0).getLocationAddress().getCountry().getName();
		}

		String strDireccionSucursal = (order.getOrganization()
				.getOrganizationInformationList().get(0).getLocationAddress()
				.getAddressLine1() == null ? " " : order.getOrganization()
				.getOrganizationInformationList().get(0).getLocationAddress()
				.getAddressLine1()
				+ " ")

				+ (order.getOrganization().getOrganizationInformationList()
						.get(0).getLocationAddress().getAddressLine2() == null ? ""
						: order.getOrganization()
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getAddressLine2()
								+ " ")

				+ (order.getOrganization().getOrganizationInformationList()
						.get(0).getLocationAddress().getPostalCode() == null ? " "
						: order.getOrganization()
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getPostalCode()
								+ " ")

				+ (order.getOrganization().getOrganizationInformationList()
						.get(0).getLocationAddress().getCityName() == null ? " "
						: order.getOrganization()
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getCityName()
								+ " ")

				+ (order.getOrganization().getOrganizationInformationList()
						.get(0).getLocationAddress().getRegion() == null ? " "
						: order.getOrganization()
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getRegion().getName()
								+ " ") + (strPais == null ? "" : strPais);

		lstFactura.add("SUCURSAL: "
				+ Depurador(strDireccionSucursal).toString());
		// CONTRIBUYENTE ESPECIAL
		lstFactura.add("CONTRIBUYENTE ESPECIAL RES. No: "
				+ Depurador(
						lstOrganizationMatriz.get(0)
								.getOrganizationInformationList().get(0)
								.getDUNS()).trim().toString());
		// OBLIGADO A LLEVAR CONTABILIDAD
		if (lstOrganizationMatriz.get(0).getOrganizationInformationList()
				.get(0).getBusinessPartner() == null) {
			throw new OBException(
					"Tercero no configurado en la organización matriz.");
		}
		String strObligadoConta = "";
		if (lstOrganizationMatriz.get(0).getOrganizationInformationList()
				.get(0).getBusinessPartner().getSSWHTaxpayer()
				.isRequiredaccounting()) {
			strObligadoConta = "SI";
		} else {
			strObligadoConta = "NO";
		}

		lstFactura.add("OBLIGADO A LLEVAR CONTABILIDAD: " + strObligadoConta);
		// RUC
		lstFactura.add("RUC: "
				+ Depurador(
						lstOrganizationMatriz.get(0)
								.getOrganizationInformationList().get(0)
								.getTaxID()).toString());

		// TIPO DOCUMENTO
		lstFactura.add("*");
		String strTipoDocumento = "";

		if (order.getBusinessPartner().isSscmbIsagreement()) {
			strTipoDocumento = "RECIBO DE CAJA";
		} else {
			strTipoDocumento = "REF. FACTURA";
		}
		lstFactura.add(strTipoDocumento);

		OBCriteria<Invoice> objInvoice = OBDal.getInstance().createCriteria(
				Invoice.class);
		objInvoice.add(Restrictions.eq("salesOrder", order));
		List<Invoice> lstInvoice = objInvoice.list();
		String strNumFactura="";
		if (lstInvoice.size() == 0) {
			//throw new OBException("No existe una factura asociada a la orden.");
			strNumFactura="--";
		}
		else{
			strNumFactura = Depurador(lstInvoice.get(0).getDocumentNo()).toString();
		}
		lstFactura.add("No. "+strNumFactura );

		lstFactura.add("DOCUMENTO SIN VALIDEZ TRIBUTARIA");
		lstFactura.add("*");
		// CLIENTE
		lstFactura.add("CLIENTE: "
				+ Depurador(order.getBusinessPartner().getName()).toString());
		// IDENTIFICACIÓN CLIENTE
		lstFactura.add("CI/RUC: "
				+ Depurador(order.getBusinessPartner().getTaxID()).toString());
		// FECHA FACTURA
		SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		lstFactura.add("FECHA: "
				+ Depurador(formatoFecha.format(order.getCreationDate())
						.toString()));
		lstFactura.add("*");
		// NÚMERO DE ÓRDEN (SIMBOLO INICIAL * PARA CENTRAR Y LLENAR LATERALES
		// CON *)
		lstFactura.add("*ORDEN No.: "
				+ Depurador(order.getDocumentNo()).toString());
		lstFactura.add("*");
		lstFactura.add("CANT.       ARTICULO        P/U     V/T");
		lstFactura.add("*");
		DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
		simbolos.setDecimalSeparator('.');
		DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);
		List<OrderLine> objLines = order.getOrderLineList();

		for (int j = 0; j < objLines.size(); j++) {
			lstFactura.add(objLines.get(j).getOrderedQuantity()
					+ " "
					+ truncate(objLines.get(j).getProduct().getName(), 20)
					+ " "
					+ formateador.format(objLines.get(j).getUnitPrice())
							.toString()
					+ "	"
					+ formateador.format(objLines.get(j).getLineNetAmount())
							.toString());
		}
		lstFactura.add("$-----------");
		lstFactura.add("$SUB-TOTAL: "
				+ formateador.format(order.getSummedLineAmount()).toString());
		lstFactura.add("$IVA %: "
				+ formateador.format(getIva(order)).toString());
		lstFactura.add("$===========");
		lstFactura.add("$TOTAL: "
				+ formateador.format(order.getGrandTotalAmount()).toString());
		if (order.getPaymentMethod() == null) {
			throw new OBException("Método de pago no seleccionado.");
		}

		lstFactura.add("FORMA DE PAGO: "
				+ Depurador(order.getPaymentMethod().getName()));
		String strUsuario = "";
		strUsuario = (user.getFirstName() == null ? "" : user.getFirstName())
				+ " " + (user.getLastName() == null ? "" : user.getLastName());
		if (strUsuario.equals(" ")) {
			if (user.getBusinessPartner() != null) {
				strUsuario = user.getBusinessPartner().getName();
			} else {
				strUsuario = "";
			}
		}
		lstFactura.add("CAJERO: " + Depurador(strUsuario));
		lstFactura.add("");
		lstFactura.add("*");

		String strPieFactura = Utility.messageBD(new DalConnectionProvider(),"Srmp_FooterTicket", language);
		String strPieFactura2[] = (strPieFactura == null ? "" : strPieFactura).split(";");

		for (int k = 0; k < strPieFactura2.length; k++) {
			lstFactura.add(Depurador(strPieFactura2[k]));
		}
		lstFactura.add("*");
		lstFactura.add("|CLIENTE");
		lstFactura.add("FONO1: "
				+ Depurador(order.getPartnerAddress().getPhone()) + " FONO2: "
				+ Depurador(order.getPartnerAddress().getAlternativePhone()));
		lstFactura.add(Depurador(order.getBusinessPartner().getName())
				.toString());
		lstFactura.add("DIRECCION ENTREGA:");
		lstFactura.add(Depurador(order.getPartnerAddress().getName())
				.toString());

		String strDescripcion[] = Depurador(order.getDescription()).split("[\n]");

		for (int i = 0; i < strDescripcion.length; i++) {
			lstFactura.add(Depurador(strDescripcion[i]));
		}

		for (int i = 0; i < lstFactura.size(); i++) {
			System.out.println(lstFactura.get(i));
		}

		/*
		 * COMANDA
		 */

		ArrayList<String> lstComanda = new ArrayList<String>();
		lstComanda.add("*");
		lstComanda.add(Depurador("ORDEN DE COCINA PARA VENTA A DOMICILIO"));
		lstComanda.add("*");
		lstComanda.add("*ORDEN No.: "
				+ Depurador(order.getDocumentNo()).toString());
		lstComanda.add("*");
		for (int j = 0; j < objLines.size(); j++) {
			lstComanda.add(objLines.get(j).getOrderedQuantity() + "	"
					+ Depurador(objLines.get(j).getProduct().getName()));
			if (objLines.get(j).getDescription() != null
					&& !objLines.get(j).getDescription().equals("")) {
				lstComanda.add("<<"
						+ Depurador(objLines.get(j).getDescription()
								.replaceAll("[\n]", " - ")) + ">>");
			}
		}
		lstComanda.add("*");
		
		for (int i = 0; i < lstComanda.size(); i++) {
			System.out.println(lstComanda.get(i));
		}
		
		sendPrint(lstFactura, lstComanda, order.getDocumentNo(), order);


		// return null;
	}

	public boolean sendPrint(ArrayList<String> lstFactura,
			ArrayList<String> lstComanda, String strDocumentno, Order order) {

		boolean boolRespuesta;

		String strURLWSOffline = null;
		try {
			strURLWSOffline = SelectParams(order.getOrganization());
			System.out.println("URL WS: " + strURLWSOffline);
			if (strURLWSOffline == null || strURLWSOffline.equals("")) {
				throw new OBException(
						"No se encontró parametrización de WebService de impresión para la organización.");
			}
		} catch (Exception e) {
			throw new OBException(
					"Error al obtener la parametrización de WebService de Impresión. "
							+ e.getMessage());
		}

		try {

			String[] lstFacturaStr = new String[lstFactura.size()];
			lstFactura.toArray(lstFacturaStr);

			String[] lstComandaStr = new String[lstComanda.size()];
			lstComanda.toArray(lstComandaStr);

			WS_ImpresionMultipleLocator wsLocator = new WS_ImpresionMultipleLocator();
			wsLocator.setUrl(strURLWSOffline);
			WS_Impresion wsRecepcionPort = wsLocator.getWS_ImpresionPort();

			boolRespuesta = wsRecepcionPort.impresion_multiple(lstFacturaStr,
					lstComandaStr, strDocumentno);

			System.out.println("RESPUESTA DE WS " + strDocumentno + " :"
					+ boolRespuesta);
		} catch (Exception e) {
			throw new OBException(e.getMessage());
		}

		return boolRespuesta;
	}

	public static String Depurador(String strTexto) {

		final String ORIGINAL = "ÁáÉéÍíÓóÚúÑñÜü";
		final String REEMPLAZO = "AaEeIiOoUuNnUu";

		if (strTexto == null) {
			return "";
		}
		char[] array = strTexto.toCharArray();
		for (int indice = 0; indice < array.length; indice++) {
			int pos = ORIGINAL.indexOf(array[indice]);
			if (pos > -1) {
				array[indice] = REEMPLAZO.charAt(pos);
			}
		}
		return new String(array);
	}

	public static double getIva(Order order) {
		Double dblIva = 0.00;
		ConnectionProvider conn = new DalConnectionProvider(false);
		try {
			String strSql = "SELECT coalesce(il.taxamt,0) as iva FROM  c_ordertax il INNER JOIN c_tax ct on ct.c_tax_id = il.c_tax_id WHERE ct.istaxdeductable = 'Y' AND ct.rate <> 0 AND il.c_order_id= ?";
			PreparedStatement st = null;
			st = conn.getPreparedStatement(strSql);
			st.setString(1, order.getId());
			ResultSet rsConsulta = st.executeQuery();

			while (rsConsulta.next()) {
				dblIva = rsConsulta.getDouble("iva");
			}

			return dblIva;

		} catch (Exception e) {

			throw new OBException("Error al consultar la tabla c_ordertax. "
					+ e.getMessage());
		} finally {
			try {
				conn.destroy();
			} catch (Exception e) {

			}
		}

	}

	public static String truncate(String value, int length) {

		if (value == null || value.equals("")) {
			return null;
		} else {
			if (value.length() > length) {
				return value.substring(0, length);
			} else {
				return value;
			}
		}
	}

	public static String SelectParams(Organization organization) {
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			String strSql = "SELECT webservice_url  FROM srmp_printsettings where isactive='Y' and ad_org_id=?";
			PreparedStatement st = null;
			String strParametro = null;
			st = conn.getPreparedStatement(strSql);
			st.setString(1, organization.getId());
			ResultSet rsConsulta = st.executeQuery();
			int contador = 0;
			while (rsConsulta.next()) {
				contador = contador + 1;
				strParametro = rsConsulta.getString("webservice_url");
			}
			if (contador == 0) {
				throw new OBException(
						"No se encontró parametrización de url de WebService de impresión.");
			} else if (contador > 1) {

				throw new OBException(
						"Existe más de una parametrización de WebService de impresión activa.");
			}
			return strParametro;
		} catch (Exception e) {

			throw new OBException(
					"Error al consultar la tabla srmp_printsettings (Parámetro WS de impresión) "
							+ e.getMessage());
		} finally {
			try {
				conn.destroy();
			} catch (Exception e) {

			}
		}

	}

}
