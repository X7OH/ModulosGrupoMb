package ec.com.sidesoft.integration.pedidosya.webservices.webhooks;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.retail.discounts.combo.ComboProduct;
import org.openbravo.retail.discounts.combo.ComboProductFamily;
import org.openbravo.service.db.DalConnectionProvider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.openbravo.utils.FormatUtilities;

import ec.com.sidesoft.integration.pedidosya.SsipyJustoconfig;
import ec.com.sidesoft.integration.pedidosya.SsipyJustopaymethod;
import ec.com.sidesoft.integration.pedidosya.helpers.YaProduct;
import ec.com.sidesoft.ws.ordercreate.data.SWSOCConfig;
import ec.com.sidesoft.ws.ordercreate.data.SWSOCHommoffer;
import ec.com.sidesoft.ws.ordercreate.data.SWSOCHommofferAlt;
import ec.com.sidesoft.ws.ordercreate.data.SWSOCHomproduct;

import org.openbravo.dal.service.OBDal;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.base.exception.OBException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
//import java.util.Base64;
import org.apache.commons.codec.binary.Base64;
import java.util.HashMap;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;


public class ProcessWebHook {
	
	private final Logger logger = Logger.getLogger(ProcessWebHook.class);
	
	public String buildJSON(String body) throws JSONException {
		JSONObject obj = new JSONObject(body);
		JSONObject JSONBuild = new JSONObject();
		String txt = "OK";
		
		try {
			OBContext.setAdminMode();
			JSONObject data = obj.getJSONObject("data");
			JSONObject order = data.getJSONObject("order");			
			JSONObject store = order.getJSONObject("store");
			String storeId = store.getString("externalId");
			Organization org = OBDal.getInstance().get(Organization.class, storeId);
			SWSOCConfig utilConfig = getConfig(org.getId());
		      if (utilConfig == null) {
		        throw new OBException(
		            "No existe una configuracion definida para la organizacion " + org.getName());
		      }
			
//			JSONArray products = order.getJSONArray("items");
			String productId = "";
			Product product = new Product();
			JSONObject JSONData = new JSONObject();

			Product prd = new Product();
			
			JSONData.put("ORG_OB", storeId);
			JSONData.put("created_at", order.get("createdAt"));
			if(!order.get("deliveryType").equals("delivery")) {
				JSONData.put("pickup_store_id", storeId);
			}else {
				JSONData.put("pickup_store_id", JSONObject.NULL);
			}
			JSONData.put("id", order.get("_id"));
			
			SsipyJustopaymethod payMeth = getPayMenthod(order.getString("paymentTypeLabel"));
			
			JSONData.put("payment_method", payMeth.getFINPaymentmethod().getId());
			
//			JSONObject transaction = order.getJSONObject("transaction");
//			Detalles de Pago
			BusinessPartner cli = null;
			String cliTaxID = null;
			String paymenType = order.getString("paymentTypeLabel");
			boolean billing = order.isNull("billing");
			if(billing) {
				cli = getCliPartner("9999999999");
				cliTaxID = cli.getTaxID();
			}else {
				JSONObject bill = order.getJSONObject("billing");
				cli = getCliPartner(bill.getString("documentId"));
				if (cli==null) {
					cliTaxID = bill.getString("documentId");
				}else {
					cliTaxID = cli.getTaxID();
				}
			}
			
			JSONData.put("ID_CLIENTE_FACTURA", cliTaxID );
			JSONData.put("billing_name", order.getString("buyerName"));
			JSONData.put("CORREO_CLIENTE", order.getString("email"));
			
//			JSONObject add = order.getJSONObject("address");
			JSONObject add = new JSONObject(); 
			if(!order.isNull("address")) {
				add = order.getJSONObject("address");
			}else {
				add = store.getJSONObject("address");
			}
			JSONObject location = add.getJSONObject("location");
			
			String direccionFormat = getDirection(add.getString("address"),
					add.getString("addressSecondary"),
					add.getString("address"),
					add.getString("addressSecondary"));
			
			JSONData.put("DIRECCION_CLIENTE", direccionFormat);
//			JSONData.put("DIRECCION_CLIENTE", "Avenida Galo Plaza Lasso| Ramon Borja| piso 2| 0987023313");
			JSONData.put("NOMBRE_CLIENTE", order.getString("buyerName"));
			String notes = "";
			notes = "Cupon:"+order.getString("couponName")+" - "+order.getString("couponDiscount")+"\n"
					+ "Descuento Final: "+ order.getString("totalDiscount");
			JSONData.put("notes", "Cupon:"+order.getString("couponName")+": "+order.getString("couponDiscount"));
			
			String phoneNumber = order.getString("phone");
			if (phoneNumber.length() > 9) {
				phoneNumber = phoneNumber.substring(phoneNumber.length() - 9);
	        }
			
			JSONData.put("phone", "0"+phoneNumber);
			JSONData.put("discount", order.getString("totalDiscount"));
			
			// Ajuste de Precio en Funcion de Descuento Total
			BigDecimal DescTot = new BigDecimal(order.getString("totalDiscount"));
			BigDecimal ValueTot = new BigDecimal(order.getString("baseTotalPrice"));
			BigDecimal AdjustValue = AdjustTotalValue(ValueTot, DescTot);
		    
			// Ajuste de Impuesto de envio
			BigDecimal newFee = new BigDecimal(order.getDouble("deliveryFee"));
			newFee = applyDiscount(newFee, AdjustValue);
			
			double taxDeliveryIndex = getTaxIndexByProd(utilConfig.getProduct().getId());
			double shippingvalue = getNewPrice(newFee.doubleValue(), taxDeliveryIndex);
			
//			JSONData.put("shipping", order.getString("deliveryFee"));
			JSONData.put("shipping", String.valueOf(shippingvalue));
			JSONData.put("branded_gift_id", JSONObject.NULL);
			String lat = location.getString("lat");
			if (lat.length()>=12) {
				lat = lat.substring(0,12);
			}
			String lng = location.getString("lng");
			if (lng.length()>=12) {
				lng = lng.substring(0,12);
			}
			String coordinates = lat
								+", "+lng;
			JSONData.put("source", order.getString("source").toUpperCase());
			JSONData.put("coordinates", coordinates);	
			
			JSONArray items = order.getJSONArray("items");
			List<YaProduct> YaProducts = new ArrayList();
			
	        HashMap<String, Double> productPrices = new HashMap<>();
	        String notaI = new String("");

	        for (int i = 0; i < items.length(); i++) {
	        	int prodQty = 0;
	        	JSONObject item = items.getJSONObject(i);
	            JSONObject productJObj = item.getJSONObject("product");
	            String productExternalId = productJObj.getString("externalId");
	            prodQty = item.getInt("amount");
	            if (item.has("comment") && !item.isNull("comment")) {
	            	notaI = item.getString("comment");
	            }
	            
	            double unitPrice = item.getDouble("productPrice");
	            productPrices.put(productExternalId, unitPrice);
	            YaProducts.add(new YaProduct(productExternalId,notaI,prodQty,unitPrice));	

	         // Comprobar si 'modifiers' existe y tiene elementos
	            if (item.has("modifiers") && item.getJSONArray("modifiers").length() > 0) {
	                JSONArray modifiers = item.getJSONArray("modifiers");
	                for (int j = 0; j < modifiers.length(); j++) {
	                    JSONObject modifier = modifiers.getJSONObject(j);
	                    JSONArray options = modifier.getJSONArray("options");
	                    for (int k = 0; k < options.length(); k++) {
	                        JSONObject option = options.getJSONObject(k);
	                        String optionExternalId = option.optString("externalId", "No ID");
	                        double price = option.optDouble("price", 0.0);

	                        // Almacenar el ID del modificador y su precio
	                        if (!optionExternalId.equals("No ID")) {
	                        	YaProducts.add(new YaProduct(optionExternalId,notaI,prodQty,price));
	                        }
	                    }
	                }
	            }
	        }
	        
	        JSONArray lineItems = new JSONArray(); 
	        
	        for (YaProduct entry : YaProducts) {
	        	JSONObject lineItem = new JSONObject();
	            String optionId = entry.getExternalId();
	            String NoteP = entry.getNotaI();
	            SWSOCHomproduct prodhomo = OBDal.getInstance().get(SWSOCHomproduct.class, optionId);
	            
	            Product nprod = getProdu(optionId, utilConfig);
	            
	            
	            String prodHom = nprod.getId();
	            double price = entry.getPrice();
	            
	            price = applyDiscount(new BigDecimal(price), AdjustValue).doubleValue();
	            
	            int qty = entry.getProdQty();
	            TaxRate tx = getTaxByProd(prodHom);
	            double taxIndex = getTaxIndexByProd(prodHom);
	            double newPrice = getNewPrice(price, taxIndex);
	            
	            
	            lineItem.put("product_id", optionId);
	            lineItem.put("notes", NoteP);
	            lineItem.put("quantity", qty);
	            lineItem.put("price", newPrice);
	            lineItem.put("tax_id", tx.getId());
	            
	            JSONArray itemoptionsarr = new JSONArray();
	            JSONObject itemoptions = new JSONObject();
				itemoptions.put("feature_name", "Observacion");
				itemoptions.put("option_name", NoteP);
				itemoptionsarr.put(itemoptions);
				lineItem.put("line_items_options", itemoptionsarr);
				
				JSONArray optionsGroupsarr = new JSONArray();
	            JSONObject optionsGroup = new JSONObject();
	            optionsGroup.put("product_id", optionId);
	            optionsGroup.put("price", newPrice);
				optionsGroupsarr.put(optionsGroup);
				lineItem.put("optionGroups", optionsGroupsarr);
	            
	            lineItems.put(lineItem);
	            
	        }
//	        JSONData.put("notes", nota);
			JSONData.put("line_items", lineItems);
			
			JSONBuild.put("data", JSONData);
			
			String x = "";
			
		} catch (Exception e) {
            logger.error("Error building JSON: " + e.getMessage(), e);
            return "Error: " + e.getMessage();
        } finally {
            OBContext.restorePreviousMode();
        }
		 
		return JSONBuild.toString();
	}
	
	private SWSOCConfig getConfig(String orgId) throws Exception {
	    SWSOCConfig config = null;

		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
        ResultSet rs = null;
        String ConfigId = null;
        
        try {
            String sql = "select SWSOC_config_id from SWSOC_config where ad_org_id = ? Limit 1";
            st = conn.getPreparedStatement(sql);
            st.setString(1, orgId);
            rs = st.executeQuery();

            if (rs.next()) {
            	ConfigId = rs.getString("SWSOC_config_id");
            }

        } catch (Exception e) {
        	e.printStackTrace();
            String msg = "Error: " + e.getMessage();
		}finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
            // No cierres la conexión aquí, ya que la necesitas para la consulta a continuación.
        }

        // Obtener el objeto SsipyJustoconfig desde la base de datos
        if (ConfigId != null) {
        	config = OBDal.getInstance().get(SWSOCConfig.class, ConfigId);
        }
		
	    return config;
	  }
	
	public String getDirection(String add1, String addSec1, String add2, String addSec2) {
		String newDir = null;
		
		if(add1.length()>22){
			add1 = add1.substring(0, 22);
		}
		if(addSec1.length()>22){
			addSec1 = addSec1.substring(0, 22);
		}
		if(add2.length()>22){
			add2 = add2.substring(0, 22);
		}
		if(addSec2.length()>22){
			addSec2 = addSec2.substring(0, 22);
		}
		
		newDir = add1+"| "+addSec1+"| "+add2+"| "+addSec2;
		
		return newDir;
	}
	
//	public 
	
	public BigDecimal AdjustTotalValue(BigDecimal TotalValue, BigDecimal TotalDiscount) {
		BigDecimal newValue = new BigDecimal("0.0000"); 
		
		newValue = TotalDiscount.divide(TotalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
        // Redondear a dos decimales
		newValue = newValue.setScale(4, RoundingMode.HALF_UP);
		
		return newValue;
	}
	
	public static BigDecimal applyDiscount(BigDecimal initialValue, BigDecimal discountPercentage) {
        // Convertir el porcentaje de descuento a decimal: discountPercentage / 100
        BigDecimal discountDecimal = discountPercentage.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
        
        // Calcular el valor del descuento: initialValue * discountDecimal
        BigDecimal discountValue = initialValue.multiply(discountDecimal);
        
        // Restar el descuento al valor inicial
        BigDecimal adjustedValue = initialValue.subtract(discountValue);
        
        // Redondear a dos decimales
        adjustedValue = adjustedValue.setScale(4, RoundingMode.HALF_UP);
        
        return adjustedValue;
    }
	
	public String getNotifyResponse(String jdata, String brand) throws Exception {
		JSONObject JSONobj = new JSONObject(jdata);
		SsipyJustoconfig justolog = new SsipyJustoconfig();
		JSONObject jsonResponse = new JSONObject();
		
		OBContext.setAdminMode();
		try {
			//
			justolog = getJustoConfig(brand);
			
			if (justolog == null) {
	            throw new OBException("No configuration found");
	        }
			
			if (justolog == null) {
	            throw new OBException("No configuration found");
	        }

			    String apiToken=justolog.getSsipyApikey();
			    String urlop = justolog.getSsipyNotifyurl();
			    
			    int responseCode = 500;				
					
					if (apiToken != null) {

						try {

							URL url = new URL(urlop);
							// Crear la conexión
				            HttpURLConnection conec = (HttpURLConnection) url.openConnection();
				            conec.setRequestMethod("POST");
				            conec.setRequestProperty("Content-Type", "application/json; utf-8");
				            conec.setRequestProperty("Accept", "application/json");			            
				         // Configurar Basic Auth
				            
				            String username =  justolog.getSsipyUsername();
//				            String password = justolog.getSsipyPassword();
				            String prepass = justolog.getSsipyPassword();
				            String password = FormatUtilities.encryptDecrypt(prepass, false);
				            
				            String auth = username + ":" + password;
//				            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
				            String encodedAuth = Base64.encodeBase64String(auth.getBytes());
				            conec.setRequestProperty("Authorization", "Basic " + encodedAuth);
				            
				            conec.setDoOutput(true);
							
							//Envia el Json
							try (OutputStream os = conec.getOutputStream()) {
				                byte[] input = JSONobj.toString().getBytes("utf-8");
				                os.write(input, 0, input.length);
				            }
							
							//Leer la respuesta
				            responseCode = conec.getResponseCode();
			                if (responseCode == HttpURLConnection.HTTP_OK) {
			                    try (Scanner scanner = new Scanner(conec.getInputStream(), "UTF-8")) {
			                        String response = scanner.useDelimiter("\\A").next();
			                        jsonResponse = new JSONObject(response); // Esto asume que la respuesta es un JSON
			                    }
			                } else {
			                    try (Scanner scanner = new Scanner(conec.getErrorStream(), "UTF-8")) {
			                        String errorResponse = scanner.useDelimiter("\\A").next();
			                        logger.error("Error Response: " + errorResponse);
			                        jsonResponse.put("status", "ERROR");
			                        jsonResponse.put("message", "Error al recibir el Token. Código de respuesta: " + responseCode);
			                        jsonResponse.put("errorDetails", errorResponse);
			                    }
			                }

						} catch (Exception e) {
			                logger.error("Error in API call: " + e.getMessage(), e);
			                jsonResponse.put("status", "ERROR");
			                jsonResponse.put("message", "Error in API call");
			                jsonResponse.put("errorDetails", e.getMessage());
			            }
					} else {
			            jsonResponse.put("status", "ERROR");
			            jsonResponse.put("message", "API token is missing");
			        }

		} catch (Exception e) {
	        logger.error("Error building JSON: " + e.getMessage(), e);
	        jsonResponse.put("status", "ERROR");
	        jsonResponse.put("message", "Error fetching configuration");
	        jsonResponse.put("errorDetails", e.getMessage());
	    } finally {
	        OBContext.restorePreviousMode();
	    }

	    return jsonResponse.toString();   
		
	}
	
	public SsipyJustopaymethod getPayMenthod(String name) throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
        ResultSet rs = null;
        SsipyJustopaymethod payMeth = null;
        String payMethId = null;
        
        try {
            String sql = "SELECT ssipy_justopaymethod_id FROM ssipy_justopaymethod WHERE ssipy_identifier = ? Limit 1";
            st = conn.getPreparedStatement(sql);
            st.setString(1, name);
            rs = st.executeQuery();

            if (rs.next()) {
            	payMethId = rs.getString("ssipy_justopaymethod_id");
            }

        } catch (Exception e) {
        	e.printStackTrace();
            String msg = "Error: " + e.getMessage();
		}finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
            // No cierres la conexión aquí, ya que la necesitas para la consulta a continuación.
        }

        // Obtener el objeto SsipyJustoconfig desde la base de datos
        if (payMethId != null) {
        	payMeth = OBDal.getInstance().get(SsipyJustopaymethod.class, payMethId);
        }
		
		return payMeth;
	}
	
	public BusinessPartner getCliPartner(String identifier) throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
        ResultSet rs = null;
        BusinessPartner client = null;
        String clientId = null;
        
        try {
            String sql = "SELECT c_bpartner_id FROM c_bpartner WHERE taxid = ? Limit 1";
            st = conn.getPreparedStatement(sql);
            st.setString(1, identifier);
            rs = st.executeQuery();

            if (rs.next()) {
            	clientId = rs.getString("c_bpartner_id");
            }

        } catch (Exception e) {
        	e.printStackTrace();
            String msg = "Error: " + e.getMessage();
		}finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
            // No cierres la conexión aquí, ya que la necesitas para la consulta a continuación.
        }

        // Obtener el objeto SsipyJustoconfig desde la base de datos
        if (clientId != null) {
        	client = OBDal.getInstance().get(BusinessPartner.class, clientId);
        }
		
		return client;
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
	
	public TaxRate getTaxByProd(String prodId) throws Exception {
		PreparedStatement st = null;
		ResultSet rs = null;
		TaxRate tax = null;
		ConnectionProvider connec = new DalConnectionProvider(false);
		String taxId = null;

		try {
			String sql = "select max(Coalesce(ct.C_Tax_id, (select max(c_tax_id) tax\n"
					+ "from c_tax\n"
					+ "where isdefault = 'Y'\n"
					+ "and SOPOType = 'B'\n"
					+ "and IsTaxDeductable = 'Y'\n"
					+ "and isactive = 'Y'\n"
					+ "and rate <> 0))) as tax_id\n"
					+ "from m_product mp\n"
					+ "LEFT join c_taxcategory ctc on ctc.c_taxcategory_id = mp.c_taxcategory_id\n"
					+ "LEFT join C_Tax ct on ct.c_taxcategory_id = ctc.c_taxcategory_id\n"
					+ "where mp.m_product_id = ?\n"
					+ "and ct.isdefault = 'Y'\n"
					+ "";

			st = connec.getPreparedStatement(sql);
			st.setString(1, prodId);
			rs = st.executeQuery();
			
			if (rs.next()) {
				taxId = rs.getString("tax_id");
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
		
		if (taxId != null) {
            tax = OBDal.getInstance().get(TaxRate.class, taxId);
        }
		//
		return tax;
	}
	
	public double getTaxIndexByProd(String prodId) throws Exception {
		PreparedStatement st = null;
		ResultSet rs = null;
		double taxIndex = 0.0;
		ConnectionProvider connec = new DalConnectionProvider(false);
		String taxId = null;

		try {
			String sql = "select max(Coalesce(ct.rate, (select max(rate) tax\n"
					+ "from c_tax\n"
					+ "where isdefault = 'Y'\n"
					+ "and SOPOType = 'B'\n"
					+ "and IsTaxDeductable = 'Y'\n"
					+ "and isactive = 'Y'\n"
					+ "and rate <> 0))) as index\n"
					+ "from m_product mp\n"
					+ "LEFT join c_taxcategory ctc on ctc.c_taxcategory_id = mp.c_taxcategory_id\n"
					+ "LEFT join C_Tax ct on ct.c_taxcategory_id = ctc.c_taxcategory_id\n"
					+ "where mp.m_product_id = ?\n"
					+ "and ct.isdefault = 'Y'\n";

			st = connec.getPreparedStatement(sql);
			st.setString(1, prodId);
			rs = st.executeQuery();
			
			if (rs.next()) {
				taxIndex = rs.getDouble("index");
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
		//
		return taxIndex;
	}

	public double getNewPrice(double precioOriginal, double impuesto) {
        if (impuesto < 0) {
            throw new IllegalArgumentException("El impuesto no puede ser negativo.");
        }
        // Calcular el nuevo precio sin el impuesto
        double nuevoPrecio = precioOriginal / (1 + impuesto / 100.0);
        return nuevoPrecio;
    }
	
	private Product getProdu(String idProduct, SWSOCConfig utilConfig) throws Exception {
	      Product product = null;
	      PriceAdjustment priceAdj = null;
	      SWSOCHomproduct homoProduct = null;

	      product = getNProd(idProduct); 

	      if (product == null) {

	        homoProduct = getNHomProd(idProduct);

	        if (homoProduct != null) {

	          product = homoProduct.getProduct();
	          
	        }else {

	          // CONSULTO SI LA ORGANIZACION ES ALTERNATIVA
	          if(utilConfig.isAlternative()) {

	            //CONSULTO LOS COMBOS QUE SON DE LA ORGANIZACION ALTERNARIVA
	            SWSOCHommofferAlt homoOfferAlt = null;
	            homoOfferAlt = getNHomAltOffer(idProduct);

	            if (homoOfferAlt != null) {

	              priceAdj = homoOfferAlt.getOffer();
	              List<ComboProductFamily> comboFamLis = priceAdj.getOBCOMBOFamilyList();
	              for (ComboProductFamily comboFam : comboFamLis) {
	                List<ComboProduct> comboProList = comboFam.getOBCOMBOProductList();
	                for (ComboProduct comboProduct : comboProList) {
	                  if (comboProduct.isSwsocDomicile()) {
	                    product = comboProduct.getProduct(); 
	                  }
	                }
	              }
	            }

	          }else {

	            //CONSULTO LOS COMBOS QUE NO SON DE LA ORGANIZACION CARRION
	            SWSOCHommoffer homoOffer = null;
	            homoOffer = getNHomOffer(idProduct);
	            if (homoOffer != null) {

	              priceAdj = homoOffer.getOffer();

	              // SE OBTIENE LISTA DE LA FAMLIA DEL COMBO
	              List<ComboProductFamily> comboFamLis = priceAdj.getOBCOMBOFamilyList();

	              // *****************************************************
	              // INICIO RECORRER EL ARRAY DE LAS FAMILIAS DEL COMBO
	              // *****************************************************
	              for (ComboProductFamily comboFam : comboFamLis) {

	                Boolean alreadySetChange = false;
	                
	                // SE OBTIENE LA LISTA DE PRODUCTOS DE LA FAMILIA
	                List<ComboProduct> comboProList = comboFam.getOBCOMBOProductList();

	                if (comboProList.size() > 0) {
	                  
	                  for (ComboProduct comboProduct : comboProList) {
	                    
	                    // SI YA EL CAMBIO ESTA APLICADO NO SE SIGUE BUSCANDO
	                    // EN LOS PRODUCTOS DE LA FAMILIA                                      

	                    if (comboProduct.isSwsocDomicile()) {
	                      product = comboProduct.getProduct();
	                    }

	                  }                  
	                  
	                }else{

	                  for (ComboProduct comboProduct : comboProList) {

	                    if (comboProduct.isSwsocDomicile()) {
	                      product = comboProduct.getProduct();;
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
	    	  
	    }

	    return product;
	  }
	
	public Product getNProd(String identifier) throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
        ResultSet rs = null;
        String prodId = null;
        Product prod = null;
        
        try {
            String sql = "SELECT m_product_id FROM m_product WHERE m_product_id = ? Limit 1";
            st = conn.getPreparedStatement(sql);
            st.setString(1, identifier);
            rs = st.executeQuery();

            if (rs.next()) {
            	prodId = rs.getString("m_product_id");
            }

        } catch (Exception e) {
        	e.printStackTrace();
            String msg = "Error: " + e.getMessage();
		}finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
            // No cierres la conexión aquí, ya que la necesitas para la consulta a continuación.
        }

        // Obtener el objeto SsipyJustoconfig desde la base de datos
        if (prodId != null) {
        	prod = OBDal.getInstance().get(Product.class, prodId);
        }
		
		return prod;
	}

	public SWSOCHomproduct getNHomProd(String identifier) throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
        ResultSet rs = null;
        String homProdId = null;
        SWSOCHomproduct homProd = null;
        
        try {
            String sql = "select SWSOC_homproduct_id from SWSOC_homproduct where code = ? Limit 1";
            st = conn.getPreparedStatement(sql);
            st.setString(1, identifier);
            rs = st.executeQuery();

            if (rs.next()) {
            	homProdId = rs.getString("SWSOC_homproduct_id");
            }

        } catch (Exception e) {
        	e.printStackTrace();
            String msg = "Error: " + e.getMessage();
		}finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
            // No cierres la conexión aquí, ya que la necesitas para la consulta a continuación.
        }

        // Obtener el objeto SsipyJustoconfig desde la base de datos
        if (homProdId != null) {
        	homProd = OBDal.getInstance().get(SWSOCHomproduct.class, homProdId);
        }
		
		return homProd;
	}
	
	public SWSOCHommofferAlt getNHomAltOffer(String identifier) throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
        ResultSet rs = null;
        String homAltOfferId = null;
        SWSOCHommofferAlt homAltOffer = null;
        
        try {
            String sql = "select SWSOC_Hommoffer_alt_id from SWSOC_Hommoffer_alt where code = ? Limit 1";
            st = conn.getPreparedStatement(sql);
            st.setString(1, identifier);
            rs = st.executeQuery();

            if (rs.next()) {
            	homAltOfferId = rs.getString("SWSOC_Hommoffer_alt_id");
            }

        } catch (Exception e) {
        	e.printStackTrace();
            String msg = "Error: " + e.getMessage();
		}finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
            // No cierres la conexión aquí, ya que la necesitas para la consulta a continuación.
        }

        // Obtener el objeto SsipyJustoconfig desde la base de datos
        if (homAltOfferId != null) {
        	homAltOffer = OBDal.getInstance().get(SWSOCHommofferAlt.class, homAltOfferId);
        }
		
		return homAltOffer;
	}

	public SWSOCHommoffer getNHomOffer(String identifier) throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
        ResultSet rs = null;
        String homOfferId = null;
        SWSOCHommoffer homOffer = null;
        
        try {
            String sql = "select SWSOC_Hommoffer_id from SWSOC_Hommoffer where code = ? Limit 1";
            st = conn.getPreparedStatement(sql);
            st.setString(1, identifier);
            rs = st.executeQuery();

            if (rs.next()) {
            	homOfferId = rs.getString("SWSOC_Hommoffer_id");
            }

        } catch (Exception e) {
        	e.printStackTrace();
            String msg = "Error: " + e.getMessage();
		}finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
            // No cierres la conexión aquí, ya que la necesitas para la consulta a continuación.
        }

        // Obtener el objeto SsipyJustoconfig desde la base de datos
        if (homOfferId != null) {
        	homOffer = OBDal.getInstance().get(SWSOCHommoffer.class, homOfferId);
        }
		
		return homOffer;
	}
	
	
}
