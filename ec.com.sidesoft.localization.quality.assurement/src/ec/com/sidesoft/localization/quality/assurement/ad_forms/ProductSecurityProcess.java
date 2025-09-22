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
 * All portions are Copyright (C) 2008-2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package ec.com.sidesoft.localization.quality.assurement.ad_forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.businessUtility.WindowTabsData;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ProductSecurityProcess extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strOrgId = vars.getGlobalVariable("inpadOrgId", "ProductSecurityProcess|AD_Org_ID",
          vars.getOrg());
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ProductSecurityProcess|DateFrom",
          "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ProductSecurityProcess|DateTo", "");
      String strDocumentno = vars.getGlobalVariable("inpDocumentno",
          "ProductSecurityProcess|Documentno", "");

      String strCode = vars.getGlobalVariable("inpCode", "ProductSecurityProcess|Code", "");

      String strProductId = vars.getGlobalVariable("inpProductId",
          "ProductSecurityProcess|M_PRODUCT_ID", "");

      vars.setSessionValue("ProductSecurityProcess|isSOTrx", "N");
      printPageDataSheet(response, vars, strOrgId, strDocumentno, strCode, strProductId,
          strDateFrom, strDateTo);
    } else if (vars.commandIn("FIND")) {
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId",
          "ProductSecurityProcess|AD_Org_ID");
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ProductSecurityProcess|DateFrom",
          "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ProductSecurityProcess|DateTo", "");
      String strDocumentno = vars.getGlobalVariable("inpDocumentno",
          "ProductSecurityProcess|Documentno", "");

      String strCode = vars.getGlobalVariable("inpCode", "ProductSecurityProcess|Code", "");

      String strProductId = vars.getGlobalVariable("inpProductId",
          "ProductSecurityProcess|M_PRODUCT_ID", "");

      printPageDataSheet(response, vars, strOrgId, strDocumentno, strCode, strProductId,
          strDateFrom, strDateTo);
    } else if (vars.commandIn("ADD")) {

      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId",
          "ProductSecurityProcess|AD_Org_ID");
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ProductSecurityProcess|DateFrom",
          "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ProductSecurityProcess|DateTo", "");
      String strDocumentno = vars.getGlobalVariable("inpDocumentno",
          "ProductSecurityProcess|Documentno", "");

      String strCode = vars.getGlobalVariable("inpCode", "ProductSecurityProcess|Code", "");

      String strProductId = vars.getGlobalVariable("inpProductId",
          "ProductSecurityProcess|M_PRODUCT_ID", "");

      // inpSafetyID donde se guarda el ID del registro, a partir del HTLM
      String strSafetyID = vars.getRequiredInStringParameter("inpSafetyID", IsIDFilter.instance);
      // lockRefundLines(vars, strRefundLines);
      // printPageDataSheet(response, vars, strOrgId);

      ProductSecurityProcessData[] productSecProcessdata = ProductSecurityProcessData.select(this,
          strSafetyID.replace("('", "").replace("')", ""));

      if (productSecProcessdata == null || productSecProcessdata.length == 0) {
        OBError myMessage = new OBError();
        myMessage.setType("Error");
        myMessage.setMessage(Utility.messageBD(this, "NotFound", vars.getLanguage()));
        advise(response, vars, "ERROR", "ERROR",
            "El registro seleccionado ya esta registrado en la pantalla de 'Datos del Control de Calidad..'");
        // return myMessage;
      } else {

        OBError myMessage = processGenerateControl(vars, strSafetyID, strOrgId, strDocumentno,
            strCode, strProductId, strDateFrom, strDateTo);

        myMessage.setMessage(Utility.messageBD(this, "Success", vars.getLanguage()));
        myMessage.setType("info");

        // if (myMessage != null) {
        // xmlDocument.setParameter("messageType", "info");
        // xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        // xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        vars.setMessage("ProductSecurityProcess", myMessage);
        // printPageCreate(response, vars, "", "", "", "", "", "");
        printPageDataSheet(response, vars, strOrgId, "", "", "", "", "");
      }

    }

    else if (vars.commandIn("REMOVE")) {

      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId",
          "ProductSecurityProcess|AD_Org_ID");
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);
      // unlockRefundLines(vars, strSelectedLines);
      // printPageDataSheet(response, vars, strOrgId);
    } else if (vars.commandIn("OPEN_CREATE")) {
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);
      // checkSelectedRequisitionLines(response, vars, strSelectedLines);
    } else if (vars.commandIn("GENERATE")) {
      String strSelectedLines = vars.getRequiredGlobalVariable("inpSelected",
          "RefundCreate|SelectedLines");
      String strDocType = vars.getRequiredGlobalVariable("inpDocTypeId", "RefundCreate|DocType");
      String strInvoiceDate = vars.getRequiredGlobalVariable("inpInvoiceDate",
          "RefundCreate|InvoiceDate");
      String strCustomer = vars.getRequiredGlobalVariable("inpInvoiceCustomerId",
          "RefundCreate|InvoiceCustomer");
      String strPriceListId = vars.getRequiredGlobalVariable("inpPriceListId",
          "RefundCreate|PriceListId");
      String strOrg = vars.getRequiredGlobalVariable("inpInvoiceOrg", "RefundCreate|Org");
      OBError myMessage = processSalesInvoice(vars, strSelectedLines, strDocType, strInvoiceDate,
          strCustomer, strPriceListId, strOrg);
      vars.setMessage("RefundCreate", myMessage);
      printPageCreate(response, vars, "", "", "", "", "", "");
    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strOrgId, String strDocumentNo, String StrCode, String strProductId,
      String strDateFrom, String strDateTo) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;

    String strTreeOrg = ProductSecurityProcessData.treeOrg(this, vars.getClient());
    ProductSecurityProcessData[] datalines = ProductSecurityProcessData.selectLines(this,
        vars.getLanguage(), strDocumentNo, StrCode, strProductId, strDateFrom, strDateTo);

    ProductSecurityProcessData[] dataselected = ProductSecurityProcessData.selectSelected(this,
        vars.getLanguage());

    String discard[] = { "" };
    if (dataselected == null || dataselected.length == 0) {
      dataselected = ProductSecurityProcessData.set();
      discard[0] = "funcSelectedEvenOddRow";
    }
    xmlDocument = xmlEngine.readXmlTemplate(
        "ec/com/sidesoft/localization/quality/assurement/ad_forms/ProductSecurityProcess", discard)
        .createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ProductSecurityProcess", false, "", "",
        "", false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sidesoft.localization.ecuador.refunds.ad_forms.ProductSecurityProcess");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ProductSecurityProcess.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ProductSecurityProcess.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    /*
     * { OBError myMessage = vars.getMessage("ProductSecurityProcess");
     * vars.removeMessage("ProductSecurityProcess"); if (myMessage != null) {
     * xmlDocument.setParameter("messageType", myMessage.getType());
     * xmlDocument.setParameter("messageTitle", myMessage.getTitle());
     * xmlDocument.setParameter("messageMessage", myMessage.getMessage()); } }
     */

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));

    xmlDocument.setParameter("paramDocumentno", strDocumentNo);
    xmlDocument.setParameter("paramCode", StrCode);
    xmlDocument.setParameter("paramMProductId", strProductId);
    // xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateTo", strDateTo);

    xmlDocument.setParameter("paramAdOrgId", strOrgId);

    /*
     * try { ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
     * "AD_User_ID", "", "AD_User - Internal", Utility.getContext(this, vars, "#AccessibleOrgTree",
     * "ProductSecurityProcess"), Utility.getContext(this, vars, "#User_Client",
     * "ProductSecurityProcess"), 0); Utility.fillSQLParameters(this, vars, null, comboTableData,
     * "ProductSecurityProcess", strRequesterId); xmlDocument.setData("reportRequester_ID",
     * "liststructure", comboTableData.select(false)); comboTableData = null; } catch (Exception ex)
     * { throw new ServletException(ex); }
     */
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org Security validation",
          Utility.getContext(this, vars, "#User_Org", "ProductSecurityProcess"),
          Utility.getContext(this, vars, "#User_Client", "ProductSecurityProcess"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductSecurityProcess",
          strOrgId);
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "M_PRODUCT",
          "53DBA46323294201858ABF80A05692DB", "",
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ProductSecurityProcess"),
          Utility.getContext(this, vars, "#User_Client", "ProductSecurityProcess"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductSecurityProcess",
          strProductId);
      xmlDocument.setData("reportM_Product_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    // We must execute the selected query.

    xmlDocument.setData("structureSearch", datalines);
    // xmlDocument.setData("structureSelected", dataselected);

    OBError myMessage = new OBError();
    myMessage.setTitle("info");
    myMessage.setType("Info");
    myMessage.setMessage("Sucess");
    vars.setMessage("Sucess", myMessage);
    xmlDocument.setParameter("messageType", myMessage.getType());
    xmlDocument.setParameter("messageTitle", myMessage.getTitle());
    xmlDocument.setParameter("messageMessage", myMessage.getMessage());

    out.println(xmlDocument.print());
    out.close();

  }

  private void advise(HttpServletResponse response, VariablesSecureApp vars, String strTipo,
      String strTitulo, String strTexto) throws IOException, ServletException {

    final XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/base/secureApp/Error")// Advise")
        .createXmlDocument();

    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("ParamTipo", strTipo.toUpperCase());
    xmlDocument.setParameter("ParamTitulo", strTitulo);
    xmlDocument.setParameter("ParamTexto", strTexto);
    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageCreate(HttpServletResponse response, VariablesSecureApp vars,
      String strInvoiceDate, String strCustomerId, String strPriceListId, String strDocTypeId,
      String strOrgId, String strSelected) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Print Create Sales Invoice");
    String strDescription = Utility.messageBD(this, "Success", vars.getLanguage());

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate(
            "ec/com/sidesoft/localization/quality/assurement/ad_forms/ProductSecurityProcess")
        .createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ProductSecurityProcess", false, "", "",
        "", false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    {
      OBError myMessage = vars.getMessage("RefundCreate");
      vars.removeMessage("RefundCreate");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    xmlDocument.setParameter("paramSelected", strSelected);
    xmlDocument.setParameter("paramInvoiceCustomerId", strCustomerId);
    xmlDocument.setParameter("paramInvoiceCustomerDescription", strCustomerId.equals("") ? ""
        : ProductSecurityProcessData.bPartnerDescription(this, strCustomerId, vars.getLanguage()));
    xmlDocument.setParameter("invoiceDate", strInvoiceDate);
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paramInvoiceOrgId", strOrgId);
    xmlDocument.setParameter("paramPriceListId", strPriceListId);
    xmlDocument.setParameter("paramDocTypeId", strDocTypeId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org is transactions allowed",
          Utility.getContext(this, vars, "#User_Org", "ProductSecurityProcess"),
          Utility.getContext(this, vars, "#User_Client", "ProductSecurityProcess"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductSecurityProcess",
          strOrgId);
      xmlDocument.setData("reportInvoiceOrg_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "M_Pricelist_ID",
          "", "Price List isSOTrx",
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ProductSecurityProcess"),
          Utility.getContext(this, vars, "#User_Client", "ProductSecurityProcess"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductSecurityProcess",
          strPriceListId);
      xmlDocument.setData("reportPriceList_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Doctype_ID", "",
          "C_DocType AR/AP Invoices and Credit Memos",
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ProductSecurityProcess"),
          Utility.getContext(this, vars, "#User_Client", "ProductSecurityProcess"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductSecurityProcess",
          strDocTypeId);
      xmlDocument.setData("reportDocType_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError processGenerateControl(VariablesSecureApp vars, String strSelected, String strOrg,
      String strDocumentno, String strCode, String strProductID, String strDateFrom,
      String strDateTo) throws IOException, ServletException {
    StringBuffer textMessage = new StringBuffer();
    Connection conn = null;
    ConnectionProvider connProv = new DalConnectionProvider(false);

    ProductSecurityProcessData[] datalines = ProductSecurityProcessData.selectLines(this,
        vars.getLanguage(), strDocumentno, strCode, strProductID, strDateFrom, strDateTo);

    String discard[] = { "" };
    if (datalines == null || datalines.length == 0) {
      datalines = ProductSecurityProcessData.set();
      discard[0] = "funcSelectedEvenOddRow";
    }

    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "ec/com/sidesoft/localization/quality/assurement/ad_forms/ProductSecurityProcess", discard)
        .createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ProductSecurityProcess", false, "", "",
        "", false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    OBError myMessage = new OBError();
    myMessage.setMessage(Utility.messageBD(this, "Success", vars.getLanguage()));
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", "info");
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }
    vars.setMessage("ProductSecurityProcess", myMessage);

    ProductSecurityProcessData[] productSecProcessdata = ProductSecurityProcessData.select(connProv,
        strSelected.replace("('", "").replace("')", ""));

    try {
      connProv.destroy();
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    if (productSecProcessdata == null || productSecProcessdata.length == 0) {
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "NotFound", vars.getLanguage()));
      return myMessage;
    }

    /*
     * if ("".equals(ProductSecurityProcessData.cBPartnerLocationId(this, strSelected))) {
     * myMessage.setType("Error"); myMessage.setMessage(Utility.messageBD(this, "NoBPLocation",
     * vars.getLanguage())); return myMessage; }
     */

    try {
      conn = getTransactionConnection();
      String strMaPcCaseId = SequenceIdData.getUUID();

      String strDocType = ProductSecurityProcessData.selectDoctype(this, "800141", "SLQS_PC");
      String strDocumentNo = Utility.getDocumentNo(this, vars, "", "Ma_PC_Case", strDocType,
          strDocType, false, true);

      String strControl = productSecProcessdata[0].maPeriodicControlId;
      String strProductId = productSecProcessdata[0].mProductId;
      String strAttribute = productSecProcessdata[0].mAttributesetinstanceId;
      String strStartdate = String.valueOf(productSecProcessdata[0].starttime);
      String strLaunched = "N";
      String strUomId = productSecProcessdata[0].cUomId;
      String strName = "Prueba de Calidad PP: " + productSecProcessdata[0].docPartWork + " - "
          + strStartdate;
      String strReferenceId = productSecProcessdata[0].slqsPrdSafetyVId;

      try {
        ProductSecurityProcessData.insertMaPcCase(conn, this, strMaPcCaseId, vars.getClient(),
            strOrg, vars.getUser(), strControl, strProductId, strAttribute, strName, strLaunched,
            strUomId, strReferenceId, strDocType, strDocumentNo);
      } catch (ServletException ex) {
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
        releaseRollbackConnection(conn);
        return myMessage;
      }
      /*
       * ProductSecurityProcessData.insertMaPcCase(conn, connProv, strMaPcCaseId, vars.getClient(),
       * strOrg, vars.getUser(), strControl, strProductId, strAttribute, strName, strLaunched,
       * strUomId, strReferenceId, strDocType, strDocumentNo);
       */

      int line = 0;
      String strCInvoicelineID = "";

      // unlockRefundLines(vars, strSelected);
      // strCInvoicelineID = SequenceIdData.getUUID();
      /*
       * for (int i = 0; lines != null && i < lines.length; i++) { if ("".equals(lines[i].tax)) {
       * Invoice il = OBDal.getInstance().get(Invoice.class, lines[i].cInvoiceId);
       * myMessage.setType("Error"); myMessage.setMessage(
       * String.format(OBMessageUtils.messageBD("NoTaxRequisition"), il.getDocumentNo()));
       * releaseRollbackConnection(conn); return myMessage; }
       * 
       * line += 10;
       * 
       * if (log4j.isDebugEnabled()) log4j.debug("qtyInvoice: " + lines[i].qtyinvoiced +
       * " new BigDecimal: " + (new BigDecimal(lines[i].qtyinvoiced)).toString());
       * 
       * try { ProductSecurityProcessData.insertCInvoiceline(conn, this, strCInvoicelineID,
       * vars.getClient(), strOrg, vars.getUser(), strCInvoiceId, Integer.toString(line),
       * strCustomer, lines[i].description, lines[i].mProductId, lines[i].mAttributesetinstanceId,
       * lines[i].cUomId, lines[i].qtyinvoiced, lines[i].grandtotal, lines[i].grandtotal,
       * lines[i].grandtotal, lines[i].grandtotal, lines[i].grandtotal, lines[i].grandtotal,
       * lines[i].tax, "", lines[i].grandtotal); } catch (ServletException ex) { myMessage =
       * Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
       * releaseRollbackConnection(conn); return myMessage; } strCInvoicelineID =
       * SequenceIdData.getUUID(); }
       */

      // cInvoicePost(conn, vars, strMaPcCaseId);

      releaseCommitConnection(conn);
      // String strWindowName = WindowTabsData.selectWindowInfo(this, vars.getLanguage(), "167");
      // textMessage.append(strWindowName).append(" ").append(strDocumentNo).append(": ");
      // myMessage.setMessage("Success");
      if (myMessage.getMessage().equals(""))
        textMessage.append(Utility.messageBD(this, "Success", vars.getLanguage()));
      else
        textMessage.append(myMessage.getMessage());

      myMessage.setType(myMessage.getType());
      myMessage.setMessage(textMessage.toString());
      return myMessage;
    } catch (Exception e) {
      try {
        if (conn != null)
          releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage.setType("info");
      myMessage.setMessage(Utility.messageBD(this, "ErrorAggregatingData", vars.getLanguage()));
      return myMessage;
    }
  }

  private OBError processSalesInvoice(VariablesSecureApp vars, String strSelected,
      String strDocType, String strInvoiceDate, String strCustomer, String strPriceListId,
      String strOrg) throws IOException, ServletException {
    StringBuffer textMessage = new StringBuffer();
    Connection conn = null;

    OBError myMessage = null;
    myMessage = new OBError();
    myMessage.setTitle("");

    String strPriceListVersionId = ProductSecurityProcessData.getPricelistVersion(this,
        strPriceListId, strInvoiceDate);

    ProductSecurityProcessData[] data1 = ProductSecurityProcessData.selectCustomerData(this,
        strCustomer);
    /*
     * if (data1[0].cPaymenttermId == null || data1[0].cPaymenttermId.equals("")) {
     * myMessage.setType("Error"); myMessage.setMessage( Utility.messageBD(this,
     * "SSRE_CustomerWithNoPaymentTerm", vars.getLanguage())); return myMessage; }
     */
    if ("".equals(ProductSecurityProcessData.cBPartnerLocationId(this, strCustomer))) {
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "NoBPLocation", vars.getLanguage()));
      return myMessage;
    }

    try {
      conn = getTransactionConnection();
      String strCInvoiceId = SequenceIdData.getUUID();
      String strDocumentNo = Utility.getDocumentNo(this, vars, "", "Ma_PC_Case", strDocType,
          strDocType, false, true);
      String cCurrencyId = ProductSecurityProcessData.selectCurrency(this, strPriceListId);

      /*
       * try { ProductSecurityProcessData.insertCInvoice(conn, this, strCInvoiceId,
       * vars.getClient(), strOrg, vars.getUser(), strDocumentNo, "DR", "CO", "0", strDocType,
       * strInvoiceDate, strInvoiceDate, strCustomer, ProductSecurityProcessData.billto(this,
       * strCustomer), cCurrencyId, isAlternativeFinancialFlow() ? "P" : data1[0].paymentrule,
       * data1[0].cPaymenttermId, strPriceListId, "", "", "", data1[0].finPaymentmethodId); } catch
       * (ServletException ex) { myMessage = Utility.translateError(this, vars, vars.getLanguage(),
       * ex.getMessage()); releaseRollbackConnection(conn); return myMessage; }
       */

      int line = 0;
      String strCInvoicelineID = "";

      ProductSecurityProcessData[] lines = ProductSecurityProcessData.linesToInvoice(this,
          strInvoiceDate, strOrg, vars.getWarehouse(),
          ProductSecurityProcessData.billto(this, strCustomer).equals("")
              ? ProductSecurityProcessData.cBPartnerLocationId(this, strCustomer)
              : ProductSecurityProcessData.billto(this, strCustomer),
          ProductSecurityProcessData.cBPartnerLocationId(this, strCustomer), strSelected);

      // unlockRefundLines(vars, strSelected);
      strCInvoicelineID = SequenceIdData.getUUID();
      /*
       * for (int i = 0; lines != null && i < lines.length; i++) { if ("".equals(lines[i].tax)) {
       * Invoice il = OBDal.getInstance().get(Invoice.class, lines[i].cInvoiceId);
       * myMessage.setType("Error"); myMessage.setMessage(
       * String.format(OBMessageUtils.messageBD("NoTaxRequisition"), il.getDocumentNo()));
       * releaseRollbackConnection(conn); return myMessage; }
       * 
       * line += 10;
       * 
       * if (log4j.isDebugEnabled()) log4j.debug("qtyInvoice: " + lines[i].qtyinvoiced +
       * " new BigDecimal: " + (new BigDecimal(lines[i].qtyinvoiced)).toString());
       * 
       * try { ProductSecurityProcessData.insertCInvoiceline(conn, this, strCInvoicelineID,
       * vars.getClient(), strOrg, vars.getUser(), strCInvoiceId, Integer.toString(line),
       * strCustomer, lines[i].description, lines[i].mProductId, lines[i].mAttributesetinstanceId,
       * lines[i].cUomId, lines[i].qtyinvoiced, lines[i].grandtotal, lines[i].grandtotal,
       * lines[i].grandtotal, lines[i].grandtotal, lines[i].grandtotal, lines[i].grandtotal,
       * lines[i].tax, "", lines[i].grandtotal); } catch (ServletException ex) { myMessage =
       * Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
       * releaseRollbackConnection(conn); return myMessage; } strCInvoicelineID =
       * SequenceIdData.getUUID(); }
       */
      for (int i = 0; lines != null && i < lines.length; i++) {
        // ProductSecurityProcessData.refunded(conn, this, vars.getUser(), lines[i].cInvoiceId);
      }

      OBError myMessageAux = cInvoicePost(conn, vars, strCInvoiceId);
      releaseCommitConnection(conn);
      String strWindowName = WindowTabsData.selectWindowInfo(this, vars.getLanguage(), "167");
      textMessage.append(strWindowName).append(" ").append(strDocumentNo).append(": ");
      if (myMessageAux.getMessage().equals(""))
        textMessage.append(Utility.messageBD(this, "Success", vars.getLanguage()));
      else
        textMessage.append(myMessageAux.getMessage());

      myMessage.setType(myMessageAux.getType());
      myMessage.setMessage(textMessage.toString());
      return myMessage;
    } catch (Exception e) {
      try {
        if (conn != null)
          releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "ProcessRunError", vars.getLanguage()));
      return myMessage;
    }
  }

  private OBError cInvoicePost(Connection conn, VariablesSecureApp vars, String strcInvoiceId)
      throws IOException, ServletException {
    String pinstance = SequenceIdData.getUUID();

    PInstanceProcessData.insertPInstance(conn, this, pinstance, "111", strcInvoiceId, "N",
        vars.getUser(), vars.getClient(), vars.getOrg());
    ProductSecurityProcessData.cInvoicePost0(conn, this, pinstance);

    PInstanceProcessData[] pinstanceData = PInstanceProcessData.selectConnection(conn, this,
        pinstance);
    OBError myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
    return myMessage;
  }

  /**
   * Checks if the any module implements and alternative Financial Management preference. It should
   * be the Advanced Payables and Receivables module.
   * 
   * @return true if any module implements and alternative Financial Management preference.
   */
  private boolean isAlternativeFinancialFlow() {
    try {
      try {
        Preferences.getPreferenceValue("FinancialManagement", true, null, null,
            OBContext.getOBContext().getUser(), null, null);
      } catch (PropertyNotFoundException e) {
        return false;
      }
    } catch (PropertyException e) {
      return false;
    }
    return true;
  }

  public String getServletInfo() {
    return "Servlet Refund.";
  } // end of getServletInfo() method

}
