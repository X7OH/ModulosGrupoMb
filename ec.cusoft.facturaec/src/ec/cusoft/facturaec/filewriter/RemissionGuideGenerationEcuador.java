/************************************************************************************ 
 * Copyright (C) 2012 Cheli Pineda Ferrer 
 * Licensed under the General Public License 3.0 
 * You may obtain a copy of the License at http://www.gnu.org/licenses/gpl.html
 ************************************************************************************/

package ec.cusoft.facturaec.filewriter;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

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

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.geography.CountryTrl;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
//import ec.cusoft.facturaec.templates.OBEInvoice_I;
//import ec.cusoft.facturaec.templates.OBWSEInvoice_I;

import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;

//import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

public class RemissionGuideGenerationEcuador {

  static Logger log4j = Logger.getLogger(RemissionGuideGenerationEcuador.class);

  public String generateFile(ShipmentInOut inout, String lang) throws Exception {
    String strDocType = inout.getDocumentType().getEeiEdocType().toString().replaceAll("\\s", "");
    boolean boolIsEDoc = inout.getDocumentType().isEeiIsEdoc();

    if (!boolIsEDoc) {
      throw new OBException(
          "No es posible generar Documento Electrónico,la parametrización del tipo de documento no es válida.");
    }
    if (!strDocType.trim().equals("06")) {
      throw new OBException("Tipo de documento electrónico no configurado como Guía de Remisión.");
    }
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

    // root elements
    Document doc = docBuilder.newDocument();
    doc.setXmlStandalone(true);
    Element rootElement = doc.createElement("guiaRemision");
    rootElement.setAttribute("id", "comprobante");
    rootElement.setAttribute("version", "1.0.0");
    doc.appendChild(rootElement);

    // infoTributaria elements
    Element infoTributaria = doc.createElement("infoTributaria");
    rootElement.appendChild(infoTributaria);

    Element tipoEmision = doc.createElement("tipoEmision");
    tipoEmision.appendChild(doc.createTextNode("1")); // 1 normal - 2
    // indisponibilidad
    // del
    // sistema(No existe
    // en offline)
    infoTributaria.appendChild(tipoEmision);

    // RUC
    String strRuc = inout.getOrganization().getOrganizationInformationList().get(0).getTaxID();
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
      strKeyAccess = inout.getEeiCodigo();
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
    String strCodDoc = inout.getDocumentType().getEeiEdocType();
    Element codDoc = doc.createElement("codDoc");
    codDoc.appendChild(doc.createTextNode(strCodDoc));
    infoTributaria.appendChild(codDoc);

    // NÚMERO DE DOCUMENTO
    String strSubDocumentNo = null;
    try {
      strSubDocumentNo = inout.getDocumentNo().substring(8);
    } catch (Exception e) {
      throw new OBException(
          "La secuencia del número de documento no es correcta. Por favor compruebe la parametrización del documento transacción.");
    }

    while (strSubDocumentNo.length() < 9) {
      strSubDocumentNo = "0" + strSubDocumentNo;
    }
    log4j.debug("parte 2    " + strSubDocumentNo);

    String strSubDocumentNo1 = truncate(inout.getDocumentNo(), 8);
    log4j.debug("parte 1   " + strSubDocumentNo1);
    String documentnoX = strSubDocumentNo1 + strSubDocumentNo;
    String[] documentno = null;

    log4j.debug(documentnoX);

    if (documentnoX.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
      documentno = documentnoX.split("-");
    } else {
      throw new OBException("El formato del número de documento es incorrecto (000-000-000000000).");
    }

    Element estab = doc.createElement("estab");
    estab.appendChild(doc.createTextNode(documentno[0]));
    infoTributaria.appendChild(estab);

    Element ptoEmi = doc.createElement("ptoEmi");
    ptoEmi.appendChild(doc.createTextNode(documentno[1]));
    infoTributaria.appendChild(ptoEmi);

    Element secuencial = doc.createElement("secuencial");
    secuencial.appendChild(doc.createTextNode(documentno[2]));
    infoTributaria.appendChild(secuencial);

    String headquartersCountry = null;
    try {
      OBContext.setAdminMode(true);
      for (CountryTrl countryTrl : inout.getOrganization().getOrganizationInformationList().get(0)
          .getLocationAddress().getCountry().getCountryTrlList()) {
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
      headquartersCountry = inout.getOrganization().getOrganizationInformationList().get(0)
          .getLocationAddress().getCountry().getName();
    }

    String headQuartersaddress = (inout.getOrganization().getOrganizationInformationList().get(0)
        .getLocationAddress().getAddressLine1() == null ? " " : inout.getOrganization()
        .getOrganizationInformationList().get(0).getLocationAddress().getAddressLine1())
        + "--"
        + (inout.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getAddressLine2() == null ? " " : inout.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getAddressLine2())
        + "--"
        + (inout.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getPostalCode() == null ? " " : inout.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getPostalCode())
        + "--"
        + (inout.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getCityName() == null ? " " : inout.getOrganization().getOrganizationInformationList()
            .get(0).getLocationAddress().getCityName())
        + "--"
        + (inout.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getRegion() == null ? " " : inout.getOrganization().getOrganizationInformationList()
            .get(0).getLocationAddress().getRegion().getName()) + "--" + headquartersCountry;

    Element infoGuiaRemision = doc.createElement("infoGuiaRemision");
    rootElement.appendChild(infoGuiaRemision);

    SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");

    Element dirEstablecimiento = doc.createElement("dirEstablecimiento");
    dirEstablecimiento.appendChild(doc.createTextNode(truncate(headQuartersaddress, 300)));
    infoGuiaRemision.appendChild(dirEstablecimiento);

    String strDireccionPartida = null;
    String strCodEstabDestino = null;
    String strM_InOut_id = null;
    List<ShipmentInOutLine> lstLineas = inout.getMaterialMgmtShipmentInOutLineList();

    Collections.sort(lstLineas, new Comparator<ShipmentInOutLine>() {
      @Override
      public int compare(ShipmentInOutLine o1, ShipmentInOutLine o2) {
        // TODO Auto-generated method stub
        return o1.getLineNo().compareTo(o2.getLineNo());
      }
    });

    if (lstLineas.get(0).getStorageBin() == null) {
      throw new OBException("Hueco no configurado en la primera línea del documento.");
    }

    if (lstLineas != null) {
      try {

        strDireccionPartida = (lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress()
            .getAddressLine1() == null ? " " : lstLineas.get(0).getStorageBin().getWarehouse()
            .getLocationAddress().getAddressLine1())
            + "--"
            + (lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress()
                .getAddressLine2() == null ? " " : lstLineas.get(0).getStorageBin().getWarehouse()
                .getLocationAddress().getAddressLine2())
            + "--"
            + (lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress().getPostalCode() == null ? " "
                : lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress()
                    .getPostalCode())
            + "--"
            + (lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress().getCityName() == null ? " "
                : lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress()
                    .getCityName())
            + "--"
            + (lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress().getRegion() == null ? " "
                : lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress().getRegion()
                    .getName())

            + "--"
            + (lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress().getCountry() == null ? " "
                : lstLineas.get(0).getStorageBin().getWarehouse().getLocationAddress().getCountry()
                    .getName());

        if (lstLineas.get(0).getStorageBin().getWarehouse().getEeiIdentifier() != null
            && isNumeric(lstLineas.get(0).getStorageBin().getWarehouse().getEeiIdentifier())) {
          strCodEstabDestino = truncate(lstLineas.get(0).getStorageBin().getWarehouse()
              .getEeiIdentifier().toString(), 3);
        } else {
          throw new OBException("Identificador de Almacén no válido.");
        }

        if (strDireccionPartida == null || strDireccionPartida.equals("")) {
          throw new OBException("Dirección de partida no configurada (Almacén). ");
        }
        strDireccionPartida = truncate(strDireccionPartida, 300);

        strM_InOut_id = lstLineas.get(0).getId();

      } catch (Exception e) {
        throw new OBException("Error al obtener dirección de sálida. " + e.getMessage().toString());
      }
    } else {
      throw new OBException("El albarán no tiene líneas.");
    }
    Element dirPartida = doc.createElement("dirPartida");
    dirPartida.appendChild(doc.createTextNode(strDireccionPartida));
    infoGuiaRemision.appendChild(dirPartida);

    if (inout.getSlciShipper() == null) {
      throw new OBException("Transportista no seleccionado (Shipper).");
    }

    if (inout.getSlciShipper().getBusinessPartner() == null) {
      throw new OBException("Tercero configurado en transportista no seleccionado (Shipper).");
    }

    String strNombreComercial = inout.getSlciShipper().getBusinessPartner().getName();
    if (strNombreComercial == null || strNombreComercial.trim().equals("")) {
      throw new OBException("El transportista no tiene nombre fiscal.");
    }
    strNombreComercial = truncate(strNombreComercial, 300);
    Element razonSocialTransportista = doc.createElement("razonSocialTransportista");
    razonSocialTransportista.appendChild(doc.createTextNode(strNombreComercial));
    infoGuiaRemision.appendChild(razonSocialTransportista);

    String idType = "";

    if (inout.getSlciShipper().getBusinessPartner().getSswhTaxidtype().equals("R")) {
      idType = "04";
    } else if (inout.getSlciShipper().getBusinessPartner().getSswhTaxidtype().equals("D")) {
      idType = "05";
    } else if (inout.getSlciShipper().getBusinessPartner().getSswhTaxidtype().equals("P")) {
      idType = "06";
    } else {
      idType = "07";
    }

    Element tipoIdentificacionTransportista = doc.createElement("tipoIdentificacionTransportista");
    tipoIdentificacionTransportista.appendChild(doc.createTextNode(idType)); // 04
    // RUC
    // //
    // 05
    // cédula
    // 06 pasaporte // 07 consumidor

    infoGuiaRemision.appendChild(tipoIdentificacionTransportista);

    String strTaxid = inout.getSlciShipper().getBusinessPartner().getTaxID();
    if (strTaxid == null || strTaxid.trim().equals("")) {
      throw new OBException("El transportista no tiene identificación (CIF/NIF).");
    }

    Element rucTransportista = doc.createElement("rucTransportista");

    /*
     * if (inout.getSlciShipper().getBusinessPartner().getSswhTaxidtype().equals ("P")) { strTaxid =
     * "999999999"; }
     */

    rucTransportista.appendChild(doc.createTextNode(strTaxid));
    infoGuiaRemision.appendChild(rucTransportista);

    // FECHA INICIO TRANSPORTE
    if (inout.getMovementDate() == null) {
      throw new OBException("No existe fecha de movimiento.");
    }
    Element fechaIniTransporte = doc.createElement("fechaIniTransporte");
    fechaIniTransporte.appendChild(doc.createTextNode(ecFormat.format(inout.getMovementDate())));
    infoGuiaRemision.appendChild(fechaIniTransporte);

    // FECHA FIN TRANSPORTE
    String strfechaFinTransporte = getRemissionGuideAddData(inout, "1");

    if (strfechaFinTransporte == null || strfechaFinTransporte.equals("")) {
      throw new OBException("Fecha fin de transporte es nula o no existe ");
    }
    Element fechaFinTransporte = doc.createElement("fechaFinTransporte");
    fechaFinTransporte.appendChild(doc.createTextNode(ecFormat.format(
        ecFormat.parse(strfechaFinTransporte)).toString()));
    infoGuiaRemision.appendChild(fechaFinTransporte);

    String strPlaca = null;
    try {
      strPlaca = (inout.getSlciShipper().getDescription() == null ? "--nd--" : inout
          .getSlciShipper().getDescription());
      if (strPlaca == null || strPlaca.trim().equals("") || strPlaca.equals("--nd--")) {
        throw new OBException("No existe información de placa (Descripción Transportista).");
      }
    } catch (Exception e) {
      throw new OBException("Error al obtener placa (Descripción Transportista).");
    }
    strPlaca = truncate(strPlaca, 20);
    Element placa = doc.createElement("placa");
    placa.appendChild(doc.createTextNode(strPlaca));
    infoGuiaRemision.appendChild(placa);

    Element destinatarios = doc.createElement("destinatarios");
    rootElement.appendChild(destinatarios);

    Element destinatario = doc.createElement("destinatario");
    destinatarios.appendChild(destinatario);

    String stridentificacionDest = inout.getBusinessPartner().getTaxID();
    if (stridentificacionDest.trim().equals("--nd--") || stridentificacionDest == null) {
      throw new OBException("El destinatario no tiene identificación (CIF/NIF).");
    }
    stridentificacionDest = truncate(stridentificacionDest, 20);
    Element identificacionDestinatario = doc.createElement("identificacionDestinatario");
    identificacionDestinatario.appendChild(doc.createTextNode(stridentificacionDest));
    destinatario.appendChild(identificacionDestinatario);

    String strRazonSocialDest = inout.getBusinessPartner().getName();
    if (strRazonSocialDest.trim().equals("") || strRazonSocialDest == null) {
      throw new OBException("El destinatario no tiene nombre fiscal.");
    }
    strRazonSocialDest = truncate(strRazonSocialDest, 300);
    Element razonSocialDestinatario = doc.createElement("razonSocialDestinatario");
    razonSocialDestinatario.appendChild(doc.createTextNode(strRazonSocialDest));
    destinatario.appendChild(razonSocialDestinatario);

    String strDireccionDestinatario = null;

    try {

      if (inout.getDeliveryLocation() != null) {

        strDireccionDestinatario = (inout.getDeliveryLocation().getLocationAddress()
            .getAddressLine1() == null ? " " : inout.getDeliveryLocation().getLocationAddress()
            .getAddressLine1().toString())
            + "--"
            + (inout.getDeliveryLocation().getLocationAddress().getAddressLine2() == null ? " "
                : inout.getDeliveryLocation().getLocationAddress().getAddressLine2().toString())
            + "--"
            + (inout.getDeliveryLocation().getLocationAddress().getPostalCode() == null ? " "
                : inout.getDeliveryLocation().getLocationAddress().getPostalCode().toString())
            + "--"
            + (inout.getDeliveryLocation().getLocationAddress().getCityName() == null ? " " : inout
                .getDeliveryLocation().getLocationAddress().getCityName().toString())
            + "--"
            + (inout.getDeliveryLocation().getLocationAddress().getRegion() == null ? " " : inout
                .getDeliveryLocation().getLocationAddress().getRegion().getName().toString())

            + "--"
            + (inout.getDeliveryLocation().getLocationAddress().getCountry() == null ? " " : inout
                .getDeliveryLocation().getLocationAddress().getCountry().getName().toString());

      } else {
        throw new OBException("No existe dirección del destinatario.");
      }
    } catch (Exception e) {
      throw new OBException("Error al obtener dirección del destinatario. " + e.getMessage());
    }

    Element dirDestinatario = doc.createElement("dirDestinatario");
    dirDestinatario.appendChild(doc.createTextNode(truncate(strDireccionDestinatario, 300)));
    destinatario.appendChild(dirDestinatario);

    String strMotivoTraslado = "";

    if (inout.getDescription() == null || inout.getDescription().equals("")) {
      throw new OBException("No existe motivo del traslado (Descripción Cabecera).");
    } else {
      strMotivoTraslado = truncate(inout.getDescription().toString().replaceAll("[\n]", " "), 300);
    }
    Element motivoTraslado = doc.createElement("motivoTraslado");
    motivoTraslado.appendChild(doc.createTextNode(strMotivoTraslado));
    destinatario.appendChild(motivoTraslado);

    // DOCUMENTO ADUANERO ÚNICO
    String strdocAduaneroUnico = getRemissionGuideAddData(inout, "2");

    if (strdocAduaneroUnico != null && !strdocAduaneroUnico.trim().equals("")) {
      Element docAduaneroUnico = doc.createElement("docAduaneroUnico");
      docAduaneroUnico.appendChild(doc.createTextNode(strdocAduaneroUnico));
      destinatario.appendChild(docAduaneroUnico);
    }

    // CÓDIGO ESTABLECIMIENTO DESTINO
    Element codEstabDestino = doc.createElement("codEstabDestino");
    codEstabDestino.appendChild(doc.createTextNode(strCodEstabDestino));
    destinatario.appendChild(codEstabDestino);

    // RUTA
    String strRuta = getRemissionGuideAddData(inout, "3");

    if (strRuta != null && !strRuta.trim().equals("")) {
      Element ruta = doc.createElement("ruta");
      ruta.appendChild(doc.createTextNode(strRuta));
      destinatario.appendChild(ruta);
    }

    boolean boolIsForSales = inout.getDocumentType().isEeiRemissionForSales();
    String strInvoiceID = getLastInvoice(strM_InOut_id);

    // CÓDIGO DE DOCUMENTO ÙLTIMA FACTURA
    if (strInvoiceID == null || strInvoiceID.equals("")) {
      if (!boolIsForSales) {
        throw new OBException(
            "No se encontró una factura referenciada a la primera línea del albarán.");
      }
    } else {

      Invoice invoiceref = OBDal.getInstance().get(Invoice.class, strInvoiceID);

      String strInvoiceDoctype = invoiceref.getDocumentType().getEeiEdocType();

      if (!strInvoiceDoctype.equals("01") && !strInvoiceDoctype.equals("04")
          && !strInvoiceDoctype.equals("06")) {
        throw new OBException(
            "Tipo de documento referenciado no válido (01=Factura - 04=Nota de Crédito - 06=Nota de Débito). ");

      } else {
        Element codDocSustento = doc.createElement("codDocSustento");
        codDocSustento.appendChild(doc.createTextNode(strInvoiceDoctype));
        destinatario.appendChild(codDocSustento);
      }
      // NÙMERO DE DOCUMENTO ÙLTIMA FACTURA numDocSustento

      String strInvoiceSubDocumentNo = null;
      try {
        strInvoiceSubDocumentNo = invoiceref.getDocumentNo().substring(8);
      } catch (Exception e) {
        throw new OBException(
            "La secuencia del número de documento de la última factura referenciada no es correcta. Por favor compruebe la parametrización del documento transacción.");
      }
      while (strInvoiceSubDocumentNo.length() < 9) {
        strInvoiceSubDocumentNo = "0" + strInvoiceSubDocumentNo;
      }
      String strInvoiceSubDocumentNo1 = truncate(invoiceref.getDocumentNo(), 8);
      String strInvoiceDocumentnoX = strInvoiceSubDocumentNo1 + strInvoiceSubDocumentNo;
      log4j.debug(strInvoiceDocumentnoX);
      if (!strInvoiceDocumentnoX.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
        throw new OBException(
            "El formato del número de documento de la última factura referenciada es incorrecto (000-000-000000000).");
      }
      Element numDocSustento = doc.createElement("numDocSustento");
      numDocSustento.appendChild(doc.createTextNode(strInvoiceDocumentnoX));
      destinatario.appendChild(numDocSustento);

      // NÙMERO DE AUTORIZACIÓN ÚLTIMA FACTURA numAutDocSustento

      if (invoiceref.getEeiCodigo() == null || invoiceref.getEeiCodigo().equals("")) {
        throw new OBException(
            "La última factura referenciada no tiene número de autorización (Clave de acceso)");
      }
      Element numAutDocSustento = doc.createElement("numAutDocSustento");
      numAutDocSustento.appendChild(doc.createTextNode(invoiceref.getEeiCodigo()));
      destinatario.appendChild(numAutDocSustento);

      // FECHA DE EMISIÓN ÚLTIMA FACTURA fechaEmisionDocSustento

      Date dtInvoiceDate = invoiceref.getInvoiceDate();

      if (dtInvoiceDate == null || dtInvoiceDate.equals("")) {
        throw new OBException("La última factura referenciada no tiene número de autorización");
      }
      Element fechaEmisionDocSustento = doc.createElement("fechaEmisionDocSustento");
      fechaEmisionDocSustento.appendChild(doc.createTextNode(ecFormat.format(dtInvoiceDate)));
      destinatario.appendChild(fechaEmisionDocSustento);
    }

    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
    simbolos.setDecimalSeparator('.');
    DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

    // detalles elements
    Element detalles = doc.createElement("detalles");
    destinatario.appendChild(detalles);

    Element detalle;
    Element codigoInterno;
    Element codigoAdicional;
    Element descripcion;
    Element cantidad;
    Element detallesAdicionales;
    Element detAdicional;

    List<ShipmentInOutLine> lstLineas2 = inout.getMaterialMgmtShipmentInOutLineList();

    if (lstLineas2 != null) {
      try {
        // ORDENAR POR NÚMERO DE LÍNEA
        Collections.sort(lstLineas2, new Comparator<ShipmentInOutLine>() {
          @Override
          public int compare(ShipmentInOutLine o1, ShipmentInOutLine o2) {
            // TODO Auto-generated method stub
            return o1.getLineNo().compareTo(o2.getLineNo());
          }
        });

        for (ShipmentInOutLine detallealbaran : lstLineas2) {

          detalle = doc.createElement("detalle");
          detalles.appendChild(detalle);

          codigoInterno = doc.createElement("codigoInterno");
          String strcodigoInterno = detallealbaran.getProduct().getSearchKey();
          if (strcodigoInterno == null || strcodigoInterno.trim().equals("")) {
            throw new OBException("El producto (" + detallealbaran.getProduct().getName()
                + ") no tiene código interno (Identificador).");
          }
          strcodigoInterno = truncate(strcodigoInterno, 25);
          log4j.debug("Código Adicional:" + strcodigoInterno);
          codigoInterno.appendChild(doc.createTextNode(strcodigoInterno));
          detalle.appendChild(codigoInterno);

          codigoAdicional = doc.createElement("codigoAdicional");
          String strcodigoAdicional = detallealbaran.getProduct().getName();
          log4j.debug("Código Adicional:" + strcodigoAdicional);
          if (strcodigoAdicional == null || strcodigoAdicional.trim().equals("")) {
            throw new OBException("El producto (" + detallealbaran.getProduct().getName()
                + ")  no tiene código adicional (Nombre).");
          }
          strcodigoAdicional = truncate(strcodigoAdicional, 25);
          codigoAdicional.appendChild(doc.createTextNode(strcodigoAdicional));
          detalle.appendChild(codigoAdicional);

          String strDescription = detallealbaran.getProduct().getDescription();
          if (strDescription == null || strDescription.trim().equals("")) {
            throw new OBException("El producto (" + detallealbaran.getProduct().getName()
                + ") no tiene descripción.");
          }
          strDescription = truncate(strDescription.replaceAll("[\n]", " "), 300);
          descripcion = doc.createElement("descripcion");
          descripcion.appendChild(doc.createTextNode(strDescription));
          detalle.appendChild(descripcion);

          double Cantidad = detallealbaran.getMovementQuantity().doubleValue();
          log4j.debug("CANTIDAD MOVIDA" + detallealbaran.getMovementQuantity() + "    -   "
              + detallealbaran.getMovementQuantity().doubleValue());
          cantidad = doc.createElement("cantidad");
          cantidad.appendChild(doc.createTextNode(formateador.format(Cantidad).toString()));
          detalle.appendChild(cantidad);

          String strAttribute = detallealbaran.getDescription();
          if (strAttribute != null && !strAttribute.trim().equals("")) {

            detallesAdicionales = doc.createElement("detallesAdicionales");
            detalle.appendChild(detallesAdicionales);
            detAdicional = doc.createElement("detAdicional");

            detAdicional.setAttribute("nombre", "Descripción");

            strAttribute = truncate(strAttribute.replaceAll("[\n]", " "), 300);
            detAdicional.setAttribute("valor", strAttribute);
            detallesAdicionales.appendChild(detAdicional);
          }

        }
      } catch (Exception e) {
        throw new OBException("Error en la lectura del detalle del albarán. " + e.getMessage());
      }
    }

    Organization org = inout.getOrganization();

    List<OrganizationInformation> lstOrgInfo = org.getOrganizationInformationList();

    OBCriteria<Organization> ObjOrganization = OBDal.getInstance().createCriteria(
    		Organization.class);
    OrganizationType objOrganizationType = OBDal.getInstance().get(OrganizationType.class, "1");
    ObjOrganization.add(Restrictions.eq(Organization.PROPERTY_ORGANIZATIONTYPE, objOrganizationType));
    
    String strWithholdingAgent = null;

    if (ObjOrganization.list().size()>0) {
    	strWithholdingAgent = ObjOrganization.list().get(0).getEeiWithholdingAgent();
    }   
    
      int intCountLocation = 0;
      org.openbravo.model.common.businesspartner.BusinessPartner bPartner = null;
      if (lstOrgInfo.size() > 0) {
	      if (lstOrgInfo.get(0).getBusinessPartner() == null) {
	
	        throw new OBException(
	            "Tercero en la pestaña Información de la Organización no configurado. ");
	      }
	
	      bPartner = lstOrgInfo.get(0)
	          .getBusinessPartner();
	
	      intCountLocation = bPartner.getBusinessPartnerLocationList().size();
      }
      

      if (intCountLocation > 0
          || (inout.getBusinessPartner().getName2() != null)
          || (((inout.getDescription() != null && !inout.getDescription().trim().equals("")) || (inout
              .getEeiDescription() != null && !inout.getEeiDescription().trim().equals(""))) && !inout
              .getDocumentType().getEeiDescriptionfields().equals("NO"))
          || strWithholdingAgent != null) {

        Element infoAdicional = doc.createElement("infoAdicional");
        rootElement.appendChild(infoAdicional);

        if (strWithholdingAgent != null) {
            Element campoAdicional0 = doc.createElement("campoAdicional");
            campoAdicional0.setAttribute("nombre", "agenteRetencion");
            campoAdicional0.appendChild(doc.createTextNode(strWithholdingAgent));
            infoAdicional.appendChild(campoAdicional0);
        }   

        if (inout.getBusinessPartner().getName2() != null) {

          Element campoAdicional4 = doc.createElement("campoAdicional");
          campoAdicional4.setAttribute("nombre", "NombreComercialTercero");
          campoAdicional4.appendChild(doc.createTextNode(truncate(inout.getBusinessPartner()
              .getName2(), 300)));
          infoAdicional.appendChild(campoAdicional4);

        }

        if (intCountLocation > 0) {
          Element campoAdicional = doc.createElement("campoAdicional");
          String strPhone = bPartner.getBusinessPartnerLocationList().get(0).getPhone() == null ? "N/A"
              : bPartner.getBusinessPartnerLocationList().get(0).getPhone();
          campoAdicional.setAttribute("nombre", "TELEFONO");
          campoAdicional.appendChild(doc.createTextNode(strPhone));
          infoAdicional.appendChild(campoAdicional);

          Element campoAdicional2 = doc.createElement("campoAdicional");
          String strEmail = bPartner.getEEIEmail() == null ? "N/A" : bPartner.getEEIEmail();
          campoAdicional2.setAttribute("nombre", "EMAIL");
          campoAdicional2.appendChild(doc.createTextNode(strEmail));
          infoAdicional.appendChild(campoAdicional2);

          Element campoAdicional3 = doc.createElement("campoAdicional");
          String strNameLocation = bPartner.getBusinessPartnerLocationList().get(0).getName() == null ? "N/A"
              : bPartner.getBusinessPartnerLocationList().get(0).getName();
          campoAdicional3.setAttribute("nombre", "SUCURSAL 03");
          campoAdicional3.appendChild(doc.createTextNode(strNameLocation));
          infoAdicional.appendChild(campoAdicional3);
        }
        if (((inout.getDescription() != null && !inout.getDescription().trim().equals("")) || (inout
            .getEeiDescription() != null && !inout.getEeiDescription().trim().equals("")))
            && !inout.getDocumentType().getEeiDescriptionfields().equals("NO")) {

          String StrUnionCadenaSinSaltos = "";

          if (inout.getDocumentType().getEeiDescriptionfields().equals("DEDA")) {

            StrUnionCadenaSinSaltos = (inout.getDescription() == null ? "" : inout.getDescription())
                + ";" + (inout.getEeiDescription() == null ? "" : inout.getEeiDescription());

          } else if (inout.getDocumentType().getEeiDescriptionfields().equals("DE")) {

            StrUnionCadenaSinSaltos = (inout.getDescription() == null ? "" : inout.getDescription());

          } else if (inout.getDocumentType().getEeiDescriptionfields().equals("DA")) {

            StrUnionCadenaSinSaltos = (inout.getEeiDescription() == null ? "" : inout
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

    String strFile = writer.toString();

    return strFile;
  }

  public static String getLastInvoice(String strInoutId) {
    ConnectionProvider con = new DalConnectionProvider(false);
    try {
      String strSql = "SELECT CI.C_INVOICE_ID AS FACTURA FROM C_INVOICE CI INNER JOIN C_INVOICELINE IL ON CI.C_INVOICE_ID=IL.C_INVOICE_ID WHERE CI.DOCSTATUS='CO'AND IL.M_INOUTLINE_ID =? ORDER BY CI.DATEINVOICED DESC";

      PreparedStatement st = null;
      ConnectionProvider conn = new DalConnectionProvider(false);
      st = conn.getPreparedStatement(strSql);
      st.setString(1, strInoutId);

      ResultSet rsConsulta = st.executeQuery();
      String strMax = null;
      int intContador = 0;
      while (rsConsulta.next()) {
        intContador = intContador + 1;
        if (intContador == 1) {

          strMax = rsConsulta.getString("FACTURA");
          break;
        }
      }
      return strMax;
    } catch (Exception e) {
      log4j.debug("Error al consultar la tabla C_INVOICELINE." + e.getMessage());
      throw new OBException("Error al consultar la tabla C_INVOICELINE.");

    } finally {
      try {
        con.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static String getRemissionGuideAddData(ShipmentInOut inoutOb, String strCodigoRetorno) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    try {

      String strSql = "SELECT eei_get_remissionguidefields(?,?,?) AS RESULTADO  FROM  DUAL ";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      st.setString(1, inoutOb.getId());
      st.setString(2, "I"); // QUEMADO M_INOUT PARA ESTA CLASE JAVA
      st.setString(3, strCodigoRetorno);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("RESULTADO");
      }

      return strResult;

    } catch (Exception e) {

      throw new OBException(
          "Error al consultar información de campos adicionales guía de remisión (fecha fin de transporte -código aduana -ruta). "
              + e.getMessage());
    }

  }

  public boolean validateFile(File file) throws Exception {
    URL schemaFile = new URL("http://cheli.aradaen.com/factura.xsd");
    Source xmlFile = new StreamSource(file);
    // SchemaFactory schemaFactory =
    // SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
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

  public static boolean isNumeric(String str) {
    try {
      double d = Double.parseDouble(str);
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

}
