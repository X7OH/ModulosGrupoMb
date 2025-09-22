package ec.com.sidesoft.retail.bank.deposits.ad_process;

import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

import ec.com.sidesoft.custom.reports.SescrTemplateReport;

public class Ssrbd_PrintReportBank_Deposits extends HttpSecureAppServlet {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static Logger log4j = Logger.getLogger(Ssrbd_PrintReportBank_Deposits.class);
  public String str_Atachment, str_FTP;
  public Connection connection_DB = null;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public String doPost(HttpServletRequest request, HttpServletResponse response,
      String strAtachments, String strFTP, Connection DBConnection, String formarReport)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    String outputFile = "";    
    str_Atachment = strAtachments;
    str_FTP = strFTP;
    connection_DB = DBConnection;

    if (log4j.isDebugEnabled())
      log4j.debug("str_Atachment: " + str_Atachment);

    outputFile = printPagePDF(response, vars, str_Atachment, str_FTP, formarReport);
   
    
    return outputFile;
  }

  private String printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strAtachment, String str_FtpDirecotry, String formarReport) throws IOException,
      ServletException {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    String outputFile = "";
    String StrWindowdID = "";
    String StrTableID = "";
    StrWindowdID = "32F43BE3E834468EA6E3186BF51F9975";
    StrTableID = "E100368B01F143D2A6122A5AE6A9754C";

    Window ADWindow = OBDal.getInstance().get(Window.class, StrWindowdID);
    Table ADTable = OBDal.getInstance().get(Table.class, StrTableID);

    OBCriteria<SescrTemplateReport> PrintWithh = OBDal.getInstance().createCriteria(
        SescrTemplateReport.class);
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_WINDOW, ADWindow));
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_TABLE, ADTable));
    // PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_ORGANIZATION, ADOrg));

    List<SescrTemplateReport> LstTemplate = PrintWithh.list();
    int ICountTemplate;
    ICountTemplate = LstTemplate.size();
    

    if (ICountTemplate == 0) {

      throw new OBException(Utility.messageBD(conn, "@Template no Found..@", language));

    }
    if (LstTemplate.get(0).getWindow().getId().equals(StrWindowdID)) {

      String StrRutaReport = LstTemplate.get(0).getTemplateDir();
      String strReportName = StrRutaReport + "/" + LstTemplate.get(0).getNameReport();
      final String strAttach = strAtachment;
      final String strLanguage = vars.getLanguage();

      strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strAttach),
          "@attach@", strAttach);

      if (log4j.isDebugEnabled())
        log4j.debug("Output: Bank Deposits - pdf");

      Date fecha = new Date();
      try {
        fecha = getDatePreviousDay();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DateFrom", fecha);
      parameters.put("DateTo", fecha);

      String StrNameReport = LstTemplate.get(0).getNameReport();
      outputFile = GetReport(strReportName, StrNameReport, parameters, formarReport);
      
     }
    
    return outputFile;

  }

  public String GetReport(String strReportName, String StrNameReport,
      HashMap<String, Object> parameters, String format) {
    JasperReport report = null;

    String SrtLinkReport = null;
    SrtLinkReport = strReportName;
    JasperPrint print;
    final String outputFile = str_FTP + "/" + StrNameReport.replace(".jrxml", format);

    try {
      parameters.put("REPORT_CONNECTION", connection_DB);
      report = JasperCompileManager.compileReport(SrtLinkReport);
      print = JasperFillManager.fillReport(report, parameters, connection_DB);
      JasperExportManager.exportReportToPdfFile(print, outputFile);
      connection_DB.close();

    } catch (Exception e) {
      throw new OBException("Error template: " + e.getMessage().toString());
    }

    return outputFile;
  }
  
  private static Date getDatePreviousDay() throws ParseException {

    Date today = new Date();

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(today);
    calendar.add(Calendar.DATE, -1);
    Date fecha = calendar.getTime();
    
    return fecha;

  }

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}