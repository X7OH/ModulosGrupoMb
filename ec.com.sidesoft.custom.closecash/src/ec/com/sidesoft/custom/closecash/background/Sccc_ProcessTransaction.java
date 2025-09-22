package ec.com.sidesoft.custom.closecash.background;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.base.ConfigParameters;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerContext;

import ec.cusoft.facturaec.filewriter.FileGeneration;

public class Sccc_ProcessTransaction extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(Sccc_ProcessTransaction.class);
  private ProcessLogger logger;
  private int maxTransactions = 0;
  FileGeneration filegeneration = new FileGeneration();
  String msgTitle = "";
  String msgMessage = "";
  String msgType = ""; // success, warning or error
  public ConfigParameters cf;
  private SchedulerContext ctx;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    cf = bundle.getConfig(); // Obtener la configuración de la App OB
    logger = bundle.getLogger();
    OBError result = new OBError();
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    try {

      OBContext.setAdminMode(false);
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));

      OBCriteria<FIN_FinaccTransaction> objFinaccTransaction = OBDal.getInstance()
          .createCriteria(FIN_FinaccTransaction.class);

      objFinaccTransaction
          .add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_SCCCISCASHCLOUSURE, true));
      objFinaccTransaction.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_PROCESSED, false));

      if (objFinaccTransaction.list().size() > 0) {
        List<FIN_FinaccTransaction> lstFinaccTransaction = objFinaccTransaction.list();
        for (FIN_FinaccTransaction colTransaction : lstFinaccTransaction) {
          try {
            HashMap<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("action", "P");
            parameters.put("Fin_FinAcc_Transaction_ID", colTransaction.getId());
            bundle.setParams(parameters);
            OBError myMessage = null;
            new FIN_TransactionProcess().execute(bundle);
            //colTransaction.setProcessed(true);

            myMessage = (OBError) bundle.getResult();
           // System.out.println(colTransaction.getId() + "     " + myMessage);
            log4j.error(colTransaction.getId() + "     " + myMessage);
            logger.logln(colTransaction.getId() + "     " + myMessage);
          } catch (Exception e) {
            //System.out.println(colTransaction.getId() + "     " + e.getMessage());
            log4j.error(colTransaction.getId(), e);
            logger.logln(colTransaction.getId() + "     " + e.getMessage());

          }

        }
      }

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

}
