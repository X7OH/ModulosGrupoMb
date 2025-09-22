package ec.com.sidesoft.backend.giftcard.ad_process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardSummary;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.ProductSummary;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.backend.giftcard.data.BKGCConfig;
import ec.com.sidesoft.backend.giftcard.data.BKGCParamgc;

public class GenerateGC extends DalBaseProcess {
  private static final Logger logger = Logger.getLogger(GenerateGC.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    OBError myMessage = new OBError();
    final String strIdParamagc = (String) bundle.getParams().get("Bkgc_Paramgc_ID");
    BKGCParamgc bkgcParamgc = OBDal.getInstance().get(BKGCParamgc.class, strIdParamagc);
    BKGCConfig config = getConfig(bkgcParamgc.getOrganization());
    if (config == null) {
      myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
          "BKGC_GenerateGC_Error", OBContext.getOBContext().getLanguage().getLanguage())));
      myMessage.setType("Error");
      myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
          OBContext.getOBContext().getLanguage().getLanguage()));
      bundle.setResult(myMessage);
      return;
    }

    if (bkgcParamgc.getStatus().equals("BR")) {
      Long from = bkgcParamgc.getSeriefrom();
      Long to = bkgcParamgc.getSerieto() + 1;
      Product product = bkgcParamgc.getProduct();
      BigDecimal amountGC = null;
      if (bkgcParamgc.getAmount() == null || bkgcParamgc.getAmount().intValue() == 0) {
        if (product.getGcnvAmount() == null || product.getGcnvAmount().intValue() == 0) {
          myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
              "BKGC_GenerateGC_Error", OBContext.getOBContext().getLanguage().getLanguage())));
          myMessage.setType("Error");
          myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
              OBContext.getOBContext().getLanguage().getLanguage()));
          bundle.setResult(myMessage);
          return;
        } else {
          amountGC = product.getGcnvAmount();
        }
      } else {
        amountGC = bkgcParamgc.getAmount();
      }

      Long fromValidate = bkgcParamgc.getSeriefrom();
      Long toValidate = bkgcParamgc.getSerieto();
      int retval = toValidate.compareTo(fromValidate);

      // VALIDO QUE NUMERO HASTA NO SEA MENOR QUE NUMERO DESDE
      if (retval < 0) {
        myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
            "BKGC_GenerateGC_Error4", OBContext.getOBContext().getLanguage().getLanguage())));
        myMessage.setType("Error");
        myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
            OBContext.getOBContext().getLanguage().getLanguage()));
        bundle.setResult(myMessage);
        return;
      }

      // VALIDO EL TERCERO
      if (bkgcParamgc.getBpartner() == null) {
        myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
            "BKGC_GenerateGC_Error5", OBContext.getOBContext().getLanguage().getLanguage())));
        myMessage.setType("Error");
        myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
            OBContext.getOBContext().getLanguage().getLanguage()));
        bundle.setResult(myMessage);
        return;
      }

      // INICIO VALIDO NUMERO DE TARJETAS CON EL MISMO NUMERO
      String secuencia = "";
      Long fromSecuencia = from;
      Long toSecuencia = to;

      String[] strSequence = new String[200000];
      int intAcumSeq = 0;

      while (fromSecuencia.compareTo(toSecuencia) != 0) {

        strSequence[intAcumSeq] = fromSecuencia.toString();
        intAcumSeq++;

        secuencia = secuencia + "'" + fromSecuencia + "',";
        fromSecuencia++;
      }

      if (secuencia.endsWith(",")) {
        secuencia = secuencia.substring(0, secuencia.length() - 1);
      }
      String[] strObjSequences = verifyDuplicitySequence(secuencia, strSequence, intAcumSeq); // CC:
                                                                                              // Actividad
                                                                                              // #8465

      // Integer verify = verifyDuplicity(secuencia); //Comentado por Actividad #8465

      Integer verify = strObjSequences == null ? 0 : strObjSequences.length; // CC: Actividad #8465

      if (verify == 0) {

        // CC: Actividad #8465
        myMessage.setMessage(String
            .format(Utility.messageBD(new DalConnectionProvider(), "BKGC_GenerateGC_ErrorDuplicate",
                OBContext.getOBContext().getLanguage().getLanguage()))
            + "<br> No. de Secuencias Activas: " + (secuencia));
        myMessage.setType("Error");
        myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
            OBContext.getOBContext().getLanguage().getLanguage()));
        bundle.setResult(myMessage);
        return;
      }
      // FIN VALIDO NUMERO DE TARJETAS CON EL MISMO NUMERO

      boolean flag = true;

      // Comentado por Actividad #8465
      /*
       * while (from.compareTo(to) != 0) { Order order = insertOrderGC(bkgcParamgc, amountGC,
       * config); OrderLine orderLine = order.getOrderLineList().get(0); flag =
       * insertGC(from.toString(), bkgcParamgc, order, orderLine, amountGC); if (!flag) break;
       * 
       * from++; }
       */

      // CC: Actividad #8465
      for (int seq = 0; seq < intAcumSeq; seq++) {

        String strFrom = strObjSequences[seq] != null
            ? (strObjSequences[seq].toString().equals("") ? "ND" : strObjSequences[seq].toString())
            : "ND";
        if (!strFrom.equals("ND")) {
          Order order = insertOrderGC(bkgcParamgc, amountGC, config);

          OrderLine orderLine = order.getOrderLineList().get(0);
          flag = insertGC(strFrom, bkgcParamgc, order, orderLine, amountGC);
          if (!flag)
            break;
        }

      }

      if (flag) {
        bkgcParamgc.setStatus("PR");
        myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
            "BKGC_GenerateGC_Exito", OBContext.getOBContext().getLanguage().getLanguage())));
        myMessage.setType("Success");
        myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Success",
            OBContext.getOBContext().getLanguage().getLanguage()));
        bundle.setResult(myMessage);
      } else {
        OBDal.getInstance().rollbackAndClose();
        myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
            "BKGC_GenerateGC_Error2", OBContext.getOBContext().getLanguage().getLanguage())));
        myMessage.setType("Error");
        myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
            OBContext.getOBContext().getLanguage().getLanguage()));
        bundle.setResult(myMessage);
      }
    } else {
      OBDal.getInstance().rollbackAndClose();
      myMessage.setMessage(String.format(Utility.messageBD(new DalConnectionProvider(),
          "BKGC_GenerateGC_Error3", OBContext.getOBContext().getLanguage().getLanguage())));
      myMessage.setType("Error");
      myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error",
          OBContext.getOBContext().getLanguage().getLanguage()));
      bundle.setResult(myMessage);
    }

  }

  private boolean insertGC(String giftcardid, BKGCParamgc bkgcParamgc, Order order,
      OrderLine orderLine, BigDecimal amountGC) {
    boolean response = true;
    OBContext.setAdminMode(false);
    OrganizationStructureProvider osp = new OrganizationStructureProvider();
    try {
      Product product = bkgcParamgc.getProduct();
      GiftCardInst giftcard = OBProvider.getInstance().get(GiftCardInst.class);
      giftcard.setClient(bkgcParamgc.getClient());
      giftcard.setOrganization(osp.getLegalEntity(bkgcParamgc.getOrganization()));
      giftcard.setActive(true);
      giftcard.setCreatedBy(bkgcParamgc.getCreatedBy());
      giftcard.setUpdatedBy(bkgcParamgc.getUpdatedBy());
      giftcard.setSearchKey(
          giftcardid == null ? UUID.randomUUID().toString().replace("-", "").toUpperCase()
              : giftcardid);
      giftcard.setProduct(product);
      giftcard.setBusinessPartner(bkgcParamgc.getBpartner());
      giftcard.setObgcneGCOwner(bkgcParamgc.getBpartner());
      giftcard.setOrderDate(bkgcParamgc.getCreationDate());
      giftcard.setSalesOrder(order);
      giftcard.setSalesOrderLine(orderLine);
      giftcard.setAlertStatus("N");
      giftcard.setObgcneExpirationdate(bkgcParamgc.getDateexpgc());
      giftcard.setBkgcBeneficiary(bkgcParamgc.getBeneficiary());

      if ("G".equals(product.getGcnvGiftcardtype())) {
        BigDecimal amount = amountGC;
        giftcard.setAmount(amount);
        giftcard.setCurrentamount(amount);
        giftcard.setType("BasedOnProductGiftCard");
      } else if ("V".equals(product.getGcnvGiftcardtype())) {
        giftcard.setType("BasedOnVoucher");
        for (ProductSummary productSumm : product.getGCNVProductSummaryList()) {
          GiftCardSummary gcSumm = OBProvider.getInstance().get(GiftCardSummary.class);
          giftcard.getGCNVGiftCardSummaryList().add(gcSumm);
          gcSumm.setGcnvGiftcardInst(giftcard);
          gcSumm.setOrganization(OBContext.getOBContext().getCurrentOrganization());
          gcSumm.setActive(true);
          gcSumm.setProduct(productSumm.getIncproduct());
          gcSumm.setQuantity(productSumm.getQuantity());
          gcSumm.setCurrentquantity(productSumm.getQuantity());
        }
      }

      OBDal.getInstance().save(giftcard);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      logger.error("Error al procesar transaccion" + e.getMessage(), e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }

    return response;
  }

  private Order insertOrderGC(BKGCParamgc bkgcParamgc, BigDecimal amount, BKGCConfig config)
      throws Exception {
    // DATOS CABECERA PEDIDO GC
    DocumentType docType = config.getDoctype();
    BusinessPartner bPartner = bkgcParamgc.getBpartner();
    PriceList priceList = config.getPricelist();
    Warehouse warehouse = config.getWarehouse();
    Order ordOB = OBProvider.getInstance().get(Order.class);

    TriggerHandler.getInstance().disable();
    try {

      ordOB.setActive(true);
      ordOB.setSalesTransaction(true);
      ordOB.setClient(bkgcParamgc.getClient());
      ordOB.setOrganization(bkgcParamgc.getOrganization());
      ordOB.setCreatedBy(bkgcParamgc.getCreatedBy());
      ordOB.setUpdatedBy(bkgcParamgc.getUpdatedBy());
      ordOB.setDocumentType(docType);
      ordOB.setTransactionDocument(docType);
      ordOB.setOrderDate(bkgcParamgc.getCreationDate());
      ordOB.setAccountingDate(bkgcParamgc.getCreationDate());
      String documentNo = Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
          new DalConnectionProvider(), RequestContext.get().getVariablesSecureApp(), "",
          Order.TABLE_NAME, docType.getId(), docType.getId(), false, true);
      ordOB.setDocumentNo(documentNo);
      ordOB.setDocumentStatus("CO");
      ordOB.setDocumentAction("--");
      ordOB.setProcessed(true);
      ordOB.setProcessNow(false);
      ordOB.setBusinessPartner(bPartner);
      ordOB.setPaymentMethod(bPartner.getPaymentMethod());
      ordOB.setDescription("PEDIDO VENTA GENERADO AUTOMATICAMENTE PARA LA CREACION DE GIF CARD");
      Location bpAddress = getBpAddress(bPartner.getBusinessPartnerLocationList());
      ordOB.setPartnerAddress(bpAddress);
      ordOB.setInvoiceAddress(bpAddress);
      ordOB.setCurrency(bPartner.getCurrency());
      ordOB.setPaymentTerms(bPartner.getPaymentTerms());
      ordOB.setPriceList(priceList);
      ordOB.setWarehouse(warehouse);
      OBDal.getInstance().save(ordOB);

      // DATOS LINEAS DEL PEDIDO
      Product product = bkgcParamgc.getProduct();
      TaxRate taxRate = config.getTax();
      OrderLine ordLineOB = OBProvider.getInstance().get(OrderLine.class);
      ordLineOB.setClient(bkgcParamgc.getClient());
      ordLineOB.setOrganization(bkgcParamgc.getOrganization());
      ordLineOB.setActive(true);
      ordLineOB.setCreatedBy(bkgcParamgc.getCreatedBy());
      ordLineOB.setUpdatedBy(bkgcParamgc.getUpdatedBy());
      ordLineOB.setSalesOrder(ordOB);
      ordLineOB.setLineNo(new Long(10));
      ordLineOB.setOrderDate(ordOB.getOrderDate());
      ordLineOB.setProduct(product);
      ordLineOB.setListPrice(amount);
      ordLineOB.setUnitPrice(amount);
      ordLineOB.setPriceLimit(amount);
      BigDecimal qty = new BigDecimal(1);
      ordLineOB.setOrderedQuantity(qty);
      ordLineOB.setUOM(product.getUOM());
      ordLineOB.setCurrency(ordOB.getCurrency());
      int stdPrecision = ordOB.getCurrency().getStandardPrecision().intValue();
      BigDecimal lineNetAmount = qty.multiply(amount).setScale(stdPrecision, RoundingMode.HALF_UP);
      ordLineOB.setLineNetAmount(lineNetAmount);
      ordOB.setSummedLineAmount(lineNetAmount);
      ordLineOB.setBusinessPartner(ordOB.getBusinessPartner());
      ordLineOB.setWarehouse(ordOB.getWarehouse());
      ordLineOB.setDescription("GIFT CARD");
      ordLineOB.setTax(taxRate);
      ordOB.getOrderLineList().add(ordLineOB);
      OBDal.getInstance().save(ordLineOB);
      if (TriggerHandler.getInstance().isDisabled()) {
        TriggerHandler.getInstance().enable();
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      throw new Exception("Error: " + e.getMessage());
    } finally {
      if (TriggerHandler.getInstance().isDisabled()) {
        TriggerHandler.getInstance().enable();
      }
    }

    return ordOB;

  }

  private Location getBpAddress(List<Location> addressBp) {
    Location billaddress = null;

    for (Location location : addressBp) {
      if (location.isInvoiceToAddress()) {
        billaddress = location;
      }
    }

    if (billaddress == null && addressBp.size() > 0) {
      billaddress = (Location) addressBp.get(0);
    }

    return billaddress;
  }

  private BKGCConfig getConfig(Organization org) {
    BKGCConfig configData = null;
    OBCriteria<BKGCConfig> crtCfg = OBDal.getInstance().createCriteria(BKGCConfig.class);
    crtCfg.add(Restrictions.eq(BKGCConfig.PROPERTY_ORGANIZATION, org));
    configData = (BKGCConfig) crtCfg.uniqueResult();

    return configData;
  }

  private static Integer verifyDuplicity(String values) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = "";
    Integer count = 0;

    try {

      String strSql = "SELECT count(*) AS contador FROM gcnv_giftcard_inst where value in(" + values
          + ") and status<>'CA';";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("contador");
      }

      count = Integer.parseInt(strResult);

      return count;

    } catch (Exception e) {
      throw new OBException(
          "Error al consultar los values de las tarjetas de regalo instanciadas . "
              + e.getMessage());
    }

  }

  private static String[] verifyDuplicitySequence(String values, String[] strObjSeq,
      int intTotalSeq) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult[] = new String[intTotalSeq];
    Integer count = 0;

    try {

      String strSql = "SELECT distinct value AS contador FROM gcnv_giftcard_inst where value in("
          + values + ") and (iscancelled = 'N' or status= 'N');";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult[count] = rsConsulta.getString("contador");
        count++;
      }
      if (count == intTotalSeq) {
        return null;
      }

      for (int i = 0; i < intTotalSeq; i++) {

        String strValueComp = strObjSeq[i].toString();

        for (int j = 0; j < count; j++) {

          String strValueComp2 = strResult[j].toString();
          if (strValueComp.equals(strValueComp2)) {
            strObjSeq[i] = "";
            break;
          }

        }

      }

      return strObjSeq;

    } catch (Exception e) {
      throw new OBException(
          "Error al consultar los values de las tarjetas de regalo instanciadas . "
              + e.getMessage());
    }

  }

}
