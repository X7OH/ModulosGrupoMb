package ec.com.sidesoft.retail.creditnote.background;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerContext;

public class SSRCNBatchBackgroundCN extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(SSRCNBatchBackgroundCN.class);
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
    try {

      OBContext.setAdminMode(false);
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));

      ArrayList<SSRCNInvoiceCreditNote> lstInvoice = SelectInvoicesCN();

      int intContador = 0;

      if (lstInvoice != null && lstInvoice.size() != 0) {

        for (SSRCNInvoiceCreditNote lstInvoiceNC : lstInvoice) {

          Invoice invoiceNotaCredito = OBDal.getInstance().get(Invoice.class,
              lstInvoiceNC.getStrInvoiceCreditNoteID());

          Invoice invoiceOrigen = OBDal.getInstance().get(Invoice.class,
              lstInvoiceNC.getStrInvoiceID());
          invoiceNotaCredito.setScnrIsrefInv(true);
          invoiceNotaCredito.setScnrInvoice(invoiceOrigen);
          invoiceNotaCredito.setEeiIsInvRef(true);
          invoiceNotaCredito.setEeiRefInv(invoiceOrigen);
          OBDal.getInstance().save(invoiceNotaCredito);
        }
        OBDal.getInstance().commitAndClose();

      } else {

        System.out.println("No existen registros para la ejecución del proceso en cola.");
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

  public static ArrayList<SSRCNInvoiceCreditNote> SelectInvoicesCN() {
    ConnectionProvider conn = new DalConnectionProvider(false);

    String strTypeOfBatch = null;

    try {
      String strSql = null;
      strSql = " select ci.c_invoice_id as invoice_id,cif.c_invoice_id,cd.name "
          + " from c_invoice ci " + " join c_doctype cd on cd.c_doctype_id = ci.c_doctype_id "
          + " join c_invoiceline cil on cil.c_invoice_id = ci.c_invoice_id "
          + " join c_orderline colv on colv.c_orderline_id = cil.c_orderline_id "
          + " join m_inoutline milv on milv.m_inoutline_id = colv.m_inoutline_id "
          + " join c_invoiceline cilf on cilf.c_orderline_id = milv.c_orderline_id "
          + " join c_invoice cif on cif.c_invoice_id = cilf.c_invoice_id " + " where  "
          + " coalesce(cif.EM_Scnr_Isref_Inv,'N') ='N' " + " AND cif.EM_Scnr_Invoice_ID is null "
          + " and cd.em_sswh_implementautoriza ='Y' " + " and cd.docbasetype = 'ARI_RM' ";

      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      ArrayList<String> strResult = new ArrayList<String>();
      strResult.clear();
      int contador = 0;
      ArrayList<SSRCNInvoiceCreditNote> listaFacturas = new ArrayList<>();

      while (rsConsulta.next()) {

        SSRCNInvoiceCreditNote modeloFactura = new SSRCNInvoiceCreditNote();
        modeloFactura.setStrInvoiceCreditNoteID(rsConsulta.getString("invoice_id"));
        modeloFactura.setStrInvoiceID(rsConsulta.getString("c_invoice_id"));
        listaFacturas.add(modeloFactura);

        // strResult.add(rsConsulta.getString("c_invoice_id"));
      }
      // System.out.println("Número de transacciones a procesar. " + contador);
      return listaFacturas;

    } catch (Exception e) {
      System.out.println("Error al consultar la tabla c_invoice " + e);
      return null;
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static String SelectParams() {
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      String strSql = "SELECT type_of_batch  FROM eei_param_facturae where isactive='Y' and type_of_batch is not null";
      PreparedStatement st = null;
      String strParametro = null;
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      int contador = 0;
      while (rsConsulta.next()) {
        contador = contador + 1;
        strParametro = rsConsulta.getString("type_of_batch");

      }
      if (contador == 0) {
        throw new OBException(
            "No se encontró parametrización de tipo de procesamiento en lote en documentos electrónicos.");
      } else if (contador > 1) {

        throw new OBException(
            "Existe más de una parametrización activa de documentos electrónicos.");
      }
      return strParametro;
    } catch (Exception e) {

      throw new OBException(
          "Error al consultar la tabla eei_param_facturae (Tipo de Lote WS) " + e);
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }
}
