package ec.com.sidesoft.retail.cashups.backgroundprocess.ad_process;

import java.sql.CallableStatement;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.xmlEngine.XmlEngine;

public class SrcbpCashupsProcess extends DalBaseProcess {

  public XmlEngine xmlEngine = null;
  public static String strDireccion;
  public String StrCodigoCompra = "";

  @SuppressWarnings({ "deprecation", "null" })
  public void doExecute(ProcessBundle bundle) throws Exception {

    final OBError message = new OBError();

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    // ConnectionProvider conn = new DalConnectionProvider(false);

    ConnectionProvider conn = bundle.getConnection();

    // VariablesSecureApp varsAux = bundle.getContext().toVars();
    // HttpServletRequest request = RequestContext.get().getRequest();
    HttpServletResponse response = RequestContext.get().getResponse();

    String strTerminalID = "";

    strTerminalID = (String) bundle.getParams().get("Obpos_App_Cashup_ID");
    try {

      CallableStatement cs = conn.getConnection().prepareCall("{call srcbp_cahups_upd (?)}");

      // client
      cs.setString(1, strTerminalID);

      cs.execute();
      cs.close();

      message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
      message.setType("Success");
      message.setMessage(Utility.messageBD(conn, "Success", language));
      /*
       * } catch (final Exception e) { e.printStackTrace(System.err);
       * 
       * message.setTitle(Utility.messageBD(conn, "Error", language)); message.setType("Error");
       * message.setMessage(e.getMessage() + e.fillInStackTrace());
       */
    } finally {
      bundle.setResult(message);
    }
  }

  protected String formatDate(java.util.Date date) {
    return new SimpleDateFormat((String) OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .get(KernelConstants.DATE_FORMAT_PROPERTY)).format(date);
  }
}
