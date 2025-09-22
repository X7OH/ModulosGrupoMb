package ec.com.sidesoft.ws.ordercreate.webservices.setorderweb;

import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
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
import org.openbravo.model.financialmgmt.tax.TaxCategory;
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

import ec.com.sidesoft.ws.ordercreate.data.SWSOCConfig;
import ec.com.sidesoft.ws.ordercreate.data.SWSOCHommoffer;
import ec.com.sidesoft.ws.ordercreate.data.SWSOCHommofferAlt;
import ec.com.sidesoft.ws.ordercreate.data.SWSOCHomproduct;
import ec.com.sidesoft.ws.ordercreate.webservices.util.ResponseWS;
import ec.com.sidesoft.delivery.sales.channel.ad_helpers.DeliveryHelpers;
import ec.com.sidesoft.delivery.sales.channel.ad_process.Sdsch_ConfigChannelsAndDelivery;
import ec.com.sidesoft.integration.picker.SspkriPickerconfig;
import ec.com.sidesoft.integration.picker.ad_process.pickerMain;
import ec.com.sidesoft.smartdelivery.ad_process.SmartDeliveryAPI;
import ec.com.sidesoft.special.customization.mb.SscmbSaleorigin;
import ec.com.sidesoft.ws.ordercreate.data.SWSOCLogsweb;


public class OrderWebWS implements WebService {
  private static final Logger logger = Logger.getLogger(OrderWebWS.class);
  private static final long serialVersionUID = 1L;
  private static final ConnectionProvider connectionProvider = new DalConnectionProvider(false);

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    JsonElement element = new JsonParser().parse(new InputStreamReader(request.getInputStream()));
    JsonObject jsonOrder = element.getAsJsonObject();
    JsonObject dataPedido = new JsonObject(); 
   
    if (jsonOrder.has("data")) {
    	dataPedido = jsonOrder.getAsJsonObject("data");
    }else {
    	dataPedido = jsonOrder;
    }
    
    ResponseWS responseWS = insertSalesOrder(dataPedido);

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
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  public ResponseWS insertSalesOrder(JsonObject dataPedido) throws JSONException {
    String documentNo = null;
    ResponseWS responseWS = new ResponseWS();

    try {

      // TriggerHandler.getInstance().disable();
      // CREANDO INSTANCIA DEL PEDIDO A INSERTAR
      Order ordOB = OBProvider.getInstance().get(Order.class);
      ordOB.setActive(true);
      ordOB.setSalesTransaction(true);

      // ORGANIZACION DEL PEDIDO
      String strOrgId = dataPedido.get("ORG_OB").getAsString();
      if (strOrgId == null || strOrgId.equals("")) {
        throw new OBException("El campo organizacion no debe estar vacio");
      }
      final Organization org = OBDal.getInstance().get(Organization.class, strOrgId);
      SWSOCConfig utilConfig = getConfig(org);
      if (utilConfig == null) {
        throw new OBException(
            "No existe una configuracion definida para la organizacion " + org.getName());
      }
      ordOB.setClient(org.getClient());
      ordOB.setOrganization(org);

      // USUARIO QUE CREA Y MODIFICA EL PEDIDO
      User usr = utilConfig.getUser();
      ordOB.setCreatedBy(usr);
      ordOB.setUpdatedBy(usr);

      // TIPO DE DOCUMENTO DEL PEDIDO
      DocumentType docType = utilConfig.getDoctype();
      ordOB.setDocumentType(docType);           // C_DOCTYPE_ID = C_DOCTYPETARGET_ID,
      ordOB.setTransactionDocument(docType);    // C_DOCTYPE_ID = C_DOCTYPETARGET_ID,

      // FECHA REGISTRO - FECHA CONTABLE
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      String strDate = dataPedido.get("created_at").getAsString();
      if (strDate == null || strDate.equals("")) {
        throw new OBException("El campo fecha no debe estar vacio");
      }
      Date invoiceDate = formatter.parse(strDate);
      Date newInvoiceDate = new Date();
      ordOB.setOrderDate(newInvoiceDate);
      ordOB.setAccountingDate(newInvoiceDate);

      if(dataPedido.get("pickup_store_id").isJsonNull()) {
        ordOB.setSsmrdrIslocaldelivery(false);
      }else{
        ordOB.setSsmrdrIslocaldelivery(true);
      }

      // NUEVOS CAMPOS
      ordOB.setScheduledDeliveryDate(newInvoiceDate);
      ordOB.setPrintDiscount(true);
      ordOB.setFormOfPayment("P");

      // NUEVOS CAMPOS PARA RECUPERAR EL PEDIDO DE VENTA EN EL POS
      ordOB.setObposApplications(utilConfig.getObposApplications());
      ordOB.setPrint(false);                            // ISPRINTED = 'N',
      ordOB.setObposIslayaway(true);                   // EM_OBPOS_ISLAYAWAY = 'N',
      ordOB.setObposCreatedabsolute(newInvoiceDate);       //EM_OBPOS_CREATEDABSOLUTE = CREATED,

      // NUMERO DE DOCUMENTO DEL PEDIDO
      documentNo = Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
          new DalConnectionProvider(), RequestContext.get().getVariablesSecureApp(), "",
          Order.TABLE_NAME, docType.getId(), docType.getId(), false, true);
      ordOB.setDocumentNo(documentNo);

      // ID Web
      String strIdWeb = dataPedido.get("id").getAsString() == null ? " "
          : dataPedido.get("id").getAsString();
      ordOB.setSwsocIdweb(strIdWeb);

      // ESTATUS DEL PEDIDO POR DEFECTO
      ordOB.setDocumentStatus("DR");
      ordOB.setDocumentAction("--");    // DOCACTION = '--'
      ordOB.setProcessed(false);
      ordOB.setProcessNow(false);

      // NOMBRE DEL CLIENTE A ENTREGAR EL PEDIDO
      String strNombreContactoEntrega = dataPedido.get("NOMBRE_CLIENTE").getAsString();

      // DIRECCION DE DOMICILIO Y SMARTDELIVERY
      String strDireccionSmartDelivery = "";
      String strDireccionReferencia = "";
      String strDireccionReferenciaObs = "";
      if (dataPedido.get("DIRECCION_CLIENTE").getAsString() == null || dataPedido.get("DIRECCION_CLIENTE").getAsString().equals("")) {
        strDireccionSmartDelivery = "";
        }else {
          strDireccionSmartDelivery = getSmartDeliveryAddress(
                  dataPedido.get("DIRECCION_CLIENTE").getAsString());
          strDireccionReferencia = dataPedido.get("DIRECCION_CLIENTE").getAsString();
          strDireccionReferenciaObs = getReference(dataPedido.get("DIRECCION_CLIENTE").getAsString());
      }
      ordOB.setSwsocHomeaddress(strDireccionSmartDelivery);

      if (dataPedido.get("DIRECCION_CLIENTE").getAsString() == null || dataPedido.get("DIRECCION_CLIENTE").getAsString().equals("")) {
        strDireccionSmartDelivery = utilConfig.getOrganization().getOrganizationInformationList().get(0).getLocationAddress().getAddressLine1();
      }

      // METODO DE PAGO DEL PEDIDO
      FIN_PaymentMethod payMet = null;
      String strPayMet = dataPedido.get("payment_method").getAsString();
      if (strPayMet == null || strPayMet.equals("")) {
        throw new OBException("El campo metodo de pago no puede estar vacio");
      }
      payMet = OBDal.getInstance().get(FIN_PaymentMethod.class, strPayMet);
      if (payMet == null) {
        throw new OBException("No existe un metodo de pago con el codigo: " + strPayMet);
      }
      ordOB.setPaymentMethod(payMet);

      // TERCERO
      BusinessPartner bp = null;
      BusinessPartner bpOB = null;
      String strIdCustomer = dataPedido.get("ID_CLIENTE_FACTURA").getAsString();
      String strNameCustomer = dataPedido.get("billing_name").getAsString();
      String strPhoneCustomer = dataPedido.get("phone").getAsString();
      if (strIdCustomer == null || strIdCustomer.equals("")) {
        throw new OBException("El campo Id del cliente no puede estar vacio");
      }

      String strEmailCustomer = dataPedido.get("CORREO_CLIENTE").getAsString();
      bp = getBpartner(strIdCustomer);
      if (bp == null) {
        //SE CREA UN TERCERO SI NO EXISTE EN OB Y TAMPOCO ES CONSUMIDOR FINAL
        bpOB = createBpartner(strIdCustomer,utilConfig,strDireccionSmartDelivery,strNameCustomer,strPhoneCustomer, strDireccionReferenciaObs, strEmailCustomer,payMet);
        if (bpOB == null) {
          throw new OBException("No existe un tercero con el codigo tax id: " + strIdCustomer);
        }else {
          bp = bpOB;
        }
      }
      ordOB.setBusinessPartner(bp);

      // NUEVOS CAMPOS PEDIDO DE VENTA
      if (dataPedido.get("NOMBRE_CLIENTE").getAsString() == null || dataPedido.get("NOMBRE_CLIENTE").getAsString().equals("")) {
        strNombreContactoEntrega = bp.getName();
      }

      ordOB.setSaqbContactdelivery(strNombreContactoEntrega);
      ordOB.setSaqbContactnumber(dataPedido.get("phone").getAsString());

      bp.getBusinessPartnerLocationList();

      // DIRECCION DEL TERCERO
      Location bpAddress = getBpAddress(bp.getBusinessPartnerLocationList());
      if (bpAddress == null) {
        throw new OBException("El terceror " + bp.getName() + " no tiene definida una direccion");
      }
      ordOB.setPartnerAddress(bpAddress);
      ordOB.setInvoiceAddress(bpAddress);

      // MONEDA DEL PEDIDO
      Currency currency = null;
      currency = utilConfig.getCurrency();
      ordOB.setCurrency(currency);

      // TERMINO DE PAGO
      PaymentTerm payTerms = utilConfig.getPaymentterm();
      if (payTerms == null) {
        throw new OBException("Debe agregar el termino de pago en la configuracion");
      }
      ordOB.setPaymentTerms(payTerms);

      // LISTA DE PRECIOS
      PriceList priceList = utilConfig.getPricelist();
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

      // FUENTE
      String strSource = dataPedido.get("source").getAsString().toUpperCase();
      if (strSource == null || strSource.equals("")) {
        throw new OBException("El campo source no debe estar vacio");
      }
      if(!strSource.equals("ND")&&!strSource.equals("WEB")&&!strSource.equals("CLC")
    	&&!strSource.equals("SGLOVO_GLV")&&!strSource.equals("CHATBOT")&&!strSource.equals("SPEYA_PEYA")) {
    	    
    	  String oldstrSource = strSource;
    	  String salesOriginID = getSscmbSaleorigin(strSource);
    	  strSource = "SSCMB_OTHER";
    	  
    	  SscmbSaleorigin SaleOrigin = OBDal.getInstance().get(SscmbSaleorigin.class, salesOriginID);
    	  if (strSource == null || strSource.equals("")) {
    	        throw new OBException("No existe un origen de venta con identificador " + oldstrSource);
    	      }
    	  ordOB.setSscmbSaleorigin(SaleOrigin);
    	  
      }
      ordOB.setSscmbSalesOrigin(strSource.toUpperCase());

      // COORDENADAS
      if(dataPedido.has("coordinates")) {
        String strCoordinates = dataPedido.get("coordinates").getAsString().trim();
        if (!strCoordinates.equals("")) {
          ordOB.setSaqbLongitudeLatitude(strCoordinates);
        }
      }

      // DESCRIPCION DEL PEDIDO
      String strDescripcion = dataPedido.get("notes").getAsString();
      if (strDescripcion == null || strDescripcion.equals("")) {
        strDescripcion = "Pedido Venta No " + documentNo + ", creado por WS Web ";
      }
      strDescripcion = strDescripcion+ " Referencia: " + strDireccionReferencia;

      String strDescInfo = "";
      if (dataPedido.get("pickup_store_id").isJsonNull()) {
        strDescInfo = "CON: " + strNombreContactoEntrega + "\n"+
                      "DIR: " + strDireccionSmartDelivery + "\n"+
                      "TEL: " + strPhoneCustomer + "\n"+
                      "REF: " + strDireccionReferenciaObs + "\n"+
                      "OBS: " + dataPedido.get("notes").getAsString() + "\n"+
                      "PAG: " + strSource + " - " + payMet.getName();
      }else {
        strDescInfo = "CON: " + strNombreContactoEntrega + "\n"+
                      "DIR: " + strDireccionSmartDelivery + "\n"+
                      "TEL: " + strPhoneCustomer + "\n"+
                      "REF: " + strDireccionReferenciaObs + "\n"+
                      "OBS: " + dataPedido.get("notes").getAsString() + "\n"+
                      "INF: " + "ENTREGA EN EL LOCAL" + "\n"+
                      "PAG: " + strSource + " - " + payMet.getName();
      }
      ordOB.setDescription(strDescInfo);

      // GUARDANDO EL PEDIDO
      OBDal.getInstance().save(ordOB);

      // MONTO DEL DESCUENTO UANDO EL CAMPO discount > 0
      BigDecimal discount = dataPedido.get("discount").getAsBigDecimal();
      if (discount.compareTo(BigDecimal.ZERO) > 0) {
        System.out.println("DESCUENTO ES > QUE 0 " + discount);
      }

      // GUARDANDO LINEAS
      JsonArray lineasPedido = dataPedido.get("line_items").getAsJsonArray();
      boolean flagLineas = true;
      long numLine = 10;
      long numLineRecharge = 0;
      for (JsonElement eleLinea : lineasPedido) {

        JsonObject lineaObj = eleLinea.getAsJsonObject();
        String strProduct = lineaObj.get("product_id").getAsString();

        if (strProduct == null || strProduct.equals("")) {
          throw new OBException("El campo producto no puede estar vacio");
        }

        // ARRAY DEL MODIFICADOR
        JsonArray optionGroups = new JsonArray();
        if(lineaObj.has("optionGroups")) {
          optionGroups = lineaObj.get("optionGroups").getAsJsonArray();
        }

        HashMap<Product, JSONObject> retrievedObject =  getProduct(strProduct,utilConfig,optionGroups);

        Set set = retrievedObject.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
          Map.Entry mentry = (Map.Entry)iterator.next();
          Product product = (Product) mentry.getKey();
          JSONObject values = (JSONObject) mentry.getValue();
          Boolean comboFamMain = values.getBoolean("comboFamMain");
          Boolean isCombo = values.getBoolean("isCombo");
          String mOfferID = values.getString("m_offer_id");
          String price = values.getString("price");
          Long famlilyQuantity = values.getLong("famlily_quantity");
          insertLineSalesOrder(numLine, lineaObj, ordOB, product,comboFamMain,isCombo,mOfferID, price, famlilyQuantity);
          numLine = numLine + 10;
          numLineRecharge = numLine;
        }

      }

      //INSERTAR LA LINEA CUANDO EL CAMPO shipping > 0
      BigDecimal priceRecharge = dataPedido.get("shipping").getAsBigDecimal();
      if (priceRecharge.compareTo(BigDecimal.ZERO) > 0) {
          Product productRecharge = utilConfig.getProduct();
          BigDecimal qtyRecharge = new BigDecimal("1");
          insertLineSalesOrderRecharge(numLine, priceRecharge,qtyRecharge, ordOB, productRecharge);
          numLine = numLine + 10;
      }

      //INSERTAR LA LINEA CUANDO VIENE UN PRODUCTO GRATIS
      if (!dataPedido.get("branded_gift_id").toString().equals("null")) {

        String idProductFree = dataPedido.get("branded_gift_id").getAsString();
        JsonArray optionGroupsGift = new JsonArray();
        HashMap<Product, JSONObject> retrievedObjectFree =  getProduct(idProductFree,utilConfig,optionGroupsGift);
        Set setFree = retrievedObjectFree.entrySet();
        Iterator iteratorFree = setFree.iterator();
        while(iteratorFree.hasNext()) {
          Map.Entry mentry = (Map.Entry)iteratorFree.next();
          Product product = (Product) mentry.getKey();
          BigDecimal qtyFree = new BigDecimal("1");
          BigDecimal priceFree = new BigDecimal("0");
          insertLineSalesOrderRecharge(numLine, priceFree,qtyFree, ordOB, product);
          numLine = numLine + 10;
        }

      }

      OBDal.getInstance().flush();

      //INSERTAR LINEA EN LA FIN_PAYMENT_SCHEDULE
      insertPaymentScheduleLineSalesOrder(ordOB, newInvoiceDate);

      responseWS.setDocumentNo(documentNo);
      responseWS.setStatus("OK");
      responseWS.setMessage("El pedido de venta fue creado exitosamente");

      DeliveryHelpers DeliveryHelper = new DeliveryHelpers(); 
      
      // ENVIO DE LA INFORMACION A SMARTDELIVERY      
      pickerMain pkrProcess = new pickerMain();
      SspkriPickerconfig pkrConfig = new SspkriPickerconfig();
      
      OBContext.setAdminMode();
      try {
          // Tu lógica aquí
    	  pkrConfig = OBDal.getInstance().get(SspkriPickerconfig.class, pkrProcess.getPkrConfig());
      } finally {
          OBContext.restorePreviousMode();
      }
      
      if(!ordOB.isSsmrdrIslocaldelivery()) {
    	  if(DeliveryHelper.getDelivery(ordOB.getOrganization().getId(), strSource).equals("PCKR")) {

        	  String PickerId = ordOB.getOrganization().getSspkriPickerid();
        	  
        	  //Pre-Checkout
        	  String preCheckoutURL = pkrConfig.getUrlprecheckout();
        	  JSONObject jsonPreCheckout = pkrProcess.buildJsonPreCheckout(ordOB, PickerId);
        	  
        	  boolean preCheckOut = pkrProcess.SendPreCheckout(preCheckoutURL, jsonPreCheckout, 
        			  PickerId, ordOB); 

        	  //Booking
        	  String preBookingURL = pkrConfig.getUrlbooking();
        	  JSONObject jsonBooking = pkrProcess.buildJsonBooking(ordOB, PickerId);
        	  
        	  String tst = pkrProcess.SendBooking(preBookingURL, jsonBooking, 
        			  PickerId, ordOB, preCheckOut);
          }else {
        	  SmartDeliveryAPI smartDelivery = new SmartDeliveryAPI();
              
              JSONObject result = new JSONObject();
              result.put("orderIdOB", ordOB.getId());
              smartDelivery.consumeSmartClientWS(result);  
          }
      }

      // ACTUALIZO EL ESTADO DEL PEDIDO DE VENTA
      Order orderUpdate = OBDal.getInstance().get(Order.class, ordOB.getId());
      orderUpdate.setDocumentStatus("CO");
      orderUpdate.setProcessed(true);         // PROCESSED = 'Y'
      orderUpdate.setProcessNow(true);        // PROCESSED = 'Y'
      OBDal.getInstance().save(orderUpdate);
      OBDal.getInstance().flush();

      saveLogWeb(dataPedido,utilConfig,documentNo,strSource,ordOB);


    } catch (OBException e) {
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

      responseWS.setDocumentNo("N/A");
      responseWS.setStatus("ERROR");
      responseWS.setMessage(errorMsg);

      return responseWS;
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return responseWS;
  }

  public void insertLineSalesOrder(long numLinea, JsonObject lineaObj, Order ordOB, Product product, Boolean familyMain,
      Boolean isCombo, String mOfferID, String priceAdditional,Long familyQuantity)
      throws OBException {

    // INSTANCIANDO OBJETO LINEA Y SETEANDO VALORES FIJOS DE LA CABECERA
    Organization org = ordOB.getOrganization();
    OrderLine ordLineOB = OBProvider.getInstance().get(OrderLine.class);
    ordLineOB.setClient(ordOB.getClient());
    ordLineOB.setOrganization(org);
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
      throw new OBException(
          "El producto con de la linea No " + numLinea + ", del JSON no existe en Openbravo");
    }
    ordLineOB.setProduct(product);

    // IMPUESTO
    TaxRate taxRate = null;
    String strTax = lineaObj.get("tax_id").getAsString();
    if (strTax == null || strTax.equals("strTax")) {
      throw new OBException("El campo impuesto no puede estar vacio");
    }

    // taxRate = OBDal.getInstance().get(TaxRate.class, strTax);
    taxRate = getTax(product);
    ordLineOB.setTax(taxRate);

    // CANTIDAD VENDIDA
    BigDecimal qty = null;
    String strQty = lineaObj.get("quantity").getAsString();
    if (strQty == null || strQty.equals("")) {
      throw new OBException("El campo cantidad no puede estar vacio");
    } else {
    	qty = new BigDecimal(strQty);
    }
    ordLineOB.setOrderedQuantity(qty);

    // UNIDAD DE MEDIDA
    ordLineOB.setUOM(product.getUOM());

    // MONEDA
    ordLineOB.setCurrency(ordOB.getCurrency());

    // PRECIO
    int stdPrecision = ordOB.getCurrency().getStandardPrecision().intValue();
    BigDecimal price = null;
    String strLineNetAmt = lineaObj.get("price").getAsString();

    if (strLineNetAmt == null || strLineNetAmt.equals("")) {
      throw new OBException("El campo precio no puede estar vacio");
    }
    BigDecimal lineNetAmt = new BigDecimal(strLineNetAmt);
    BigDecimal pricetmp = new BigDecimal(priceAdditional);

    price = getPrice(product, lineNetAmt, qty, taxRate, stdPrecision);

    if (pricetmp.equals(BigDecimal.ZERO)) {

        if(isCombo) {

          if(familyMain) {
              ordLineOB.setGrossUnitPrice(price);
              ordLineOB.setUnitPrice(price);
              ordLineOB.setPriceLimit(price);

              ordLineOB.setGrossListPrice(price);
              ordLineOB.setBaseGrossUnitPrice(price);

          }else {
              BigDecimal zeros = new BigDecimal("0.00");
              ordLineOB.setGrossUnitPrice(zeros);
              ordLineOB.setUnitPrice(zeros);
              ordLineOB.setPriceLimit(zeros);

              ordLineOB.setGrossListPrice(zeros);
              ordLineOB.setBaseGrossUnitPrice(zeros);

              price = zeros;
          }

      }else {
          ordLineOB.setGrossUnitPrice(price);
          ordLineOB.setUnitPrice(price);
          ordLineOB.setPriceLimit(price);
          ordLineOB.setGrossListPrice(price);
          ordLineOB.setBaseGrossUnitPrice(price);
      }

    }else {

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
    String descLinea = null;
    JsonArray opcionesProducto = lineaObj.get("line_items_options").getAsJsonArray();
    for (JsonElement eleOpcion : opcionesProducto) {
      JsonObject opcionObj = eleOpcion.getAsJsonObject();
      String feature = opcionObj.get("feature_name").getAsString();
      String option = opcionObj.get("option_name").getAsString();
      if (descLinea == null) {
        descLinea = feature + " - " + option;
      } else {
        descLinea += "; " + feature + " - " + option;
      }
    }
    ordLineOB.setDescription(descLinea);

    // AGRENDADO LA LINEA DEL PEDIDO
    ordOB.getOrderLineList().add(ordLineOB);
    OBDal.getInstance().save(ordLineOB);

    if(isCombo) {

      // INSERTO EN LA ORDERLINEOFFER CUANDO SON COMBOS
      String strOrderLine = ordLineOB.getId();
      OrderLine objOrderLine = OBDal.getInstance().get(OrderLine.class, strOrderLine);

      PriceAdjustment prcAdj = OBDal.getInstance().get(PriceAdjustment.class, mOfferID);

      long numLineOffer = 10;
      BigDecimal zerosMoffer = new BigDecimal("0");

      OrderLineOffer ordLineOfferOB = OBProvider.getInstance().get(OrderLineOffer.class);
      ordLineOfferOB.setClient(ordOB.getClient());
      ordLineOfferOB.setOrganization(org);
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

  public void insertLineSalesOrderRecharge(long numLinea, BigDecimal priceRecharge, BigDecimal qtyRecharge, Order ordOB,
    Product product) throws OBException {

    // INSTANCIANDO OBJETO LINEA Y SETEANDO VALORES FIJOS DE LA CABECERA
    Organization org = ordOB.getOrganization();
    OrderLine ordLineOB = OBProvider.getInstance().get(OrderLine.class);
    ordLineOB.setClient(ordOB.getClient());
    ordLineOB.setOrganization(org);
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
      throw new OBException(
          "El producto con de la linea No " + numLinea + ", del JSON no existe en Openbravo");
    }
    ordLineOB.setProduct(product);

    // IMPUESTO
    TaxRate taxRate = null;
    taxRate = getTax(product);
    ordLineOB.setTax(taxRate);

    ordLineOB.setOrderedQuantity(qtyRecharge);

    // UNIDAD DE MEDIDA
    ordLineOB.setUOM(product.getUOM());

    // MONEDA
    ordLineOB.setCurrency(ordOB.getCurrency());

    // PRECIO
    int stdPrecision = ordOB.getCurrency().getStandardPrecision().intValue();
    BigDecimal price = null;
    BigDecimal lineNetAmt = priceRecharge;

    price = getPrice(product, lineNetAmt, qtyRecharge, taxRate, stdPrecision);

    ordLineOB.setGrossUnitPrice(price);
    ordLineOB.setUnitPrice(price);
    ordLineOB.setPriceLimit(price);

    //CAMPOS NUEVOS
    ordLineOB.setGrossListPrice(price);
    ordLineOB.setBaseGrossUnitPrice(price);

    // MONTO NETO DE LA LINEA
    BigDecimal lineNetAmount = qtyRecharge.multiply(price).setScale(stdPrecision, RoundingMode.HALF_UP);
    ordLineOB.setLineNetAmount(lineNetAmount);

    // TERCERO
    ordLineOB.setBusinessPartner(ordOB.getBusinessPartner());

    // AlMACEN
    ordLineOB.setWarehouse(ordOB.getWarehouse());

    // DESCRIPCION DE LA LINEA
    String descLinea = "";
    ordLineOB.setDescription(descLinea);

    // AGRENDADO LA LINEA DEL PEDIDO
    ordOB.getOrderLineList().add(ordLineOB);
    OBDal.getInstance().save(ordLineOB);

	}

  public void insertPaymentScheduleLineSalesOrder(Order ordOB, Date invoiceDate)
      throws OBException {

    String gandTotal = getGrandTotal(ordOB.getId());
    BigDecimal total;
    BigDecimal zero = new BigDecimal("0");

    if(gandTotal != null) {
      total = new BigDecimal(gandTotal);
    }else {
      total = zero;
    }

    Organization org = ordOB.getOrganization();
    FIN_PaymentSchedule finPaymentSchedule = OBProvider.getInstance().get(FIN_PaymentSchedule.class);
    finPaymentSchedule.setClient(ordOB.getClient());
    finPaymentSchedule.setOrganization(org);
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

  private SWSOCConfig getConfig(Organization org) {
    SWSOCConfig config = null;

    OBCriteria<SWSOCConfig> cfgCrt = OBDal.getInstance().createCriteria(SWSOCConfig.class);
    cfgCrt.add(Restrictions.eq(SWSOCConfig.PROPERTY_ORGANIZATION, org));
    config = (SWSOCConfig) cfgCrt.uniqueResult();

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
  
  private static String getSscmbSaleorigin(String value) {
	    ConnectionProvider conn = new DalConnectionProvider(false);
	    String strResult = null;
	    try {

	      String strSql = "Select SSCMB_saleorigin_id from SSCMB_saleorigin where value = '" + value + "' AND isactive = 'Y'";
	      PreparedStatement st = null;

	      st = conn.getPreparedStatement(strSql);
	      ResultSet rsConsulta = st.executeQuery();

	      while (rsConsulta.next()) {
	        strResult = rsConsulta.getString("SSCMB_saleorigin_id");
	      }

	      return strResult;

	    } catch (Exception e) {
	      throw new OBException("Error al consultar el Origen del pedido. " + e.getMessage());
	    }

	  }

  private BusinessPartner getBpartner(String taxID) {
    BusinessPartner bp = null;

    OBCriteria<BusinessPartner> cfgCrt = OBDal.getInstance().createCriteria(BusinessPartner.class);
    cfgCrt.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, taxID));
    bp = (BusinessPartner) cfgCrt.uniqueResult();

    return bp;
  }

  private BusinessPartner createBpartner(String taxID, SWSOCConfig config, String direccion, String nombre, String telefono, String referencia, String correo, FIN_PaymentMethod payMet) {

    BusinessPartner bp = null;
    String typeTaxID;

		UUID uuid = UUID.randomUUID();
		String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

		Integer sizeTaxID = taxID.length();
		if(sizeTaxID == 13) {
			typeTaxID = "R";
		}else {
			typeTaxID = "D";
		}

    String strSqlBPartner = null;

    strSqlBPartner = "INSERT INTO c_bpartner(\n"
        + "            c_bpartner_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
        + "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, VALUE, NAME, NAME2, TAXID,  \n"
        + "            EM_SSWH_TAXPAYER_ID, EM_SSWH_TAXIDTYPE, C_BP_GROUP_ID, AD_LANGUAGE,"
        + "            m_pricelist_id,BP_Currency_ID, EM_EEI_Eeioice, EM_Gcnv_Uniquecreditnote,"
        + "            EM_EEI_Email,EM_Eei_Portal_Pass, C_PaymentTerm_ID, FIN_Paymentmethod_ID)\n"
        + "    VALUES (?, ?, ?, 'Y', \n"
        + "    		NOW(), ?, NOW(), ?, ?, ?, ?, ?, \n"
        + "          	?, ?, ?, ?, ?, ?, 'Y', 'Y', ?, ?, ?, ?)";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSqlBPartner);

      st.setString(1, randomUUIDString);
      st.setString(2, config.getClient().getId());
      st.setString(3, "0");
      st.setString(4, config.getCreatedBy().getId());
      st.setString(5, config.getCreatedBy().getId());
      st.setString(6, taxID);
      st.setString(7, nombre.toUpperCase());
      st.setString(8, nombre.toUpperCase());
      st.setString(9, taxID);
      st.setString(10, config.getSswhTaxpayer().getId());
      st.setString(11, typeTaxID);
      st.setString(12, config.getBpGroup().getId());
      st.setString(13, config.getLanguage().getLanguage());
      st.setString(14, config.getPricelist().getId());
      st.setString(15, config.getCurrency().getId());
      st.setString(16, correo);
      st.setString(17, taxID);
      st.setString(18, config.getOrganization().getObretcoDbpPtermid().getId());
      st.setString(19, config.getOrganization().getObretcoDbpPmethodid().getId());

      updateCount = st.executeUpdate();
      if (updateCount > 0) {
        createLocGeo(randomUUIDString,config,direccion, telefono, referencia, nombre.toUpperCase(), correo);
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
        ignore.printStackTrace();
      }
    }

    return bp;
  }

  private void createLocGeo(String c_bpartner_id, SWSOCConfig config, String direccion, String telefono,String referencia, String nombre, String correo) {

		UUID uuidLocation = UUID.randomUUID();
		String randomUUIDStringLocation = uuidLocation.toString().replaceAll("-", "").toUpperCase();

    String strSqlLocGeo = null;

    strSqlLocGeo = "INSERT INTO c_location(\n"
        + "            c_location_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
        + "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, address1, address2,c_country_id )  \n"
        + "    		VALUES ( ?, ?, ?, 'Y', NOW(), ?, NOW(), ?, ?, ?, ? )";

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
      st.setString(2, config.getClient().getId());
      //st.setString(3, config.getOrganization().getId());
      st.setString(3, "0");
      st.setString(4, config.getCreatedBy().getId());
      st.setString(5, config.getCreatedBy().getId());
      st.setString(6, direccion);
      st.setString(7, referencia);
      st.setString(8, OBDal.getInstance().get(Country.class,"171").getId());

      updateCount = st.executeUpdate();
      if (updateCount > 0) {
        createLocationBPartner(c_bpartner_id,randomUUIDStringLocation, config, telefono, nombre, correo);
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
        ignore.printStackTrace();
      }
    }

  }

  private void createLocationBPartner(String c_bpartner_id, String c_location_id,SWSOCConfig config,String telefono, String nombre, String correo) {

		UUID uuidLoc = UUID.randomUUID();
		String randomUUIDStringLoc = uuidLoc.toString().replaceAll("-", "").toUpperCase();

		String strSqlLocation = null;

    strSqlLocation = "INSERT INTO c_bpartner_location(\n"
          + "            c_bpartner_location_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
          + "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, name, em_saqb_alias, phone , c_bpartner_id, c_location_id, isbillto, isshipto, ispayfrom, isremitto)\n"
          + "    		VALUES ( ? , ? , ? , 'Y' , NOW() , ? , NOW() , ? , ? , ? , ? , ? , ?, 'Y', 'Y', 'Y','Y')";


    int updateCount = 0;
    PreparedStatement st = null;

    try {

      st = connectionProvider.getPreparedStatement(strSqlLocation);

      st.setString(1, randomUUIDStringLoc);
      st.setString(2, config.getClient().getId());
      st.setString(3, "0");
      st.setString(4, config.getCreatedBy().getId());
      st.setString(5, config.getCreatedBy().getId());
      st.setString(6, "CONTACTO WEB");
      st.setString(7, "CONTACTO WEB");
      st.setString(8, telefono);
      st.setString(9, c_bpartner_id);
      st.setString(10, c_location_id);

      updateCount = st.executeUpdate();
      if (updateCount > 0) {
        createContactPersonBPartner(c_bpartner_id,config,telefono, nombre, correo);
      }

      st.close();
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }

  }

  private void createContactPersonBPartner(String c_bpartner_id, SWSOCConfig config, String telefono, String nombre, String correo) {

    UUID uuidLoc = UUID.randomUUID();
    String randomUUIDStringContact = uuidLoc.toString().replaceAll("-", "").toUpperCase();

    String strSqlLocation = null;

    strSqlLocation = "INSERT INTO ad_user(\n"
    + "            ad_user_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
    + "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, name, email, phone , c_bpartner_id, EM_Opcrm_Donotcall, username)\n"
    + "             VALUES ( ? , ? , ? , 'Y' , NOW() , ? , NOW() , ? , ? , ?, ?, ?, 'N', ?)";


    int updateCount = 0;
    PreparedStatement st = null;

    try {

      st = connectionProvider.getPreparedStatement(strSqlLocation);

      st.setString(1, randomUUIDStringContact);
      st.setString(2, config.getClient().getId());
      st.setString(3, "0");
      st.setString(4, config.getCreatedBy().getId());
      st.setString(5, config.getCreatedBy().getId());
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

  private HashMap<Product, JSONObject> getProduct(String idProduct, SWSOCConfig utilConfig,
    JsonArray optionGroups) throws JSONException {

      HashMap<Product, JSONObject> hmap = new HashMap<Product, JSONObject>();
      List<Product> lProduct = new ArrayList<Product>();
      Product product = null;
      PriceAdjustment priceAdj = null;
      SWSOCHomproduct homoProduct = null;
      SWSOCHomproduct homoProductChange = null;
      boolean hasoptionGroups = false;

      OBCriteria<Product> proCrt = OBDal.getInstance().createCriteria(Product.class);
      proCrt.add(Restrictions.eq(Product.PROPERTY_SEARCHKEY, idProduct));
      product = (Product) proCrt.uniqueResult();

      if (product == null) {

        OBCriteria<SWSOCHomproduct> cfgCrt = OBDal.getInstance()
            .createCriteria(SWSOCHomproduct.class);
        cfgCrt.add(Restrictions.eq(SWSOCHomproduct.PROPERTY_CODE, idProduct));
        homoProduct = (SWSOCHomproduct) cfgCrt.uniqueResult();

        if (homoProduct != null) {

          product = homoProduct.getProduct();
          JSONObject result = new JSONObject();
          result.put("comboFamMain", false);
          result.put("isCombo", false);
          result.put("m_offer_id", "");
          result.put("famlily_quantity", Long.parseLong("1"));
          result.put("price", "0");
          hmap.put(product,result);

        }else {

          // CONSULTO SI LA ORGANIZACION ES ALTERNATIVA
          if(utilConfig.isAlternative()) {

            //CONSULTO LOS COMBOS QUE SON DE LA ORGANIZACION ALTERNARIVA
            SWSOCHommofferAlt homoOfferAlt = null;
            OBCriteria<SWSOCHommofferAlt> offerCrt = OBDal.getInstance()
                .createCriteria(SWSOCHommofferAlt.class);
            offerCrt.add(Restrictions.eq(SWSOCHommofferAlt.PROPERTY_CODE, idProduct));
            homoOfferAlt = (SWSOCHommofferAlt) offerCrt.uniqueResult();

            if (homoOfferAlt != null) {

              priceAdj = homoOfferAlt.getOffer();
              List<ComboProductFamily> comboFamLis = priceAdj.getOBCOMBOFamilyList();
              for (ComboProductFamily comboFam : comboFamLis) {
              Boolean comboFamMain = comboFam.isSwsocMain();
                List<ComboProduct> comboProList = comboFam.getOBCOMBOProductList();
                for (ComboProduct comboProduct : comboProList) {
                  if (comboProduct.isSwsocDomicile()) {
                    product = comboProduct.getProduct();
                    JSONObject result = new JSONObject();
                    result.put("comboFamMain", comboFamMain);
                    result.put("isCombo", true);
                    result.put("m_offer_id", priceAdj.getId());
                    result.put("famlily_quantity", comboFam.getQuantity());
                    result.put("price", "0");
                    hmap.put(product,result);
                  }
                }
              }
            }

          }else {

            //CONSULTO LOS COMBOS QUE NO SON DE LA ORGANIZACION CARRION
            SWSOCHommoffer homoOffer = null;
            OBCriteria<SWSOCHommoffer> offerCrt = OBDal.getInstance()
                .createCriteria(SWSOCHommoffer.class);
            offerCrt.add(Restrictions.eq(SWSOCHommoffer.PROPERTY_CODE, idProduct));
            homoOffer = (SWSOCHommoffer) offerCrt.uniqueResult();
            if (homoOffer != null) {

              priceAdj = homoOffer.getOffer();

              // SE OBTIENE LISTA DE LA FAMLIA DEL COMBO
              List<ComboProductFamily> comboFamLis = priceAdj.getOBCOMBOFamilyList();

              // *****************************************************
              // INICIO RECORRER EL ARRAY DE LAS FAMILIAS DEL COMBO
              // *****************************************************
              for (ComboProductFamily comboFam : comboFamLis) {

                Boolean comboFamMain = comboFam.isSwsocMain();
                Boolean alreadySetChange = false;
                
                // SE OBTIENE LA LISTA DE PRODUCTOS DE LA FAMILIA
                List<ComboProduct> comboProList = comboFam.getOBCOMBOProductList();

                if (optionGroups.size() > 0) {

                  // SI VIENE UN MODIFICADOR LO BUSCO Y LO AGREGO AL ARRAY DE PRODUCTOS
                  for (ComboProduct comboProduct : comboProList) {
                    
                    if(alreadySetChange){
                      break;
                    }                  

                    // SE RECORRE EL ARRAY DE MODIFCADORES
                    for (JsonElement option : optionGroups) {

                      JsonObject optionJSon = option.getAsJsonObject();

                      // SE BUSCA EL SEARCHKEY DEL PRODUCTO DESDE LA HOMOLOGACION
                      String productIdHomologation = optionJSon.get("product_id").getAsString();

                      OBCriteria<SWSOCHomproduct> changeProduct = OBDal.getInstance()
                          .createCriteria(SWSOCHomproduct.class);
                      changeProduct.add(Restrictions.eq(SWSOCHomproduct.PROPERTY_CODE, productIdHomologation));
                      homoProductChange = (SWSOCHomproduct) changeProduct.uniqueResult();

                      if(homoProductChange != null) {
                        
                        if (comboProduct.getProduct().getSearchKey()
                            .equals(homoProductChange.getProduct().getSearchKey())) {
                          
                          String price;
                          product = comboProduct.getProduct();
                          price = optionJSon.get("price").getAsString().trim();

                          JSONObject result = new JSONObject();
                          result.put("comboFamMain", comboFamMain);
                          result.put("isCombo", true);
                          result.put("m_offer_id", priceAdj.getId());
                          result.put("famlily_quantity", comboFam.getQuantity());
                          result.put("price", price);
                          hmap.put(product, result);
                          
                          alreadySetChange = true;

                          break;
                        }
                      }

                    }

                    if(alreadySetChange){
                      break;
                    }
                    
                  }
                  
                  for (ComboProduct comboProduct : comboProList) {
                    
                    // SI YA EL CAMBIO ESTA APLICADO NO SE SIGUE BUSCANDO
                    // EN LOS PRODUCTOS DE LA FAMILIA                    
                    if(alreadySetChange){
                      break;
                    }                    

                    if (comboProduct.isSwsocDomicile()) {
                      product = comboProduct.getProduct();
                      JSONObject result = new JSONObject();
                      result.put("comboFamMain", comboFamMain);
                      result.put("isCombo", true);
                      result.put("m_offer_id", priceAdj.getId());
                      result.put("famlily_quantity", comboFam.getQuantity());
                      result.put("price", "0");
                      hmap.put(product, result);
                    }

                  }                  
                  
                }else{

                  for (ComboProduct comboProduct : comboProList) {

                    if (comboProduct.isSwsocDomicile()) {
                      product = comboProduct.getProduct();
                      JSONObject result = new JSONObject();
                      result.put("comboFamMain", comboFamMain);
                      result.put("isCombo", true);
                      result.put("m_offer_id", priceAdj.getId());
                      result.put("famlily_quantity", comboFam.getQuantity());
                      result.put("price", "0");
                      hmap.put(product, result);
                    }

                  }
                }
                

              }
              // ********************************************************************
              // FIN RECORRER EL ARRAY DE LOS PRODUCTOS DE LAS FAMILIAS DEL COMBO
              // ********************************************************************

            }
            // *****************************************************
            // FIN RECORRER EL ARRAY DE LAS FAMILIAS DEL COMBO
            // *****************************************************

          }

        }

      }else{

      JSONObject result = new JSONObject();
      result.put("comboFamMain", false);
      result.put("isCombo", false);
      result.put("m_offer_id", "");
      result.put("famlily_quantity", Long.parseLong("1"));
      result.put("price", "0");
      hmap.put(product,result);

    }

    return hmap;
  }

  private BigDecimal getPrice(Product product, BigDecimal lineNetAmt, BigDecimal qty,
      TaxRate taxRate, int stdPrecision) {
    BigDecimal price = null;

    BigDecimal factor = new BigDecimal(100);

    if (taxRate.isSummaryLevel()) {
      OBCriteria<TaxRate> taxCrt = OBDal.getInstance().createCriteria(TaxRate.class);
      taxCrt.add(Restrictions.eq(TaxRate.PROPERTY_PARENTTAXRATE, taxRate));
      List<TaxRate> lisTax = taxCrt.list();
      for (TaxRate taxRate2 : lisTax) {
        BigDecimal rate = taxRate2.getRate();
        BigDecimal ratePorc = rate.divide(factor).setScale(stdPrecision, RoundingMode.HALF_UP);
        BigDecimal taxAmt = lineNetAmt.multiply(ratePorc).setScale(stdPrecision,
            RoundingMode.HALF_UP);
        //lineNetAmt = lineNetAmt.subtract(taxAmt).setScale(stdPrecision, RoundingMode.HALF_UP);
        lineNetAmt = lineNetAmt.add(taxAmt).setScale(stdPrecision, RoundingMode.HALF_UP);
      }
    } else {
      BigDecimal rate = taxRate.getRate();
      BigDecimal ratePorc = rate.divide(factor).setScale(stdPrecision, RoundingMode.HALF_UP);
      BigDecimal taxAmt = lineNetAmt.multiply(ratePorc).setScale(stdPrecision,
          RoundingMode.HALF_UP);
      //lineNetAmt = lineNetAmt.subtract(taxAmt).setScale(stdPrecision, RoundingMode.HALF_UP);
      lineNetAmt = lineNetAmt.add(taxAmt).setScale(stdPrecision, RoundingMode.HALF_UP);
    }
    //price = lineNetAmt.divide(qty);
    price = lineNetAmt;
    return price;
  }

  private TaxRate getTax(Product product) {
    TaxCategory taxCat = product.getTaxCategory();
    OBCriteria<TaxRate> taxCrt = OBDal.getInstance().createCriteria(TaxRate.class);
    taxCrt.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, taxCat));
    TaxRate tax = (TaxRate) taxCrt.uniqueResult();

    return tax;
  }

  private String getResponse(ResponseWS response) {
    Gson gson = new Gson();
    String json = gson.toJson(response);
    return json;
  }

  private String getSmartDeliveryAddress(String direccion) {
    String[] arraySmartDelivery = direccion.split("\\|");
    String strDireccion = arraySmartDelivery[0] + " " + arraySmartDelivery[1] + " y "
        + arraySmartDelivery[2];
    return strDireccion;
  }

  private String getContactDelivery(String direccion) {
    String[] arraySmartDelivery = direccion.split("\\|");
    String strContactDelivery = arraySmartDelivery[4];
    return strContactDelivery;
}

  private String getReference(String direccion) {
    String[] arraySmartDelivery = direccion.split("\\|");
    String strContactDelivery = arraySmartDelivery[3];
    return strContactDelivery;
}

  private static void saveLogWeb(JsonObject dataPedido,SWSOCConfig config, String documentno, String source, Order order) {

    UUID uuid = UUID.randomUUID();
    String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

    SWSOCLogsweb temp = OBProvider.getInstance().get(SWSOCLogsweb.class);
    temp.setNewOBObject(true);
    temp.setId(randomUUIDString);
    temp.setClient(config.getClient());
    temp.setOrganization(config.getOrganization());
    temp.setJson(dataPedido.toString());
    temp.setOrdernumber(documentno);
    temp.setDeliveryType(source);
    temp.setOrder(order);
    temp.setIdweb(dataPedido.get("id").getAsString());
    temp.setCreatedBy(config.getUser());
    temp.setUpdatedBy(config.getUser());
    OBDal.getInstance().save(temp);
    OBDal.getInstance().flush();

  }

}
