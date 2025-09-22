package ec.com.sidesoft.retail.bank.deposits.background;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.FormatUtilities;

import ec.com.sidesoft.retail.bank.deposits.ad_process.Ssrbd_PrintReportBank_Deposits;
import ec.com.sidesoft.retail.bank.deposits.SSRBDConfig;
import ec.com.sidesoft.retail.bank.deposits.SSRBDConfiglines;


public class RetailBankDepositsBackground extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(RetailBankDepositsBackground.class);
  private ProcessLogger logger;
  String msgTitle = "";
  String msgMessage = "";
  String msgType = ""; // success, warning or error
  public ConfigParameters cf;
  private static String strAttachment;
  private static String strFTP;
  private static Connection connectionDB = null;
  //private static ConnectionProvider conn = new DalConnectionProvider(false);
  private String configBody = "";
  private String configFooter = "";
  private String configSubject = "";
  private String reportFormat = "";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    cf = bundle.getConfig(); // Obtener la configuración de la App OB
    logger = bundle.getLogger();
    OBError result = new OBError();
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    
    try {
      
      OBContext.setAdminMode(false);
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));

      // Variables de Envio mensaje
      String host = null;
      boolean auth = true;
      String username = null;
      String password = null;
      String connSecurity = null;
      int port = 25;
      String senderAddress = "";
      String recipientTO = "";
      String recipientCC = "";
      String recipientBCC = "";
      String replyTo = null;
      String contentType = "";
      String emailSubject = null, emailBody = null;
      List<File> attachments = new ArrayList<File>();
      String routeReport = "";
      
      try {
        connectionDB = conn.getTransactionConnection();
      } catch (Exception e) {}

      // *********************************************************************
      // INICIO VERIFICO QUE EXISTA CONFIGURACIO DEPOSITOS BANCARIOS
      // *********************************************************************
      OBCriteria<SSRBDConfig> config = OBDal.getInstance().createCriteria(SSRBDConfig.class);
      config.add(Restrictions.eq(SSRBDConfig.PROPERTY_ACTIVE, true));
      config.addOrderBy(SSRBDConfig.PROPERTY_CREATIONDATE, false);
      config.setMaxResults(1);

      if (config.list() != null && config.list().size() > 0) { 
        
        configBody = config.list().get(0).getBody().trim();
        configFooter = config.list().get(0).getFooter().trim();
        configSubject = config.list().get(0).getSubject();
        reportFormat = config.list().get(0).getReportformat().trim();
        
        VariablesSecureApp vars = bundle.getContext().toVars();
        
        strAttachment = cf.getBaseDesignPath() + "/design/";
        strFTP = cf.strFTPDirectory;
        
        routeReport = printReport(vars, reportFormat); 
        if(!routeReport.equals("")) {
          
          File file = new File(routeReport);
          attachments.add(file);

           // ***********************************************************
          // INICIO RECUPERAR CORREO DE LOS CONTACTOS
          // ***********************************************************
          OBCriteria<SSRBDConfiglines> companyEmail = OBDal.getInstance().createCriteria(SSRBDConfiglines.class);
          companyEmail.add(Restrictions.eq(SSRBDConfiglines.PROPERTY_ACTIVE, true));

          if (companyEmail.list().size() > 0) { 
            
            List<SSRBDConfiglines> linesObj = companyEmail.list();
            for (SSRBDConfiglines objLine : linesObj) { 
       
              // RECUPERAR CORREO DE LOS USUARIOS CONFIGURADOS EN LAS
              // LINEAS - SOLO CONTACTOS QUE ESTEN ACTIVOS              
              OBCriteria<User> userEmail = OBDal.getInstance().createCriteria(User.class);
              userEmail.add(Restrictions.eq(User.PROPERTY_ACTIVE, true));
              userEmail.add(Restrictions.eq(User.PROPERTY_ID, objLine.getUser().getId()));
              
              if (userEmail.list().size() > 0) {
                
                  List<User> User_email_obj = userEmail.list();
                  for (User obj_User : User_email_obj) {
                      if (obj_User.getEmail() == null || obj_User.getEmail().equals("")) {
                        logger.logln(obj_User.getName() + " sin correo electrónico.");
                      } else {
                        recipientTO = recipientTO + obj_User.getEmail() + "; ";
                      }
                  } 
                  
              }
              
            }
            
            // SE GENERA EL DETALLE DEL CORREO    
            emailSubject = configSubject;
            emailBody = "<html> \n"
                + "<head> \n"
                + "<meta charset=\"UTF-8\">  \n"
                + "</head> \n"
                + "<body> \n"
                + configBody
                + configFooter
                + "<br><br> \n"
                + "</body> \n"
                + "</html> \n";
            contentType = "text/html; charset=utf-8";
            
            /* ********************************************************** */
            /* INICIO CONFIGURACION DEL CORREO
            /* ********************************************************** */
            try {
              OBCriteria<EmailServerConfiguration> EmailServerConfiguration2 = OBDal.getInstance()
                  .createCriteria(EmailServerConfiguration.class);

              EmailServerConfiguration2
                  .add(Restrictions.eq(EmailServerConfiguration.PROPERTY_ACTIVE, true));

              if (EmailServerConfiguration2.list().size() == 0) {
                logger.logln("Error Configuración correo electrónico - No esta configurado el correo electrónico para la entidad. ");
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
              logger.logln("Error Recuperando datos para correo de la entidad ");
              return;
            } finally {
              OBContext.restorePreviousMode();
            }  
            /* ********************************************************** */
            /* FIN CONFIGURACION DEL CORREO
            /* ********************************************************** */
            
            /* ********************************************************** */
            /* INICIO ENVIO DEL CORREO
            /* ********************************************************** */
            try {
              EmailManager.sendEmail(host, auth, username, password, connSecurity, port, senderAddress,
                  recipientTO, recipientCC, recipientBCC, replyTo, emailSubject, emailBody, contentType,
                  attachments, new Date(), null);
              logger.logln("Proceso completado satisfactoriamente.");
            } catch (Exception exception) {
              msgMessage = exception.toString();
              logger.logln("Error " + msgMessage);
            } finally {
              // Delete the temporary files generated for the email attachments
              for (File attachment : attachments) {
                if (attachment.exists() && !attachment.isDirectory()) {
                  attachment.delete();
                }
              }
            }          
            /* ********************************************************** */
            /* FIN ENVIO DEL CORREO
            /* ********************************************************** */  
           
          }else {
            logger.logln("No hay usuarios registrados en la configurados del proceso de Depósitos Bancarios");
          }   
          // ***********************************************************
          // FIN RECUPERAR CORREO DE LOS CONTACTOS
          // ***********************************************************

        }else { 
          // no se genero el reporte
          logger.logln("No se genero el reporte para adjuntarlo en el correo.");
        }
          
      }
      else {
        logger.logln("No existe Configuracion para el proceso de Depósitos Bancarios.");
      }
      // *********************************************************************
      // FIN VERIFICO QUE EXISTA CONFIGURACIO DEPOSITOS BANCARIOS
      // *********************************************************************
      
      connectionDB.close();
      OBDal.getInstance().commitAndClose();
      
    } catch (Exception e) {
      result.setTitle(Utility.messageBD(conn, "Error", language));
      result.setType("Error");
      result.setMessage(e.getMessage());
      log4j.error(result.getMessage(), e);
      logger.logln(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }  
    
    
  }
  
  public String printReport(VariablesSecureApp vars, String format) {
    final HttpServletRequest request = RequestContext.get().getRequest();
    final HttpServletResponse response = RequestContext.get().getResponse();

    String strReport = "";
    Ssrbd_PrintReportBank_Deposits printReport = new Ssrbd_PrintReportBank_Deposits();
    try {
      strReport = printReport.doPost(request, response, strAttachment, strFTP, connectionDB, format);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ServletException e) {
      e.printStackTrace();
    }
    
    return strReport;

  }

}
