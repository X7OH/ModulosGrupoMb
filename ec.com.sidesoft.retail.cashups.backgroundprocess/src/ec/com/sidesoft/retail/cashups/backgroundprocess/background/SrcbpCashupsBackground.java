package ec.com.sidesoft.retail.cashups.backgroundprocess.background;

import java.sql.CallableStatement;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.retail.posterminal.OBPOSAppCashup;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerContext;

public class SrcbpCashupsBackground extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(SrcbpCashupsBackground.class);
  private ProcessLogger logger;
  private int maxTransactions = 0;
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
    String strTerminalID = "";
    try {

      OBCriteria<OBPOSAppCashup> obAppCashups = OBDal.getInstance()
          .createCriteria(OBPOSAppCashup.class);
      obAppCashups.add(Restrictions.eq(OBPOSAppCashup.PROPERTY_ACTIVE, true));
      obAppCashups.add(Restrictions.eq(OBPOSAppCashup.PROPERTY_ISPROCESSED, false));

      List<OBPOSAppCashup> lstappCashups = obAppCashups.list();
      if (lstappCashups.size() > 0) {

        for (OBPOSAppCashup appCashups : obAppCashups.list()) {

          strTerminalID = appCashups.getPOSTerminal().getId();

          CallableStatement cs = conn.getConnection().prepareCall("{call srcbp_cahups_upd (?)}");

          // client
          cs.setString(1, strTerminalID);

          cs.execute();
          cs.close();

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
