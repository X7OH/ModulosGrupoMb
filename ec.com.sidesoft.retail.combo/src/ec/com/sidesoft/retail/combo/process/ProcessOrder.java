package ec.com.sidesoft.retail.combo.process;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.fop.afp.util.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationTree;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.CallStoredProcedure;

import ec.com.sidesoft.document.sequence.PoinOfSaleSequenceLine;
import ec.com.sidesoft.document.sequence.PointOfSaleSeq;
import ec.com.sidesoft.process.print.PrintProcessFromTemplate;
import ec.com.sidesoft.quickbilling.advanced.SaqbCallcenterinvoiceConf;
import ec.com.sidesoft.smartdelivery.SSMRDRConfigSmartdelivery;
import ec.com.sidesoft.smartdelivery.ad_process.SmartDeliveryAPI;

public class ProcessOrder extends BaseProcessActionHandler {
  private static Logger log = Logger.getLogger(ProcessOrder.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    OBCriteria<SSMRDRConfigSmartdelivery> config = OBDal.getInstance()
        .createCriteria(SSMRDRConfigSmartdelivery.class);
    config.add(Restrictions.eq(SSMRDRConfigSmartdelivery.PROPERTY_ACTIVE, true));
    config.addOrderBy(SSMRDRConfigSmartdelivery.PROPERTY_CREATIONDATE, false);
    config.setMaxResults(1);

    if (config.list() != null && config.list().size() > 0) {

      try {
        JSONObject contentObject = new JSONObject(content);

        String originalProcessId = (String) parameters.get("processId");

        String orderId = contentObject.getString("inpcOrderId");

        Order order = OBDal.getInstance().get(Order.class, orderId);
        String docStatus = order.getDocumentStatus();

        // VALIDACIÓN DE STOCK

        try {
          ValidateStock objValidateStock = new ValidateStock();

          Product product = objValidateStock.validateStock(order);

          if (product != null) {
            calculatePromotions(order);

            // Once the transactions are being fixed, the system needs to generate the new sequence
            if (!order.getBusinessPartner().isSscmbIsagreement()) {
              order = OBDal.getInstance().get(Order.class, orderId);
              String invoiceDocumentNo = getInvoiceDocumentNo(order);
              try {
                String validationCode = getValidationCode(order, invoiceDocumentNo);
                order.setEcsdsDocumentno(invoiceDocumentNo);
                order.setEcsdsCodigo(validationCode);
                OBDal.getInstance().save(order);
              } finally {
                try {
                  OBDal.getInstance().flush();
                  updateSequenceList(
                      getSequenceFromTerminal(getTerminalFromOrg(order.getOrganization())),
                      invoiceDocumentNo);
                } catch (Throwable ignored) {
                }
              }
            }

            if ("DR".equals(docStatus) && !order.getSscmbSalesOrigin().equals("ND")) {
              HashMap<String, String> printParams = new HashMap<String, String>();
              PrintProcessFromTemplate template = new PrintProcessFromTemplate();
              template.printProcess(originalProcessId, orderId, printParams);
            }

            order.setDocumentStatus("SAQB_LS");
            OBDal.getInstance().save(order);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();

            return createResult(MessageType.WARNING, "Alerta de stock",
                OBMessageUtils.messageBD("NotEnoughStocked") + " " + product.getName());
          }

        } catch (Exception e) {
          return createResult(MessageType.ERROR, null, e.getMessage());
        }

        // PROCESO BOM

        OwnBOMProcess bomProcess = new OwnBOMProcess();

        ArrayList<MaterialTransaction> transactionsList = new ArrayList<MaterialTransaction>();

        try {
          transactionsList = bomProcess.createProcessBOM(order);
        } catch (Exception e) {
          return createResult(MessageType.ERROR, null, e.getMessage());
        }

        // COMPLETAR PEDIDO

        OBContext.setAdminMode(true);
        Process process = null;
        try {
          process = OBDal.getInstance().get(Process.class, "9F809A2A1634406DB59DEC4297301D89");
        } finally {
          OBContext.restorePreviousMode();
        }

        HashMap<String, String> processParams = new HashMap<String, String>();
        HashMap<String, String> printParams = new HashMap<String, String>();
        OBDal.getInstance().flush();
        SessionHandler.getInstance().commitAndStart();
        final ProcessInstance pInstance = CallProcess.getInstance().call(process, orderId,
            processParams);

        if (pInstance.getResult() != 0L) {
          // Update the documentno sequence.
          // SessionHandler.getInstance().commitAndStart();
          order = OBDal.getInstance().get(Order.class, orderId);
          if (!order.getBusinessPartner().isSscmbIsagreement()) {

            // order = OBDal.getInstance().get(Order.class, orderId);
            String invoiceDocumentNo = getInvoiceDocumentNo(order);

            // TriggerHandler.getInstance().disable();
            // try {
            Invoice invoice = getInvoice(order);
            String validationCode = getValidationCode(order, invoiceDocumentNo);
            TriggerHandler.getInstance().disable();
            try {
              if (invoice != null && (invoiceDocumentNo != null && !"".equals(invoiceDocumentNo))) {
                invoice.setDocumentNo(invoiceDocumentNo);
                invoice.setEeiCodigo(validationCode);
                OBDal.getInstance().save(invoice);
                printParams.put("invoiceId", invoice.getId());
              }

              order.setEcsdsDocumentno(invoiceDocumentNo);
              order.setEcsdsCodigo(validationCode);
              OBDal.getInstance().save(order);
            } finally {
              try {
                OBDal.getInstance().flush();
                TriggerHandler.getInstance().enable();
                if (invoiceDocumentNo != null && !"".equals(invoiceDocumentNo)) {
                  updateSequenceList(
                      getSequenceFromTerminal(getTerminalFromOrg(order.getOrganization())),
                      invoiceDocumentNo);
                }
              } catch (Throwable ignored) {
              } finally {
                if (TriggerHandler.getInstance().isDisabled()) {
                  TriggerHandler.getInstance().enable();
                }
              }
            }

          }

          if ("DR".equals(docStatus)) {
            PrintProcessFromTemplate template = new PrintProcessFromTemplate();
            template.printProcess(originalProcessId, orderId, printParams);
          }

          // ENVIO DE LA INFORMACION A SMARTDELIVERY
          SmartDeliveryAPI smartDelivery = new SmartDeliveryAPI();
          JSONObject result = new JSONObject();
          result.put("orderIdOB", order.getId());
          smartDelivery.consumeSmartClientWS(result);

          return createResult(MessageType.SUCCESS, "Éxito",
              OBMessageUtils.getI18NMessage("SSRCM_process_success", null));
        } else {
          String msg = pInstance.getErrorMsg();

          if (msg.startsWith("@ERROR=")) {
            msg = msg.substring(7);
            // msg = msg.replaceAll("@", "");
          }
          // tirar para atras las transacciones
          revertTransactions(transactionsList);
          // return createResult(MessageType.ERROR, OBMessageUtils.getI18NMessage(msg, null));
          String message = OBMessageUtils.parseTranslation(msg);

          return createResult(MessageType.ERROR, null, message);
        }

      } catch (JSONException e) {
        log.error(e.getMessage());
        e.printStackTrace();
        return createResult(MessageType.ERROR, null, e.getMessage());
      }

    } else {
      // NO HAY LINEAS EN LA VENTANA DE CONFIGURACION DE SMARTDELIVERY
      String notCOfig = "La URL, Usuario, Password o Cuenta del WEB SERVICE no estan configurados en la ventana de Configuracion SmartDelivery.";
      return createResult(MessageType.ERROR, null, notCOfig);
    }

  }

  private void calculatePromotions(Order order) {
    List<Object> parameters = new ArrayList<Object>();
    parameters.add("O");
    parameters.add(order.getId());
    parameters.add(OBContext.getOBContext().getUser().getId());
    CallStoredProcedure.getInstance().call("M_PROMOTION_CALCULATE", parameters, null, true, false);
  }

  private String getInvoiceDocumentNo(Order order) {
    String invoiceDocumentNo = order.getEcsdsDocumentno();
    if (invoiceDocumentNo == null || "".equals(invoiceDocumentNo)) {
      invoiceDocumentNo = getNextDocno(order.getOrganization());

    }
    return invoiceDocumentNo;
  }

  private Invoice getInvoice(Order order) {
    Invoice invoice = null;
    // The order has an invoice associated so it is needed to modify the invoice document

    OBCriteria<Invoice> invoiceCri = OBDal.getInstance().createCriteria(Invoice.class);
    invoiceCri.add(Restrictions.eq(Invoice.PROPERTY_SALESORDER, order));
    if (invoiceCri.count() == 0) {
      List<OrderLine> orderLineList = order.getOrderLineList();
      if (orderLineList.size() > 0) {
        for (OrderLine orderLine : orderLineList) {
          OBCriteria<InvoiceLine> invoiceLineCri = OBDal.getInstance()
              .createCriteria(InvoiceLine.class);
          invoiceLineCri.add(Restrictions.eq(InvoiceLine.PROPERTY_SALESORDERLINE, orderLine));
          if (invoiceLineCri.count() > 0) {
            invoice = invoiceLineCri.list().get(0).getInvoice();
            break;
          }
        }
      }
    } else {
      invoice = invoiceCri.list().get(0);
    }

    return invoice;
  }

  synchronized static public void updateSequenceList(PointOfSaleSeq seq, String documentNo) {

    try {
      String secuenceToUpdate = documentNo.replaceAll("-", "");
      secuenceToUpdate = secuenceToUpdate.replaceFirst(seq.getStore(), "");

      OBContext.setAdminMode(false);
      OBCriteria<PoinOfSaleSequenceLine> criteriaSeqLines = OBDal.getInstance()
          .createCriteria(PoinOfSaleSequenceLine.class);

      criteriaSeqLines.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_ECSDSPSALESEQ, seq));
      criteriaSeqLines.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_SEQUENCE,
          Long.parseLong(secuenceToUpdate)));

      PoinOfSaleSequenceLine seqLine = (PoinOfSaleSequenceLine) criteriaSeqLines.uniqueResult();
      seqLine.setUsed(true);
      OBDal.getInstance().save(seqLine);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private OBPOSApplications getTerminalFromOrg(Organization org) {
    OBPOSApplications terminal = null;
    OBCriteria<SaqbCallcenterinvoiceConf> confCriteria = OBDal.getInstance()
        .createCriteria(SaqbCallcenterinvoiceConf.class);
    confCriteria.add(Restrictions.eq(SaqbCallcenterinvoiceConf.PROPERTY_ORGANIZATION, org));

    if (confCriteria.count() > 0) {
      SaqbCallcenterinvoiceConf conf = confCriteria.list().get(0);
      terminal = conf.getOBPOSPOSTerminal();
    }
    return terminal;
  }

  private PointOfSaleSeq getSequenceFromTerminal(OBPOSApplications terminal) {
    List<PointOfSaleSeq> sequenceList = terminal.getECSDSPointOfSaleSeqList();

    for (PointOfSaleSeq sequence : sequenceList) {
      if (sequence.isInvoiceSeq()) {
        return sequence;
      }
    }
    return null;
  }

  private String getNextDocno(Organization org) {
    String invoiceDocumentNo = "";

    OBPOSApplications terminal = getTerminalFromOrg(org);

    if (terminal != null) {

      PointOfSaleSeq sequence = getSequenceFromTerminal(terminal);

      if (sequence != null) {
        OBCriteria<PoinOfSaleSequenceLine> seqLineCri = OBDal.getInstance()
            .createCriteria(PoinOfSaleSequenceLine.class);
        seqLineCri.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_ECSDSPSALESEQ, sequence));
        seqLineCri.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_USED, false));
        seqLineCri.addOrderBy(PoinOfSaleSequenceLine.PROPERTY_SEQUENCE, true);
        List<PoinOfSaleSequenceLine> seqLineList = seqLineCri.list();

        if (seqLineList.size() > 0) {
          invoiceDocumentNo = seqLineList.get(0).getSequence().toString();

          // Add the store and add -
          invoiceDocumentNo = sequence.getStore() + invoiceDocumentNo;

          String newKey = "";
          for (int i = 0; i < invoiceDocumentNo.length(); i++) {
            newKey = ((i % 3) == 0 && i != 0) ? newKey + "-" + invoiceDocumentNo.substring(i, i + 1)
                : newKey + invoiceDocumentNo.substring(i, i + 1);
          }
          if (!"".equals(newKey)) {
            invoiceDocumentNo = newKey;
          }
        }

      }

    }

    return invoiceDocumentNo;
  }

  private String getValidationCode(Order order, String invoiceDocNumber) {
    String validationCode = order.getEcsdsCodigo();
    if (validationCode != null && !"".equals(validationCode)) {
      return validationCode;
    }
    String date = formatDate(new Date()); // Field 1
    PointOfSaleSeq sequence = getSequenceFromTerminal(getTerminalFromOrg(order.getOrganization()));
    String identifier = "--";// identifier Field 2
    if (sequence != null && sequence.getEcsdsComproType() != null) {
      identifier = sequence.getEcsdsComproType().getIdentifier();
      if (identifier != null && !"".equals(identifier)) {
        identifier = identifier.substring(0, 2);
      } else {
        identifier = "--";
      }
    }
    String rucNumber = StringUtils.lpad(getRUCFromLegalWithAccountingOrg(order.getOrganization()),
        '0', 13);// Field 3
    String tipoDeAmbiente = order.getOrganization().isEcsdsIsdevelopment() ? "1" : "2"; // Field 4
    String serie = StringUtils.lpad(
        getSequenceFromTerminal(getTerminalFromOrg(order.getOrganization())).getStore(), '0', 6); // Campo
                                                                                                  // 5
    String numComproSec = StringUtils.lpad(invoiceDocNumber.replace("-", ""), '0', 9); // Campo 6
    Random r = new Random();
    String random8Digit = new Integer(r.nextInt((90000000 - 10000000) + 1) + 10000000).toString(); // Field
                                                                                                   // 7
    /*
     * String random8Digit = StringUtils.lpad(new Double( Math.floor(Math.random() * 90000000) +
     * 10000000).toString(), '0', 8);
     */
    String tipoDeEmision = "1"; // Field 8
    String verificationCode = getVerificationCode(date + identifier + rucNumber + tipoDeAmbiente
        + serie + numComproSec + random8Digit + tipoDeEmision); // Field 9

    if ("-1".equals(verificationCode)) {
      return verificationCode;
    }

    return date + identifier + rucNumber + tipoDeAmbiente + serie + numComproSec + random8Digit
        + tipoDeEmision + verificationCode;
  }

  private String getVerificationCode(String key) {
    int mod11, result, total = 0;

    if (!key.matches("^\\d{48}$")) {
      return "-1";
    }

    int weight = 2;
    for (int i = key.length() - 1; i >= 0; i--) {
      total = total + (new Integer(key.charAt(i)) * weight);
      if (weight == 7) {
        weight = 2;
      } else {
        weight++;
      }

      mod11 = 11 - (total % 11);
      switch (mod11) {
      case 11:
        result = 0;
        break;
      case 10:
        result = 1;
        break;
      default:
        result = mod11;
        break;
      }
      return new Integer(result).toString();
    }
    return "-1";

  }

  private String formatDate(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    String month = new Integer(calendar.get(Calendar.MONTH) + 1).toString();
    String day = new Integer(calendar.get(Calendar.DAY_OF_MONTH)).toString();
    String year = new Integer(calendar.get(Calendar.YEAR)).toString();

    if (month.length() < 2)
      month = '0' + month;
    if (day.length() < 2)
      day = '0' + day;

    return day + month + year;
  }

  private static String getRUCFromLegalWithAccountingOrg(Organization orgRegion) {

    if (orgRegion.getOrganizationType().isLegalEntityWithAccounting()) {
      return orgRegion.getOrganizationInformationList().get(0).getTaxID();
    } else {
      return getRUCFromLegalWithAccountingOrg(getParentOfOrg(orgRegion));
    }
  }

  private static Organization getParentOfOrg(Organization orgRegion) {

    OrganizationTree orgTree;
    try {
      OBContext.setAdminMode(false);
      OBCriteria<OrganizationTree> orgTreeCriteria = OBDal.getInstance()
          .createCriteria(OrganizationTree.class);
      orgTreeCriteria.add(Restrictions.eq(OrganizationTree.PROPERTY_ORGANIZATION, orgRegion));
      orgTreeCriteria.add(Restrictions.gt(OrganizationTree.PROPERTY_LEVELNO, 1L));
      orgTreeCriteria.addOrderBy(OrganizationTree.PROPERTY_LEVELNO, true);
      orgTreeCriteria.setMaxResults(1);
      orgTree = (OrganizationTree) orgTreeCriteria.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
    return orgTree.getParentOrganization();
  }

  private JSONObject createResult(MessageType messageType, String strMessageTitle, String text) {
    return getResponseBuilder().showMsgInProcessView(messageType,
        (strMessageTitle == null ? "Error" : strMessageTitle), text).refreshGrid().build();
  }

  private void revertTransactions(ArrayList<MaterialTransaction> transactionsList) {
    for (MaterialTransaction transaction : transactionsList) {
      MaterialTransaction materialTransaction = OBProvider.getInstance()
          .get(MaterialTransaction.class);
      materialTransaction.setOrganization(transaction.getOrganization());
      materialTransaction.setProductionLine(transaction.getProductionLine());
      materialTransaction.setStorageBin(transaction.getStorageBin());
      materialTransaction.setMovementType("P+");
      materialTransaction.setProduct(transaction.getProduct());
      materialTransaction.setMovementDate(transaction.getMovementDate());
      materialTransaction.setMovementQuantity(transaction.getMovementQuantity().negate());
      materialTransaction.setUOM(transaction.getUOM());
      materialTransaction.setOrderUOM(transaction.getOrderUOM());
      materialTransaction.setOrderQuantity(transaction.getOrderQuantity());
      materialTransaction.setAttributeSetValue(transaction.getAttributeSetValue());
      OBDal.getInstance().save(materialTransaction);

    }
  }
}

