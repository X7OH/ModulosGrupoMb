package ec.com.sidesoft.retail.reverse.authorization;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.mobile.core.process.MobileService.MobileServiceQualifier;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.retail.posterminal.JSONProcessSimple;
import org.openbravo.utils.FormatUtilities;

import ec.com.sidesoft.custom.reports.ad_process.GenerateRandomCode;

@MobileServiceQualifier(serviceName = "ec.com.sidesoft.retail.reverse.authorization.sendMailReverse")
public class sendMailReverse extends JSONProcessSimple {

  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {

    JSONObject result = new JSONObject();

    String username = jsonsent.getString("username");
    String supervisorid = jsonsent.getString("supervisorid");
    String documentno = jsonsent.getString("documentno");
    String terminal = jsonsent.getString("terminal");
    String codigo = "";
    String codigoEncriptado = "";
    Date fechaUpdatePasssword = new Date();

    // INICIO DE VARIABLES PARA EL ENVIO DE CORREO
    final OBError msg = new OBError();
    // variables message the answer;
    String msgTitle = "";
    String msgMessage = "";
    String msgType = "Success"; // success, warning or error
    // Variables de Envio mensaje
    String host = null;
    boolean auth = true;
    String password = null;
    String connSecurity = null;
    int port = 25;
    String senderAddress = "";
    String recipientTO = "";
    String recipientCC = null;
    String recipientBCC = null;
    String replyTo = null;
    String contentType = "";
    String emailSubject = null, emailBody = null;

    msgType = "Success";
    msgTitle = "Proceso exitoso";
    msgMessage = "Contacte con el supervisor de su zona para confirmación de transacción.";

    // RECUPERAR OBJETO DE SUPERVISOR
    User objSupervisor = null;
    objSupervisor = OBDal.getInstance().get(User.class, supervisorid);
    String emailSUpervisor = objSupervisor.getEmail();

    // tipo de errores
    // 0 todo ok
    // 1 supervisor no tiene correo asignado
    // 2 configuracion del correo electonico
    // 3 error recuperando datos para correo de la entidad
    // 4 envio fallido de correo electronico

    if (emailSUpervisor == null) {
      // el supervisor no tiene asignado un correo
      result.put("message", "El Supervisor no tiene asignado un correo electronico");
      result.put("tipo", 1);

    } else {

      System.out.println("Email SUpervisor: " + emailSUpervisor);
      codigo = GenerateRandomCode.generate_code();

      // ENCRIPTAR CLAVE DEL SUPERVISOR
      try {
        codigoEncriptado = encryptPassword(codigo);
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      System.out.println("Codigo: " + codigo);

      String mensaje = "Estimado; \n \nEl usuario " + username + " en el terminal " + terminal
          + ", ha solicitado autorización para anulación del pedido " + documentno
          + ". \n \nSu nueva clave para la autorización es: " + codigo + "\n \n"
          + "Su clave caducará en el transcurso de 6 minutos.";

      // INFORMACION DEL ENVIO DEL CORREO
      emailSubject = "Código de autorización para anulación de pedido";
      emailBody = mensaje;
      contentType = "text/plain; charset=utf-8";
      recipientTO = emailSUpervisor;

      // RECUPERACION DE LA INFORMACION DE ENVIO DE CORREO
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
          result.put("message", msg);
          result.put("tipo", 2);
          return result;

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
        result.put("message", msg);
        result.put("tipo", 3);
        return result;

      } finally {
        OBContext.restorePreviousMode();
      }

      // METODO DE ENVIO DE CORREO
      try {
        EmailManager.sendEmail(host, auth, username, password, connSecurity, port, senderAddress,
            recipientTO, recipientCC, recipientBCC, replyTo, emailSubject, emailBody, contentType,
            null, new Date(), null);

        result.put("message", "Envio Exitoso de correo al supervisor");
        result.put("tipo", 0);

      } catch (Exception exception) {

        msgType = "Error";
        msgTitle = "No se pudo enviar código.";
        msgMessage = exception.toString();
        result.put("message", msg);
        result.put("tipo", 4);
      }

      // ACTUALIZO INFORMACION DEL SUPERVISOR
      objSupervisor.setPassword(codigoEncriptado);
      objSupervisor.setLastPasswordUpdate(fechaUpdatePasssword);
      objSupervisor.setPasswordExpired(false);
      OBDal.getInstance().save(objSupervisor);
      OBDal.getInstance().flush();

    }

    return result;

  }

  /* FUNCION PARA ENCRYPTAR EL PASSWORD */
  public String encryptPassword(String codigo)
      throws NoSuchAlgorithmException, UnsupportedEncodingException {

    MessageDigest algorithm = MessageDigest.getInstance("SHA");

    byte[] bytes = codigo.getBytes("UTF-8");

    algorithm.reset();
    algorithm.update(bytes);
    byte[] md5Digest = algorithm.digest();

    String encString = new String(Base64.encodeBase64(md5Digest));

    return encString;
  }

}