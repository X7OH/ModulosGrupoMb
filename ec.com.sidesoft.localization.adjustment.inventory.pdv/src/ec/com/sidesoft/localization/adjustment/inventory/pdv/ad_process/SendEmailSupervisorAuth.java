package ec.com.sidesoft.localization.adjustment.inventory.pdv.ad_process;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.FormatUtilities;

import ec.com.sidesoft.custom.reports.ad_process.GenerateRandomCode;

public class SendEmailSupervisorAuth extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    final OBError msg = new OBError();
    
    ConnectionProvider conn = new DalConnectionProvider(false);
    
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
    String emailSubject = null;
    String emailBody = null;
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    msgType = "Success";
    msgTitle = "Proceso exitoso";
    msgMessage = "Contacte con el supervisor de su zona para confirmación de transacción.";

    // Código de authorizacion
    String codigo = "";
    // movementID
    String strInventoryID = (String) bundle.getParams().get("M_Inventory_ID");
    // Tercero_de_Comparacion
    String bPartner_id = "";
    // Variables para cuerpo de mensaje
    String organization = "";
    String documentno = "";

    // RECUPERARA VARIABLES DE REGISTRO
    InventoryCount inventory = null;
    inventory = OBDal.getInstance().get(InventoryCount.class, strInventoryID);
    
    // Crear Detalle de correo
    String strEmailDetail ="";
    
    
    //Recupero las lineas del Inventario
    OBCriteria<InventoryCountLine> inventoryLine = OBDal.getInstance()
            .createCriteria(InventoryCountLine.class);
    inventoryLine
            .add(Restrictions.eq(InventoryCountLine.PROPERTY_ACTIVE, true));
    inventoryLine
    .add(Restrictions.eq(InventoryCountLine.PROPERTY_PHYSINVENTORY, inventory));      
    
    
    if (inventoryLine.count()==0) {
  	
        msgType = "Error";
        msgTitle = "Error";
        msgMessage = "La transacción no tiene lineas de inventario";
        msg.setType(msgType);
        msg.setTitle(msgTitle);
        msg.setMessage(msgMessage);
        bundle.setResult(msg);
        return;
        
    }else {
    	
    	strEmailDetail = "<br><br> <h1>AJUSTES DE INVENTARIO</h1> <br><br>";
    	strEmailDetail = strEmailDetail + "CODIGO&nbsp;&nbsp;&nbsp;PRODUCTO&nbsp;&nbsp;&nbsp;CANTIDAD CONTADA&nbsp;&nbsp;&nbsp;CANTIDAD TEORICA&nbsp;&nbsp;&nbsp;AJUSTE<br><br>"; 
    	List<InventoryCountLine> invlineList = inventoryLine.list();
    	for (InventoryCountLine colInvLine: invlineList){
    		
    		BigDecimal bgDOrderQuantity = (colInvLine.getBookQuantity()==null?BigDecimal.ZERO:colInvLine.getBookQuantity()).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    		BigDecimal BookQuantity = (colInvLine.getQuantityCount()==null?BigDecimal.ZERO:colInvLine.getQuantityCount()).setScale(2, BigDecimal.ROUND_HALF_EVEN);

    		strEmailDetail = strEmailDetail + colInvLine.getProduct().getSearchKey() + "&nbsp;&nbsp;&nbsp;" +  
    				colInvLine.getProduct().getName() + "&nbsp;&nbsp;&nbsp;" + 
    				BookQuantity + "&nbsp;&nbsp;&nbsp;" +
    				bgDOrderQuantity + "&nbsp;&nbsp;&nbsp;" +
    				BookQuantity.subtract(bgDOrderQuantity) + "<br>";
    	}
    	
    }

    organization = inventory.getOrganization().getName();
    if (inventory.getDocumentNo() == null || inventory.getDocumentNo() == "") {
      msgType = "Error";
      msgTitle = "Número de documento vacío";
      msgMessage = "Número de documento necesarío para envío de mensaje.";
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;
    } else {
      documentno = inventory.getDocumentNo();
    }

    documentno = inventory.getDocumentNo();

    if (inventory.getOrganization().getStatCBpartner() == null
        || inventory.getOrganization().getStatCBpartner().getId() == "") {
      msgType = "Error";
      msgTitle = "Supervisor no encontrado";
      msgMessage = "Supervisor no asignado en la organización ";
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;
    } else {
      bPartner_id = inventory.getOrganization().getStatCBpartner().getId();
    }

    // RECUPERAR OBJETO DE TERCERO
    BusinessPartner objBusiness = null;
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

    codigo = GenerateRandomCode.generate_code();

    emailSubject = "Código de autorización de Inventario Físico";
    emailBody = "Estimado, <br> <br> El local " + organization
        + " ha solicitado la autorización de una transacción de Inventario Físico"
        + " con el Nro. de documento " + documentno
        + "<br> <br> El código de autorización es: " + codigo + "<br>"
        + "Su clave caducará en el transcurso de 7 minutos." + strEmailDetail + "<br><br>Saludos Cordiales.";
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
      msgTitle = "Recuperando configuración del correo de la entidad.";
      msgMessage = exception.toString();
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;

    } finally {
      OBContext.restorePreviousMode();
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
    	  
    	  inventory.setSsipdvKey(codigo);
    	  inventory.setSsipdvStatusKey("WC");
    	  inventory.setSsipdvDateKey(new Date());
    	  OBDal.getInstance().save(inventory);
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().refresh(inventory);
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



}
