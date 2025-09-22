package ec.com.sidesoft.special.customization.mb.ad_process.data;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.*;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.module.idljava.proc.IdlServiceJava;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DbUtility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;

public class ImportPurchaseOrdersStnd extends IdlServiceJava {

	Map<String, Order> HeadOrders = new HashMap<>();
	Long Linea = (long) 0;
	
@Override
protected boolean executeImport(String filename, boolean insert) throws Exception {
	// TODO Auto-generated method stub
	super.executeImport(filename, insert);
	
	org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get(org.openbravo.model.ad.ui.Process.class, "104");
	
	for (Order order : HeadOrders.values()) {
		try {
			ProcessInstance pInstance = CallProcess.getInstance().call(process, order.getId(), null);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	return true;
}
	
  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] {
    	new Parameter("OrganizationName", Parameter.STRING), // 0
    	new Parameter("Cliente", Parameter.STRING), // 1
		new Parameter("No Documento", Parameter.STRING), // 2
		new Parameter("Doc Transaccion", Parameter.STRING), // 3
		new Parameter("Fecha Pedido", Parameter.STRING), // 4
		new Parameter("Fecha Comprometida", Parameter.STRING), // 5
//		new Parameter("Metodo de Pago", Parameter.STRING), // 6
//		new Parameter("Condicion de Pago", Parameter.STRING), // 7
//		new Parameter("Tarifa", Parameter.STRING), // 8
		new Parameter("Centro de Costos", Parameter.STRING), // 9
		new Parameter("Almacen", Parameter.STRING), // 10
//		new Parameter("Linea", Parameter.STRING), // 11
//	    new Parameter("No. Pedido", Parameter.STRING), // 12
	    new Parameter("Producto", Parameter.STRING), // 13
	    new Parameter("Cantidad", Parameter.STRING), // 14
	    new Parameter("Nombre", Parameter.STRING) // 15
	    };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

	validator.checkString(values[0], 60);
	validator.checkString(values[1], 60);
	validator.checkString(values[2], 60);
    validator.checkString(values[3], 60);
    validator.checkString(values[4], 60);
    validator.checkString(values[5], 20);
//    validator.checkString(values[6], 20);
//    validator.checkString(values[7], 20);
//    validator.checkString(values[8], 60);
    validator.checkString(values[6], 60);
    validator.checkString(values[7], 60);
//    validator.checkString(values[11], 20);
//    validator.checkString(values[12], 60);
    validator.checkString(values[8], 60);
    validator.checkString(values[9], 17);
    validator.checkString(values[10], 60);

    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {
	  
    return createOrderline(
    		(String) values[0], (String) values[1], (String) values[2], (String) values[3],
	        (String) values[4], (String) values[5], (String) values[6], (String) values[7],
	        (String) values[8],
    		(String) values[9], (String) values[10]
	        		);
  }

  public BaseOBObject createOrderline(final String OrganizationName, final String Cliente, final String NoDocumento, final String DocTransaccion,
	      final String FechaPedido, final String FechaComprometida,  final String CentrodeCostos, final String Almacen,
		  final String Producto, final String Cantidad, final String Nombre) throws Exception {
	
	Order ObjHeadOrder = new Order();
	int exist = 0;
	  
	for (String docno : HeadOrders.keySet()) {
		if (NoDocumento.equals(docno)) {
			exist += 1;
		}
	}
	
	if (exist==0) {
		Linea = (long) 10;
		ObjHeadOrder = createOrder(OrganizationName, Cliente, NoDocumento, DocTransaccion, FechaPedido, FechaComprometida, 
				  CentrodeCostos, Almacen);
		
		HeadOrders.put(NoDocumento, ObjHeadOrder);
	} else {
		for (String docno : HeadOrders.keySet()) {
			if (NoDocumento.equals(docno)) {
				ObjHeadOrder = HeadOrders.get(docno);
				Linea += (long) 10;
			}
		}
	}
	  
	  
	

    OrderLine ObjOrderLine = OBProvider.getInstance().get(OrderLine.class);
    // Validar Pedido existente
    Order ObjOrder = findDALInstance(false, Order.class,
        new Value(Order.PROPERTY_DOCUMENTNO, ObjHeadOrder.getDocumentNo()), // NoPedido),
        new Value(Order.PROPERTY_SALESTRANSACTION, false));
    if (ObjOrder == null || ObjOrder.equals("")) {
      throw new OBException("Pedido con el número de documento: " + ObjHeadOrder.getDocumentNo() + " no existe.");
    }

    // Validar Producto Existente
    Product ObjProduct = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, Producto));
    if (ObjProduct == null || ObjProduct.equals("")) {
      throw new OBException("Producto con el identificador: " + Producto + " no existe.");
    }

	//Obtener el impuesto ligado al producto
	TaxCategory CatImpuest = findDALInstance(false, TaxCategory.class,
		new Value(TaxCategory.PROPERTY_ID, ObjProduct.getTaxCategory().getId()));
	if (ObjProduct == null || ObjProduct.equals("")) {
	throw new OBException("La Categoria de Impuesto para : " + Producto + " no existe.");
	}

    OBCriteria<TaxRate> impu = OBDal.getInstance()
        .createCriteria(TaxRate.class);
		impu.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, CatImpuest));
		impu.addOrderBy(TaxRate.PROPERTY_VALIDFROMDATE, false);
		impu.setFilterOnReadableOrganization(false);
		impu.setMaxResults(1);

	TaxRate Impuesto = (TaxRate) impu.uniqueResult();

    // Validar Impuesto Existente
    TaxRate ObjTaxRate = findDALInstance(false, TaxRate.class,
        new Value(TaxRate.PROPERTY_NAME, Impuesto.getName()));
    if (ObjTaxRate == null || ObjTaxRate.equals("")) {
      throw new OBException("Impuesto con el nombre: " + Impuesto + " no existe.");
    }
    PriceList ObjPriceList = ObjOrder.getPriceList();

    // Validar Unidad de medidad Existente
    UOMTrl ObjUOM = findDALInstance(false, UOMTrl.class, new Value(UOMTrl.PROPERTY_NAME, ObjProduct.getUOM().getName()));
    if (ObjUOM == null || ObjUOM.equals("")) {
      throw new OBException("Unidad de medida con el nombre: " + ObjProduct.getUOM().getName() + " no existe.");
    }

    // Buscar precio tarifa
    OBCriteria<PriceListVersion> ObjsPriceListVersion = OBDal.getInstance()
        .createCriteria(PriceListVersion.class);
    ObjsPriceListVersion.add(Restrictions.eq(PriceListVersion.PROPERTY_PRICELIST, ObjPriceList));
    ObjsPriceListVersion.addOrderBy(PriceListVersion.PROPERTY_VALIDFROMDATE, false);
    ObjsPriceListVersion.setFilterOnReadableOrganization(false);
    ObjsPriceListVersion.setMaxResults(1);

    PriceListVersion priceVersion = (PriceListVersion) ObjsPriceListVersion.uniqueResult();

    String PrecioTarifa = "0.0";

    if (priceVersion != null) {
      OBCriteria<ProductPrice> ObjsProductPrice = OBDal.getInstance()
          .createCriteria(ProductPrice.class);
      ObjsProductPrice.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION, priceVersion));
      ObjsProductPrice.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, ObjProduct));

      if (ObjsProductPrice.list().size() > 0) {
        for (ProductPrice prodPriceList : ObjsProductPrice.list()) {
          if (prodPriceList.getListPrice().compareTo(BigDecimal.ZERO) != 0) {
            PrecioTarifa = prodPriceList.getListPrice().toString();
          }
        }
      }
    }
	BigDecimal Cant = new BigDecimal(Cantidad);
	BigDecimal Total = new BigDecimal(PrecioTarifa);
	BigDecimal ValorImpuest = ObjTaxRate.getRate().divide(new BigDecimal("100")); 
	
	BigDecimal impTLine = (Cant.multiply(Total)).subtract((Cant.multiply(Total)).multiply(ValorImpuest));

    try {
      // Setear organización por defecto
      ObjOrderLine.setOrganization(ObjOrder.getOrganization());
      // Pedido padre
      ObjOrderLine.setSalesOrder(ObjOrder);
      // fecha
      ObjOrderLine.setOrderDate(new Date());
      // moneda del tercero
      ObjOrderLine.setCurrency(ObjOrder.getCurrency());
      // Almacen
      ObjOrderLine.setWarehouse(ObjOrder.getWarehouse());
      // Linea
      ObjOrderLine.setLineNo(new Long(Linea));
      // Producto
      ObjOrderLine.setProduct(ObjProduct);
      // Cantidad
      ObjOrderLine.setOrderedQuantity(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Cantidad)));
      // Precio
      ObjOrderLine.setUnitPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(PrecioTarifa)));
      // Precio Tarifa
      ObjOrderLine.setListPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(PrecioTarifa)));
      // Imp. Linea
      //ObjOrderLine.setLineNetAmount(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Cantidad)));
	  ObjOrderLine.setLineNetAmount(impTLine);
      // impuesto
      ObjOrderLine.setTax(ObjTaxRate);
      // Unidad de medida
      ObjOrderLine.setUOM(ObjUOM.getUOM());
      // Precio Base Neto Unitario
      ObjOrderLine
          .setStandardPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(PrecioTarifa)));
      // taxbaseAmount base Imponible
      //ObjOrderLine.setTaxableAmount(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Total)));
	  ObjOrderLine.setTaxableAmount(ObjTaxRate.getRate());
	  
      /*
       * // Total
       * ObjOrderLine.setUnitPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Precio))); //
       * TipoTarjeta ObjOrderLine.setCardType(StrTipoTarjeta); // Lote
       * ObjOrderLine.setLotName(Lote); // Fecha
       * ObjOrderLine.setDateTransaction((Parameter.DATE.parse(Fecha))); // ValorCobrado
       */
      OBDal.getInstance().save(ObjOrderLine);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      e.printStackTrace();
    }

    OBDal.getInstance().commitAndClose();
    return ObjOrderLine;
  }
  
  public Order createOrder(
		  final String OrganizationName, final String Cliente, final String NoDocumento, final String DocTransaccion,
	      final String FechaPedido, final String FechaComprometida, final String CentrodeCostos, final String Almacen
	      ) throws Exception {
	  
	  
	  
	  Order Header = OBProvider.getInstance().get(Order.class);
	  //Variables a utilizar
	  String tableDocTran = "259";
	  String TipoDocbase = "POO";
	  
	  //Configurando la Tabla
	  Table table = findDALInstance(false, Table.class, 
			  new Value(Table.PROPERTY_ID, tableDocTran),
			  new Value(Table.PROPERTY_ACTIVE, true));
	  
	  
	  //Validar Documento de Transaccion
	  DocumentType ObjDocTransaction = findDALInstance(false, DocumentType.class, 
			  new Value(DocumentType.PROPERTY_NAME, DocTransaccion),
			  new Value(DocumentType.PROPERTY_TABLE, table),
			  new Value(DocumentType.PROPERTY_DOCUMENTCATEGORY, TipoDocbase),
			  new Value(DocumentType.PROPERTY_ACTIVE, true));
	  if (ObjDocTransaction == null || ObjDocTransaction.equals("")) {
		  throw new OBException("Documento de transaccion con el nombre: " + DocTransaccion + " no existe o no cumple con los valores.");
		  }
	  
	  //Validar Cliente
	  BusinessPartner ObjBPartner = findDALInstance(false, BusinessPartner.class, 
			  new Value(BusinessPartner.PROPERTY_NAME, Cliente),
			  new Value(BusinessPartner.PROPERTY_ACTIVE, true));
	  if (ObjBPartner == null || ObjBPartner.equals("")) {
		  throw new OBException("Tercero con el nombre: " + DocTransaccion + " no existe o no cumple con los valores.");
		  }
	  
	  //Buscando Direccion Cliente
	  Location partnerDir = findDALInstance(false, Location.class,
			  new Value(Location.PROPERTY_BUSINESSPARTNER, ObjBPartner),
			  new Value(Location.PROPERTY_ACTIVE, true));
	  if (partnerDir == null || partnerDir.equals("")) {
		  throw new OBException("La Direccion del tercero: " + DocTransaccion + " no existe o no cumple con los valores.");
		  }
	  
	  //Obtener el No Documento
	  Sequence DocSequence =  findDALInstance(false, Sequence.class,
			  new Value(Sequence.PROPERTY_ID, ObjDocTransaction.getDocumentSequence().getId()),
			  new Value(Sequence.PROPERTY_ACTIVE, true));
	  
	  String pre = DocSequence.getPrefix();
	  String value = DocSequence.getNextAssignedNumber().toString();
	  String suf = DocSequence.getSuffix();
	  
	  if (pre == null || pre.equals("")) {
		  pre = "";
		  }
	  if (suf == null || suf.equals("")) {
		  suf = "";
		  }
	  if (value == null || value.equals("")) {
		  value = "";
		  }
	  
	  
	  String DocSequ = pre+value+suf;
	  try {
		DocSequence.setNextAssignedNumber(DocSequence.getNextAssignedNumber()+DocSequence.getIncrementBy());
		OBDal.getInstance().save(DocSequence);
		OBDal.getInstance().flush();				
	  } catch (Exception e) {
		  e.printStackTrace();
	  }
	  
	  //Validar Fecha Comprometida
	  Date promissedDate = getDateFormat("dd-MM-yyyy", FechaComprometida);
	  
	  //Validar Fecha Comprometida
	  Date orderedDate = getDateFormat("dd-MM-yyyy", FechaPedido);	  

	  //Validar el metodo de Pago	  
	  FIN_PaymentMethod headerPayMethod = findDALInstance(false, FIN_PaymentMethod.class, 
			  new Value(FIN_PaymentMethod.PROPERTY_NAME, ObjBPartner.getPOPaymentMethod().getName()), // MetododePago),
			  new Value(FIN_PaymentMethod.PROPERTY_ACTIVE, true));
	  if (headerPayMethod == null || headerPayMethod.equals("")) {
		  throw new OBException("El tercero "+ ObjBPartner.getName()+" no poseeMetodo de pago.");
		  }
	  
	  //Validar Condicion de Pago
	  PaymentTerm headerPayterm = findDALInstance(false, PaymentTerm.class, 
			  new Value(PaymentTerm.PROPERTY_NAME, ObjBPartner.getPOPaymentTerms().getName()),// CondiciondePago),
			  new Value(PaymentTerm.PROPERTY_ACTIVE, true));
	  if (headerPayterm == null || headerPayterm.equals("")) {
		  throw new OBException("El tercero "+ ObjBPartner.getName()+" no poseeMetodo de pago.");
		  }
	  
	  //Validar Tarifa
	  PriceList headerPriceList = findDALInstance(false, PriceList.class, 
			  new Value(PriceList.PROPERTY_NAME, ObjBPartner.getPurchasePricelist().getName()), // Tarifa),
			  new Value(PriceList.PROPERTY_ACTIVE, true));
	  if (headerPriceList == null || headerPriceList.equals("")) {
		  throw new OBException("El tercero "+ ObjBPartner.getName()+" no poseeMetodo de pago.");
		  }
	  
	  //Validar Centro de Costos
	  Costcenter headerCostCenter = findDALInstance(false, Costcenter.class, 
			  new Value(Costcenter.PROPERTY_NAME, CentrodeCostos),
			  new Value(Costcenter.PROPERTY_ACTIVE, true));
	  if (headerCostCenter == null || headerCostCenter.equals("")) {
		  throw new OBException("Centro de Costos con el nombre: " + CentrodeCostos + " no existe.");
		  }
	  
//	  OBCriteria<Organization> ObjOrga = OBDal.getInstance().createCriteria(Organization.class);
//	    ObjOrga.add(Restrictions.sqlRestriction("UPPER(" + Organization.PROPERTY_NAME + ") = ?", OrganizationName.toUpperCase(), StringType.INSTANCE));
//	    Organization organization = (Organization) ObjOrga.uniqueResult();
	  
	  OBCriteria<Organization> ObjOrga = OBDal.getInstance().createCriteria(Organization.class);
	  	ObjOrga.add(Restrictions.eq(Organization.PROPERTY_NAME, OrganizationName));
	  	ObjOrga.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
	    Organization organization = (Organization) ObjOrga.uniqueResult();
	    
//	    Organization ObjOrga = findDALInstance(false, Organization.class, 
//				  new Value(Organization.PROPERTY_NAME, OrganizationName),
//				  new Value(Organization.PROPERTY_ACTIVE, true));
//	    if (ObjOrga == null || ObjOrga.equals("")) {
//			  throw new OBException("La Organizacion: " + CentrodeCostos + " no existe.");
//			  }
	    
	  //Validar Almacen
	    Warehouse headerWarehouse = findDALInstance(false, Warehouse.class, 
				  new Value(Warehouse.PROPERTY_NAME, Almacen),
//				  new Value(Warehouse.PROPERTY_ORGANIZATION, organization),
				  new Value(Warehouse.PROPERTY_ACTIVE, true));
		  if (headerWarehouse == null || headerWarehouse.equals("")) {
			  throw new OBException("Almacen con el nombre: " + CentrodeCostos + " no existe o no pertenece ala organizacion.");
			  }
		  
		  
	  
	  final User LoggedUser = OBContext.getOBContext().getUser();
	  
	  OBContext.setAdminMode();
	  try {
		  Header.setOrganization(organization);
		  Header.setAccountingDate(orderedDate);
		  Header.setUpdatedBy(LoggedUser);
		  Header.setCreatedBy(LoggedUser);
		  Header.setDocumentNo(DocSequ);
		  Header.setDocumentStatus("DR");
		  Header.setDocumentAction("CO");
		  Header.setDocumentType(ObjDocTransaction);
		  Header.setTransactionDocument(ObjDocTransaction);
		  Header.setBusinessPartner(ObjBPartner);
		  Header.setPartnerAddress(partnerDir);
		  Header.setOrderDate(orderedDate);
		  Header.setSsmrdrProcessedDate(promissedDate);
		  Header.setScheduledDeliveryDate(promissedDate);
		  Header.setDeliveryLocation(partnerDir);
		  Header.setCurrency(organization.getCurrency());
		  Header.setPaymentTerms(headerPayterm);
		  Header.setPaymentMethod(headerPayMethod);
		  Header.setFreightCostRule("I");
		  Header.setInvoiceTerms("I");
		  Header.setDeliveryTerms("A");
		  Header.setDeliveryMethod("P");
		  Header.setPriority("5");
		  Header.setCostcenter(headerCostCenter);
		  Header.setPriceList(headerPriceList);
		  Header.setSalesTransaction(false);
		  Header.setWarehouse(headerWarehouse);
		  
		  OBDal.getInstance().save(Header);
	      OBDal.getInstance().flush();
		  
	  }catch (Exception e) {
		  Throwable ex = DbUtility.getUnderlyingSQLException(e);
	        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();


//	              String message = FIN_Utility.getExceptionMessage(e);


	      message = FIN_Utility.getExceptionMessage(e);
	      message = OBMessageUtils.translateError(message).getMessage();
	      
		  e.printStackTrace();
	  }
	  
	  
	  return Header;
	  
  }
  
  public Date getDateFormat(String formatPattern, String date) {
	    SimpleDateFormat formatter = new SimpleDateFormat(formatPattern);
	    Date fecha = new Date();
	    try {
			fecha = formatter.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return fecha;
	}

  public String changeFormatBigDecimal(String numbers) {
    String Remplace = "0";

    Remplace = numbers.replace(".", "");
    Remplace = numbers.replace(",", ".");

    return Remplace;
  }

}
