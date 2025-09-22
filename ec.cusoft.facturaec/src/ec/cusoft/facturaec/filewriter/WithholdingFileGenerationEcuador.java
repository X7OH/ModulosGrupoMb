package ec.cusoft.facturaec.filewriter;

import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.geography.CountryTrl;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.apache.commons.lang.StringUtils;
import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;
import ec.cusoft.facturaec.generador.ECWSClient;
import ec.cusoft.facturaec.templates.OBEInvoice_I;
import ec.cusoft.facturaec.templates.OBWSEInvoice_I;

public class WithholdingFileGenerationEcuador extends AbstractFileGeneration implements
    OBEInvoice_I, OBWSEInvoice_I {

  static Logger log4j = Logger.getLogger(WithholdingFileGenerationEcuador.class);
	
  public String getFTPFolderName() {
    return "Retenciones";
  }

  public String generateFile(Invoice invoice, String rootDirectory, String lang) throws Exception {
    String strDocType = invoice.getSswhCDoctype().getEeiEdocType().toString().replaceAll("\\s", "");
    boolean boolIsEDoc = invoice.getSswhCDoctype().isEeiIsEdoc();

    if (!boolIsEDoc) {
      throw new OBException(
          "No es posible generar Documento Electrónico,la parametrización del tipo de documento no es válida.");
    }
    if (!strDocType.trim().equals("07")) {
      throw new OBException("Tipo de documento electrónico no configurado como Retención.");
    }
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

    //ELEMENTOS PRINCIPALES
    Document doc = docBuilder.newDocument();
    doc.setXmlStandalone(true);
    Element rootElement = doc.createElement("comprobanteRetencion");
    rootElement.setAttribute("id", "comprobante");
    rootElement.setAttribute("version", "1.0.0");
    doc.appendChild(rootElement);

    //INFOTRIBUTARIA
    Element infoTributaria = doc.createElement("infoTributaria");
    rootElement.appendChild(infoTributaria);

    Element tipoEmision = doc.createElement("tipoEmision");
    tipoEmision.appendChild(doc.createTextNode("1"));
    infoTributaria.appendChild(tipoEmision);

    // RUC
    String strRuc = invoice.getOrganization().getOrganizationInformationList().get(0).getTaxID();
    if (!strRuc.matches("^\\d{13}$")) {
      throw new OBException("El formato del RUC es incorrecto.");
    }
    Element ruc = doc.createElement("ruc");
    ruc.appendChild(doc.createTextNode(strRuc));
    infoTributaria.appendChild(ruc);

    // CLAVE DE ACCESO
    boolean strKeyAccessGenerate = (ClientSOAP.SelectParams(3).equals("Y") ? true : false);

    if (strKeyAccessGenerate) {
      String strKeyAccess = null;
      strKeyAccess = invoice.getEeiCodigo();
      if (strKeyAccess == null || strKeyAccess.equals("")) {
        throw new OBException("Clave de acceso no encontrada.");
      }
      if (strKeyAccess.length() != 49) {
        throw new OBException("Extensión de clave de acceso no válida (49 dígitos).");
      }
      Element keyAccess = doc.createElement("claveAcceso");
      keyAccess.appendChild(doc.createTextNode(strKeyAccess));
      infoTributaria.appendChild(keyAccess);
    }

    // TIPO DE DOCUMENTO
    if (invoice.getSswhCDoctype() == null){
        throw new OBException("No se encontró un tipo de documento para la retención.");
    }
    String strCodDoc = invoice.getSswhCDoctype().getEeiEdocType();
    Element codDoc = doc.createElement("codDoc");
    codDoc.appendChild(doc.createTextNode(strCodDoc));
    infoTributaria.appendChild(codDoc);

    // NÚMERO DE DOCUMENTO
    String orderRef = invoice.getSswhWithholdingref();

    if (orderRef == null || orderRef.equals("")) {
      throw new OBException("La factura no tiene asociada el Número de Retención.");
    }

    if (invoice.getSswhWithholdingref().length() < 8) {
      throw new OBException("Formato de número de documento inválido. (Prefijo 000-000-).");
    }
    String strSubDocumentNo = orderRef.substring(8);
    while (strSubDocumentNo.length() < 9) {
      strSubDocumentNo = "0" + strSubDocumentNo;
    }

    log4j.debug("parte 2    " + strSubDocumentNo);

    String strSubDocumentNo1 = invoice.getSswhWithholdingref().substring(0, 8);
    log4j.debug("parte 1   " + strSubDocumentNo1);
    String documentnoX = strSubDocumentNo1 + strSubDocumentNo;
    String[] documentno = null;

    log4j.debug(documentnoX);

    if (documentnoX.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
      documentno = documentnoX.split("-");
    } else {
      throw new OBException(
          "El formato del número de documento de la Orden de Compra es incorrecto (000-000-000000000).");
    }

    // ESTABLECIMIENTO
    Element estab = doc.createElement("estab");
    estab.appendChild(doc.createTextNode(documentno[0]));
    infoTributaria.appendChild(estab);

    // PUNTO DE EMISIÓN
    Element ptoEmi = doc.createElement("ptoEmi");
    ptoEmi.appendChild(doc.createTextNode(documentno[1]));
    infoTributaria.appendChild(ptoEmi);

    // SECUENCIAL
    Element secuencial = doc.createElement("secuencial");
    secuencial.appendChild(doc.createTextNode(documentno[2]));
    infoTributaria.appendChild(secuencial);

    // DIRECCIÓN MATRIZ
    String headquartersCountry = null;
    try {
      OBContext.setAdminMode(true);
      for (CountryTrl countryTrl : invoice.getOrganization().getOrganizationInformationList()
          .get(0).getLocationAddress().getCountry().getCountryTrlList()) {
        if (countryTrl.getLanguage().getLanguage().equals(lang)) {
          headquartersCountry = countryTrl.getName();
        }
      }
    } catch (NullPointerException e) {
      throw new OBException("La Organización no tiene dirección.");
    } finally {
      OBContext.restorePreviousMode();
    }

    if (headquartersCountry == null) {
      headquartersCountry = invoice.getOrganization().getOrganizationInformationList().get(0)
          .getLocationAddress().getCountry().getName();
    }

    String headQuartersaddress = (invoice.getOrganization().getOrganizationInformationList().get(0)
        .getLocationAddress().getAddressLine1() == null ? " " : invoice.getOrganization()
        .getOrganizationInformationList().get(0).getLocationAddress().getAddressLine1())
        + "--"
        + (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getAddressLine2() == null ? " " : invoice.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getAddressLine2())
        + "--"
        + (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getPostalCode() == null ? " " : invoice.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getPostalCode())
        + "--"
        + (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getCityName() == null ? " " : invoice.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getCityName())
        + "--"
        + (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getRegion() == null ? " " : invoice.getOrganization().getOrganizationInformationList()
            .get(0).getLocationAddress().getRegion().getName()) + "--" + headquartersCountry;


    // INFO COMPROBANTE RETENCIÓN
    Element infoCompRetencion = doc.createElement("infoCompRetencion");
    rootElement.appendChild(infoCompRetencion);

    log4j.debug("Clave" + infoCompRetencion);

    // FECHA DE EMISIÓN
    Element fechaEmision = doc.createElement("fechaEmision");
    SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");

    if (invoice.getSswhDatewithhold() == null) {
      throw new OBException("Fecha Retención no seleccionada.");
    }
    fechaEmision.appendChild(doc.createTextNode(ecFormat.format(invoice.getSswhDatewithhold())));
    infoCompRetencion.appendChild(fechaEmision);

    // DIRECCIÓN ESTABLECIMIENTO
    String country = null;
    try {
      OBContext.setAdminMode(true);
      for (CountryTrl countryTrl : invoice.getPartnerAddress().getLocationAddress().getCountry()
          .getCountryTrlList()) {
        if (countryTrl.getLanguage().getLanguage().equals(lang)) {
          country = countryTrl.getName();
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    if (country == null) {
      country = invoice.getPartnerAddress().getLocationAddress().getCountry().getName();
    }

    String address = (invoice.getPartnerAddress().getLocationAddress().getAddressLine1() == null ? " "
        : invoice.getPartnerAddress().getLocationAddress().getAddressLine1())
        + "--"
        + (invoice.getPartnerAddress().getLocationAddress().getAddressLine2() == null ? " "
            : invoice.getPartnerAddress().getLocationAddress().getAddressLine2())
        + "--"
        + (invoice.getPartnerAddress().getLocationAddress().getPostalCode() == null ? " " : invoice
            .getPartnerAddress().getLocationAddress().getPostalCode())
        + "--"
        + (invoice.getPartnerAddress().getLocationAddress().getCityName() == null ? " " : invoice
            .getPartnerAddress().getLocationAddress().getCityName())
        + "--"
        + (invoice.getPartnerAddress().getLocationAddress().getRegion() == null ? " " : invoice
            .getPartnerAddress().getLocationAddress().getRegion().getName()) + "--" + country;

    if (headQuartersaddress == null) {
      throw new OBException("Dirección de Establecimiento es nula.");
    } else {
      headQuartersaddress = headQuartersaddress.replaceAll("[\n]", " ");
    }
    Element dirEstablecimiento = doc.createElement("dirEstablecimiento");
    dirEstablecimiento.appendChild(doc.createTextNode(truncate(headQuartersaddress, 300)));
    infoCompRetencion.appendChild(dirEstablecimiento);

    String idType;

    if (invoice.getBusinessPartner().getSswhTaxidtype().equals("R")) {
      idType = "04";
    } else if (invoice.getBusinessPartner().getSswhTaxidtype().equals("D")) {
      idType = "05";
    } else if (invoice.getBusinessPartner().getSswhTaxidtype().equals("P")) {
      idType = "06";
    } else {
      idType = "07";
    }

    Element tipoIdentificacionSujetoRetenido = doc
        .createElement("tipoIdentificacionSujetoRetenido");
    tipoIdentificacionSujetoRetenido.appendChild(doc.createTextNode(idType)); // 04 RUC
    // 05 CÉDULIA
    // 06 PASAPORTE
    // 07 CONSUMIDOR

    infoCompRetencion.appendChild(tipoIdentificacionSujetoRetenido);

    // RAZÓN SOCIAL
    String strName2 = invoice.getBusinessPartner().getName();
    if (strName2 == null || strName2.trim().equals("")) {
      throw new OBException("El cliente no tiene nombre fiscal.");
    }
    strName2 = truncate(strName2, 300);
    strName2 = strName2.replaceAll("[\n]", " ");
    Element razonSocialComprador = doc.createElement("razonSocialSujetoRetenido");
    razonSocialComprador.appendChild(doc.createTextNode(strName2));
    infoCompRetencion.appendChild(razonSocialComprador);

    log4j.debug("Razon Social" + strName2);

    // IDENTIFICACIÓN
    String strTaxid = invoice.getBusinessPartner().getTaxID();
    if (strTaxid == null || strTaxid.trim().equals("")) {
      throw new OBException("El cliente no tiene RUC.");
    }

    Element identificacionComprador = doc.createElement("identificacionSujetoRetenido");
    identificacionComprador.appendChild(doc.createTextNode(strTaxid));
    infoCompRetencion.appendChild(identificacionComprador);

    // PERÍODO FISCAL
    int year = invoice.getSswhDatewithhold().getYear() + 1900;
    int month = invoice.getSswhDatewithhold().getMonth() + 1;

    String strYear = String.valueOf(year);
    String strMonth = String.valueOf(month);

    if (month < 10)
      strMonth = "0" + strMonth;

    String fiscalPeriod = strMonth + "/" + strYear;

    Element fiscalP = doc.createElement("periodoFiscal");
    fiscalP.appendChild(doc.createTextNode(fiscalPeriod));
    infoCompRetencion.appendChild(fiscalP);

    // IMPUESTOS
    Element impuestos = doc.createElement("impuestos");
    rootElement.appendChild(impuestos);

    ConnectionProvider conn = new DalConnectionProvider(false);
    Statement sentencia = null;
    ResultSet resultado;
    sentencia = conn.getStatement();
    String StrQuery = "select * from eei_view_invoice where c_invoice_id ='"
        + invoice.getId().toString() + "'";
    resultado = sentencia.executeQuery(StrQuery);

    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
    simbolos.setDecimalSeparator('.');
    DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

    int lineNO = 0;
    int countInvoice = 0;

    log4j.debug("\nNumero de lineas encontradas " + resultado.getRow());

    // GENERACIÓN DE LÍNEAS
    while (resultado.next()) {

      lineNO += 10;

      double taxAmount = resultado.getDouble("taxamt");
      double taxableAmount = resultado.getDouble("taxbaseamt");

      TaxRate objTaxRate = OBDal.getInstance().get(TaxRate.class, resultado.getString("c_tax_id"));

      if (taxAmount <= 0 && !objTaxRate.isTaxdeductable() && taxableAmount > 0) {
        countInvoice++;

        				// Ticket 8906 - No iva 332
				boolean taxRateNoElectronic = false;
				if (objTaxRate.isEeiNoElectronic() != null) {
				  taxRateNoElectronic = objTaxRate.isEeiNoElectronic();
				}

				boolean taxPayerNoElectronic = false;
				if (invoice.getBusinessPartner().getSSWHTaxpayer().isEEINoElectronic() != null) {
				  taxPayerNoElectronic = invoice.getBusinessPartner().getSSWHTaxpayer().isEEINoElectronic();
				}

 				if ((!taxRateNoElectronic && !taxPayerNoElectronic)
				    || (taxRateNoElectronic && !taxPayerNoElectronic)
				    || (!taxRateNoElectronic && taxPayerNoElectronic)) {

            Element impuesto = doc.createElement("impuesto");
            impuestos.appendChild(impuesto);

            String taxCategoryStr = resultado.getString("em_eei_sri_tax_type");
            String taxRetentionCodeStr = "00000";

            String taxName = resultado.getString("em_eei_sri_taxcat_code");

            log4j.debug("taxName" + taxName);
            if (taxName == null || taxName.equals("")) {
              String msg = "En la Tasa de Impuesto de la línea de impuestos número " + lineNO
                  + " no está configurado el campo \"Identificador SRI de Impuestos\".";

              throw new OBException(msg);
            }

            if (taxCategoryStr.equals("1")) {
              taxRetentionCodeStr = taxName;
              log4j.debug("taxRetentionCodeStr" + taxRetentionCodeStr);

              if (taxRetentionCodeStr.equals("")) {
                String msg = "Para las facturas con impuesto del tipo Rentención RENTA los únicos conceptos de impuesto permitidos son aquellos cuyos códigos son:"
                    + " 303, 304, 307, 308, 309, 310, 311, 312, 320, 322, 332, 340, 341 , 421 , 500 , 501 , 502 , 503 y 524. Verificar que en la configuración de la Tasa de Impuesto"
                    + " definida para la línea de impuestos número "
                    + lineNO
                    + " el campo "
                    + " \"Nombre\" incluya uno de estos códigos.";

                throw new OBException(msg);
              }
            } else if (taxCategoryStr.equals("2")) {
              taxRetentionCodeStr = taxName;

              if (taxRetentionCodeStr.equals("")) {
                String msg = "Para las facturas con impuesto del tipo Rentención IVA los únicos conceptos de impuesto permitidos son aquellos con códigos: "
                    + "1, 2 y 3. Verificar que en la configuración de la Tasa de Impuesto"
                    + " definida para la línea de impuestos número "
                    + lineNO
                    + " el campo "
                    + " \"Nombre\" incluya uno de estos códigos.";

                throw new OBException(msg);
              }
            } else if (taxCategoryStr.equals("6"))
              taxCategoryStr = "4580";
            else {
              String msg = "La línea No." + " tiene el Tipo de Impuesto " + taxCategoryStr
                  + ". Los valores válidos son RETENCIONES RENTA, RETENCIONES IVA o ISD.";

              throw new OBException(msg);
            }

            Element taxCod = doc.createElement("codigo");
            taxCod.appendChild(doc.createTextNode(taxCategoryStr));
            impuesto.appendChild(taxCod);

            log4j.debug("TaxCod" + taxCategoryStr);

            Element codigoRetencion = doc.createElement("codigoRetencion");
            codigoRetencion.appendChild(doc.createTextNode(taxRetentionCodeStr));
            impuesto.appendChild(codigoRetencion);

            log4j.debug("codigoRetencion" + taxRetentionCodeStr);

            Element baseImponible = doc.createElement("baseImponible");
            baseImponible.appendChild(doc.createTextNode(formateador.format(taxableAmount).toString()));
            impuesto.appendChild(baseImponible);

            log4j.debug("baseImponible" + formateador.format(taxableAmount).toString());
            // PORCENTAJE A RETENER
            double drate = resultado.getDouble("rate");

            if (drate < 0)
              drate = drate * (-1);

            Element porcientoRetener = doc.createElement("porcentajeRetener");
            porcientoRetener.appendChild(doc.createTextNode(String.valueOf(formateador.format(drate))));
            impuesto.appendChild(porcientoRetener);

            log4j.debug("porcentajeRetener" + formateador.format(drate).toString());

            // VALOR RETENIDO
            if (taxAmount < 0)
              taxAmount = taxAmount * (-1);

            Element valorRetenido = doc.createElement("valorRetenido");
            valorRetenido.appendChild(doc.createTextNode(formateador.format(taxAmount).toString()));
            impuesto.appendChild(valorRetenido);

            log4j.debug("valorRetenido" + formateador.format(taxAmount).toString());

            // CÓDIGO DOCUMENTO SUSTENTO
            if (invoice.getSswhLivelihood() == null) {
              throw new OBException("No existe un tipo de comprobante de sustento seleccionado.");
            }
            String strCodigoSustento = invoice.getSswhLivelihood().getSearchKey();
            Element codDocSustento = doc.createElement("codDocSustento");
            codDocSustento.appendChild(doc.createTextNode(strCodigoSustento));
            impuesto.appendChild(codDocSustento);

            // NÚMERO DE DOCUMENTO SUSTENTO
            if (invoice.getOrderReference() == null) {
              throw new OBException("No existe número de documento de sustento.");
            }
            Element numDocSustento = doc.createElement("numDocSustento");
            numDocSustento.appendChild(doc.createTextNode(invoice.getOrderReference().replaceAll("-",
                "")));
            impuesto.appendChild(numDocSustento);

            log4j.debug("numDocSustento" + invoice.getOrderReference().replaceAll("-", ""));

            // FECHA EMISIÓN
            Element fechaEmisionDocSustento = doc.createElement("fechaEmisionDocSustento");
            fechaEmisionDocSustento.appendChild(doc.createTextNode(ecFormat.format(invoice
                .getInvoiceDate())));
            impuesto.appendChild(fechaEmisionDocSustento);
            log4j.debug("fechaEmisionDocSustento" + ecFormat.format(invoice.getInvoiceDate()));
          }
      }

    }
    
    try{
    	conn.destroy();
    }catch(Exception e){
    	
    }
    
    if (countInvoice == 0) {
      throw new OBException("No se ha encontrado impuestos válidos para la retención.");
    }

    // INFOADICIONAL

    OBCriteria<Organization> ObjOrganization = OBDal.getInstance().createCriteria(
    		Organization.class);
    OrganizationType objOrganizationType = OBDal.getInstance().get(OrganizationType.class, "1");
    ObjOrganization.add(Restrictions.eq(Organization.PROPERTY_ORGANIZATIONTYPE, objOrganizationType));
    
    String strWithholdingAgent = null;

    if (ObjOrganization.list().size()>0) {
    	strWithholdingAgent = ObjOrganization.list().get(0).getEeiWithholdingAgent();
    }
    
    if ((((invoice.getDescription() != null && !invoice.getDescription().trim().equals("")) || (invoice
        .getEeiDescription() != null && !invoice.getEeiDescription().trim().equals(""))) && !invoice
        .getSswhCDoctype().getEeiDescriptionfields().equals("NO"))
        || (invoice.getBusinessPartner().getSswhTaxidtype().equals("P"))
        || (invoice.getBusinessPartner().getName2() != null)
        || (strWithholdingAgent != null)) {

      Element infoAdicional = doc.createElement("infoAdicional");
      rootElement.appendChild(infoAdicional);

      if (strWithholdingAgent != null) {
          Element campoAdicional0 = doc.createElement("campoAdicional");
          campoAdicional0.setAttribute("nombre", "agenteRetencion");
          campoAdicional0.appendChild(doc.createTextNode(strWithholdingAgent));
          infoAdicional.appendChild(campoAdicional0);
      }   

      if (invoice.getBusinessPartner().getSswhTaxidtype().equals("P")) {
        Element campoAdicional1 = doc.createElement("campoAdicional");
        campoAdicional1.setAttribute("nombre", "Pasaporte");
        campoAdicional1.appendChild(doc.createTextNode(invoice.getBusinessPartner().getTaxID()));
        infoAdicional.appendChild(campoAdicional1);
      }

      if (invoice.getBusinessPartner().getName2() != null) {

        Element campoAdicional3 = doc.createElement("campoAdicional");
        campoAdicional3.setAttribute("nombre", "NombreComercialTercero");
        campoAdicional3.appendChild(doc.createTextNode(truncate(invoice.getBusinessPartner()
            .getName2(), 300)));
        infoAdicional.appendChild(campoAdicional3);
      }

      if (((invoice.getDescription() != null && !invoice.getDescription().trim().equals("")) || (invoice
          .getEeiDescription() != null && !invoice.getEeiDescription().trim().equals("")))
          && !invoice.getSswhCDoctype().getEeiDescriptionfields().equals("NO")) {
        String StrUnionCadenaSinSaltos = "";

        if (invoice.getSswhCDoctype().getEeiDescriptionfields().equals("DEDA")) {

          StrUnionCadenaSinSaltos = (invoice.getDescription() == null ? "" : invoice
              .getDescription())
              + ";"
              + (invoice.getEeiDescription() == null ? "" : invoice.getEeiDescription());

        } else if (invoice.getSswhCDoctype().getEeiDescriptionfields().equals("DE")) {

          StrUnionCadenaSinSaltos = (invoice.getDescription() == null ? "" : invoice
              .getDescription());

        } else if (invoice.getSswhCDoctype().getEeiDescriptionfields().equals("DA")) {

          StrUnionCadenaSinSaltos = (invoice.getEeiDescription() == null ? "" : invoice
              .getEeiDescription());

        }

        StrUnionCadenaSinSaltos = String.valueOf(StrUnionCadenaSinSaltos).replaceAll("[\n]", ";");

        StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos.replaceAll(";;;", ";");
        StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos.replaceAll(";;", ";");

        String strCadenaParcial = "";
        String strCadenaConcatenada = "";
        int intContador = 0;
        int j = 0;
        for (int i = 0; i < StrUnionCadenaSinSaltos.length(); i = i + 300) {
          j = i + 300;
          if (j > StrUnionCadenaSinSaltos.length()) {
            break;
          }
          strCadenaParcial = StrUnionCadenaSinSaltos.substring(i, j);
          strCadenaConcatenada = strCadenaConcatenada + strCadenaParcial;
          intContador = intContador + 1;
          Element campoAdicional2 = doc.createElement("campoAdicional");
          campoAdicional2.setAttribute("nombre", "Descripción" + intContador);
          campoAdicional2.appendChild(doc.createTextNode(truncate(strCadenaParcial, 300)));
          infoAdicional.appendChild(campoAdicional2);
        }
        if (strCadenaConcatenada.length() < StrUnionCadenaSinSaltos.length()) {
          intContador = intContador + 1;
          strCadenaParcial = StrUnionCadenaSinSaltos.substring(strCadenaConcatenada.length(),
              StrUnionCadenaSinSaltos.length());
          Element campoAdicional2 = doc.createElement("campoAdicional");
          campoAdicional2.setAttribute("nombre", "Descripción" + intContador);
          campoAdicional2.appendChild(doc.createTextNode(truncate(strCadenaParcial, 300)));
          infoAdicional.appendChild(campoAdicional2);
        }

      }
    }

    DOMSource domSource = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.transform(domSource, result);

    return writer.toString();
  }

  public String generateFile(Set<Invoice> invoices, String rootDirectory, String lang)
      throws Exception {
    return null;// new File(rootDirectory, "ecuador.xml");
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

  public boolean validateFile(File file) throws Exception {
    URL schemaFile = new URL("http://cheli.aradaen.com/factura.xsd");
    Source xmlFile = new StreamSource(file);
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
    Schema schema = schemaFactory.newSchema(schemaFile);
    Validator validator = schema.newValidator();
    try {
      validator.validate(xmlFile);
    } catch (SAXException e) {
      return false;
    }
    return true;
  }

  @Override
  public String sendFile(ConnectionProvider con, File file, Invoice invoice, String strLanguage)
      throws Exception {

    ECWSClient client = new ECWSClient();
    String res = null;
    return res;
  }

  private String getRetentionRentCode(String taxName) {
    String code = "";

    if (taxName.contains("311"))
      code = "311";

    if (taxName.contains("309"))
      code = "309";

    if (taxName.contains("310"))
      code = "310";

    if (taxName.contains("341"))
      code = "341";

    if (taxName.contains("303"))
      code = "303";

    if (taxName.contains("304"))
      code = "304";

    if (taxName.contains("308"))
      code = "308";

    if (taxName.contains("320"))
      code = "320";

    if (taxName.contains("322"))
      code = "322";

    if (taxName.contains("340"))
      code = "340";

    if (taxName.contains("307"))
      code = "307";

    if (taxName.contains("312"))
      code = "312";

    if (taxName.contains("332"))
      code = "332";

    if (taxName.contains("323"))
      code = "323";

    if (taxName.contains("327"))
      code = "327";

    if (taxName.contains("340A"))
      code = "340A";

    if (taxName.contains("340B"))
      code = "340B";

    if (taxName.contains("343"))
      code = "343";

    if (taxName.contains("344"))
      code = "344";

    if (taxName.contains("421"))
      code = "421";

    if (taxName.contains("500"))
      code = "500";

    if (taxName.contains("501"))
      code = "501";

    if (taxName.contains("502"))
      code = "502";

    if (taxName.contains("503"))
      code = "503";

    if (taxName.contains("524"))
      code = "524";

    return code;
  }

  private String getRetentionIVACode(String taxName) {
    String code = "";

    if (taxName.contains("1"))
      code = "1";

    if (taxName.contains("2"))
      code = "2";

    if (taxName.contains("3"))
      code = "3";

    if (taxName.contains("9"))

      code = "9";

    if (taxName.contains("10"))

      code = "10";

    return code;
  }
}
