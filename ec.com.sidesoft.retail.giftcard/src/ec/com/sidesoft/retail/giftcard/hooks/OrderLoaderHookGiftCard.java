/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.retail.giftcard.hooks;

import java.util.Iterator;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import ec.com.sidesoft.retail.giftcard.GiftCardModel;
import org.openbravo.retail.posterminal.OrderLoaderPaymentHook;

@ApplicationScoped
public class OrderLoaderHookGiftCard extends OrderLoaderPaymentHook {

  @Override
  public void exec(JSONObject jsonorder, Order order, ShipmentInOut shipment, Invoice invoice)
      throws Exception {
    BusinessPartner bp = order.getBusinessPartner();
    GiftCardHookUtils.updateGCPaymentTransaction(order, jsonorder, paymentSchedule);
    JSONArray orderlines = jsonorder.getJSONArray("lines");
    GiftCardModel gc = new GiftCardModel();
    for (int i = 0; i < orderlines.length(); i++) {
      JSONObject line = orderlines.getJSONObject(i);
      OrderLine orderline = order.getOrderLineList().get(i);
      String transaction = line.getJSONObject("product").optString("giftCardTransaction", null);
      if (transaction == null) {
        // Is not a gift card transaction...

        Product product = OBDal.getInstance().get(Product.class,
            line.getJSONObject("product").getString("id"));

        String giftcardid = line.optString("giftcardid", null);
        // BigDecimal amount = jsonsent.has("amount") ? new BigDecimal(jsonsent.getString("amount"))
        // : null;

        @SuppressWarnings("rawtypes")
        Iterator keys = line.keys();
        JSONObject giftCardProperties = new JSONObject();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          if (key.startsWith("giftcard") && !key.equals("giftcardid")) {
            giftCardProperties.put(key.replace("giftcard", ""), line.get(key));
          }
        }
        /*gc.createGiftCard(giftcardid, product, bp, order.getOrderDate(), order, orderline,
            orderline.getLineNetAmount(), giftCardProperties);*/
      } else {
        // is a gift card transaction so just fill in the order and order-line fields
        gc.populateGiftCardTransaction(transaction, order, orderline);
      }
    }
  }

}
