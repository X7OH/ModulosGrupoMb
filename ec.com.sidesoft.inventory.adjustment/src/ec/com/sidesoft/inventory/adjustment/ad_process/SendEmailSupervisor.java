package ec.com.sidesoft.inventory.adjustment.ad_process;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.FormatUtilities;

import ec.com.sidesoft.inventory.adjustment.SIVAPhysicalInventory;
import ec.com.sidesoft.inventory.adjustment.SIVAPhysicalInvtlines;

public class SendEmailSupervisor extends DalBaseProcess {
  // private FileGeneration fileGeneration = new FileGeneration();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    final OBError msg = new OBError();

    // variables message the answer;
    String msgTitle = "";
    String msgMessage = "";
    String msgType = "Success"; // success, warning or error

    // Variables de Envio mensaje
    String host = null;
    boolean auth = true;
    String username = null;
    String password = null;
    String connSecurity = null;
    int port = 25;
    String senderAddress = "";
    String recipientTO = "";
    String recipientCC = null;
    String recipientBCC = null;
    String replyTo = null;
    String subject = "";
    String content = "";
    String contentType = "";
    List<File> attachments = new ArrayList<File>();
    String emailSubject = null, emailBody = null;
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    // Variables datos de mensaje y cuerpo
    String codigo = "";
    String bPartner_id = ""; // Tercero_de_Comparacion
    String organization = "";
    String documentno = "";

    // Variables de Registro.
    String Physcal_Inventory_PDV_ID = null;
    SIVAPhysicalInventory Physcal_Inventory_PDV = null;
    SIVAPhysicalInvtlines Physcal_Inventory_PDV_Lines = null;
    BusinessPartner objBusiness = null;

    msgType = "Success";
    msgTitle = "Proceso exitoso";
    msgMessage = "Contacte con el supervisor de su zona para confirmación de transacción.";

    // RECUPERARA VARIABLES DE REGISTRO

    Physcal_Inventory_PDV_ID = (String) bundle.getParams().get("Siva_Physical_Inventory_ID"); // RECORD_ID

    Physcal_Inventory_PDV = OBDal.getInstance().get(SIVAPhysicalInventory.class,
        Physcal_Inventory_PDV_ID);
    
    organization = Physcal_Inventory_PDV.getOrganization().getName();

    if (Physcal_Inventory_PDV.getDocumentNo() == null
        || Physcal_Inventory_PDV.getDocumentNo() == "") {

      msgType = "Error";
      msgTitle = "Número de documento vacío";
      msgMessage = "Número de documento necesarío para envío de mensaje.";
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;

    } else {
      documentno = Physcal_Inventory_PDV.getDocumentNo();
    }

    if (Physcal_Inventory_PDV.getOrganization().getStatCBpartner() == null
        || Physcal_Inventory_PDV.getOrganization().getStatCBpartner().getId() == "") {

      msgType = "Error";
      msgTitle = "Supervisor no encontrado";
      msgMessage = "Supervisor no asignado en la organización ";
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;

    } else {

      bPartner_id = Physcal_Inventory_PDV.getOrganization().getStatCBpartner().getId();

    }

    // RECUPERAR OBJETO DE TERCERO

    objBusiness = OBDal.getInstance().get(BusinessPartner.class, bPartner_id);

    // RECUPERAR CORREO DEL USUARIO ATADO AL TERCERO

    OBCriteria<User> User_emali = OBDal.getInstance().createCriteria(User.class);

    User_emali.add(Restrictions.eq(User.PROPERTY_ACTIVE, true));
    User_emali.add(Restrictions.eq(User.PROPERTY_BUSINESSPARTNER, objBusiness));

    if (User_emali.list().size() == 0) {

      msgType = "Error";
      msgTitle = "Usiario no encontrado";
      msgMessage = "No existe usuario asociado a algun supervisor.";
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;

    } else {

      List<User> User_email_obj = User_emali.list();

      for (User obj_User : User_email_obj) {

        if (obj_User.getEmail() == null || obj_User.getEmail().equals("")) {

          msgType = "Error";
          msgTitle = "Usuario sin correo electrónico";
          msgMessage = "No existe correo electrónico asociado al usuario.";
          msg.setType(msgType);
          msg.setTitle(msgTitle);
          msg.setMessage(msgMessage);
          bundle.setResult(msg);
          return;

        } else {

          recipientTO = obj_User.getEmail();

        }
      }
    }

    codigo = generate_code();
    
    // SE GENERA EL DETALLE DE LOS PRODUCTOS QUE SE VAN A AJUSTAR
    // INICIO ITERAR SOBRE ERRORES DE TERCEROS EN PEDIDOS
    OBCriteria<SIVAPhysicalInvtlines> invtListLines = OBDal.getInstance().createCriteria(
        SIVAPhysicalInvtlines.class);
    invtListLines.add(Restrictions.eq(SIVAPhysicalInvtlines.PROPERTY_SIVAPHYSICALINVENTORY, Physcal_Inventory_PDV));
    
    List<SIVAPhysicalInvtlines> listIntLines = invtListLines.list();
    
    String ajuste = "<table border=\"1\" style=\"width:100%\">";
    ajuste = ajuste + "<thead><tr>";
    ajuste = ajuste + "<th>CODIGO</th>";
    ajuste = ajuste + "<th>PRODUCTO</th>";
    ajuste = ajuste + "<th>CANTIDAD AJUSTADA</th>";
    ajuste = ajuste + "</tr></thead>";
    ajuste = ajuste + "<tbody>";
    for (SIVAPhysicalInvtlines lines : listIntLines) {
      ajuste = ajuste + "<tr>";
      ajuste = ajuste + "<td width=\"20%\">"+lines.getProduct().getSearchKey().trim()+"</td>";
      ajuste = ajuste + "<td width=\"60%\">"+lines.getProduct().getName().trim()+"</td>";
      ajuste = ajuste + "<td width=\"20%\" align=\"center\">"+lines.getQtyandjust().setScale(2, BigDecimal.ROUND_HALF_UP)+"</td>";
      ajuste = ajuste + "</tr>";
    }
    ajuste = ajuste + "</tbody>";
    ajuste = ajuste + "</table>";
    // INICIO ITERAR SOBRE ERRORES DE TERCEROS EN PEDIDOS
    
    // SE GENERA EL DETALLE DE LOS PRODUCTOS QUE SE VAN A AJUSTAR    
    emailSubject = "Código de autorización para Ajuste de inventario";
    emailBody = "<html><body>"  
        + "<p>Estimado Colaborador. <br><br> "
        + "El local <strong>" + organization + "</strong>"
        + " ha solicitado la autorización de un ajuste de inventario en PDV "
        + "con número de documento <strong>" + documentno + "</strong>. <br><br>"
        + "El código de autorización es: <strong>" + codigo + "</strong><br>"
        + "Su clave caducará en el transcurso de 6 minutos. <br><br>"
        + "Los Items a ajustar son:<p> <br>"
        + ajuste
        + "  </body></html>";
    contentType = "text/html; charset=utf-8";

    // RECUPERA REGISTROS DE NECCESIDADES DE ENVIO
    try {
      OBCriteria<EmailServerConfiguration> EmailServerConfiguration2 = OBDal.getInstance()
          .createCriteria(EmailServerConfiguration.class);

      EmailServerConfiguration2
          .add(Restrictions.eq(EmailServerConfiguration.PROPERTY_ACTIVE, true));

      if (EmailServerConfiguration2.list().size() == 0) {

        msgType = "Error";
        msgTitle = "Configuración correo electrónico";
        msgMessage = "No esta configurado el correo electrónico para la entidad.";
        msg.setType(msgType);
        msg.setTitle(msgTitle);
        msg.setMessage(msgMessage);
        bundle.setResult(msg);
        return;

      } else {

        List<EmailServerConfiguration> ContractCriteriaList = EmailServerConfiguration2.list();

        for (EmailServerConfiguration mailConfig : ContractCriteriaList) {

          host = mailConfig.getSmtpServer();

          if (!mailConfig.isSMTPAuthentification()) {
            auth = false;
          }

          username = mailConfig.getSmtpServerAccount();
          password = FormatUtilities.encryptDecrypt(mailConfig.getSmtpServerPassword(), false);
          connSecurity = mailConfig.getSmtpConnectionSecurity();
          port = mailConfig.getSmtpPort().intValue();
          senderAddress = mailConfig.getSmtpServerAccount();

        }
      }

    } catch (Exception exception) {

      msgType = "Error";
      msgTitle = "Recuperando datos para correo de la entidad.";
      msgMessage = exception.toString();
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;

    } finally {

      OBContext.restorePreviousMode();

    }
    
    //Inicio validaciones stock
 // VALIDAR TAXID
    boolean boolErrorExist = false;
    String strMessage = null;
    org.openbravo.database.ConnectionProvider conn = new DalConnectionProvider(false);
    String languages = OBContext.getOBContext().getLanguage().getLanguage();
    
    strMessage = validatestock(Physcal_Inventory_PDV_ID,conn);
    if (strMessage != null) {
      msgType = "Error";
      msgTitle = "Error";
      msgMessage = strMessage;
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;
   
    }

    // METODO PARA ENVIO DE MENSAJE
    try {

      EmailManager.sendEmail(host, auth, username, password, connSecurity, port, senderAddress,
          recipientTO, recipientCC, recipientBCC, replyTo, emailSubject, emailBody, contentType,
          attachments, new Date(), null);

    } catch (Exception exception) {

      msgType = "Error";
      msgTitle = "No se pudo enviar código.";
      msgMessage = exception.toString();

    } finally {

      // Delete the temporary files generated for the email attachments
      for (File attachment : attachments) {

        if (attachment.exists() && !attachment.isDirectory()) {

          attachment.delete();

        }
      }
    }

    if (msgType.equals("Success")) {

      try {

        Physcal_Inventory_PDV.setCodeAuthorization(codigo);
        Physcal_Inventory_PDV.setCreatecodedate(new Date());
        Physcal_Inventory_PDV.setDocumentStatus("GE");

        OBDal.getInstance().save(Physcal_Inventory_PDV);
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().refresh(Physcal_Inventory_PDV);
        OBDal.getInstance().commitAndClose();

      } catch (Exception exception) {

        msgType = "Error";
        msgTitle = "No se pudo guardar el registro.";
        msgMessage = exception.toString();

      }
    }

    // registrar mensaje

    msg.setType(msgType);
    msg.setTitle(msgTitle);
    msg.setMessage(msgMessage);

    bundle.setResult(msg);

  }

  public String generate_code() {
    Random aleatorio = new Random();
    String abecedario = "ABCDEFGHIJKLMNOPQRSTVWXYZ";
    String cadena = ""; // Inicializamos la Variable//
    int m = 0, pos = 0, num;
    while (m < 1) {
      pos = (int) (aleatorio.nextDouble() * abecedario.length() - 1 + 0);

      num = (int) (aleatorio.nextDouble() * 9999 + 100);
      cadena = cadena + abecedario.charAt(pos) + num;
      pos = (int) (aleatorio.nextDouble() * abecedario.length() - 1 + 0);
      cadena = cadena + abecedario.charAt(pos);

      // System.out.println("Tu codigo es: " + cadena);
      // cadena = "";
      m++;
    }
    return cadena;
  }
  
  public String validatestock(String strPhyscalInventoryPDVID,
      org.openbravo.database.ConnectionProvider conn) {
    // throw new OBException("EXCEPCION TEST");
    String strResult = null;

    // CallableStatement cllTaxidvalidate = null;
    PreparedStatement cllTaxidvalidate = null;
    ResultSet result = null;
    try {

      cllTaxidvalidate = conn.getConnection().prepareCall(
          "select siva_validate_stockproduct(?) from dual");

      cllTaxidvalidate.setString(1, strPhyscalInventoryPDVID);
      result = cllTaxidvalidate.executeQuery();
      while (result.next()) {
        strResult = result.getString(1);
      }
    } catch (Exception e) {
      strResult = e.getMessage();
    } finally {
      try {
        cllTaxidvalidate.close();
        conn.destroy();
      } catch (Exception ex) {

      }
    }

    return strResult;

  }


}
