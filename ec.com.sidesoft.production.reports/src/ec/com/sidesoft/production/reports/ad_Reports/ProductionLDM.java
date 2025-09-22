package ec.com.sidesoft.production.reports.ad_Reports;

import java.sql.Connection;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.xmlEngine.XmlEngine;
//import org.openbravo.base.secureApp;
import org.quartz.SchedulerContext;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

public class ProductionLDM extends DalBaseProcess {
  private ConfigParameters cf;
  private SchedulerContext ctx;
  public XmlEngine xmlEngine = null;
  public static String strDireccion;

  @SuppressWarnings({ "deprecation", "null" })
  public void doExecute(ProcessBundle bundle) throws Exception {

    final OBError message = new OBError();

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    // ConnectionProvider conn = new DalConnectionProvider(false);

    ConnectionProvider conn = bundle.getConnection();

    // VariablesSecureApp varsAux = bundle.getContext().toVars();
    HttpServletResponse response = RequestContext.get().getResponse();
    HttpServletRequest request = RequestContext.get().getRequest();

    try {

      // retrieve the parameters from the bundle
      // Recupera los parametros de la sesión

      final String strOrgID = (String) bundle.getParams().get("adOrgId");
      final String strDateFrom = (String) bundle.getParams().get("datefrom");
      final String strDateTo = (String) bundle.getParams().get("dateto");
      final String strDocumentType = (String) bundle.getParams().get("cDoctypeId");
      final String strCategoryID = (String) bundle.getParams().get("mProductCategoryId");
      final String strProductID = (String) bundle.getParams().get("mProductId");
      // final String strOutputType = (String) bundle.getParams().get("outputtype");
      ProductionLDMData data[] = ProductionLDMData.select(conn, strOrgID, strDateFrom, strDateTo,
          strDocumentType, strCategoryID, strProductID);
      // ProductionLDMData data[] = ProductionLDMData.select(conn, strOrgID);

      // if (strOutputType.equals("XLS")) {
      try {

        // if (data != null && data.length > 0) { bundle.setResult(message);

        response.setContentType("application/vnd.ms-excel");

        response.setHeader("Content-Disposition", "attachment; filename=ProductionLDM.xls");
        WritableWorkbook w = null;
        w = Workbook.createWorkbook(response.getOutputStream());
        WritableSheet s = w.createSheet("ProductionLDM", 0);

        s.addCell(new Label(0, 0, "PRODUCCION LDM"));
        s.addCell(new Label(0, 1, "Fecha Desde:"));
        s.addCell(new Label(1, 1, strDateFrom));
        s.addCell(new Label(0, 2, "Fecha Hasta:"));
        s.addCell(new Label(1, 2, strDateTo));

        s.addCell(new Label(0, 3, "PUNTO DE VENTA"));
        s.addCell(new Label(1, 3, "CATEGORIA"));
        s.addCell(new Label(2, 3, "CODIGO"));
        s.addCell(new Label(3, 3, "NOMBRE EL PRODUCTO"));
        s.addCell(new Label(4, 3, "CANTIDAD ELABORADA"));
        s.addCell(new Label(5, 3, "UNIDAD"));
        s.addCell(new Label(6, 3, "COSTO UNITARIO"));
        s.addCell(new Label(7, 3, "COSTO TOTAL"));

        int i = 4;
        for (ProductionLDMData ProductionData : data) {
          s.addCell(new Label(0, i, ProductionData.puntoventa));
          s.addCell(new Label(1, i, ProductionData.categoria));
          s.addCell(new Label(2, i, ProductionData.codProducto));
          s.addCell(new Label(3, i, ProductionData.producto));
          s.addCell(new Label(4, i, ProductionData.cantidad));
          s.addCell(new Label(5, i, ProductionData.unidad));
          s.addCell(new Label(6, i, String.valueOf(ProductionData.costounitario)));
          s.addCell(new Label(7, i, String.valueOf(ProductionData.costototal)));
          i++;
        }

        w.write();
        w.close();

      } catch (final Exception e) {
        e.printStackTrace(System.err);
        message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
        message.setType("Error");
        message.setMessage(e.getMessage() + e.fillInStackTrace());
      } finally {
        bundle.setResult(message);
      }

      // } // else {
      //
      // VariablesSecureApp vars = new VariablesSecureApp(request);
      //
      // String strReportName = "ProductionLDM.jrxml";
      // String StrNameReport = "ProductionLDM.jrxml";
      // String outputFile = "";
      //
      // HashMap<String, Object> parameters = new HashMap<String, Object>();
      // parameters.put("Ad_Org_ID", strOrgID);
      // parameters.put("DateFrom", strDateFrom);
      // parameters.put("DateTo", strDateTo);
      // parameters.put("C_Doctype_ID", strDocumentType);
      // parameters.put("M_Product_Category_ID", strCategoryID);
      // parameters.put("M_Product_ID", strProductID);
      //
      // outputFile = GetReport(strReportName, StrNameReport, parameters);
      //
      // File pdfFile = new File(outputFile);
      // response.setContentLength((int) pdfFile.length());
      // try {
      // FileInputStream fileInputStream = null;
      // fileInputStream = new FileInputStream(pdfFile);
      // OutputStream responseOutputStream = response.getOutputStream();
      // int bytes;
      // while ((bytes = fileInputStream.read()) != -1) {
      // responseOutputStream.write(bytes);
      // }
      // responseOutputStream.close();
      // responseOutputStream.flush();
      // // printPageClosePopUpAndRefreshParent(response, vars);
      // } catch (Exception e) {
      //
      // }
      //
      // }

    } finally

    {
      bundle.setResult(message);
    }
  }

  public String GetReport(String strReportName, String StrNameReport,
      HashMap<String, Object> parameters) {
    JasperReport report = null;

    String SrtLinkReport = null;
    SrtLinkReport = strReportName;
    JasperPrint print;
    Connection con = null;
    final String outputFile = "@basedesign@/ec/com/sidesoft/production/payroll/ad_reports/reports/ProductionLDM.jrxml"
        + "/" + StrNameReport.replace(".jrxml", ".pdf");

    try {
      // con = getTransactionConnection();
      parameters.put("REPORT_CONNECTION", con);
      report = JasperCompileManager.compileReport(SrtLinkReport);
      print = JasperFillManager.fillReport(report, parameters, con);
      JasperExportManager.exportReportToPdfFile(print, outputFile);
      // con.close();

    } catch (Exception e) {
      // log4j.debug("Error: Goods Movement - pdf");
      throw new OBException("Error template: " + e.getMessage().toString());
    }

    return outputFile;
  }
  /*
   * private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars, String
   * strDocumentId) throws IOException, ServletException { ConnectionProvider conn = new
   * DalConnectionProvider(false); String language =
   * OBContext.getOBContext().getLanguage().getLanguage();
   * 
   * String StrRutaReport = "@basedesign@/ec/com/sidesoft/production/payroll/ad_reports/reports";
   * 
   * String strReportName = StrRutaReport +
   * "/@basedesign@/ec/com/sidesoft/production/payroll/ad_reports/reports/ProductionLDM.jrxml";
   * 
   * final String strAttach = globalParameters.strFTPDirectory + "/284-" + classInfo.id; String
   * strFTPDirectory = getResolvedParameter(ServletContext context, "AttachmentDirectory");
   * 
   * final String strLanguage = vars.getLanguage();
   * 
   * final String strBaseDesign = getBaseDesignPath(strLanguage);
   * 
   * strReportName = Replace.replace( Replace.replace(strReportName, "@basedesign@", strBaseDesign),
   * "@attach@", strAttach);
   * 
   * if (log4j1.isDebugEnabled()) log4j1.debug("Output: Liquidations Project - pdf");
   * 
   * // VALIDACION PARA SQL
   * 
   * HashMap<String, Object> parameters = new HashMap<String, Object>();
   * parameters.put("DOCUMENT_ID", strDocumentId); String StrBaseWeb =
   * getBaseDesignPath(strLanguage); parameters.put("BASE_WEB", StrBaseWeb);
   * parameters.put("AD_USER_ID", strADUSerID); // String StrNameReport =
   * LstTemplate.get(0).getTitle().replace(" ", "_") + ".jrxml";
   * 
   * renderJR(vars, response, strReportName, "pdf", parameters, null, null);
   * 
   * 
   * 
   * }
   */

  public static String removeAcute(String input) {
    // Cadena de caracteres original a sustituir.
    String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
    // Cadena de caracteres ASCII que reemplazarán los originales.
    String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
    String output = input;
    for (int i = 0; i < original.length(); i++) {
      // Reemplazamos los caracteres especiales.
      output = output.replace(original.charAt(i), ascii.charAt(i));
    } // for i
    return output;
  }// removeAcute
}
