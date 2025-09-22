package ec.com.sidesoft.integration.peya.webservices.setorderpeya;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
//import java.time.ZoneOffset;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderLineOffer;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.retail.discounts.combo.ComboProduct;
import org.openbravo.retail.discounts.combo.ComboProductFamily;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.web.WebService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ec.com.sidesoft.integration.delivery.SDELVRConfig;
import ec.com.sidesoft.integration.delivery.SDELVR_Utility;
import ec.com.sidesoft.integration.peya.speyaConfig;
import ec.com.sidesoft.integration.peya.helpers.SPEYA_Helper;
import ec.com.sidesoft.integration.peya.webservices.util.ResponseWS;

@AuthenticationManager.Stateless
public class OrderPeyaWS implements WebService {

	private static final Logger logger = Logger.getLogger(OrderPeyaWS.class);
	private static final long serialVersionUID = 1L;
	private static final ConnectionProvider connectionProvider = new DalConnectionProvider(false);
	private static SDELVRConfig utilConfig = null;
	private static Organization org = null;
	private static BusinessPartner bpByDefault = null;
	private static SDELVR_Utility utilityDelivery = new SDELVR_Utility();
	private static String descriptionPeyaLog = null;
	private static String orderJsonString = "";
	private static JsonArray attrArr = new JsonArray();
	private String adtLineNote = "";

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception, AuthenticationException {

		JsonElement element = new JsonParser().parse(new InputStreamReader(request.getInputStream()));
		JsonObject dataPedido = element.getAsJsonObject();
		ResponseWS responseWS = new ResponseWS();

		if (dataPedido.has("token") && !dataPedido.get("token").isJsonNull()) {
			String OrderToken = dataPedido.get("token").getAsString();
			String JSOrderID = dataPedido.get("code").getAsString();
			System.out.println("DATA:" + dataPedido);

			JsonObject CallBackUrls = dataPedido.getAsJsonObject("callbackUrls");
			String AcceptedURL = CallBackUrls.get("orderAcceptedUrl").getAsString();

			// Obtener Authorizacion
			String AuthToken = obtenerToken();
			String OrderRespons = actualizarEstadoOrden(OrderToken, JSOrderID, AuthToken, AcceptedURL);

			if (OrderRespons.equals("OK")) {
				responseWS = insertSalesOrder(dataPedido, JSOrderID);
			} else {
				responseWS.setDocumentNo("N/A");
				responseWS.setMessage("Error");
				responseWS.setStatus("ERROR");
			}
		} else {
			responseWS = insertSalesOrder(dataPedido);
		}

		final String json = getResponse(responseWS);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final Writer w = response.getWriter();
		w.write(json);
		w.close();
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
	}

	@Override
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
	}

	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
	}

	public ResponseWS insertSalesOrder(JsonObject dataPedido, String JSOrderID) throws JSONException {

		// Obetener los CallBackURL
		JsonObject CallBackUrls = dataPedido.getAsJsonObject("callbackUrls");
		String AcceptedURL = CallBackUrls.get("orderAcceptedUrl").getAsString();
		String RejectedURL = CallBackUrls.get("orderRejectedUrl").getAsString();
		String PreparedURL = CallBackUrls.get("orderPreparedUrl").getAsString();
		String PickedUpURL = null;
		if (CallBackUrls.has("orderPickedUpUrl") && !CallBackUrls.get("orderPickedUpUrl").isJsonNull()) {
			PickedUpURL = CallBackUrls.get("orderPickedUpUrl").getAsString();
		}
		String documentNo = null;
		ResponseWS responseWS = new ResponseWS();

		try {

			// OBTENGO STORE_ID DEL JSON ENVIADO
			JsonObject restaurant = dataPedido.get("restaurant").getAsJsonObject();
			String storeId = null;
			storeId = restaurant.get("id").getAsString().trim();
			if (storeId == null || storeId.equals("")) {
				throw new OBException("El campo organizacion no debe estar vacio");
			}

			// CONFIGURACION DEL STORE_ID DE LA CONFIGURACION
			utilConfig = SPEYA_Helper.getConfig(storeId);
			if (utilConfig == null) {
				throw new OBException(
						"No existe una configuracion definida para la tienda con identificador  " + storeId);
			}

			// CONVERSION A STRING DEL JSON ENVIADO - PARA GUARDAR EN EL LOG
			orderJsonString = dataPedido.toString();

			// CREANDO INSTANCIA DEL PEDIDO A INSERTAR
			Order ordOB = OBProvider.getInstance().get(Order.class);
			ordOB.setActive(true);
			ordOB.setSalesTransaction(true);

			// ORGANIZACION
			org = utilConfig.getOrganization();
			if (org == null) {
				throw new OBException("No existe la organizacion");
			}

			ordOB.setClient(org.getClient());
			ordOB.setOrganization(org);

			// DESCRIPCION PEDIDO EXITO PARA EL LOG
			descriptionPeyaLog = "El pedido de venta fue creado exitosamente";

			// USUARIO QUE CREA Y MODIFICA EL PEDIDO
			User usr = utilConfig.getUserContact();
			ordOB.setCreatedBy(usr);
			ordOB.setUpdatedBy(usr);

			// TIPO DE DOCUMENTO DEL PEDIDO
			DocumentType docType = utilConfig.getDocumentType();
			ordOB.setDocumentType(docType);
			ordOB.setTransactionDocument(docType);

			// FECHA REGISTRO - FECHA CONTABLE
			Date newInvoiceDate = new Date();
			ordOB.setOrderDate(newInvoiceDate);
			ordOB.setAccountingDate(newInvoiceDate);

			// NUEVOS CAMPOS
			ordOB.setScheduledDeliveryDate(newInvoiceDate);
			ordOB.setPrintDiscount(true);
			ordOB.setFormOfPayment("P");

			// NUEVOS CAMPOS PARA RECUPERAR EL PEDIDO DE VENTA EN EL POS
			ordOB.setObposApplications(utilConfig.getObposApplications());
			ordOB.setPrint(false);
			ordOB.setObposIslayaway(true);
			ordOB.setObposCreatedabsolute(newInvoiceDate);

			// NUMERO DE DOCUMENTO DEL PEDIDO
			documentNo = Utility.getDocumentNo(OBDal.getInstance().getConnection(false), new DalConnectionProvider(),
					RequestContext.get().getVariablesSecureApp(), "", Order.TABLE_NAME, docType.getId(),
					docType.getId(), false, true);
			ordOB.setDocumentNo(documentNo);

			// ESTATUS DEL PEDIDO POR DEFECTO
			ordOB.setDocumentStatus("DR");
			ordOB.setDocumentAction("--");
			ordOB.setProcessed(false);
			ordOB.setProcessNow(false);

			// METODO DE PAGO DEL PEDIDO
			FIN_PaymentMethod payMet = null;
			payMet = utilConfig.getSpeyaPaymentmeth();
			if (payMet == null) {
				throw new OBException("No existe un metodo de pago asignado a la Configuracion Pedido YA.");
			}
			ordOB.setPaymentMethod(payMet);

			// INFORMACION DEL CLIENTE PARA LA FACTURACION
			String strNombreContactoEntrega = "";
			String strDireccion = "";
			String strIdCustomer = "";
			String strNameCustomer = "";
			String strPhoneCustomer = ".";
			String strDireccionReferencia = "--";
			String strEmailCustomer = utilConfig.getDefaultEmail();

			// TERCERO
			BusinessPartner bp = null;
			BusinessPartner bpOB = null;
			// TERCERO POR DEFECTO
			bpByDefault = utilConfig.getBpartner();
			bp = bpByDefault;

			JsonObject customer = dataPedido.get("user").getAsJsonObject();
//			JsonObject company = customer.get("company").getAsJsonObject();
			JsonObject company = customer.has("company") && !customer.get("company").isJsonNull()
					? customer.get("company").getAsJsonObject()
					: new JsonObject();

			JsonObject customerInfo = dataPedido.get("address").getAsJsonObject();

			if ((customer.get("identityCard").isJsonNull() || customer.get("identityCard").getAsString().equals(""))
					&& (company.get("document").isJsonNull() || company.get("document").getAsString().equals(""))) {

				// NO EXISTEN DATOS DE FACTURACION - TERCERO POR DEFECTO CONSUMIDOR FINAL
				bp = bpByDefault;
				descriptionPeyaLog = "El pedido de venta fue creado exitosamente. Sin datos de Facturacion. Tercero: Consumidor Final";

			} else {

				strDireccion = customerInfo.get("description").getAsString().trim();
				if (!customer.get("identityCard").isJsonNull()
						&& !customer.get("identityCard").getAsString().equals("")) {
					strIdCustomer = customer.get("identityCard").getAsString().trim();
					strNombreContactoEntrega = customer.get("name").getAsString().trim() + " "
							+ customer.get("lastName").getAsString().trim();
				} else if (!company.get("document").isJsonNull() && !company.get("document").getAsString().equals("")) {
					strIdCustomer = company.get("document").getAsString().trim();
					strNombreContactoEntrega = company.get("name").getAsString().trim();
				}
				strNameCustomer = strNombreContactoEntrega;
				strEmailCustomer = customer.get("email").getAsString().trim();
				strPhoneCustomer = customerInfo.get("phone").getAsString().trim();
				strDireccionReferencia = customerInfo.get("notes").getAsString().trim();
				if (strIdCustomer == null || strIdCustomer.equals("")) {
					throw new OBException("El campo Id del cliente no puede estar vacio");
				}

				bp = getBpartner(strIdCustomer);
				if (bp == null) {

					Boolean createValidate = utilityDelivery.validateIdentification(strIdCustomer);
					if (createValidate) {

						// SE CREA UN TERCERO SI NO EXISTE EN OB Y TAMPOCO ES CONSUMIDOR FINAL
						bpOB = createBpartner(strIdCustomer, strDireccion, strNameCustomer, strPhoneCustomer,
								strDireccionReferencia, strEmailCustomer, payMet);
						if (bpOB == null) {
							throw new OBException("No existe un tercero con el codigo tax id: " + strIdCustomer);
						} else {
							bp = bpOB;
							descriptionPeyaLog = "El pedido de venta fue creado exitosamente. PEDIDOS YA - Tercero Creado: "
									+ bp.getName();
						}

					} else {

						// TAX ID INVALIDO
						bp = bpByDefault;
						descriptionPeyaLog = "El pedido de venta fue creado exitosamente. TAXID Invalido. Tercero: Consumidor Final";

					}

				}

			}

			// SE ASIGNA EL TERCERO AL PEDIDO
			ordOB.setBusinessPartner(bp);

			// DESCRIPCION DEL PEDIDO - ALERGIAS
			String strDescInfo = "";
			strNombreContactoEntrega = customer.get("name").getAsString().trim() + " "
					+ customer.get("lastName").getAsString().trim();
			strDescInfo = "CON: " + strNombreContactoEntrega + "\n" + "NOT: " + dataPedido.get("notes").getAsString()
					+ "\n" + "OBS: " + "PEDIDOSYA ORDER CODE " + dataPedido.get("id").getAsString() + "\n" + "PAG: "
					+ "PEDIDOSYA - " + payMet.getName();
			ordOB.setDescription(strDescInfo);

			// DIRECCION DEL TERCERO
			Location bpAddress = getBpAddress(bp.getBusinessPartnerLocationList());
			if (bpAddress == null) {
				throw new OBException("El tercero " + bp.getName() + " no tiene definida una direccion");
			}
			ordOB.setPartnerAddress(bpAddress);
			ordOB.setInvoiceAddress(bpAddress);

			// MONEDA DEL PEDIDO
			Currency currency = null;
			currency = utilConfig.getCurrency();
			ordOB.setCurrency(currency);

			// TERMINO DE PAGO
			PaymentTerm payTerms = utilConfig.getPaymentTerms();
			if (payTerms == null) {
				throw new OBException("Debe agregar el termino de pago en la configuracion");
			}
			ordOB.setPaymentTerms(payTerms);

			// LISTA DE PRECIOS
			PriceList priceList = utilConfig.getPriceList();
			if (priceList == null) {
				throw new OBException("Debe configurar una price list");
			}
			ordOB.setPriceList(priceList);

			// AlMACEN
			Warehouse warehouse = utilConfig.getWarehouse();
			if (warehouse == null) {
				throw new OBException("Debe agregar en la configuracion el almacen por defecto");
			}
			ordOB.setWarehouse(warehouse);

			// ORIGEN DE LA VENTA PEDIDO YA
			ordOB.setSscmbSalesOrigin("SPEYA_PEYA");

			// GUARDANDO EL PEDIDO
			OBDal.getInstance().save(ordOB);

			// GUARDANDO LINEAS PEDIDOSYA
			JsonArray lineasPedidoPeya = dataPedido.get("details").getAsJsonArray();
			if (lineasPedidoPeya.size() > 0) {

				long numLine = 10;
				for (JsonElement lineaPeya : lineasPedidoPeya) {

					JsonObject lineaObjPeya = lineaPeya.getAsJsonObject();
					System.out.println("DETALLES DE ORDEN: " + lineaObjPeya);
					JsonObject productInfo = lineaObjPeya.get("product").getAsJsonObject();
					System.out.println(productInfo.get("integrationCode").getAsString());

					String strIdOB = productInfo.get("integrationCode").getAsString().trim();
					if (strIdOB == null || strIdOB.equals("")) {
						throw new OBException("El campo integrationCode no puede estar vacio");
					}

					// JsonArray optionGroups = lineaObjPeya.get("optionGroups").getAsJsonArray();
					JsonArray optionGroups = lineaObjPeya.has("optionGroups")
							&& !lineaObjPeya.get("optionGroups").isJsonNull()
									? lineaObjPeya.get("optionGroups").getAsJsonArray()
									: new JsonArray();

					HashMap<Product, JSONObject> retrievedObject = getProduct(strIdOB, optionGroups, false);

					Set set = retrievedObject.entrySet();
					Iterator iterator = set.iterator();
					while (iterator.hasNext()) {
						Map.Entry mentry = (Map.Entry) iterator.next();
						Product product = (Product) mentry.getKey();
						JSONObject values = (JSONObject) mentry.getValue();
						Boolean comboFamMain = values.getBoolean("comboFamMain");
						Boolean isCombo = values.getBoolean("isCombo");
						String mOfferID = values.getString("m_offer_id");
						Long famlilyQuantity = values.getLong("famlily_quantity");
						String price = values.getString("additional_price");
						boolean printNotes = values.getBoolean("printAddNotes");
						Boolean is_attribute = false;
						String notes = lineaObjPeya.get("notes").getAsString().trim();
						insertLineSalesOrder(numLine, lineaObjPeya, ordOB, product, comboFamMain, isCombo, mOfferID,
								famlilyQuantity, is_attribute, "", notes, price, printNotes);
						numLine = numLine + 10;
					}

					// SE SETEA EN NULL EL ARRAY DE OBJETOS DEL MODIFICADOR
					attrArr = new JsonArray();
				}

			} else {

				descriptionPeyaLog = "Pedido sin items";
				utilityDelivery.saveLogs("SPEYA_PEYA", orderJsonString, ordOB, utilConfig, false, descriptionPeyaLog,
						AcceptedURL, RejectedURL, PreparedURL, PickedUpURL, JSOrderID);
				throw new OBException("El pedido no tiene items.");

			}

			OBDal.getInstance().flush();

			// INSERTAR LINEA EN LA FIN_PAYMENT_SCHEDULE
			insertPaymentScheduleLineSalesOrder(ordOB, newInvoiceDate);

			responseWS.setDocumentNo(documentNo);
			responseWS.setStatus("OK");
			responseWS.setMessage("El pedido de venta fue creado exitosamente");

			// ACTUALIZO EL ESTADO DEL PEDIDO DE VENTA
			Order orderUpdate = OBDal.getInstance().get(Order.class, ordOB.getId());
			orderUpdate.setDocumentStatus("CO");
			orderUpdate.setProcessed(true); // PROCESSED = 'Y'
			orderUpdate.setProcessNow(true); // PROCESSED = 'Y'
			OBDal.getInstance().save(orderUpdate);
			OBDal.getInstance().flush();

			// GUARDO LA INFORMACION EN EL LOG
			utilityDelivery.saveLogs("SPEYA_PEYA", orderJsonString, ordOB, utilConfig, true, descriptionPeyaLog,
					AcceptedURL, RejectedURL, PreparedURL, PickedUpURL, JSOrderID);

		} catch (OBException e) {
			e.printStackTrace();
			String errorMsg = null;
			logger.error("Error al procesar transaccion" + e.getMessage(), e);
			OBDal.getInstance().rollbackAndClose();
			Throwable ex = DbUtility.getUnderlyingSQLException(e);
			if (ex.getMessage() != null) {
				errorMsg = "Error al insertar cabecera del pedido, " + ex.getMessage();
			} else if (e.getMessage() != null) {
				errorMsg = "Error al insertar cabecera del pedido, " + e.getMessage();
			} else {
				errorMsg = "Error al insertar cabecera del pedido, Error no tipificado por el sistema, revise la data enviada.";
			}

			if (utilConfig != null) {
				utilityDelivery.saveLogs("SPEYA_PEYA", orderJsonString, null, utilConfig, false, errorMsg, AcceptedURL,
						RejectedURL, PreparedURL, PickedUpURL, JSOrderID);
			}

			responseWS.setDocumentNo("N/A");
			responseWS.setStatus("ERROR");
			responseWS.setMessage(errorMsg);

			return responseWS;
		}

		return responseWS;
	}

	public ResponseWS insertSalesOrder(JsonObject dataPedido) throws JSONException {

		String documentNo = null;
		ResponseWS responseWS = new ResponseWS();

		try {

			// OBTENGO STORE_ID DEL JSON ENVIADO
			JsonObject restaurant = dataPedido.get("restaurant").getAsJsonObject();
			String storeId = null;
			storeId = restaurant.get("id").getAsString().trim();
			if (storeId == null || storeId.equals("")) {
				throw new OBException("El campo organizacion no debe estar vacio");
			}

			// CONFIGURACION DEL STORE_ID DE LA CONFIGURACION
			utilConfig = SPEYA_Helper.getConfig(storeId);
			if (utilConfig == null) {
				throw new OBException(
						"No existe una configuracion definida para la tienda con identificador  " + storeId);
			}

			// CONVERSION A STRING DEL JSON ENVIADO - PARA GUARDAR EN EL LOG
			orderJsonString = dataPedido.toString();

			// CREANDO INSTANCIA DEL PEDIDO A INSERTAR
			Order ordOB = OBProvider.getInstance().get(Order.class);
			ordOB.setActive(true);
			ordOB.setSalesTransaction(true);

			// ORGANIZACION
			org = utilConfig.getOrganization();
			if (org == null) {
				throw new OBException("No existe la organizacion");
			}

			ordOB.setClient(org.getClient());
			ordOB.setOrganization(org);

			// DESCRIPCION PEDIDO EXITO PARA EL LOG
			descriptionPeyaLog = "El pedido de venta fue creado exitosamente";

			// USUARIO QUE CREA Y MODIFICA EL PEDIDO
			User usr = utilConfig.getUserContact();
			ordOB.setCreatedBy(usr);
			ordOB.setUpdatedBy(usr);

			// TIPO DE DOCUMENTO DEL PEDIDO
			DocumentType docType = utilConfig.getDocumentType();
			ordOB.setDocumentType(docType);
			ordOB.setTransactionDocument(docType);

			// FECHA REGISTRO - FECHA CONTABLE
			Date newInvoiceDate = new Date();
			ordOB.setOrderDate(newInvoiceDate);
			ordOB.setAccountingDate(newInvoiceDate);

			// NUEVOS CAMPOS
			ordOB.setScheduledDeliveryDate(newInvoiceDate);
			ordOB.setPrintDiscount(true);
			ordOB.setFormOfPayment("P");

			// NUEVOS CAMPOS PARA RECUPERAR EL PEDIDO DE VENTA EN EL POS
			ordOB.setObposApplications(utilConfig.getObposApplications());
			ordOB.setPrint(false);
			ordOB.setObposIslayaway(true);
			ordOB.setObposCreatedabsolute(newInvoiceDate);

			// NUMERO DE DOCUMENTO DEL PEDIDO
			documentNo = Utility.getDocumentNo(OBDal.getInstance().getConnection(false), new DalConnectionProvider(),
					RequestContext.get().getVariablesSecureApp(), "", Order.TABLE_NAME, docType.getId(),
					docType.getId(), false, true);
			ordOB.setDocumentNo(documentNo);

			// ESTATUS DEL PEDIDO POR DEFECTO
			ordOB.setDocumentStatus("DR");
			ordOB.setDocumentAction("--");
			ordOB.setProcessed(false);
			ordOB.setProcessNow(false);

			// METODO DE PAGO DEL PEDIDO
			FIN_PaymentMethod payMet = null;
			payMet = utilConfig.getSpeyaPaymentmeth();
			if (payMet == null) {
				throw new OBException("No existe un metodo de pago asignado a la Configuracion Pedido YA.");
			}
			ordOB.setPaymentMethod(payMet);

			// INFORMACION DEL CLIENTE PARA LA FACTURACION
			String strNombreContactoEntrega = "";
			String strDireccion = "";
			String strIdCustomer = "";
			String strNameCustomer = "";
			String strPhoneCustomer = ".";
			String strDireccionReferencia = "--";
			String strEmailCustomer = utilConfig.getDefaultEmail();

			// TERCERO
			BusinessPartner bp = null;
			BusinessPartner bpOB = null;
			// TERCERO POR DEFECTO
			bpByDefault = utilConfig.getBpartner();
			bp = bpByDefault;

			JsonObject customer = dataPedido.get("user").getAsJsonObject();
			JsonObject company = customer.get("company").getAsJsonObject();
			JsonObject customerInfo = dataPedido.get("address").getAsJsonObject();

			if ((customer.get("identityCard").isJsonNull() || customer.get("identityCard").getAsString().equals(""))
					&& (company.get("document").isJsonNull() || company.get("document").getAsString().equals(""))) {

				// NO EXISTEN DATOS DE FACTURACION - TERCERO POR DEFECTO CONSUMIDOR FINAL
				bp = bpByDefault;
				descriptionPeyaLog = "El pedido de venta fue creado exitosamente. Sin datos de Facturacion. Tercero: Consumidor Final";

			} else {

				strDireccion = customerInfo.get("description").getAsString().trim();
				if (!customer.get("identityCard").isJsonNull()
						&& !customer.get("identityCard").getAsString().equals("")) {
					strIdCustomer = customer.get("identityCard").getAsString().trim();
					strNombreContactoEntrega = customer.get("name").getAsString().trim() + " "
							+ customer.get("lastName").getAsString().trim();
				} else if (!company.get("document").isJsonNull() && !company.get("document").getAsString().equals("")) {
					strIdCustomer = company.get("document").getAsString().trim();
					strNombreContactoEntrega = company.get("name").getAsString().trim();
				}
				strNameCustomer = strNombreContactoEntrega;
				strEmailCustomer = customer.get("email").getAsString().trim();
				strPhoneCustomer = customerInfo.get("phone").getAsString().trim();
				strDireccionReferencia = customerInfo.get("notes").getAsString().trim();
				if (strIdCustomer == null || strIdCustomer.equals("")) {
					throw new OBException("El campo Id del cliente no puede estar vacio");
				}

				bp = getBpartner(strIdCustomer);
				if (bp == null) {

					Boolean createValidate = utilityDelivery.validateIdentification(strIdCustomer);
					if (createValidate) {

						// SE CREA UN TERCERO SI NO EXISTE EN OB Y TAMPOCO ES CONSUMIDOR FINAL
						bpOB = createBpartner(strIdCustomer, strDireccion, strNameCustomer, strPhoneCustomer,
								strDireccionReferencia, strEmailCustomer, payMet);
						if (bpOB == null) {
							throw new OBException("No existe un tercero con el codigo tax id: " + strIdCustomer);
						} else {
							bp = bpOB;
							descriptionPeyaLog = "El pedido de venta fue creado exitosamente. PEDIDOS YA - Tercero Creado: "
									+ bp.getName();
						}

					} else {

						// TAX ID INVALIDO
						bp = bpByDefault;
						descriptionPeyaLog = "El pedido de venta fue creado exitosamente. TAXID Invalido. Tercero: Consumidor Final";

					}

				}

			}

			// SE ASIGNA EL TERCERO AL PEDIDO
			ordOB.setBusinessPartner(bp);

			// DESCRIPCION DEL PEDIDO - ALERGIAS
			String strDescInfo = "";
			strNombreContactoEntrega = customer.get("name").getAsString().trim() + " "
					+ customer.get("lastName").getAsString().trim();
			strDescInfo = "CON: " + strNombreContactoEntrega + "\n" + "NOT: " + dataPedido.get("notes").getAsString()
					+ "\n" + "OBS: " + "PEDIDOSYA ORDER CODE " + dataPedido.get("id").getAsString() + "\n" + "PAG: "
					+ "PEDIDOSYA - " + payMet.getName();
			ordOB.setDescription(strDescInfo);

			// DIRECCION DEL TERCERO
			Location bpAddress = getBpAddress(bp.getBusinessPartnerLocationList());
			if (bpAddress == null) {
				throw new OBException("El tercero " + bp.getName() + " no tiene definida una direccion");
			}
			ordOB.setPartnerAddress(bpAddress);
			ordOB.setInvoiceAddress(bpAddress);

			// MONEDA DEL PEDIDO
			Currency currency = null;
			currency = utilConfig.getCurrency();
			ordOB.setCurrency(currency);

			// TERMINO DE PAGO
			PaymentTerm payTerms = utilConfig.getPaymentTerms();
			if (payTerms == null) {
				throw new OBException("Debe agregar el termino de pago en la configuracion");
			}
			ordOB.setPaymentTerms(payTerms);

			// LISTA DE PRECIOS
			PriceList priceList = utilConfig.getPriceList();
			if (priceList == null) {
				throw new OBException("Debe configurar una price list");
			}
			ordOB.setPriceList(priceList);

			// AlMACEN
			Warehouse warehouse = utilConfig.getWarehouse();
			if (warehouse == null) {
				throw new OBException("Debe agregar en la configuracion el almacen por defecto");
			}
			ordOB.setWarehouse(warehouse);

			// ORIGEN DE LA VENTA PEDIDO YA
			ordOB.setSscmbSalesOrigin("SPEYA_PEYA");

			// GUARDANDO EL PEDIDO
			OBDal.getInstance().save(ordOB);

			// GUARDANDO LINEAS PEDIDOSYA
			JsonArray lineasPedidoPeya = dataPedido.get("details").getAsJsonArray();
			if (lineasPedidoPeya.size() > 0) {

				long numLine = 10;
				for (JsonElement lineaPeya : lineasPedidoPeya) {

					JsonObject lineaObjPeya = lineaPeya.getAsJsonObject();
					System.out.println("DETALLES DE ORDEN: " + lineaObjPeya);
					JsonObject productInfo = lineaObjPeya.get("product").getAsJsonObject();
					System.out.println(productInfo.get("integrationCode").getAsString());

					String strIdOB = productInfo.get("integrationCode").getAsString().trim();
					if (strIdOB == null || strIdOB.equals("")) {
						throw new OBException("El campo integrationCode no puede estar vacio");
					}

					JsonArray optionGroups = lineaObjPeya.get("optionGroups").getAsJsonArray();

					HashMap<Product, JSONObject> retrievedObject = getProduct(strIdOB, optionGroups, false);

					Set set = retrievedObject.entrySet();
					Iterator iterator = set.iterator();
					while (iterator.hasNext()) {
						Map.Entry mentry = (Map.Entry) iterator.next();
						Product product = (Product) mentry.getKey();
						JSONObject values = (JSONObject) mentry.getValue();
						Boolean comboFamMain = values.getBoolean("comboFamMain");
						Boolean isCombo = values.getBoolean("isCombo");
						String mOfferID = values.getString("m_offer_id");
						Long famlilyQuantity = values.getLong("famlily_quantity");
						String price = values.getString("additional_price");
						boolean printNotes = values.getBoolean("printAddNotes");
						Boolean is_attribute = false;
						String notes = lineaObjPeya.get("notes").getAsString().trim();
						insertLineSalesOrder(numLine, lineaObjPeya, ordOB, product, comboFamMain, isCombo, mOfferID,
								famlilyQuantity, is_attribute, "", notes, price, printNotes);
						numLine = numLine + 10;
					}

					// SE SETEA EN NULL EL ARRAY DE OBJETOS DEL MODIFICADOR
					attrArr = new JsonArray();
				}

			} else {

				descriptionPeyaLog = "Pedido sin items";
				utilityDelivery.saveLogs("SPEYA_PEYA", orderJsonString, ordOB, utilConfig, false, descriptionPeyaLog);
				throw new OBException("El pedido no tiene items.");

			}

			OBDal.getInstance().flush();

			// INSERTAR LINEA EN LA FIN_PAYMENT_SCHEDULE
			insertPaymentScheduleLineSalesOrder(ordOB, newInvoiceDate);

			responseWS.setDocumentNo(documentNo);
			responseWS.setStatus("OK");
			responseWS.setMessage("El pedido de venta fue creado exitosamente");

			// ACTUALIZO EL ESTADO DEL PEDIDO DE VENTA
			Order orderUpdate = OBDal.getInstance().get(Order.class, ordOB.getId());
			orderUpdate.setDocumentStatus("CO");
			orderUpdate.setProcessed(true); // PROCESSED = 'Y'
			orderUpdate.setProcessNow(true); // PROCESSED = 'Y'
			OBDal.getInstance().save(orderUpdate);
			OBDal.getInstance().flush();

			// GUARDO LA INFORMACION EN EL LOG
			utilityDelivery.saveLogs("SPEYA_PEYA", orderJsonString, ordOB, utilConfig, true, descriptionPeyaLog);

		} catch (OBException e) {
			e.printStackTrace();
			String errorMsg = null;
			logger.error("Error al procesar transaccion" + e.getMessage(), e);
			OBDal.getInstance().rollbackAndClose();
			Throwable ex = DbUtility.getUnderlyingSQLException(e);
			if (ex.getMessage() != null) {
				errorMsg = "Error al insertar cabecera del pedido, " + ex.getMessage();
			} else if (e.getMessage() != null) {
				errorMsg = "Error al insertar cabecera del pedido, " + e.getMessage();
			} else {
				errorMsg = "Error al insertar cabecera del pedido, Error no tipificado por el sistema, revise la data enviada.";
			}

			if (utilConfig != null) {
				utilityDelivery.saveLogs("SPEYA_PEYA", orderJsonString, null, utilConfig, false, errorMsg);
			}

			responseWS.setDocumentNo("N/A");
			responseWS.setStatus("ERROR");
			responseWS.setMessage(errorMsg);

			return responseWS;
		}

		return responseWS;
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

			PeyaConfig.setPyloginToken(TokenRP);
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

	public String actualizarEstadoOrden(String TokenOrder, String OrderID, String token, String AcceptedUrl) {

		speyaConfig PeyaConfig = null;

		try {
			OBContext.setAdminMode(true);
			OBCriteria<speyaConfig> PeyaConfigCrt = OBDal.getInstance().createCriteria(speyaConfig.class);
			PeyaConfigCrt.add(Restrictions.eq(speyaConfig.PROPERTY_ACTIVE, true));
			PeyaConfigCrt.setMaxResults(1);
			PeyaConfig = (speyaConfig) PeyaConfigCrt.uniqueResult();

			String url = AcceptedUrl;

			URL endpoint = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + token);
			connection.setDoOutput(true);

			// Crear el cuerpo de la solicitud en formato JSON
			JSONObject jsonBody = new JSONObject();
			DateTimeZone timeZone = DateTimeZone.forOffsetHours(5);
			DateTime now = new DateTime(timeZone);
			DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
			String formattedDate = formatter.print(now);
			jsonBody.put("acceptanceTime", formattedDate);
			jsonBody.put("remoteOrderId", OrderID);
			jsonBody.put("status", "order_accepted");

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
			e.printStackTrace();
			return e.getMessage();
		} finally {
			PeyaConfig.setPyaceptorderToken(token);
			OBDal.getInstance().save(PeyaConfig);
			OBDal.getInstance().flush();
			OBContext.restorePreviousMode();
		}
	}

	public void insertLineSalesOrder(long numLinea, JsonObject lineaObj, Order ordOB, Product product,
			Boolean familyMain, Boolean isCombo, String mOfferID, Long familyQuantity, Boolean is_attribute,
			String srtMainQuantity, String notes, String priceAdditional, boolean printNotes) throws OBException {

		// INSTANCIANDO OBJETO LINEA Y SETEANDO VALORES FIJOS DE LA CABECERA
		OrderLine ordLineOB = OBProvider.getInstance().get(OrderLine.class);
		ordLineOB.setClient(ordOB.getClient());
		ordLineOB.setOrganization(utilConfig.getOrganization());
		ordLineOB.setActive(true);
		ordLineOB.setCreatedBy(ordOB.getCreatedBy());
		ordLineOB.setUpdatedBy(ordOB.getUpdatedBy());
		ordLineOB.setSalesOrder(ordOB);
		ordLineOB.setLineNo(numLinea);

		// FECHA PEDIDO
		ordLineOB.setOrderDate(ordOB.getOrderDate());

		// DATE PROMISED
		ordLineOB.setScheduledDeliveryDate(ordOB.getOrderDate());

		// PRODUCTO
		if (product == null) {
			throw new OBException("El producto con de la linea No " + numLinea + ", del JSON no existe en Openbravo");
		}
		ordLineOB.setProduct(product);

		int stdPrecision = ordOB.getCurrency().getStandardPrecision().intValue();

		// IMPUESTO
		TaxRate taxRate = null;
		taxRate = utilConfig.getTax();

		ordLineOB.setTax(taxRate);

		// CANTIDAD VENDIDA
		BigDecimal qty = null;
		String strQty = lineaObj.get("quantity").getAsString();
		if (strQty == null || strQty.equals("")) {
			throw new OBException("El campo cantidad no puede estar vacio");
		} else {
			if (is_attribute) {
				// CANTIDADES PARA LOS ATRIBUTOS DE COMBO
				BigDecimal familyQty = new BigDecimal(familyQuantity);
				BigDecimal mainquantity = new BigDecimal(srtMainQuantity);
				qty = new BigDecimal(strQty);
				// PRIMERO SE MULTIPLICA POR LA CANTIDAD DE LA FAMILIA CONFIGURADA
				// EN LA MODIFICACION DE PRECIOS
				qty = qty.multiply(familyQty).setScale(stdPrecision, RoundingMode.HALF_UP);
				// SEGUNDO SE MULTIPLICA POR LA CANTIDAD DEL COMBO
				qty = qty.multiply(mainquantity).setScale(stdPrecision, RoundingMode.HALF_UP);
			} else {
				BigDecimal familyQty = new BigDecimal(familyQuantity);
				qty = new BigDecimal(strQty);
				qty = qty.multiply(familyQty).setScale(stdPrecision, RoundingMode.HALF_UP);
			}
		}
		ordLineOB.setOrderedQuantity(qty);

		// UNIDAD DE MEDIDA
		ordLineOB.setUOM(product.getUOM());

		// MONEDA
		ordLineOB.setCurrency(ordOB.getCurrency());

		// PRECIO
		String strPrice = lineaObj.get("unitPrice").getAsString();
		BigDecimal lineNetAmt = new BigDecimal(strPrice);
		BigDecimal pricetmp = new BigDecimal(priceAdditional);

		BigDecimal price = null;
		price = lineNetAmt.setScale(stdPrecision, RoundingMode.HALF_UP);

		if (pricetmp.equals(BigDecimal.ZERO)) {
			if (isCombo) {
				if (familyMain) {
					ordLineOB.setGrossUnitPrice(price);
					ordLineOB.setUnitPrice(price);
					ordLineOB.setPriceLimit(price);
					ordLineOB.setGrossListPrice(price);
					ordLineOB.setBaseGrossUnitPrice(price);
				} else {

					BigDecimal zeros = new BigDecimal("0.00");

					// EXISTEN ATRIBUTOS CON PRECIOS
					if (attrArr.size() > 0) {
						for (JsonElement attrWPrice : attrArr) {
							JsonObject lineaattr = attrWPrice.getAsJsonObject();
							String idModifierChange = lineaattr.get("id").getAsString().trim();
							if (product.getSearchKey().equals(idModifierChange)) {
								String priceModifierChange = lineaattr.get("price").getAsString();
								zeros = new BigDecimal(priceModifierChange);
							}
						}
					}

					ordLineOB.setGrossUnitPrice(zeros);
					ordLineOB.setUnitPrice(zeros);
					ordLineOB.setPriceLimit(zeros);
					ordLineOB.setGrossListPrice(zeros);
					ordLineOB.setBaseGrossUnitPrice(zeros);
					price = zeros;
				}

			} else {

				ordLineOB.setGrossUnitPrice(price);
				ordLineOB.setUnitPrice(price);
				ordLineOB.setPriceLimit(price);
				ordLineOB.setGrossListPrice(price);
				ordLineOB.setBaseGrossUnitPrice(price);

			}
		} else {
			ordLineOB.setGrossUnitPrice(pricetmp);
			ordLineOB.setUnitPrice(pricetmp);
			ordLineOB.setPriceLimit(pricetmp);
			ordLineOB.setGrossListPrice(pricetmp);
			ordLineOB.setBaseGrossUnitPrice(pricetmp);
			price = pricetmp;
		}
		// MONTO NETO DE LA LINEA
		BigDecimal lineNetAmount = qty.multiply(price).setScale(stdPrecision, RoundingMode.HALF_UP);
		ordLineOB.setLineNetAmount(lineNetAmount);

		// TERCERO
		ordLineOB.setBusinessPartner(ordOB.getBusinessPartner());

		// AlMACEN
		ordLineOB.setWarehouse(ordOB.getWarehouse());

		// DESCRIPCION DE LA LINEA
		if (printNotes)
			ordLineOB.setDescription(notes + " " + adtLineNote);

		// AGRENDADO LA LINEA DEL PEDIDO
		ordOB.getOrderLineList().add(ordLineOB);
		OBDal.getInstance().save(ordLineOB);

		if (isCombo) {

			// INSERTO EN LA ORDERLINEOFFER CUANDO SON COMBOS
			String strOrderLine = ordLineOB.getId();
			OrderLine objOrderLine = OBDal.getInstance().get(OrderLine.class, strOrderLine);

			PriceAdjustment prcAdj = OBDal.getInstance().get(PriceAdjustment.class, mOfferID);

			long numLineOffer = 10;
			BigDecimal zerosMoffer = new BigDecimal("0");

			OrderLineOffer ordLineOfferOB = OBProvider.getInstance().get(OrderLineOffer.class);
			ordLineOfferOB.setClient(ordOB.getClient());
			ordLineOfferOB.setOrganization(utilConfig.getOrganization());
			ordLineOfferOB.setActive(true);
			ordLineOfferOB.setCreatedBy(ordOB.getCreatedBy());
			ordLineOfferOB.setUpdatedBy(ordOB.getUpdatedBy());
			ordLineOfferOB.setSalesOrderLine(objOrderLine);
			ordLineOfferOB.setPriceAdjustment(prcAdj);
			ordLineOfferOB.setPriceAdjustmentAmt(zerosMoffer);
			ordLineOfferOB.setAdjustedPrice(zerosMoffer);
			ordLineOfferOB.setBaseGrossUnitPrice(price);
			ordLineOfferOB.setDisplayedTotalAmount(zerosMoffer);
			ordLineOfferOB.setTotalAmount(zerosMoffer);
			ordLineOfferOB.setObdiscIdentifier(prcAdj.getName());
			ordLineOfferOB.setLineNo(numLineOffer);

			OBDal.getInstance().save(ordLineOfferOB);

		}

	}

	public void insertPaymentScheduleLineSalesOrder(Order ordOB, Date invoiceDate) throws OBException {

		String gandTotal = getGrandTotal(ordOB.getId());
		BigDecimal total;
		BigDecimal zero = new BigDecimal("0");

		if (gandTotal != null) {
			total = new BigDecimal(gandTotal);
		} else {
			total = zero;
		}

		Organization orgOB = ordOB.getOrganization();
		FIN_PaymentSchedule finPaymentSchedule = OBProvider.getInstance().get(FIN_PaymentSchedule.class);
		finPaymentSchedule.setClient(ordOB.getClient());
		finPaymentSchedule.setOrganization(orgOB);
		finPaymentSchedule.setCreatedBy(ordOB.getCreatedBy());
		finPaymentSchedule.setUpdatedBy(ordOB.getUpdatedBy());
		finPaymentSchedule.setInvoice(null);
		finPaymentSchedule.setOrder(ordOB);
		finPaymentSchedule.setDueDate(invoiceDate);
		finPaymentSchedule.setFinPaymentmethod(ordOB.getPaymentMethod());
		finPaymentSchedule.setCurrency(ordOB.getCurrency());
		finPaymentSchedule.setAmount(total);
		finPaymentSchedule.setPaidAmount(zero);
		finPaymentSchedule.setOutstandingAmount(total);
		finPaymentSchedule.setActive(true);
		finPaymentSchedule.setFINPaymentPriority(null);
		finPaymentSchedule.setUpdatePaymentPlan(true);
		finPaymentSchedule.setOrigDueDate(invoiceDate);
		finPaymentSchedule.setDescription(null);
		finPaymentSchedule.setExpectedDate(invoiceDate);
		finPaymentSchedule.setAprmModifPaymentINPlan(false);
		finPaymentSchedule.setAprmModifPaymentOUTPlan(false);

		// AGRENDADO LA LINEA DEL PEDIDO
		ordOB.getFINPaymentScheduleList().add(finPaymentSchedule);
		OBDal.getInstance().save(finPaymentSchedule);

	}

	private SDELVRConfig getConfig(String glovoOrg) {
		SDELVRConfig config = null;

		OBCriteria<SDELVRConfig> cfgCrt = OBDal.getInstance().createCriteria(SDELVRConfig.class);
		cfgCrt.add(Restrictions.eq(SDELVRConfig.PROPERTY_SGLOVOSTORE, glovoOrg));
		config = (SDELVRConfig) cfgCrt.uniqueResult();

		return config;
	}

	private static String getGrandTotal(String order_id) {
		ConnectionProvider conn = new DalConnectionProvider(false);
		String strResult = null;
		try {

			String strSql = "SELECT grandtotal FROM c_order WHERE c_order_id = '" + order_id + "'";
			PreparedStatement st = null;

			st = conn.getPreparedStatement(strSql);
			ResultSet rsConsulta = st.executeQuery();

			while (rsConsulta.next()) {
				strResult = rsConsulta.getString("grandtotal");
			}

			return strResult;

		} catch (Exception e) {
			throw new OBException("Error al consultar el grandtotal del pedido. " + e.getMessage());
		}

	}

	private BusinessPartner getBpartner(String taxID) {
		BusinessPartner bp = null;

		OBCriteria<BusinessPartner> cfgCrt = OBDal.getInstance().createCriteria(BusinessPartner.class);
		cfgCrt.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, taxID));
		bp = (BusinessPartner) cfgCrt.uniqueResult();

		return bp;
	}

	private BusinessPartner createBpartner(String taxID, String direccion, String nombre, String telefono,
			String referencia, String correo, FIN_PaymentMethod payMet) {

		BusinessPartner bp = null;
		String typeTaxID;

		UUID uuid = UUID.randomUUID();
		String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

		Integer sizeTaxID = taxID.length();
		// SI LA CEDULA QUE ENVIAN ES SOLO NUMEROS
		if (taxID.matches("[0-9]+")) {

			if (sizeTaxID == 13) {
				typeTaxID = "R";
			} else if (sizeTaxID == 10) {
				typeTaxID = "D";
			} else {
				typeTaxID = "P";
			}

		} else {
			typeTaxID = "P";
		}

		String strSqlBPartner = null;

		strSqlBPartner = "INSERT INTO c_bpartner(\n"
				+ "            c_bpartner_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, VALUE, NAME, NAME2, TAXID,  \n"
				+ "            EM_SSWH_TAXPAYER_ID, EM_SSWH_TAXIDTYPE, C_BP_GROUP_ID, AD_LANGUAGE,"
				+ "            m_pricelist_id,BP_Currency_ID, EM_EEI_Eeioice, EM_Gcnv_Uniquecreditnote,"
				+ "            EM_EEI_Email,EM_Eei_Portal_Pass, C_PaymentTerm_ID, FIN_Paymentmethod_ID)\n"
				+ "    VALUES (?, ?, ?, 'Y', \n" + "             NOW(), ?, NOW(), ?, ?, ?, ?, ?, \n"
				+ "             ?, ?, ?, ?, ?, ?, 'Y', 'Y', ?, ?, ?, ?)";

		int updateCount = 0;
		PreparedStatement st = null;

		try {
			st = connectionProvider.getPreparedStatement(strSqlBPartner);

			st.setString(1, randomUUIDString);
			st.setString(2, utilConfig.getClient().getId());
			st.setString(3, "0");
			st.setString(4, utilConfig.getUserContact().getId());
			st.setString(5, utilConfig.getUserContact().getId());
			st.setString(6, taxID);
			st.setString(7, nombre.toUpperCase());
			st.setString(8, nombre.toUpperCase());
			st.setString(9, taxID);
			st.setString(10, utilConfig.getSswhTaxpayer().getId());
			st.setString(11, typeTaxID);
			st.setString(12, utilConfig.getBusinessPartnerCategory().getId());
			st.setString(13, utilConfig.getLanguage().getLanguage());
			st.setString(14, utilConfig.getPriceList().getId());
			st.setString(15, utilConfig.getCurrency().getId());
			st.setString(16, correo);
			st.setString(17, taxID);
			st.setString(18, utilConfig.getOrganization().getObretcoDbpPtermid().getId());
			st.setString(19, utilConfig.getOrganization().getObretcoDbpPmethodid().getId());

			updateCount = st.executeUpdate();
			if (updateCount > 0) {
				createLocGeo(randomUUIDString, direccion, telefono, referencia, nombre.toUpperCase(), correo);
				OBCriteria<BusinessPartner> cfgCrt = OBDal.getInstance().createCriteria(BusinessPartner.class);
				cfgCrt.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, taxID));
				bp = (BusinessPartner) cfgCrt.uniqueResult();
			} else {
				System.out.println("TERCERO NO INSERTADO.");
			}
			st.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				System.out.println(ignore.getMessage());
				ignore.printStackTrace();
			}
		}

		return bp;
	}

	private void createLocGeo(String c_bpartner_id, String direccion, String telefono, String referencia, String nombre,
			String correo) {

		UUID uuidLocation = UUID.randomUUID();
		String randomUUIDStringLocation = uuidLocation.toString().replaceAll("-", "").toUpperCase();

		String strSqlLocGeo = null;

		strSqlLocGeo = "INSERT INTO c_location(\n" + "            c_location_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, address1, address2,c_country_id )  \n"
				+ "             VALUES ( ?, ?, ?, 'Y', NOW(), ?, NOW(), ?, ?, ?, ? )";

		int updateCount = 0;
		PreparedStatement st = null;

		try {

			// SE VALIDA LONGUITUD DE LA DIRECCION
			Integer maxSize = 59;
			if (direccion.length() > maxSize) {
				direccion = direccion.substring(0, maxSize);
			}

			// SE VALIDA LONGUITUD DE LA DIRECCION
			if (referencia.length() > maxSize) {
				referencia = referencia.substring(0, maxSize);
			}

			st = connectionProvider.getPreparedStatement(strSqlLocGeo);

			st.setString(1, randomUUIDStringLocation);
			st.setString(2, utilConfig.getClient().getId());
			st.setString(3, "0");
			st.setString(4, utilConfig.getUserContact().getId());
			st.setString(5, utilConfig.getUserContact().getId());
			st.setString(6, direccion);
			st.setString(7, referencia);
			st.setString(8, OBDal.getInstance().get(Country.class, "171").getId());

			updateCount = st.executeUpdate();
			if (updateCount > 0) {
				createLocationBPartner(c_bpartner_id, randomUUIDStringLocation, telefono, nombre, correo);
			} else {
				System.out.println("LOCATION GEO NO INSERTADA.");
			}

			st.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				System.out.println(ignore.getMessage());
				ignore.printStackTrace();
			}
		}

	}

	private void createLocationBPartner(String c_bpartner_id, String c_location_id, String telefono, String nombre,
			String correo) {

		UUID uuidLoc = UUID.randomUUID();
		String randomUUIDStringLoc = uuidLoc.toString().replaceAll("-", "").toUpperCase();

		String strSqlLocation = null;

		strSqlLocation = "INSERT INTO c_bpartner_location(\n"
				+ "            c_bpartner_location_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, name, em_saqb_alias, phone , c_bpartner_id, c_location_id, isbillto, isshipto, ispayfrom, isremitto)\n"
				+ "             VALUES ( ? , ? , ? , 'Y' , NOW() , ? , NOW() , ? , ? , ? , ? , ? , ?, 'Y', 'Y', 'Y','Y')";

		int updateCount = 0;
		PreparedStatement st = null;

		try {

			st = connectionProvider.getPreparedStatement(strSqlLocation);

			st.setString(1, randomUUIDStringLoc);
			st.setString(2, utilConfig.getClient().getId());
			st.setString(3, "0");
			st.setString(4, utilConfig.getUserContact().getId());
			st.setString(5, utilConfig.getUserContact().getId());
			st.setString(6, "CONTACTO PEDIDOYA");
			st.setString(7, "CONTACTO PEDIDOYA");
			st.setString(8, telefono);
			st.setString(9, c_bpartner_id);
			st.setString(10, c_location_id);

			updateCount = st.executeUpdate();
			if (updateCount > 0) {
				createContactPersonBPartner(c_bpartner_id, telefono, nombre, correo);
			}

			st.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				System.out.println(ignore.getMessage());
				ignore.printStackTrace();
			}
		}

	}

	private void createContactPersonBPartner(String c_bpartner_id, String telefono, String nombre, String correo) {

		UUID uuidLoc = UUID.randomUUID();
		String randomUUIDStringContact = uuidLoc.toString().replaceAll("-", "").toUpperCase();

		String strSqlLocation = null;

		strSqlLocation = "INSERT INTO ad_user(\n" + "            ad_user_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, name, email, phone , c_bpartner_id, EM_Opcrm_Donotcall, username)\n"
				+ "             VALUES ( ? , ? , ? , 'Y' , NOW() , ? , NOW() , ? , ? , ?, ?, ?, 'N', ?)";

		int updateCount = 0;
		PreparedStatement st = null;

		try {

			st = connectionProvider.getPreparedStatement(strSqlLocation);

			st.setString(1, randomUUIDStringContact);
			st.setString(2, utilConfig.getClient().getId());
			st.setString(3, "0");
			st.setString(4, utilConfig.getUserContact().getId());
			st.setString(5, utilConfig.getUserContact().getId());
			st.setString(6, nombre);
			st.setString(7, correo);
			st.setString(8, telefono);
			st.setString(9, c_bpartner_id);
			st.setString(10, nombre);

			updateCount = st.executeUpdate();
			if (updateCount > 0) {
				System.out.println("CONTACT PERSON BPARTNER INSERTADA");
			}

			st.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				System.out.println(ignore.getMessage());
				ignore.printStackTrace();
			}
		}

	}

	private Location getBpAddress(List<Location> addressBp) {
		Location billaddress = null;

		for (Location location : addressBp) {
			if (location.isInvoiceToAddress()) {
				billaddress = location;
			}
		}

		if (billaddress == null && addressBp.size() > 0) {
			billaddress = (Location) addressBp.get(0);
		}

		return billaddress;
	}

	private HashMap<Product, JSONObject> getProduct(String idProduct, JsonArray optionGroups, boolean aditional)
			throws JSONException {

		HashMap<Product, JSONObject> hmap = new HashMap<Product, JSONObject>();
		Product product = null;
		PriceAdjustment priceAdj = null;

		// BUSCO EN LOS PRODUCTOS
		OBCriteria<Product> proCrt = OBDal.getInstance().createCriteria(Product.class);
		proCrt.add(Restrictions.eq(Product.PROPERTY_SEARCHKEY, idProduct));
		product = (Product) proCrt.uniqueResult();
		boolean hasoptionGroups = false;

		if (product == null) {

			// CONSULTO LOS COMBOS EN LA MODIFICACION DE PRECIOS
			OBCriteria<PriceAdjustment> offerCrt = OBDal.getInstance().createCriteria(PriceAdjustment.class);
			offerCrt.add(Restrictions.eq(PriceAdjustment.PROPERTY_SDELVRVALUE, idProduct));
			priceAdj = (PriceAdjustment) offerCrt.uniqueResult();

			if (priceAdj != null) {

				// SE OBTIENE LISTA DE LA FAMLIA DEL COMBO
				List<ComboProductFamily> comboFamLis = priceAdj.getOBCOMBOFamilyList();

				// CONTIENE ELEMTOS EN OPTION GROUP
				if (optionGroups.size() > 0)
					hasoptionGroups = true;

				// LISTADO DE PRODUCTOS EN OPTIONGROUP QUE SON MODIFICADORES
				List<ComboProduct> optionGroupUsed = new ArrayList<ComboProduct>();

				// *****************************************************
				// INICIO RECORRER EL ARRAY DE LAS FAMILIAS DEL COMBO
				// *****************************************************
				for (ComboProductFamily comboFam : comboFamLis) {

					Boolean comboFamMain = comboFam.isSdelvrDeliprice();
					ComboProduct defaultProduct = null;
					ComboProduct optionProduct = null;
					String defaultPrice = null;
					String optionPrice = null;

					// SE OBTIENE LA LISTA DE PRODUCTOS DE LA FAMILIA
					List<ComboProduct> comboProList = comboFam.getOBCOMBOProductList();

					// SE RECORREN LOS PRODUCTOS DE LAS FAMILIAS Y SE BUSCA MODIFICADOR O PRODUCTO
					// POR DEFECTO
					for (ComboProduct comboProduct : comboProList) {
						if (hasoptionGroups) {
							for (JsonElement option : optionGroups) {
								JsonObject optionJSon = option.getAsJsonObject();
								JsonArray options = optionJSon.get("options").getAsJsonArray();
								for (JsonElement optProd : options) {
									JsonObject optProdJSon = optProd.getAsJsonObject();
									if (comboProduct.getProduct().getSearchKey()
											.equals(optProdJSon.get("integrationCode").getAsString())) {
										optionProduct = comboProduct;
										optionPrice = optProdJSon.get("amount").getAsString();
										break;
									}
								}
								if (optionProduct != null) {
									break;
								}
							}
						}
						if (comboProduct.isSpeyaDefault()) {
							defaultProduct = comboProduct;
							defaultPrice = "0";
						}
						if (optionProduct != null) {
							break;
						}
					}

					if (optionProduct != null || defaultProduct != null) {
						String price;
						if (optionProduct != null) {
							product = optionProduct.getProduct();
							price = optionPrice;
						} else {
							product = defaultProduct.getProduct();
							price = defaultPrice;
						}
						JSONObject result = new JSONObject();
						result.put("comboFamMain", comboFamMain);
						result.put("isCombo", true);
						result.put("m_offer_id", priceAdj.getId());
						result.put("famlily_quantity", comboFam.getQuantity());
						result.put("additional_price", price);
						result.put("printAddNotes", comboFamMain);
						hmap.put(product, result);
					} else {
						throw new OBException("La familia" + comboFam.getName()
								+ " no tiene producto por defecto ni se seleccionó un modificador");
					}

					// *********************************************************
					// FIN RECORRER EL ARRAY DE LAS PRODUCTOS DE LA FAMILIA
					// *********************************************************

				}

				// SE AÑADEN PRODUCTOS ADICIONALES
				boolean isregister = false;
				if (hasoptionGroups) {
					for (JsonElement option : optionGroups) {
						JsonObject optionJSon = option.getAsJsonObject();
						JsonArray options = optionJSon.get("options").getAsJsonArray();
						for (JsonElement optProd : options) {
							JsonObject optProdJSon = optProd.getAsJsonObject();
							for (ComboProduct comboProduct : optionGroupUsed) {
								if (comboProduct.getProduct().getSearchKey()
										.equals(optProdJSon.get("integrationCode").getAsString())) {
									isregister = true;
								}
							}
							if (!isregister) {
								PriceAdjustment priceAdjAdd = null;
								OBCriteria<Product> proAdCrt = OBDal.getInstance().createCriteria(Product.class);
								proAdCrt.add(Restrictions.eq(Product.PROPERTY_SEARCHKEY,
										optProdJSon.get("integrationCode").getAsString()));
								Product prodAdicional = (Product) proAdCrt.uniqueResult();
								if (prodAdicional != null) {
									JSONObject result = new JSONObject();
									result.put("comboFamMain", false);
									result.put("isCombo", true);
									result.put("m_offer_id", priceAdj.getId());
									result.put("famlily_quantity", Long.parseLong("1"));
									result.put("additional_price", optProdJSon.get("amount").getAsString());
									result.put("printAddNotes", false);
									hmap.put(prodAdicional, result);
								} else if (optProdJSon.get("name").getAsString().equals("Salsa de soya")
										|| optProdJSon.get("name").getAsString().equals("Salsa agridulce")) {
									adtLineNote = optProdJSon.get("name").getAsString();
								} else {
									OBCriteria<PriceAdjustment> offerCrtAdd = OBDal.getInstance()
											.createCriteria(PriceAdjustment.class);
									offerCrtAdd.add(Restrictions.eq(PriceAdjustment.PROPERTY_SDELVRVALUE,
											optProdJSon.get("integrationCode").getAsString()));
									priceAdjAdd = (PriceAdjustment) offerCrtAdd.uniqueResult();
									if (priceAdjAdd != null) {
										List<ComboProductFamily> comboFamLisAdd = priceAdjAdd.getOBCOMBOFamilyList();
										for (ComboProductFamily comboFam : comboFamLisAdd) {
											Boolean comboFamMainAdd = comboFam.isSdelvrDeliprice();
											List<ComboProduct> comboProList = comboFam.getOBCOMBOProductList();
											for (ComboProduct comboProduct : comboProList) {
												if (comboProduct.isSpeyaDefault()) {
													JSONObject result = new JSONObject();
													result.put("comboFamMain", comboFamMainAdd);
													result.put("isCombo", true);
													result.put("m_offer_id", priceAdj.getId());
													result.put("famlily_quantity", comboFam.getQuantity());
													result.put("additional_price",
															comboFamMainAdd ? optProdJSon.get("amount").getAsString()
																	: '0');
													result.put("printAddNotes", false);
													hmap.put(comboProduct.getProduct(), result);
													break;
												}
											}
										}
									} else {
										throw new OBException("El producto con código"
												+ optProdJSon.get("integrationCode").getAsString() + " y nombre "
												+ optProdJSon.get("name").getAsString() + " no existe en el sistema");

									}
								}
							}
						}
					}
				}

				// *****************************************************
				// FIN RECORRER EL ARRAY DE LAS FAMILIAS DEL COMBO
				// *****************************************************

			} else {
				throw new OBException("El producto " + idProduct + " no existe en el sistema");
			}

		} else {

			JSONObject result = new JSONObject();
			result.put("comboFamMain", false);
			result.put("isCombo", false);
			result.put("m_offer_id", "");
			result.put("famlily_quantity", Long.parseLong("1"));
			result.put("additional_price", "0");
			result.put("printAddNotes", true);
			hmap.put(product, result);

		}

		return hmap;
	}

	private String getResponse(ResponseWS response) {
		Gson gson = new Gson();
		String json = gson.toJson(response);
		return json;
	}

}
