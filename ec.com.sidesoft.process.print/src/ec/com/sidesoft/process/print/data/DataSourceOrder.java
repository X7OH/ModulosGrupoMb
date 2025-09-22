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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package ec.com.sidesoft.process.print.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderLineOffer;
import org.openbravo.model.common.order.OrderTax;
import org.openbravo.retail.discounts.combo.ComboProduct;
import org.openbravo.retail.discounts.combo.ComboProductFamily;

import ec.com.sidesoft.process.print.model.BusinessPartner;
import ec.com.sidesoft.process.print.model.Currency;
import ec.com.sidesoft.process.print.model.Payment;
import ec.com.sidesoft.process.print.model.Product;
import ec.com.sidesoft.process.print.model.Promotion;
import ec.com.sidesoft.process.print.model.Tax;
import ec.com.sidesoft.process.print.model.User;
import ec.com.sidesoft.quickbilling.advanced.SaqbCallcenterinvoiceConf;
import ec.com.sidesoft.quickbilling.advanced.payments.SaqbpPaymentMethods;

public class DataSourceOrder extends DataSource {
  private static final Logger log = Logger.getLogger(DataSourceOrder.class);

  private String objectId;
  private List<String> attributes;
  private List<String> initilize;
  private List<String> finalParams;
  private String params;
  private String docType;
  private Order order;
  private Invoice invoice;

  public DataSourceOrder(String processId, String id, HashMap<String, String> printParams) {
    objectId = id;
    attributes = new ArrayList<String>();
    initilize = new ArrayList<String>();
    finalParams = new ArrayList<String>();
    order = OBDal.getInstance().get(Order.class, objectId);
    if (printParams.containsKey("invoiceId")) {
      invoice = OBDal.getInstance().get(Invoice.class, printParams.get("invoiceId"));
    }
    getTemplatesFromDb(processId, order.getTransactionDocument(), order.getOrganization());
  }

  @Override
  public void doGet() {

    try {
      OBContext.setAdminMode(true);

      // Load document
      ec.com.sidesoft.process.print.model.Order orderModel = new ec.com.sidesoft.process.print.model.Order();

      orderModel.setArgument("id", order.getId());
      orderModel.setArgument("erp", "Y");
      orderModel.setArgument("description",
          order.getDescription() != null ? order.getDescription().replace("\n", "aldatzeko") : "");
      orderModel.setArgument("addressDesc", order.getSwsocHomeaddress());
      orderModel.setArgument("organization", order.getOrganization().getId());
      orderModel.setArgument("orderDate", new Long(order.getOrderDate().getTime()).toString());
      orderModel.setArgument("documentNo", order.getDocumentNo());
      if (order.isPriceIncludesTax()) {
        orderModel.setArgument("priceIncludeTax", order.isPriceIncludesTax().toString());
      }
      BusinessPartner bp = new BusinessPartner();
      bp.setArgument("id", order.getBusinessPartner().getId());
      bp.setArgument("identifier", order.getBusinessPartner().getIdentifier());
      bp.setArgument("taxID", order.getBusinessPartner().getTaxID());
      orderModel.setArgument("bp", bp.getArgumentsJS());

      Currency cu = new Currency();
      cu.setArgument("id", order.getCurrency().getId());
      cu.setArgument("identifier", order.getCurrency().getIdentifier());
      orderModel.setArgument("currency", cu.getArgumentsJS());

      orderModel.setArgument("currencyidentifier", order.getCurrency().getIdentifier());

      // Invoice related to order
      if (invoice != null) {
        orderModel.setArgument("invoiceDocumentNo", invoice.getDocumentNo());
        orderModel.setArgument("validationCode", invoice.getEeiCodigo());

      } else {
        orderModel.setArgument("invoiceDocumentNo", order.getEcsdsDocumentno());
        orderModel.setArgument("validationCode", order.getEcsdsCodigo());
      }

      // Lines of the order
      List<String> orderLinesModel = new ArrayList<String>();

      for (OrderLine orderLine : order.getOrderLineList()) {
        if (orderLine.getUnitPrice().compareTo(BigDecimal.ZERO) != 0) {
          ec.com.sidesoft.process.print.model.OrderLine orderLineModel = new ec.com.sidesoft.process.print.model.OrderLine();
          Product product = new Product();
          product.setArgument("prodid", orderLine.getProduct().getId());
          product.setArgument("prodidentifier", orderLine.getProduct().getIdentifier());
          product.setArgument("prodlistPrice", orderLine.getListPrice().toString());

          orderLineModel.setArgument("productidentifier", orderLine.getProduct().getIdentifier());
          orderLineModel.setArgument("id", orderLine.getId());
          orderLineModel.setArgument("product", product.getArgumentsJS());

          orderLineModel.setArgument("qty", orderLine.getOrderedQuantity().toString());
          orderLineModel.setArgument("linerate", orderLine.getOrderLineTaxList().get(0).getTax()
              .getRate().divide(new BigDecimal(100)).toString());

          List<String> promotions = new ArrayList<String>();
          // This is a field stored in the combo-product-family. To get it, we need to filter by
          // the
          // combo and in the family, get the product with the value
          boolean isExtraSupplement = false;
          String comboId = "";
          for (OrderLineOffer orderLineOffer : orderLine.getOrderLineOfferList()) {
            Promotion promotion = new Promotion();
            promotion.setArgument("discountType",
                orderLineOffer.getPriceAdjustment().getDiscountType().getId());
            promotion.setArgument("amt", orderLineOffer.getPriceAdjustmentAmt().toString());
            promotion.setArgument("name", orderLineOffer.getPriceAdjustment().getName());
            promotion.setArgument("chunks",
                orderLineOffer.getObdiscQtyoffer() != null
                    ? orderLineOffer.getObdiscQtyoffer().toString()
                    : "1");
            // Hidden means amount of the discount is 0.
            promotion.setArgument("hidden",
                new Boolean(orderLineOffer.getTotalAmount().equals(BigDecimal.ZERO)).toString());
            // Chunks means the number of combos that are included in the lines.
            promotions.add(promotion.getArgumentsJS());

            // To know if this is a combo, we get families
            if (orderLineOffer.getPriceAdjustment().getOBCOMBOFamilyList() != null
                && orderLineOffer.getPriceAdjustment().getOBCOMBOFamilyList().size() > 0) {
              for (ComboProductFamily comboProductFamily : orderLineOffer.getPriceAdjustment()
                  .getOBCOMBOFamilyList()) {
                for (ComboProduct comboProduct : comboProductFamily.getOBCOMBOProductList()) {
                  if (comboProduct.getProduct() != null
                      && comboProduct.getProduct().getId().equals(orderLine.getProduct().getId())) {
                    isExtraSupplement = comboProduct.isSsrcmExtraSuplement();
                  }
                }
              }
            }

            // Add the comboId if it is a combo.
            if ("71895FA82C9645949CB752564FB1389D"
                .equals(orderLineOffer.getPriceAdjustment().getDiscountType().getId())
                || "7899A7A4204749AD92881133C4EE7A57"
                    .equals(orderLineOffer.getPriceAdjustment().getDiscountType().getId())) {
              comboId = orderLineOffer.getPriceAdjustment().getId();
            }
          }
          if (!"".equals(comboId)) {
            orderLineModel.setArgument("comboId", comboId);
          }
          orderLineModel.setArgument("promotions", promotions.toString());
          if (isExtraSupplement) {
            orderLineModel.setArgument("isExtraSupplement",
                new Boolean(isExtraSupplement).toString());
          }
          // if (order.isPriceIncludesTax()) {
          orderLineModel.setArgument("gross", orderLine.getLineGrossAmount().toString());
          orderLineModel.setArgument("net", orderLine.getLineNetAmount().toString());
          // } else
          // orderLineModel.setArgument("net", orderLine.getLineNetAmount().toString());

          orderLineModel.setArgument("description",
              orderLine.getDescription() != null ? orderLine.getDescription().replace("\n", " ")
                  : "");
          orderLineModel.setArgument("price", orderLine.getUnitPrice().toString());
          // COMBOID????
          orderLinesModel.add(orderLineModel.getArgumentsJS());
        }
      }
      orderModel.setArgument("lines", orderLinesModel.toString());

      List<String> taxes = new ArrayList<String>();

      for (OrderTax orderTax : order.getOrderTaxList()) {
        Tax tax = new Tax();
        tax.setArgument("net", orderTax.getTaxableAmount().setScale(2, BigDecimal.ROUND_HALF_EVEN));
        tax.setArgument("amount", orderTax.getTaxAmount());
        taxes.add(tax.getArgumentsJS());
      }
      orderModel.setArgument("taxes", taxes.toString());

      List<String> payments = new ArrayList<String>();

      // Get the payments from the call center order
      if (order.getSaqbOrder() != null) {
        for (SaqbpPaymentMethods paymentCall : order.getSaqbOrder().getSaqbpPaymentMethodsList()) {
          Payment payment = new Payment();
          payment.setArgument("name", paymentCall.getPaymentMethod().getName());
          payment.setArgument("rate", "1");
          payment.setArgument("amount", paymentCall.getAmount().toString());
          payment.setArgument("origAmount", paymentCall.getAmount().toString());
          payment.setArgument("isoCode", order.getCurrency().getISOCode());
          payments.add(payment.getArgumentsJS());
        }
      }

      orderModel.setArgument("payments", payments.toString());

      orderModel.setArgument("net", order.getSummedLineAmount().toString());
      orderModel.setArgument("gross", order.getGrandTotalAmount().toString());

      // Change is not already implemented in ERP
      orderModel.setArgument("change", BigDecimal.ZERO.toString());

      // posTerminal$_identifier is got from call center configuration
      OBCriteria<SaqbCallcenterinvoiceConf> confCriteria = OBDal.getInstance()
          .createCriteria(SaqbCallcenterinvoiceConf.class);
      confCriteria.add(Restrictions.eq(SaqbCallcenterinvoiceConf.PROPERTY_ORGANIZATION,
          order.getOrganization()));

      if (confCriteria.count() > 0) {
        SaqbCallcenterinvoiceConf conf = confCriteria.list().get(0);
        orderModel.setArgument("posTerminal", conf.getOBPOSPOSTerminal().getIdentifier());
      }

      User user = new User();
      user.setArgument("user", OBContext.getOBContext().getUser().getName());
      user.setArgument("org", OBContext.getOBContext().getCurrentOrganization().getName());
      orderModel.setArgument("user", user.getArgumentsJS());

      String attribute = "var attributes = " + orderModel.getArgumentsJS() + ";";
      String orderJS = "var order = new Order(attributes);";
      String attributeUser = "var attributeUser = " + user.getArgumentsJS() + ";";
      String userJS = "var user = " + user.getArgumentsJS() + ";";

      attributes.add(attribute);
      attributes.add(attributeUser);

      initilize.add(orderJS);
      initilize.add(userJS);

      String finalParam = "OB.Model = {};\n";
      finalParam += "OB.Model.OrderLine = order.get('lines').model;\n";

      finalParams.add(finalParam);
      params = "{order:order, user: user}";
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public List<String> getAttributes() {
    return attributes;
  }

  public List<String> getInitialization() {
    return initilize;
  }

  public List<String> getFinalParams() {
    return finalParams;
  }

  public String getParams() {
    return params;
  }

  public String getDocType() {
    return docType;
  }

  public String parseDate(Date date) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sssZ");
    return simpleDateFormat.format(date);
  }

  public void sendDocument(String document) throws IOException {

    OBCriteria<SaqbCallcenterinvoiceConf> confCriteria = OBDal.getInstance()
        .createCriteria(SaqbCallcenterinvoiceConf.class);
    confCriteria.add(
        Restrictions.eq(SaqbCallcenterinvoiceConf.PROPERTY_ORGANIZATION, order.getOrganization()));

    if (confCriteria.count() > 0) {
      SaqbCallcenterinvoiceConf conf = confCriteria.list().get(0);
      String urlStr = conf.getOBPOSPOSTerminal().getHardwareurl();
      final URL url = new URL(urlStr);
      final URLConnection urlConnection = url.openConnection();
      urlConnection.setDoOutput(true);
      urlConnection.setRequestProperty("Content-Type", "application/xml; charset=utf-8");
      urlConnection.connect();
      final OutputStream outputStream = urlConnection.getOutputStream();
      outputStream.write(document.getBytes("UTF-8"));
      outputStream.flush();
      final InputStream inputStream = urlConnection.getInputStream();

      /*
       * url: sendurl + '/printer', cacheBust: false, method: 'POST', handleAs: 'json', timeout:
       * 20000, contentType: 'application/xml;charset=utf-8', data: data,
       */
    }
  }
}
