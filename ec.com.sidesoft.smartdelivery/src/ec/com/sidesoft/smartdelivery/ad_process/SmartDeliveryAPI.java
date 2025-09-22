package ec.com.sidesoft.smartdelivery.ad_process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.service.db.DalConnectionProvider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ec.com.sidesoft.smartdelivery.SSMRDRConfigSmartdelivery;
import ec.com.sidesoft.smartdelivery.SSMRDRLogsSmartdelivery;
import ec.com.sidesoft.smartdelivery.SSMRDRNotAllowedPayment;
import ec.com.sidesoft.quickbilling.advanced.SaqbOrder;

public class SmartDeliveryAPI {

  public void consumeSmartClientWS(JSONObject jsonsent) throws JSONException {

    String urlWS = "";
    String token = "";
    String adress = "";
    String phone = "";
    Order order;
    String phoneAux;
    String contractNumber;
    String clientType;
    Client ad_client_id;
    Organization ad_org_id;
    User ad_user_id;
    String latLng = "";

    OBCriteria<SSMRDRConfigSmartdelivery> config = OBDal.getInstance()
        .createCriteria(SSMRDRConfigSmartdelivery.class);
    config.add(Restrictions.eq(SSMRDRConfigSmartdelivery.PROPERTY_ACTIVE, true));
    config.addOrderBy(SSMRDRConfigSmartdelivery.PROPERTY_CREATIONDATE, false);
    config.setMaxResults(1);

    if (config.list() != null && config.list().size() > 0) {

      urlWS = config.list().get(0).getUrl().trim();
      token = config.list().get(0).getToken().trim();
      ad_client_id = config.list().get(0).getClient();
      ad_org_id = config.list().get(0).getOrganization();
      ad_user_id = config.list().get(0).getCreatedBy();
      phoneAux = config.list().get(0).getPhonealternative();
      contractNumber = config.list().get(0).getContractNumber();
      clientType = config.list().get(0).getClientType();

      String orderIdOB = jsonsent.getString("orderIdOB");

      order = OBDal.getInstance().get(Order.class, orderIdOB);

      // ORIGEN DE LA VENTA
      String origen = order.getSscmbSalesOrigin().trim();

      // SE ENVIA CUANDO ES TIPO WEB O TIPO CALL CENTER O TIPO CHATBOT
      if(origen.equals("WEB") || origen.equals("CLC") || origen.equals("CHATBOT")) {

        String sucursalid = order.getOrganization().getSsmrdrSmartdeliveryCode().trim();
        if(!sucursalid.equals("N/A") && sucursalid != null && !sucursalid.equals("")) {

          FIN_PaymentMethod metodo = order.getPaymentMethod();

          // LATITUD Y LONGUITUD DEL PEDIDO DE VENTA
          String tempLatlng = null;
          tempLatlng = order.getSaqbLongitudeLatitude();
          if(tempLatlng != null) {
            if(!tempLatlng.equals("")) {
              latLng = order.getSaqbLongitudeLatitude().trim();
            }
          }

          // NO SE ENVIA SI EL METODO DE PAGO ES UBER O GLOVO
          // SE ACTUALIZA EL CHEK DEL SMARTDELIVERY
          OBCriteria<SSMRDRNotAllowedPayment> paymnetMethod = OBDal.getInstance()
              .createCriteria(SSMRDRNotAllowedPayment.class);
          paymnetMethod.add(Restrictions.eq(SSMRDRNotAllowedPayment.PROPERTY_ACTIVE, true));
          paymnetMethod.add(Restrictions.eq(SSMRDRNotAllowedPayment.PROPERTY_FINPAYMENTMETHOD, metodo));
          paymnetMethod.addOrderBy(SSMRDRNotAllowedPayment.PROPERTY_CREATIONDATE, false);
          paymnetMethod.setMaxResults(1);

          // SI EL METODO DE PAGO ES UBER O GLOVO NO SE ENVIA A SMARTDELIVERY
          if(paymnetMethod.list() != null && paymnetMethod.list().size() > 0) {
            saveOrderCheck(order.getId());
          }else{

            // SE ENVIA SI EL PEDIDO NO ES ENTREGA EN EL LOCAL
            if(order.isSsmrdrIslocaldelivery() == false) {

              // TELEFONO DEL TERCERO ASOCIADO A LA DIRECCION DEL PEDIDO
              String telefono;
              String referencia = "";

              if(origen.equals("CLC")) {
                SaqbOrder callCenterOrder = null;

                OBCriteria<SaqbOrder> clcOrder = OBDal.getInstance().createCriteria(SaqbOrder.class);
                clcOrder.add(Restrictions.eq(SaqbOrder.PROPERTY_DOCUMENTNO, order.getDocumentNo()));
                callCenterOrder = (SaqbOrder) clcOrder.uniqueResult();

                referencia = callCenterOrder.getAddress1();
              }

              phone = order.getSaqbContactnumber().replace(".", "");
              // SI EL TELEFONO DEL TERCERO CONTIENE MAS DE 8 DIGITOS Y TIENE SOLO NUMEROS
              // DE LO CONTRARIO SACO EL TELEFONO DE LA CONFIGURACION DEL SMARTDELIVERY
              if(phone == null) {
                  // TELEFONO ES NULL
                  telefono = phoneAux;
              }else if (phone.matches("[0-9]+") && phone.length() > 8) {
                  // TELEFONO ES SOLO NUMEROS Y ES MAYOR QUE 8 DIGITOS
                  telefono = phone;
              }else if(phone.toString().equals("")) {
                  // TELEFONO ES VACIO
                  telefono = phoneAux;
              }else {
                  // TELEFONO ES CUALQUIER OTRA COSA
                  telefono = phoneAux;
              }

              // DIRECCION DEL PEDIDO DE VENTA
              adress = order.getSwsocHomeaddress();

              // SE OBTIENE LAS LINEAS DEL PEDIDO
              String productLines = "";
              List<OrderLine> listLine = order.getOrderLineList();;
              for (OrderLine line : listLine) {
                if(line.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                  String temp ="{"
                      + "\"cod_sku\": \""+line.getProduct().getSearchKey()+"\",\"nombre\": \""+line.getProduct().getName()+"\","
                      + "\"peso\": \"0.0\",\"cantidad\": \""+line.getOrderedQuantity()+"\","
                      + "\"alto\": \"0\",\"ancho\": \"0\",\"largo\": \"0\",\"seguro\": \"NO\",\"precio\": \"\","
                      + "\"fragil\": \"NO\"},";
                  productLines = productLines + temp;
                }
              }

              if(productLines.endsWith(",")) {
                productLines = productLines.substring(0, productLines.length() - 1);
              }

              String ordenid = order.getDocumentNo();
              String identificacion = order.getBusinessPartner().getTaxID();
              String clienteTmp = order.getSaqbContactdelivery();
              if (clienteTmp == null || clienteTmp.toString().equals("")) {
                clienteTmp = order.getBusinessPartner().getName().trim();
              }
              String cliente = clienteTmp;
              String mail = order.getBusinessPartner().getEEIEmail();
              String total = getGrandTotal(orderIdOB);
              String direccion = adress.trim();
              String fechaRecojo = "";
              String horaRecojo = "";
              try {
                fechaRecojo = formatDate(getCreationDate(orderIdOB),"fecha");
                horaRecojo = formatDate(getCreationDate(orderIdOB),"hora");
              } catch (ParseException e1) {
                e1.printStackTrace();
              }

              String typePaymentMethod = order.getPaymentMethod().getSaqbTypeCallCenter();
              String orderPaymentMethod = "000";
              String annotations = "";
              if(typePaymentMethod.equals("EFE")) {
                orderPaymentMethod = "EFE";
                if(origen.equals("CLC")) {
                  annotations = order.getSaqbPaymentdetail().trim();
                }
              }else {
                // NO HAY COBRANZA
                total = "";
              }

              if(!direccion.equals("") || direccion != null) {
                String lat = "0.0";
                String lng = "0.0";
                if(latLng != null) {
                  if(!latLng.equals("")) {
                    String[] parts = latLng.split(",");
                    lat = parts[0].trim();
                    lng = parts[1].trim();
                  }
                }

                direccion = direccion.replaceAll("[^a-zA-Z0-9]", " ").replaceAll("\n", "");
                referencia = referencia.replaceAll("[^a-zA-Z0-9]", " ").replaceAll("\n", "");
                annotations = annotations.replaceAll("[^a-zA-Z0-9]", " ").replaceAll("\n", "");

                String jsonToSent = "json={\"contrato\": \""+contractNumber+"\",\"cod_seguimiento\": \""+ordenid+"\","
                    + "\"num_pedido\": \""+ordenid+"\",\"fecha_pedido\": \"\","
                    + "\"hora_pedido\": \"\",\"cod_cliente\": \""+identificacion+"\",\"nom_cliente\": \""+cliente+"\","
                    + "\"telf_cliente\": \""+telefono+"\",\"email_cliente\": \""+mail+"\",\"empresa\": \"Urbano Express\","
                    + "\"marca_cliente\": \""+clientType+"\",\"tipo_cobranza\": \""+orderPaymentMethod+"\","
                    + "\"monto_cobro\": \""+total+"\",\"anotaciones\": \""+annotations+"\","
                    + "\"fecha_recojo\": \""+fechaRecojo+"\",\"hora_recojo\": \""+horaRecojo+"\",\"puntos_recojo\": [{"
                    + "\"cod_tienda\": \""+sucursalid+"\",\"contacto_tienda\": \""+order.getOrganization().getSsmrdrStoreContact()+"\","
                    + "\"apuntes\": \"\",\"productos\": ["+productLines+"]}],"
                    + "\"direcciones\": [{\"cod_localidad\": \"170150\",\"direccion\": \""+direccion+"\","
                    + "\"transversal\": \"\",\"referencia\": \""+referencia+"\",\"punto_x\":  \""+lat+"\",\"punto_y\": \""+lng+"\","
                    + "\"fecha_entrega\": \"\",\"anotacion_entrega\": \"\"}],\"documento_pod\": ["
                    + "{\"retorno_doc\": \"NO\",\"cod_pod\": \"\",\"num_documento\": \"\"}]}";

                try {

                  StringBuilder tokenUri=new StringBuilder("json=");
                  tokenUri.append(URLEncoder.encode(jsonToSent,"UTF-8"));

                  URL url = new URL(urlWS);

                  HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                  conn.setDoOutput(true);
                  conn.setRequestMethod("POST");
                  conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                  conn.setRequestProperty("token", token);
                  conn.setRequestProperty("Accept-Language", "UTF-8");
                  conn.setUseCaches(false);

                  OutputStream os = conn.getOutputStream();
                  os.write(jsonToSent.getBytes());
                  os.flush();
                  os.close();

                  BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                  String output;
                  while ((output = br.readLine()) != null) {
                    JsonParser parser = new JsonParser();
                    JsonObject json = (JsonObject) parser.parse(output);
                    String error = null;
                    error = json.get("error").getAsString().trim();
                    if(error != null && error.equals("0")) {
                      String guia_ue = json.get("guia_ue").getAsString().trim();
                      saveLogs(output, jsonToSent, order, true, ad_client_id, ad_org_id, ad_user_id,guia_ue,origen);
                    }else {
                      String message = json.get("mensaje").getAsString().trim();
                      saveLogs(message, jsonToSent, order, false, ad_client_id, ad_org_id, ad_user_id,"",origen);
                    }
                  }

                  conn.disconnect();

                } catch (MalformedURLException e) {
                  e.printStackTrace();
                } catch (IOException e) {
                  e.printStackTrace();
                }

              }

            }else {
              saveOrderCheck(order.getId());
            }

          }

        }

      }else{
        saveOrderCheck(order.getId());
      }

    }

  }

  private static String formatDate(String fecha, String type) throws ParseException {

    Date date = null;
    String dateString = null;
    SimpleDateFormat format;
    if(type.equals("fecha")){
      date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(fecha);
      format = new SimpleDateFormat("yyyy/MM/dd");
      dateString = format.format(date);
    }else{
      date = new Date();
      format = new SimpleDateFormat("HH:mm");
      dateString = format.format(date);
    }

    return dateString;

  }

  private void saveLogs(String description, String json, Order order, Boolean type,
      Client ad_client_id, Organization ad_org_id, User ad_user_id, String guia_ue, String source) {

    UUID uuid = UUID.randomUUID();
    String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

    // SE VALIDA LONGUITUD DEL JSON
    Integer maxSize = 2999;
    if(json.length() > maxSize ){
      json = json.substring(0, maxSize);
    }

    SSMRDRLogsSmartdelivery log = OBProvider.getInstance().get(SSMRDRLogsSmartdelivery.class);
    log.setNewOBObject(true);
    log.setId(randomUUIDString);
    log.setClient(ad_client_id);
    log.setOrganization(ad_org_id);
    log.setDescription(description);
    log.setJson(json);
    log.setDocumentno(order.getDocumentNo());
    log.setOrder(order);
    log.setGuide(guia_ue);
    log.setDeliveryType(source);
    if (type) {
      log.setStatus(true);
      updateOrderUrbano(order.getId(),guia_ue);
    } else {
      log.setStatus(false);
    }
    log.setCreatedBy(ad_user_id);
    log.setUpdatedBy(ad_user_id);
    OBDal.getInstance().save(log);
    OBDal.getInstance().flush();

  }

  private void updateOrderUrbano(String orderId, String guide) {
    Order orderUpdate;
    orderUpdate = OBDal.getInstance().get(Order.class, orderId);
    orderUpdate.setSsmrdrSmartdeliveryCheck(true);
    orderUpdate.setSsmrdrGuidenumber(guide);
    OBDal.getInstance().save(orderUpdate);
    OBDal.getInstance().flush();
  }

  private void saveOrderCheck(String orderId) {
    Order orderUpdate;
    orderUpdate = OBDal.getInstance().get(Order.class, orderId);
    orderUpdate.setSsmrdrSmartdeliveryCheck(true);
    OBDal.getInstance().save(orderUpdate);
    OBDal.getInstance().flush();
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
      throw new OBException("Error al consultar el totallines del pedido. " + e.getMessage());
    }
  }

  private static String getCreationDate(String order_id) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    try {

      String strSql = "SELECT created FROM c_order WHERE c_order_id = '" + order_id + "'";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("created");
      }

      return strResult;

    } catch (Exception e) {
      throw new OBException("Error al consultar la fecha de creacion del pedido. " + e.getMessage());
    }

  }

  private static String getSummedLineAmount(String order_id) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    try {

      String strSql = "SELECT totallines FROM c_order WHERE c_order_id = '" + order_id + "'";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("totallines");
      }

      return strResult;

    } catch (Exception e) {
      throw new OBException("Error al consultar el grandtotal del pedido. " + e.getMessage());
    }

  }

}
