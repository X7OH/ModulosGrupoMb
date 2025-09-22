package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.ServerVersionChecker;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Add_Stock extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  private static final Logger log = LoggerFactory.getLogger(ServerVersionChecker.class);

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpmProductId");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      log.debug("Server: " + strChanged);
      String strOrgId = vars.getStringParameter("inpadOrgId");
      log.debug("Server 2: " + strOrgId);

      try {
        printPage(response, vars, strChanged, strOrgId);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String strOrgId) throws IOException, ServletException {

    if (strOrgId != null && !strOrgId.equals("")) {
      if (log4j.isDebugEnabled())
        log4j.debug("Output: dataSheet");
      XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
          "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

      AddStockData[] data = AddStockData.select(this, strChanged, strOrgId);
      AddStockData[] dataprice = AddStockData.selectPrice(this, strChanged);
	  AddStockData[] datatax = AddStockData.selectTax(this, strChanged);

      StringBuffer resultado = new StringBuffer();

      resultado.append("var calloutName='Add_Stock';\n\n");
      resultado.append("var respuesta = new Array(");

      if (data == null || data.length == 0) {
        resultado.append("new Array(\"inpstock\", \"\"),");
        resultado.append("new Array(\"inpprice\", \"\"),");
		resultado.append("new Array(\"inpcTaxId\", \"\")");

      } else {
        resultado.append("new Array(\"inpstock\", \"" + data[0].qtyonhand + "\"),");
        resultado.append("new Array(\"inpprice\", \"" + dataprice[0].pricestd + "\"),");
		resultado.append("new Array(\"inpcTaxId\", \"" + datatax[0].tax + "\")");
      }

      resultado.append(");");
      xmlDocument.setParameter("array", resultado.toString());
      xmlDocument.setParameter("frameName", "appFrame");
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println(xmlDocument.print());
      out.close();
    }
  }
}
