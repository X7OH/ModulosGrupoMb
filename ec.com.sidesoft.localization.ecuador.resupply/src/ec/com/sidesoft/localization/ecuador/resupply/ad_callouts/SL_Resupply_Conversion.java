/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package ec.com.sidesoft.localization.ecuador.resupply.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.ProductAUM;
import org.openbravo.xmlEngine.XmlDocument;

public class SL_Resupply_Conversion extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  public static final String UOM_PRIMARY = "P";

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpLastFieldChanged");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      String strUOM = vars.getStringParameter("inpcUomId");
      // String strMProductUOMID = vars.getStringParameter("inpmProductUomId");
      String strCAum = vars.getStringParameter("inpcAum");
      String strQuantityOrder = vars.getNumericParameter("inpquantityorder");
      String strTabId = vars.getStringParameter("inpTabId");
      String strProductId = vars.getStringParameter("inpmProductId");

      try {
        // printPage(response, vars, strUOM, strMProductUOMID, strQuantityOrder, strChanged,
        // strTabId);
        printPage(response, vars, strUOM, strCAum, strQuantityOrder, strChanged, strTabId,
            strProductId);

      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strUOM,
      // String strMProductUOMID, String strQuantityOrder, String strChanged, String strTabId)
      String strCAum, String strQuantityOrder, String strChanged, String strTabId,
      String strProductId) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
    if (strUOM.startsWith("\""))
      strUOM = strUOM.substring(1, strUOM.length() - 1);
    String strPrecision = SLResupplyLineConversionData.stdPrecision(this, strUOM);
    int stdPrecision = strPrecision.equals("") ? 0 : Integer.valueOf(strPrecision).intValue();
    // String strInitUOM = SLResupplyLineConversionData.initUOMId(this, strMProductUOMID);
    String strMultiplyRate;
    boolean check = false;
    BigDecimal RateAUM = BigDecimal.ONE;

    // strMultiplyRate = SLResupplyLineConversionData.multiplyRate(this, strInitUOM, strUOM);
    try {
      OBCriteria<ProductAUM> pAUMCriteria = OBDal.getInstance().createCriteria(ProductAUM.class);
      pAUMCriteria.add(Restrictions.eq("product.id", strProductId));
      pAUMCriteria.add(Restrictions.eq(ProductAUM.PROPERTY_LOGISTICS, UOM_PRIMARY));

      ProductAUM primaryAum = (ProductAUM) pAUMCriteria.uniqueResult();
      if (primaryAum != null) {
        if (!strUOM.equals(strCAum)) {
          RateAUM = primaryAum.getConversionRate();
        }
      }
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    BigDecimal quantityOrder, qtyOrdered, multiplyRate;

    multiplyRate = RateAUM;

    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='SL_Resupply_Conversion';\n\n");
    resultado.append("var respuesta = new Array(");
    if (!strQuantityOrder.equals("")) {
      quantityOrder = new BigDecimal(strQuantityOrder);
      qtyOrdered = quantityOrder.multiply(multiplyRate);
      if (qtyOrdered.scale() > stdPrecision)
        qtyOrdered = qtyOrdered.setScale(stdPrecision, BigDecimal.ROUND_HALF_UP);
     resultado.append("new Array(\"inpqty\", " + qtyOrdered.toString() + "),\n\n");
      resultado.append("new Array(\"inpsecqtyuom\", " + multiplyRate.toString() + ")");

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
