package ec.com.sidesoft.transfer.authorization.ad_process;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.utils.FormatUtilities;

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

    msgType = "Success";
    msgTitle = "Proceso exitoso";
    msgMessage = "Contacte con el supervisor de su zona para confirmación de transacción.";

    // Código de authorizacion
    String codigo = "";
    // movementID
    String movementID = (String) bundle.getParams().get("M_Movement_ID");
    // Tercero_de_Comparacion
    String bPartner_id = "";
    // Variables para cuerpo de mensaje
    String organization = "";
    String documentno = "";

    // RECUPERARA VARIABLES DE REGISTRO
    InternalMovement Movement_head = null;
    OBCriteria<InternalMovementLine> InternalMovementLineObj = OBDal.getInstance()
        .createCriteria(InternalMovementLine.class);
    Movement_head = OBDal.getInstance().get(InternalMovement.class, movementID);

    organization = Movement_head.getOrganization().getName();
    if (Movement_head.getDocumentNo() == null || Movement_head.getDocumentNo() == "") {
      msgType = "Error";
      msgTitle = "Número de documento vacío";
      msgMessage = "Número de documento necesarío para envío de mensaje.";
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;
    } else {
      documentno = Movement_head.getDocumentNo();

      InternalMovementLineObj
          .add(Restrictions.eq(InternalMovementLine.PROPERTY_MOVEMENT, Movement_head));
    }

    documentno = Movement_head.getDocumentNo();

    if (Movement_head.getOrganization().getStatCBpartner() == null
        || Movement_head.getOrganization().getStatCBpartner().getId() == "") {
      msgType = "Error";
      msgTitle = "Supervisor no encontrado";
      msgMessage = "Supervisor no asignado en la organización ";
      msg.setType(msgType);
      msg.setTitle(msgTitle);
      msg.setMessage(msgMessage);
      bundle.setResult(msg);
      return;
    } else {
      bPartner_id = Movement_head.getOrganization().getStatCBpartner().getId();
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

    codigo = generate_code();
    // senderAddress = "rjacome@sidesoft.com.ec";
    // recipientTO = "rjacome@sidesoft.com.ec";
    // recipientCC = "";
    // recipientBCC = "";
    // replyTo = "rjacome@sidesoft.com.ec";
    emailSubject = "Código de autorización de movimiento entre almacenes";
    emailBody = "<p>Estimado,</br> El local " + organization
        + " ha solicitado la autorización de una transacción de movimiento"
        + " entre almacenes con número de documento " + documentno
        + "</br></br> El código de autorización es: " + codigo + ". </br>"
        + "Su clave caducará en el transcurso de 6 minutos.</p></br>";

    if (InternalMovementLineObj.list().size() > 0) {
      emailBody = emailBody + "<table border=\"1\" style=\"width:100%\">";
      emailBody = emailBody + "<thead><tr>";
      emailBody = emailBody + "<th>Identificador</th>";
      emailBody = emailBody + "<th>Producto</th>";
      emailBody = emailBody + "<th>Cant.Movida</th>";
      emailBody = emailBody + "<th>Unidad</th>";
      emailBody = emailBody + "<th>Ubic. Origen</th>";
      emailBody = emailBody + "<th>Ubic. Destino</th>";
      emailBody = emailBody + "</tr></thead>";
      emailBody = emailBody + "<tbody>";

      for (int i = 0; i < InternalMovementLineObj.list().size(); i++) {
        emailBody = emailBody + "<tr>";
        emailBody = emailBody + "<td width=\"20%\">"
            + InternalMovementLineObj.list().get(i).getSprliIdentifier() + "</td>";
        emailBody = emailBody + "<td width=\"20%\">"
            + InternalMovementLineObj.list().get(i).getProduct().getName() + "</td>";
        emailBody = emailBody + "<td width=\"10%\">"
            + InternalMovementLineObj.list().get(i).getMovementQuantity() + "</td>";
        emailBody = emailBody + "<td width=\"20%\">"
            + InternalMovementLineObj.list().get(i).getUOM().getName() + "</td>";
        emailBody = emailBody + "<td width=\"20%\">"
            + InternalMovementLineObj.list().get(i).getStorageBin().getRowX() + "</td>";
        emailBody = emailBody + "<td width=\"20%\">"
            + InternalMovementLineObj.list().get(i).getNewStorageBin().getRowX() + "</td>";
        emailBody = emailBody + "</tr>";
      }

      emailBody = emailBody + "</tbody>";
      emailBody = emailBody + "</table>";
    }
    // contentType = "text/plain; charset=utf-8";
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
        Movement_head.setStatCodeAuthorization(codigo);
        Movement_head.setStatCreateCodeDate(new Date());

        OBDal.getInstance().save(Movement_head);
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().refresh(Movement_head);
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

}
