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

public class Update_PriceTotal extends HttpSecureAppServlet {
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
      String strChanged = vars.getStringParameter("inpqtyordered");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      log.debug("Server: " + strChanged);
      String strPrice = vars.getStringParameter("inpprice");
      log.debug("Server 2: " + strPrice);

      try {
        printPage(response, vars, strChanged, strPrice);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String strPrice) throws IOException, ServletException {

    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    Double SubTotal = Double.valueOf(strChanged) * Double.valueOf(strPrice);
    Double Total = SubTotal * 1.12;

    StringBuffer resultado = new StringBuffer();

    resultado.append("var calloutName='Update_PriceTotal';\n\n");
    resultado.append("var respuesta = new Array(");

    resultado.append("new Array(\"inpsubtotal\", \"" + SubTotal + "\"),");
    resultado.append("new Array(\"inplinenetamt\", \"" + Total + "\")");

    resultado.append(");");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();

  }
}
