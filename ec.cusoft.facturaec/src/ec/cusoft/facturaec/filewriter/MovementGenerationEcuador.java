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
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
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
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
//import ec.cusoft.facturaec.templates.OBEInvoice_I;
//import ec.cusoft.facturaec.templates.OBWSEInvoice_I;

import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;

//import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

public class MovementGenerationEcuador {

  static Logger log4j = Logger.getLogger(MovementGenerationEcuador.class);

  public String generateFile(InternalMovement internalMovement, String lang) throws Exception {

    String strIsEDoc = "", strDocType = "";
    strIsEDoc = String.valueOf((eei_get_existresuply(internalMovement, "1")));
    strIsEDoc = (strIsEDoc == null || strIsEDoc.equals("") || strIsEDoc.equals("null") ? ""
        : strIsEDoc);
    strDocType = String.valueOf(eei_get_existresuply(internalMovement, "2").replaceAll("\\s", ""));
    strDocType = (strDocType == null || strDocType.equals("") || strIsEDoc.equals("null") ? ""
        : strDocType);

    if (strIsEDoc.equals("") && strDocType.equals("")) {
      throw new OBException(
          "No es posible generar Documento Electrónico,no se encontró el módulo de inventarios (Resupply).");
    }
    if (!strIsEDoc.trim().equals("Y")) {
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
    String strRuc = internalMovement.getOrganization().getOrganizationInformationList().get(0)
        .getTaxID();
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
      strKeyAccess = internalMovement.getEeiCodigo();
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
    String strCodDoc = strDocType;
    Element codDoc = doc.createElement("codDoc");
    codDoc.appendChild(doc.createTextNode(strCodDoc));
    infoTributaria.appendChild(codDoc);

    // NÚMERO DE DOCUMENTO
    String strSubDocumentNo = null;
    try {
      strSubDocumentNo = internalMovement.getDocumentNo().substring(8);
    } catch (Exception e) {
      throw new OBException(
          "La secuencia del número de documento no es correcta. Por favor compruebe la parametrización del documento transacción.");
    }
    while (strSubDocumentNo.length() < 9) {
      strSubDocumentNo = "0" + strSubDocumentNo;
    }
    log4j.debug("parte 2    " + strSubDocumentNo);

    String strSubDocumentNo1 = truncate(internalMovement.getDocumentNo(), 8);
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
      for (CountryTrl countryTrl : internalMovement.getOrganization()
          .getOrganizationInformationList().get(0).getLocationAddress().getCountry()
          .getCountryTrlList()) {
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
      headquartersCountry = internalMovement.getOrganization().getOrganizationInformationList()
          .get(0).getLocationAddress().getCountry().getName();
    }

    String headQuartersaddress = (internalMovement.getOrganization()
        .getOrganizationInformationList().get(0).getLocationAddress().getAddressLine1() == null ? " "
        : internalMovement.getOrganization().getOrganizationInformationList().get(0)
            .getLocationAddress().getAddressLine1())
        + "--"
        + (internalMovement.getOrganization().getOrganizationInformationList().get(0)
            .getLocationAddress().getAddressLine2() == null ? " " : internalMovement
            .getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getAddressLine2())
        + "--"
        + (internalMovement.getOrganization().getOrganizationInformationList().get(0)
            .getLocationAddress().getPostalCode() == null ? " " : internalMovement
            .getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getPostalCode())
        + "--"
        + (internalMovement.getOrganization().getOrganizationInformationList().get(0)
            .getLocationAddress().getCityName() == null ? " " : internalMovement.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getCityName())
        + "--"
        + (internalMovement.getOrganization().getOrganizationInformationList().get(0)
            .getLocationAddress().getRegion() == null ? " " : internalMovement.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getRegion().getName())
        + "--" + headquartersCountry;

    // infoGuiaRemision elements
    Element infoGuiaRemision = doc.createElement("infoGuiaRemision");
    rootElement.appendChild(infoGuiaRemision);

    SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");

    if (headQuartersaddress.equals("") || headQuartersaddress == null) {
      throw new OBException("No existe la dirección en la organización.");
    }

    Element dirEstablecimiento = doc.createElement("dirEstablecimiento");
    dirEstablecimiento.appendChild(doc.createTextNode(truncate(headQuartersaddress, 300)));
    infoGuiaRemision.appendChild(dirEstablecimiento);

    String strDireccionPartida = null;
    String strCodEstabDestino = null;
    List<InternalMovementLine> lstLineas = internalMovement
        .getMaterialMgmtInternalMovementLineList();

    if (lstLineas != null) {

      Collections.sort(lstLineas, new Comparator<InternalMovementLine>() {
        @Override
        public int compare(InternalMovementLine o1, InternalMovementLine o2) {
          // TODO Auto-generated method stub
          return o1.getLineNo().compareTo(o2.getLineNo());
        }
      });

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
                : lstLineas.get(0).getNewStorageBin().getWarehouse().getLocationAddress()
                    .getCountry().getName());

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

      } catch (Exception e) {
        e.getStackTrace();
        log4j.debug(strDireccionPartida);
        throw new OBException("Error al obtener dirección de partida. " + e.getMessage());
      }

      if (strDireccionPartida == null || strDireccionPartida.equals("")) {
        throw new OBException("Error al obtener dirección de partida ");
      }

    } else {
      throw new OBException("La transacción no tiene líneas.");
    }

    Element dirPartida = doc.createElement("dirPartida");
    dirPartida.appendChild(doc.createTextNode(truncate(strDireccionPartida, 300)));
    infoGuiaRemision.appendChild(dirPartida);

    if (eei_get_existresuply(internalMovement, "3") == null) {
      throw new OBException("Transportista no seleccionado (Shipper).");
    }

    if (eei_get_existresuply(internalMovement, "4") == null) {
      throw new OBException("Tercero configurado en transportista no seleccionado (Shipper).");
    }

    String strNombreFiscal = eei_get_existresuply(internalMovement, "5");
    if (strNombreFiscal == null || strNombreFiscal.trim().equals("")) {
      throw new OBException("El transportista no tiene nombre fiscal.");
    }
    strNombreFiscal = truncate(strNombreFiscal, 300);

    Element razonSocialTransportista = doc.createElement("razonSocialTransportista");
    razonSocialTransportista.appendChild(doc.createTextNode(strNombreFiscal));
    infoGuiaRemision.appendChild(razonSocialTransportista);

    String idType = "";
    String strCodeIdType = null;
    strCodeIdType = eei_get_existresuply(internalMovement, "6");
    if (strCodeIdType.equals("R")) {
      idType = "04";
    } else if (strCodeIdType.equals("D")) {
      idType = "05";
    } else if (strCodeIdType.equals("P")) {
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

    String strTaxid = eei_get_existresuply(internalMovement, "7");
    if (strTaxid == null || strTaxid.trim().equals("")) {
      throw new OBException("El transportista no tiene identificación (CIF/NIF).");
    }

    Element rucTransportista = doc.createElement("rucTransportista");

    // if
    // (internalMovement.getSsrsMShipper().getBusinessPartner().getSswhTaxidtype().equals("P"))
    // {
    // strTaxid = "999999999";
    // }

    rucTransportista.appendChild(doc.createTextNode(strTaxid));
    infoGuiaRemision.appendChild(rucTransportista);

    if (internalMovement.getMovementDate() == null) {
      throw new OBException("No existe fecha de movimiento.");
    }
    Element fechaIniTransporte = doc.createElement("fechaIniTransporte");
    fechaIniTransporte.appendChild(doc.createTextNode(ecFormat.format(internalMovement
        .getMovementDate())));
    infoGuiaRemision.appendChild(fechaIniTransporte);

    String strfechaFinTransporte = getRemissionGuideAddData(internalMovement, "1");

    if (strfechaFinTransporte == null || strfechaFinTransporte.equals("")) {
      throw new OBException("Fecha fin de transporte es nula o no existe. ");
    }
    Element fechaFinTransporte = doc.createElement("fechaFinTransporte");

    fechaFinTransporte.appendChild(doc.createTextNode(ecFormat.format(
        ecFormat.parse(strfechaFinTransporte)).toString()));
    infoGuiaRemision.appendChild(fechaFinTransporte);

    String strPlaca = eei_get_existresuply(internalMovement, "8");
    ;
    try {
      strPlaca = (strPlaca == null ? "--nd--" : strPlaca);
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

    Organization organization = internalMovement.getOrganization();

    String strDireccionDestinatario = "";

    List<OrganizationInformation> orgInfoList = organization.getOrganizationInformationList();

    if (orgInfoList.size() == 0 || orgInfoList == null) {
      throw new OBException(
          "Falta configurar el Tercero en la pestaña Información de la Organización");

    }

    String stridentificacionDest = orgInfoList.get(0).getBusinessPartner().getTaxID();
    if (stridentificacionDest.trim().equals("--nd--") || stridentificacionDest == null
        || stridentificacionDest.trim().equals("")) {
      throw new OBException("El destinatario no tiene identificación (CIF/NIF).");
    }
    stridentificacionDest = truncate(stridentificacionDest, 20);
    Element identificacionDestinatario = doc.createElement("identificacionDestinatario");
    identificacionDestinatario.appendChild(doc.createTextNode(stridentificacionDest));
    destinatario.appendChild(identificacionDestinatario);

    String strRazonSocialDest = orgInfoList.get(0).getBusinessPartner().getName2();
    if (strRazonSocialDest == null || strRazonSocialDest.trim().equals("")) {
      throw new OBException("El destinatario no tiene nombre fiscal.");
    }
    strRazonSocialDest = truncate(strRazonSocialDest, 300);
    Element razonSocialDestinatario = doc.createElement("razonSocialDestinatario");
    razonSocialDestinatario.appendChild(doc.createTextNode(strRazonSocialDest));
    destinatario.appendChild(razonSocialDestinatario);

    List<InternalMovementLine> lstLineasDelivery = internalMovement
        .getMaterialMgmtInternalMovementLineList();

    Collections.sort(lstLineasDelivery, new Comparator<InternalMovementLine>() {
      @Override
      public int compare(InternalMovementLine o1, InternalMovementLine o2) {
        // TODO Auto-generated method stub
        return o1.getLineNo().compareTo(o2.getLineNo());
      }
    });

    if (lstLineas.get(0).getStorageBin() == null) {
      throw new OBException("Hueco no configurado en la primera línea del documento.");
    }

    try {

      if (lstLineasDelivery.get(0).getNewStorageBin().getWarehouse().getLocationAddress() != null) {

        strDireccionDestinatario = (lstLineasDelivery.get(0).getNewStorageBin().getWarehouse()
            .getLocationAddress().getAddressLine1() == null ? " " : lstLineasDelivery.get(0)
            .getNewStorageBin().getWarehouse().getLocationAddress().getAddressLine1().toString())
            + "--"
            + (lstLineasDelivery.get(0).getNewStorageBin().getWarehouse().getLocationAddress()
                .getAddressLine2() == null ? " " : lstLineasDelivery.get(0).getNewStorageBin()
                .getWarehouse().getLocationAddress().getAddressLine2().toString())
            + "--"
            + (lstLineasDelivery.get(0).getNewStorageBin().getWarehouse().getLocationAddress()
                .getPostalCode() == null ? " " : lstLineasDelivery.get(0).getNewStorageBin()
                .getWarehouse().getLocationAddress().getPostalCode().toString())
            + "--"
            + (lstLineasDelivery.get(0).getNewStorageBin().getWarehouse().getLocationAddress()
                .getCityName() == null ? " " : lstLineasDelivery.get(0).getNewStorageBin()
                .getWarehouse().getLocationAddress().getCityName().toString())
            + "--"
            + (lstLineasDelivery.get(0).getNewStorageBin().getWarehouse().getLocationAddress()
                .getRegion() == null ? " " : lstLineasDelivery.get(0).getNewStorageBin()
                .getWarehouse().getLocationAddress().getRegion().getName().toString())

            + "--"
            + (lstLineasDelivery.get(0).getNewStorageBin().getWarehouse().getLocationAddress()
                .getCountry() == null ? " " : lstLineasDelivery.get(0).getNewStorageBin()
                .getWarehouse().getLocationAddress().getCountry().getName().toString());

      } else {
        throw new OBException("No existe dirección del destinatario.");
      }
    } catch (Exception e) {
      throw new OBException("Error al obtener dirección de partida. " + e.getMessage());
    }

    if (strDireccionDestinatario == null || strDireccionDestinatario.equals("")) {
      throw new OBException("Error al obtener dirección de partida ");
    }
    Element dirDestinatario = doc.createElement("dirDestinatario");

    if (strDireccionDestinatario == null || strDireccionDestinatario.equals("")) {
      throw new OBException("Error al obtener dirección de destinatario ");
    }
    dirDestinatario.appendChild(doc.createTextNode(truncate(strDireccionDestinatario, 300)));
    destinatario.appendChild(dirDestinatario);

    String strMotivoTraslado = "";

    if (internalMovement.getDescription() == null || internalMovement.getDescription().equals("")) {
      throw new OBException("No existe motivo del traslado (Descripción Cabecera).");
    } else {
      strMotivoTraslado = truncate(
          internalMovement.getDescription().toString().replaceAll("[\n]", " "), 300);
    }
    Element motivoTraslado = doc.createElement("motivoTraslado");
    motivoTraslado.appendChild(doc.createTextNode(strMotivoTraslado));
    destinatario.appendChild(motivoTraslado);

    // Element docAduaneroUnico = doc.createElement("docAduaneroUnico");
    // docAduaneroUnico.appendChild(doc.createTextNode("N/A"));
    // destinatario.appendChild(docAduaneroUnico);

    Element codEstabDestino = doc.createElement("codEstabDestino");
    codEstabDestino.appendChild(doc.createTextNode(strCodEstabDestino));
    destinatario.appendChild(codEstabDestino);

    // to do ruta - codDocSustento - numDocSustento - numAutDocSustento -
    // fechaEmisionDocSustento
    String strRuta = getRemissionGuideAddData(internalMovement, "3");

    if (strRuta != null && !strRuta.trim().equals("")) {

      Element ruta = doc.createElement("ruta");
      ruta.appendChild(doc.createTextNode(strRuta));
      destinatario.appendChild(ruta);

    }
    /*
     * Element codDocSustento = doc.createElement("codDocSustento");
     * codDocSustento.appendChild(doc.createTextNode("01"));
     * destinatario.appendChild(codDocSustento);
     * 
     * Element numDocSustento = doc.createElement("numDocSustento");
     * numDocSustento.appendChild(doc.createTextNode("001-001-000123456"));
     * destinatario.appendChild(numDocSustento);
     * 
     * Element numAutDocSustento = doc.createElement("numAutDocSustento"); numAutDocSustento
     * .appendChild(doc.createTextNode( "1234567890123456789745120124586212101245789512368"));
     * destinatario.appendChild(numAutDocSustento);
     * 
     * Element fechaEmisionDocSustento = doc.createElement("fechaEmisionDocSustento");
     * fechaEmisionDocSustento. appendChild(doc.createTextNode("18/03/2018"));
     * destinatario.appendChild(fechaEmisionDocSustento);
     */
    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
    simbolos.setDecimalSeparator('.');
    DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

    // detalles elements
    Element detalles = doc.createElement("detalles");
    destinatario.appendChild(detalles);

    List<InternalMovementLine> listInternalMovLine = internalMovement
        .getMaterialMgmtInternalMovementLineList();

    for (InternalMovementLine collInternalMov : listInternalMovLine) {

      Element detalle = doc.createElement("detalle");
      detalles.appendChild(detalle);

      Element codigoInterno = doc.createElement("codigoInterno");
      codigoInterno.appendChild(doc.createTextNode(truncate(collInternalMov.getProduct()
          .getSearchKey(), 25)));
      detalle.appendChild(codigoInterno);

      Element codigoAdicional = doc.createElement("codigoAdicional");
      codigoAdicional.appendChild(doc.createTextNode(truncate(collInternalMov.getProduct()
          .getName(), 25)));
      detalle.appendChild(codigoAdicional);

      String strDescription = "";
      strDescription = collInternalMov.getProduct().getDescription();
      if (strDescription == null) {
        throw new OBException("El producto (" + collInternalMov.getProduct().getName()
            + ") no tiene descripción.");
      }
      Element descripcion = doc.createElement("descripcion");
      strDescription = truncate(strDescription.replaceAll("[\n]", " "), 300);
      descripcion = doc.createElement("descripcion");
      descripcion.appendChild(doc.createTextNode(strDescription));
      detalle.appendChild(descripcion);

      double Cantidad = 0;
      Cantidad = collInternalMov.getMovementQuantity().doubleValue();
      Element cantidad = doc.createElement("cantidad");
      cantidad.appendChild(doc.createTextNode(formateador.format(Cantidad).toString()));
      detalle.appendChild(cantidad);

      String strAttribute = collInternalMov.getDescription();
      if (strAttribute != null && !strAttribute.trim().equals("")) {

        Element detallesAdicionales = doc.createElement("detallesAdicionales");
        detalle.appendChild(detallesAdicionales);
        Element detAdicional = doc.createElement("detAdicional");

        detAdicional.setAttribute("nombre", "Descripción");

        strAttribute = truncate(strAttribute.replaceAll("[\n]", " "), 300);
        detAdicional.setAttribute("valor", strAttribute);
        detallesAdicionales.appendChild(detAdicional);
      }

    }

	Organization org = internalMovement.getOrganization();

	List<OrganizationInformation> lstOrgInfo = org
			.getOrganizationInformationList();

    OBCriteria<Organization> ObjOrganization = OBDal.getInstance().createCriteria(
    		Organization.class);
    OrganizationType objOrganizationType = OBDal.getInstance().get(OrganizationType.class, "1");
    ObjOrganization.add(Restrictions.eq(Organization.PROPERTY_ORGANIZATIONTYPE, objOrganizationType));
    
    String strWithholdingAgent = null;

    if (ObjOrganization.list().size()>0) {
    	strWithholdingAgent = ObjOrganization.list().get(0).getEeiWithholdingAgent();
    }

	if (lstOrgInfo.size() > 0 || (strWithholdingAgent != null)) {
		
		Element infoAdicional = doc.createElement("infoAdicional");
		rootElement.appendChild(infoAdicional);

	    if (strWithholdingAgent != null) {
	        Element campoAdicional0 = doc.createElement("campoAdicional");
	        campoAdicional0.setAttribute("nombre", "agenteRetencion");
	        campoAdicional0.appendChild(doc.createTextNode(strWithholdingAgent));
	        infoAdicional.appendChild(campoAdicional0);
	    }     
		
		int intCountLocation = 0;
		org.openbravo.model.common.businesspartner.BusinessPartner bPartner = null;
		
		if (lstOrgInfo.size() > 0) {
			if (lstOrgInfo.get(0).getBusinessPartner() == null) {
	
				throw new OBException(
						"Falta Configurar el Tercero en la pestaña Información de la Organización ");
			}
	
			bPartner = lstOrgInfo.get(0).getBusinessPartner();
	
			intCountLocation = bPartner.getBusinessPartnerLocationList().size();
		}

		if (intCountLocation > 0) {

			if (bPartner.getName2() != null
					&& !bPartner.getName2().trim().equals("")) {

				Element campoAdicional4 = doc
						.createElement("campoAdicional");
				campoAdicional4.setAttribute("nombre",
						"NombreComercialTercero");
				campoAdicional4.appendChild(doc.createTextNode(truncate(
						bPartner.getName2(), 300)));
				infoAdicional.appendChild(campoAdicional4);
			}

			Element campoAdicional = doc.createElement("campoAdicional");
			String strPhone = bPartner.getBusinessPartnerLocationList()
					.get(0).getPhone() == null ? "N/A" : bPartner
					.getBusinessPartnerLocationList().get(0).getPhone();
			campoAdicional.setAttribute("nombre", "TELEFONO");
			campoAdicional.appendChild(doc.createTextNode(strPhone));
			infoAdicional.appendChild(campoAdicional);

			Element campoAdicional2 = doc.createElement("campoAdicional");
			String strEmail = bPartner.getEEIEmail() == null ? "N/A"
					: bPartner.getEEIEmail();
			campoAdicional2.setAttribute("nombre", "EMAIL");
			campoAdicional2.appendChild(doc.createTextNode(strEmail));
			infoAdicional.appendChild(campoAdicional2);

			Element campoAdicional3 = doc.createElement("campoAdicional");
			String strNameLocation = bPartner
					.getBusinessPartnerLocationList().get(0).getName() == null ? "N/A"
					: bPartner.getBusinessPartnerLocationList().get(0)
							.getName();
			campoAdicional3.setAttribute("nombre", "SUCURSAL 03");
			campoAdicional3
					.appendChild(doc.createTextNode(strNameLocation));
			infoAdicional.appendChild(campoAdicional3);
		}

	}

    log4j.debug(toString(doc));
    DOMSource domSource = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.transform(domSource, result);

    String strFile = writer.toString();

    return strFile;
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

  public static String getRemissionGuideAddData(InternalMovement inoutOb, String strCodigoRetorno) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    try {

      String strSql = "SELECT eei_get_remissionguidefields(?,?,?) AS RESULTADO  FROM  DUAL ";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      st.setString(1, inoutOb.getId());
      st.setString(2, "M"); // QUEMADO M_MOVEMENT PARA ESTA CLASE JAVA
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

  public static String eei_get_existresuply(InternalMovement inoutOb, String strCodigoRetorno) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    try {

      String strSql = "SELECT eei_get_existresuply(?,?) AS RESULTADO  FROM  DUAL ";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      st.setString(1, inoutOb.getId());
      st.setString(2, strCodigoRetorno);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("RESULTADO");
      }

      return strResult;

    } catch (Exception e) {

      throw new OBException(
          "Error al consultar existencia de módulo de inventarios (tipo de documento). "
              + e.getMessage());
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

  public static String toString(Document doc) throws TransformerException {
    DOMSource domSource = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.transform(domSource, result);
    return writer.toString();
  }

}
