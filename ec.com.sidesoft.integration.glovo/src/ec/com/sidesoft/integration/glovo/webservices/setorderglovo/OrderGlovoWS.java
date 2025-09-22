package ec.com.sidesoft.integration.glovo.webservices.setorderglovo;

import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Enumeration;
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

import ec.com.sidesoft.integration.delivery.SDELVR_Utility;
import ec.com.sidesoft.integration.delivery.SDELVRConfig;
import ec.com.sidesoft.integration.glovo.webservices.util.ResponseWS;

@AuthenticationManager.Stateless
public class OrderGlovoWS implements WebService {

  private static final Logger logger = Logger.getLogger(OrderGlovoWS.class);
  private static final long serialVersionUID = 1L;
  private static final ConnectionProvider connectionProvider = new DalConnectionProvider(false);
  private static SDELVRConfig utilConfig = null;
  private static Organization org = null;
  private static BusinessPartner bpByDefault = null;
  private static SDELVR_Utility utilityDelivery = new SDELVR_Utility();
  private static String descriptionGlovoLog = null;
  private static String orderJsonString = "";
  private static JsonArray attrArr = new JsonArray();
    
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception, AuthenticationException{

    Map<String, String> map = new HashMap<String, String>();

    Enumeration<?> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
        String key = (String) headerNames.nextElement();
        String value = request.getHeader(key);
        map.put(key, value);
    }
    
    // SE OBTIENE LA CABECERA QUE ENVIA GLOVO
    String authHeaderGlovo = request.getHeader("X-Glovo-Api-Key");
    
    JsonElement element = new JsonParser().parse(new InputStreamReader(request.getInputStream()));
    JsonObject dataPedido = element.getAsJsonObject();
    ResponseWS responseWS = insertSalesOrder(dataPedido, authHeaderGlovo);

    final String json = getResponse(responseWS);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
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

  public ResponseWS insertSalesOrder(JsonObject dataPedido, String authHeaderGlovo) throws JSONException {
    
    String documentNo = null;
    ResponseWS responseWS = new ResponseWS();
    
    try {

      // OBTENGO STORE_ID DEL JSON ENVIADO
      String storeId = null; 
      storeId = dataPedido.get("store_id").getAsString().trim();
      if (storeId == null || storeId.equals("")) {
        throw new OBException("El campo organizacion no debe estar vacio");
      }
      
      // CONFIGURACION DEL STORE_ID DE LA CONFIGURACION 
      utilConfig = getConfig(storeId);
      if (utilConfig == null) {
        throw new OBException("No existe una configuracion definida para la tienda con identificador  " + storeId);
      }
      
      // CONVERSION A STRING DEL JSON ENVIADO - PARA GUARDAR EN EL LOG
      orderJsonString = dataPedido.toString();
      
      // OBTENGO EL SECRET TOKEN DE LA CONFIGURACION GLOVO
      String secretToken = "";
      secretToken = utilConfig.getSglovoApikey();
      if (secretToken.equals("")) {
        throw new OBException("No existe el secret token para la configuracion de Glovo");
      }
      
      // SECRET TOKEN DE LA CONFIGURACION DEBE SER IGUAL A 
      // AUTHORIZATION DE LA CABECERA DE LA PETICION
      if(!secretToken.equals(authHeaderGlovo)) {
        descriptionGlovoLog = "Peticion de origen desconocido. Glovo Secret Token no coincide con el Header. " + authHeaderGlovo;
        utilityDelivery.saveLogs("SGLOVO_GLV", orderJsonString, null, utilConfig, false, descriptionGlovoLog);        
        throw new OBException("Peticion de origen desconocido - Glovo ");
      }      
      
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
      descriptionGlovoLog = "El pedido de venta fue creado exitosamente";
      
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
      documentNo = Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
          new DalConnectionProvider(), RequestContext.get().getVariablesSecureApp(), "",
          Order.TABLE_NAME, docType.getId(), docType.getId(), false, true);
      ordOB.setDocumentNo(documentNo);

      // ESTATUS DEL PEDIDO POR DEFECTO
      ordOB.setDocumentStatus("DR");
      ordOB.setDocumentAction("--");
      ordOB.setProcessed(false);          
      ordOB.setProcessNow(false);      
      
      // METODO DE PAGO DEL PEDIDO
      FIN_PaymentMethod payMet = null;
      payMet =  utilConfig.getSglovoPaymentmeth();
      if (payMet == null) {
        throw new OBException("No existe un metodo de pago asignado a la Configuracion Glovo.");
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
          
      JsonObject customer = dataPedido.get("customer").getAsJsonObject();
      strNombreContactoEntrega = customer.get("name").getAsString();
      
      // DETALLES DE FACTURACION
      if(customer.has("invoicing_details")) {
        
        if(customer.get("invoicing_details").isJsonObject()) {
          
          JsonObject invoiceDetails = customer.get("invoicing_details").getAsJsonObject();
          if(invoiceDetails.get("tax_id").getAsString().equals("")) {
            
            // NO EXISTEN DATOS DE FACTURACION - TERCERO POR DEFECTO CONSUMIDOR FINAL
            bp = bpByDefault;
            descriptionGlovoLog = "El pedido de venta fue creado exitosamente. Sin datos de Facturacion. Tercero: Consumidor Final";
            
          }else {
            
            strNombreContactoEntrega = invoiceDetails.get("company_name").getAsString().trim();
            strDireccion = invoiceDetails.get("company_address").getAsString().trim();
            strIdCustomer = invoiceDetails.get("tax_id").getAsString().trim();
            strNameCustomer = invoiceDetails.get("company_name").getAsString().trim();
            if (strIdCustomer == null || strIdCustomer.equals("")) {
              throw new OBException("El campo Id del cliente no puede estar vacio");
            }

            bp = getBpartner(strIdCustomer);
            if (bp == null) {

              Boolean createValidate = utilityDelivery.validateIdentification(strIdCustomer);
              if(createValidate) {

                //SE CREA UN TERCERO SI NO EXISTE EN OB Y TAMPOCO ES CONSUMIDOR FINAL
                bpOB = createBpartner(strIdCustomer,strDireccion,strNameCustomer,strPhoneCustomer, strDireccionReferencia, strEmailCustomer,payMet);
                if (bpOB == null) {
                  throw new OBException("No existe un tercero con el codigo tax id: " + strIdCustomer);   
                }else {
                  bp = bpOB;
                  descriptionGlovoLog = "El pedido de venta fue creado exitosamente. GLOVO - Tercero Creado: " + bp.getName();
                }
                
              }else {    
                
                // TAX ID INVALIDO
                bp = bpByDefault;
                descriptionGlovoLog = "El pedido de venta fue creado exitosamente. TAXID Invalido. Tercero: Consumidor Final";
                
              }

            }        
            
          } 
                  
        } else {

          // NO EXISTEN DATOS DE FACTURACION - TERCERO POR DEFECTO CONSUMIDOR FINAL
          bp = bpByDefault;
          descriptionGlovoLog = "El pedido de venta fue creado exitosamente. Sin datos de Facturacion. Tercero: Consumidor Final";
          
        } 
        
      }else {
        
        // NO EXISTEN DATOS DE FACTURACION - TERCERO POR DEFECTO CONSUMIDOR FINAL
        bp = bpByDefault;
        descriptionGlovoLog = "El pedido de venta fue creado exitosamente. Sin datos de Facturacion. Tercero: Consumidor Final";
        
      }

      // SE ASIGNA EL TERCERO AL PEDIDO
      ordOB.setBusinessPartner(bp);
      
      // SE VERIFICA QUE EXISTA INFORMACION DE LAS ALERGIAS
      String infoAdc = "SIN INFORMACION";
      if(dataPedido.has("allergy_info")) {
        infoAdc = dataPedido.get("allergy_info").getAsString();
      }
      
      // DESCRIPCION DEL PEDIDO - ALERGIAS      
      String strDescInfo = "";
      strNombreContactoEntrega = bp.getName();
          strDescInfo = "CON: " + strNombreContactoEntrega + "\n"+
          "ALERGIAS: " + infoAdc + "\n"+
          "OBS: " + "GLOVO ORDER CODE " + dataPedido.get("order_code").getAsString() + "\n"+
          "PAG: " + "GLOVO - " + payMet.getName();            
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

      // ORIGEN DE LA VENTA GLOVO
      ordOB.setSscmbSalesOrigin("SGLOVO_GLV");

      // GUARDANDO EL PEDIDO
      OBDal.getInstance().save(ordOB);

      // GUARDANDO LINEAS GLOVO
      JsonArray lineasPedidoGlovo = dataPedido.get("products").getAsJsonArray();
      if(lineasPedidoGlovo.size() > 0) {
        
        long numLine = 10;
        for (JsonElement lineaGlovo : lineasPedidoGlovo) {
          
          JsonObject lineaObjGlovo = lineaGlovo.getAsJsonObject();
          String strIdOB = lineaObjGlovo.get("id").getAsString().trim();
          if (strIdOB == null || strIdOB.equals("")) {
            throw new OBException("El campo producto no puede estar vacio");
          }
          
          // EXTRAS - ATRIBUTOS CON PRECIO 0
          String extraID = "";
          if(lineaObjGlovo.has("attributes")) {
            JsonArray extras = lineaObjGlovo.get("attributes").getAsJsonArray();
            if(extras.size() > 0) {               
              extraID = verifyExtrasGlovo(extras, ordOB);
            }              
          }
          
          // MODIFICADOR DEL COMBO - ATRIBUTOS CON PRECIO > 0
          // DEVUELVO UN ARRAY DE OBJETOS CON LOS ATRIBUTOS CON PRECIO > 0
          if(lineaObjGlovo.has("attributes")) {
            JsonArray extras = lineaObjGlovo.get("attributes").getAsJsonArray();
            if(extras.size() > 0) {               
              attrArr = verifyModifierChangeGlovo(extras, ordOB);
            }              
          }  
          
          HashMap<Product, JSONObject> retrievedObject =  getProduct(strIdOB, extraID);   
          
          Set set = retrievedObject.entrySet();
          Iterator iterator = set.iterator();
          while(iterator.hasNext()) {
             Map.Entry mentry = (Map.Entry)iterator.next();
             Product product = (Product) mentry.getKey();
             JSONObject values = (JSONObject) mentry.getValue();
             Boolean comboFamMain = values.getBoolean("comboFamMain");
             Boolean isCombo = values.getBoolean("isCombo");
             String mOfferID = values.getString("m_offer_id");
             Long famlilyQuantity = values.getLong("famlily_quantity");  
             Boolean is_attribute = false;
             insertLineSalesOrder(numLine, lineaObjGlovo, ordOB, product,comboFamMain,isCombo,mOfferID,famlilyQuantity,is_attribute,"");
             numLine = numLine + 10;
          }  
          
          String strMainQty = lineaObjGlovo.get("quantity").getAsString();
          
          if(lineaObjGlovo.has("attributes")) {
            
            JsonArray attributes = lineaObjGlovo.get("attributes").getAsJsonArray();
            if(attributes.size() > 0) {

              for (JsonElement lineaAtributosGlovo : attributes) {

                JsonObject lineaObjAttGlovo = lineaAtributosGlovo.getAsJsonObject();
                BigDecimal attrPrice = utilityDelivery.convertCentsToDollars(lineaObjAttGlovo.get("price").getAsString(),2);
                String attrID = lineaObjAttGlovo.get("id").getAsString().trim();

                if(attrPrice.compareTo(BigDecimal.ZERO) > 0) {
                  
                  // EXISTEN ATRIBUTOS CON PRECIOS
                  Boolean selectedModWithPrice = false;
                  if(attrArr.size() > 0) {
                    for (JsonElement attrWPrice : attrArr) {       
                      JsonObject lineaattr = attrWPrice.getAsJsonObject();
                      String  idModifierChange = lineaattr.get("id").getAsString().trim();
                      Boolean verify = lineaattr.get("is_modifier").getAsBoolean();
                      if(attrID.equals(idModifierChange) && verify){
                        selectedModWithPrice = true;
                      }            
                    }     
                  }                   
                  
                  if(!selectedModWithPrice) {
                    
                    HashMap<Product, JSONObject> retrievedObjectAttr =  getProduct(attrID, "");  
                    
                    Set setAttr = retrievedObjectAttr.entrySet();
                    Iterator iteratorAttr = setAttr.iterator();
                    while(iteratorAttr.hasNext()) {
                      Map.Entry mentryAttr = (Map.Entry)iteratorAttr.next();
                      Product productAttr = (Product) mentryAttr.getKey();
                      JSONObject valuesAttr = (JSONObject) mentryAttr.getValue();
                      Boolean comboFamMainAttr = valuesAttr.getBoolean("comboFamMain");
                      Boolean isComboAttr = valuesAttr.getBoolean("isCombo");
                      String mOfferIDAttr = valuesAttr.getString("m_offer_id");
                      Long famlilyQuantityAttr = valuesAttr.getLong("famlily_quantity");
                      Boolean is_attribute = true;
                      insertLineSalesOrder(numLine, lineaObjAttGlovo, ordOB, productAttr,comboFamMainAttr,isComboAttr,mOfferIDAttr,famlilyQuantityAttr,is_attribute,strMainQty);
                      numLine = numLine + 10;
                    }  
                    
                  }                  
                }                
              }
              
            }
            
          }
          
          // SE SETEA EN NULL EL ARRAY DE OBJETOS DEL MODIFICADOR
          attrArr = new JsonArray();
        }

      }else {
        
        descriptionGlovoLog = "Pedido sin items";
        utilityDelivery.saveLogs("SGLOVO_GLV", orderJsonString, ordOB, utilConfig, false, descriptionGlovoLog);
        throw new OBException("El pedido no tiene items.");
        
      }
      
      OBDal.getInstance().flush();
      
      //INSERTAR LINEA EN LA FIN_PAYMENT_SCHEDULE
      insertPaymentScheduleLineSalesOrder(ordOB, newInvoiceDate);
      
      responseWS.setDocumentNo(documentNo);
      responseWS.setStatus("OK");
      responseWS.setMessage("El pedido de venta fue creado exitosamente");

      // ACTUALIZO EL ESTADO DEL PEDIDO DE VENTA
      Order orderUpdate = OBDal.getInstance().get(Order.class, ordOB.getId());
      orderUpdate.setDocumentStatus("CO");
      orderUpdate.setProcessed(true);         // PROCESSED = 'Y' 
      orderUpdate.setProcessNow(true);        // PROCESSED = 'Y'      
      OBDal.getInstance().save(orderUpdate);
      OBDal.getInstance().flush();
      
      // GUARDO LA INFORMACION EN EL LOG
      utilityDelivery.saveLogs("SGLOVO_GLV", orderJsonString, ordOB, utilConfig, true, descriptionGlovoLog);      
      
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
      
      if(utilConfig != null) {
        utilityDelivery.saveLogs("SGLOVO_GLV", orderJsonString, null, utilConfig, false, errorMsg);  
      }
            
      responseWS.setDocumentNo("N/A");
      responseWS.setStatus("ERROR");
      responseWS.setMessage(errorMsg);

      return responseWS;
    }

    return responseWS;
  }

  private String verifyExtrasGlovo(JsonArray extras, Order ordOB) {

    int stdPrecision = ordOB.getCurrency().getStandardPrecision().intValue();
    String cambioIdProduct = "";
    
    for (JsonElement lineaExtraGlovo : extras) {
      JsonObject lineaObjExGlovo = lineaExtraGlovo.getAsJsonObject();
      BigDecimal extraPrice = utilityDelivery.convertCentsToDollars(lineaObjExGlovo.get("price").getAsString(),stdPrecision);
      if(extraPrice.compareTo(BigDecimal.ZERO) == 0) {
        cambioIdProduct = lineaObjExGlovo.get("id").getAsString();
      }
    }    
    
    return cambioIdProduct;
  }
  
  private JsonArray verifyModifierChangeGlovo(JsonArray extras, Order ordOB) {

    int stdPrecision = ordOB.getCurrency().getStandardPrecision().intValue();
    JsonArray attrWtihPriceArr = new JsonArray();
    
    for (JsonElement lineaExtraGlovo : extras) {
      JsonObject lineaObjExGlovo = lineaExtraGlovo.getAsJsonObject();
      BigDecimal extraPrice = utilityDelivery.convertCentsToDollars(lineaObjExGlovo.get("price").getAsString(),stdPrecision);
      //  MODIFICADORES CON PRECIO
      if(extraPrice.compareTo(BigDecimal.ZERO) == 1) {
        JsonObject item = new JsonObject();
        item.addProperty("quantity", lineaObjExGlovo.get("quantity").getAsString().trim());
        item.addProperty("id", lineaObjExGlovo.get("id").getAsString().trim());
        item.addProperty("name", lineaObjExGlovo.get("name").getAsString().trim());
        item.addProperty("price", lineaObjExGlovo.get("price").getAsString().trim());
        item.addProperty("is_modifier", false);
        attrWtihPriceArr.add(item);
      }
    }    
    
    return attrWtihPriceArr;
  }  
  
  public void insertLineSalesOrder(long numLinea, JsonObject lineaObj, Order ordOB, Product product, Boolean familyMain, 
      Boolean isCombo, String mOfferID, Long familyQuantity, Boolean is_attribute, String srtMainQuantity)
      throws OBException {

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
      throw new OBException(
          "El producto con de la linea No " + numLinea + ", del JSON no existe en Openbravo");
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
      if(is_attribute) {
        // CANTIDADES PARA LOS ATRIBUTOS DE COMBO
        BigDecimal familyQty = new BigDecimal(familyQuantity);
        BigDecimal mainquantity = new BigDecimal(srtMainQuantity);
        qty = new BigDecimal(strQty);
        // PRIMERO SE MULTIPLICA POR LA CANTIDAD DE LA FAMILIA CONFIGURADA
        // EN LA MODIFICACION DE PRECIOS
        qty = qty.multiply(familyQty).setScale(stdPrecision, RoundingMode.HALF_UP);
        // SEGUNDO SE MULTIPLICA POR LA CANTIDAD DEL COMBO
        qty = qty.multiply(mainquantity).setScale(stdPrecision, RoundingMode.HALF_UP);
      }else {
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
    String strPrice = lineaObj.get("price").getAsString();
    BigDecimal lineNetAmt = utilityDelivery.convertCentsToDollars(strPrice,stdPrecision);
    
    BigDecimal price = null;
    price = lineNetAmt.setScale(stdPrecision, RoundingMode.HALF_UP);
    
    if(isCombo) {
        
      if(familyMain) {
        ordLineOB.setGrossUnitPrice(price);
        ordLineOB.setUnitPrice(price);
        ordLineOB.setPriceLimit(price);
        ordLineOB.setGrossListPrice(price);
        ordLineOB.setBaseGrossUnitPrice(price);
      }else {
        
        BigDecimal zeros = new BigDecimal("0.00");
        
        // EXISTEN ATRIBUTOS CON PRECIOS
        if(attrArr.size() > 0) {
          for (JsonElement attrWPrice : attrArr) {       
            JsonObject lineaattr = attrWPrice.getAsJsonObject();
            String  idModifierChange = lineaattr.get("id").getAsString().trim();
            if(product.getSearchKey().equals(idModifierChange)){
              String priceModifierChange = lineaattr.get("price").getAsString();
              zeros = utilityDelivery.convertCentsToDollars(priceModifierChange,stdPrecision);
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
        
    }else {
        
      ordLineOB.setGrossUnitPrice(price);
      ordLineOB.setUnitPrice(price);
      ordLineOB.setPriceLimit(price);
      ordLineOB.setGrossListPrice(price);
      ordLineOB.setBaseGrossUnitPrice(price);

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
    descLinea = "";
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
  
  private BusinessPartner createBpartner(String taxID, String direccion, String nombre, String telefono, String referencia, String correo, FIN_PaymentMethod payMet) {
          
    BusinessPartner bp = null;
    String typeTaxID;
    
    UUID uuid = UUID.randomUUID();
    String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();
    
    Integer sizeTaxID = taxID.length();
    // SI LA CEDULA QUE ENVIAN ES SOLO NUMEROS
    if(taxID.matches("[0-9]+")) {
      
      if(sizeTaxID == 13) {
          typeTaxID = "R";
      } else if(sizeTaxID == 10) {
          typeTaxID = "D";
      } else {
          typeTaxID = "P";
      }
      
    }else {
      typeTaxID = "P";
    }

    String strSqlBPartner = null;

    strSqlBPartner = "INSERT INTO c_bpartner(\n"
        + "            c_bpartner_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
        + "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, VALUE, NAME, NAME2, TAXID,  \n"
        + "            EM_SSWH_TAXPAYER_ID, EM_SSWH_TAXIDTYPE, C_BP_GROUP_ID, AD_LANGUAGE,"
        + "            m_pricelist_id,BP_Currency_ID, EM_EEI_Eeioice, EM_Gcnv_Uniquecreditnote,"
        + "            EM_EEI_Email,EM_Eei_Portal_Pass, C_PaymentTerm_ID, FIN_Paymentmethod_ID)\n"
        + "    VALUES (?, ?, ?, 'Y', \n"
        + "             NOW(), ?, NOW(), ?, ?, ?, ?, ?, \n"
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
        createLocGeo(randomUUIDString,direccion, telefono, referencia, nombre.toUpperCase(), correo);
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
  
  private void createLocGeo(String c_bpartner_id, String direccion, String telefono,String referencia, String nombre, String correo) {

    UUID uuidLocation = UUID.randomUUID();
    String randomUUIDStringLocation = uuidLocation.toString().replaceAll("-", "").toUpperCase();

    String strSqlLocGeo = null;

    strSqlLocGeo = "INSERT INTO c_location(\n"
        + "            c_location_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
        + "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, address1, address2,c_country_id )  \n"
        + "             VALUES ( ?, ?, ?, 'Y', NOW(), ?, NOW(), ?, ?, ?, ? )";
    
    int updateCount = 0;
    PreparedStatement st = null;
    
    try {
      
      // SE VALIDA LONGUITUD DE LA DIRECCION 
      Integer maxSize = 59;
      if(direccion.length() > maxSize ){
        direccion = direccion.substring(0, maxSize);
      }
      
      // SE VALIDA LONGUITUD DE LA DIRECCION       
      if(referencia.length() > maxSize ){
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
      st.setString(8, OBDal.getInstance().get(Country.class,"171").getId());

      updateCount = st.executeUpdate();
      if (updateCount > 0) {
        createLocationBPartner(c_bpartner_id,randomUUIDStringLocation, telefono, nombre, correo);
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
  
  private void createLocationBPartner(String c_bpartner_id, String c_location_id,String telefono, String nombre, String correo) {

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
      st.setString(6, "CONTACTO GLOVO");
      st.setString(7, "CONTACTO GLOVO");
      st.setString(8, telefono);
      st.setString(9, c_bpartner_id);
      st.setString(10, c_location_id);
      
      updateCount = st.executeUpdate();
      if (updateCount > 0) {
        createContactPersonBPartner(c_bpartner_id,telefono, nombre, correo);
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

    strSqlLocation = "INSERT INTO ad_user(\n"
    + "            ad_user_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
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

  private HashMap<Product, JSONObject> getProduct(String idProduct, String idProductModifier) throws JSONException {        

    HashMap<Product, JSONObject> hmap = new HashMap<Product, JSONObject>();
    Product product = null;
    Product productMod = null;
    Product productModWithPrice = null;
    PriceAdjustment priceAdj = null;
    Boolean selectedMod = false;
    Boolean selectedModWithPrice = false;
    Boolean selectedComb;
    String idModifierChange = "";
    Boolean alreadySetChange = false;
    
    // BUSCO EN LOS PRODUCTOS
    OBCriteria<Product> proCrt = OBDal.getInstance().createCriteria(Product.class);
    proCrt.add(Restrictions.eq(Product.PROPERTY_SEARCHKEY, idProduct));
    product = (Product) proCrt.uniqueResult();
    
    if (product == null) {
      
      //CONSULTO LOS COMBOS EN LA MODIFICACION DE PRECIOS
      OBCriteria<PriceAdjustment> offerCrt = OBDal.getInstance()
          .createCriteria(PriceAdjustment.class);
      offerCrt.add(Restrictions.eq(PriceAdjustment.PROPERTY_SDELVRVALUE, idProduct));
      priceAdj = (PriceAdjustment) offerCrt.uniqueResult();
      
      if (priceAdj != null) {
        
        // SE OBTIENE LISTA DE LA FAMLIA DEL COMBO
        List<ComboProductFamily> comboFamLis = priceAdj.getOBCOMBOFamilyList();
        
        // *****************************************************
        // INICIO RECORRER EL ARRAY DE LAS FAMILIAS DEL COMBO
        // *****************************************************        
        for (ComboProductFamily comboFam : comboFamLis) {
          
          selectedComb = false;
          Boolean comboFamMain = comboFam.isSdelvrDeliprice();
          
          // SE OBTIENE LA LISTA DE PRODUCTOS DE LA FAMILIA
          List<ComboProduct> comboProList = comboFam.getOBCOMBOProductList();
          
          // EXISTEN ATRIBUTOS CON PRECIOS
          if(attrArr.size() > 0) {
            
            int position = 0;
            // RECORRER EL ARRAY DE ATRIBUTOS CON PRECIO
            for (JsonElement attrWPrice : attrArr) {
              
              if(selectedModWithPrice) {
                break;
              }              
              
              JsonObject lineaattr = attrWPrice.getAsJsonObject();
              idModifierChange = lineaattr.get("id").getAsString().trim();
              
              // SE BUSCA EL PRODUCTO CON PRECIO QUE SE DESEA HACER EL CAMBIO
              OBCriteria<Product> proCrtModWithPrice = OBDal.getInstance().createCriteria(Product.class);
              proCrtModWithPrice.add(Restrictions.eq(Product.PROPERTY_SEARCHKEY, idModifierChange));
              productModWithPrice = (Product) proCrtModWithPrice.uniqueResult();
              
              // *******************************************************
              // INICIO RECORRER EL ARRAY DE PRODUCTOS DE LA FAMILIA
              // PARA IDENTIFICAR CUAL ES EL CAMBIO
              // *******************************************************
              for (int i = 0; i < comboProList.size(); i++) {
                
                // VERIFICAR SI EL PRODUCTO DE LA FAMILIA ES IGUAL AL IDENTIFICADOR
                // DEL PRODUCTO PARA EL CAMBIO
                if(selectedModWithPrice) {
                  break;
                }
                
                if(comboProList.get(i).getProduct().getSearchKey().equals(idModifierChange)){
                  attrArr = new JsonArray();
                  JsonObject item = new JsonObject();
                  item.addProperty("quantity", lineaattr.get("quantity").getAsString().trim());
                  item.addProperty("id", lineaattr.get("id").getAsString().trim());
                  item.addProperty("name", lineaattr.get("name").getAsString().trim());
                  item.addProperty("price", lineaattr.get("price").getAsString().trim());
                  item.addProperty("is_modifier", true);
                  attrArr.add(item);
                  selectedModWithPrice = true;
                }
                
              }
              // *******************************************************
              // *******************************************************              

              position = position++;
            
            }  

          }      
          
          if(!idProductModifier.equals("")) {                
            // BUSCO EN LOS PRODUCTOS EL MODIFICADOR
            OBCriteria<Product> proCrtMod = OBDal.getInstance().createCriteria(Product.class);
            proCrtMod.add(Restrictions.eq(Product.PROPERTY_SEARCHKEY, idProductModifier));
            productMod = (Product) proCrtMod.uniqueResult();                
            for (int i = 0; i < comboProList.size(); i++) {
              if(comboProList.get(i).getProduct().getSearchKey().equals(idProductModifier)){
                selectedMod = true;
              }
            }                 
          }
          
          // *********************************************************
          // INICIO RECORRER EL ARRAY DE LAS PRODUCTOS DE LA FAMILIA
          // *********************************************************           
          for (ComboProduct comboProduct : comboProList) {
            
            // SI YA EL CAMBIO ESTA APLICADO NO SE SIGUE BUSCANDO 
            // EN LOS PRODUCTOS DE LA FAMILIA
            if(alreadySetChange) {
              alreadySetChange = false;
              break;
            }
            
            if(selectedMod) {  
              
              JSONObject result = new JSONObject();
              result.put("comboFamMain", comboFamMain);
              result.put("isCombo", true);
              result.put("m_offer_id", priceAdj.getId());
              result.put("famlily_quantity", comboFam.getQuantity());
              hmap.put(productMod,result);
              selectedMod = false;
              selectedComb = true;
              
            }else {

              if(selectedModWithPrice) {
                
                if(comboProduct.getProduct().getSearchKey().equals(idModifierChange)) {

                  JSONObject result = new JSONObject();
                  result.put("comboFamMain", comboFamMain);
                  result.put("isCombo", true);
                  result.put("m_offer_id", priceAdj.getId());
                  result.put("famlily_quantity", comboFam.getQuantity());
                  hmap.put(productModWithPrice,result);    
                  selectedModWithPrice = false;
                  alreadySetChange = true;
                  
                }else {
                  
                  if (comboProduct.isSglovoDefault() && !selectedModWithPrice) {

                    product = comboProduct.getProduct();
                    JSONObject result = new JSONObject();
                    result.put("comboFamMain", comboFamMain);
                    result.put("isCombo", true);
                    result.put("m_offer_id", priceAdj.getId());
                    result.put("famlily_quantity", comboFam.getQuantity());
                    hmap.put(product,result);  
                    
                  }                     
                  
                }                   

              }else {


                if (comboProduct.isSglovoDefault() && !selectedComb) {
                  product = comboProduct.getProduct();
                  JSONObject result = new JSONObject();
                  result.put("comboFamMain", comboFamMain);
                  result.put("isCombo", true);
                  result.put("m_offer_id", priceAdj.getId());
                  result.put("famlily_quantity", comboFam.getQuantity());
                  hmap.put(product,result);                      
                }

              }
              
            }

          }
          // *********************************************************
          // FIN RECORRER EL ARRAY DE LAS PRODUCTOS DE LA FAMILIA
          // *********************************************************          
          
        }
        // *****************************************************
        // FIN RECORRER EL ARRAY DE LAS FAMILIAS DEL COMBO
        // *****************************************************        
        
      }else {
        throw new OBException("El producto " + idProduct + " no existe en el sistema");
      } 
      
    } else {
      
      JSONObject result = new JSONObject();
      result.put("comboFamMain", false);
      result.put("isCombo", false);
      result.put("m_offer_id", "");
      result.put("famlily_quantity", Long.parseLong("1"));
      hmap.put(product,result);
      
    }           

    return hmap;
  }

  private String getResponse(ResponseWS response) {
    Gson gson = new Gson();
    String json = gson.toJson(response);
    return json;
  }
  
}
