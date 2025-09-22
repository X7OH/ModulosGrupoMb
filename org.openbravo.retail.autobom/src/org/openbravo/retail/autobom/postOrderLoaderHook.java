/*
 ************************************************************************************
 * Copyright (C) 2014-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.autobom;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.retail.posterminal.OrderLoaderHook;

@ApplicationScoped
public class postOrderLoaderHook implements OrderLoaderHook {

  @Override
  public void exec(JSONObject jsonorder, Order order, ShipmentInOut shipment, Invoice invoice)
      throws Exception {

    if (jsonorder.has("bomIds") && jsonorder.getJSONArray("bomIds").length() > 0) {

      for (int i = 0; i < jsonorder.getJSONArray("bomIds").length(); i++) {
        String bomId = (String) jsonorder.getJSONArray("bomIds").get(i);
        ProductionTransaction productionTransaction = OBDal.getInstance().get(
            ProductionTransaction.class, bomId);
        productionTransaction.setObbomCOrder(order);
        OBDal.getInstance().save(productionTransaction);
      }

    }

  }
}
