package org.openbravo.retail.returns;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.retail.posterminal.OrderLoaderHook;

@ApplicationScoped
public class ReturnsShipmentOrderLoaderHook implements OrderLoaderHook {

  @Override
  public void exec(JSONObject jsonorder, Order order, ShipmentInOut shipment, Invoice invoice)
      throws Exception {
    JSONArray orderlines = jsonorder.getJSONArray("lines");

    for (int i = 0; i < orderlines.length(); i++) {

      JSONObject jsonOrderLine = orderlines.getJSONObject(i);
      if (jsonOrderLine.has("shipmentlineId")) {

        ShipmentInOutLine originalShipmentLine = OBDal.getInstance().get(ShipmentInOutLine.class,
            jsonOrderLine.getString("shipmentlineId"));

        order.getOrderLineList().get(i).setGoodsShipmentLine(originalShipmentLine);

      } else if (jsonOrderLine.has("originalOrderLineId")) {
        OrderLine originalOrderLine = OBDal.getInstance().get(OrderLine.class,
            jsonOrderLine.getString("originalOrderLineId"));

        OBCriteria<ShipmentInOutLine> inOutCriteria = OBDal.getInstance().createCriteria(
            ShipmentInOutLine.class);
        inOutCriteria.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE,
            originalOrderLine));
        ShipmentInOutLine inOutLine = (ShipmentInOutLine) inOutCriteria.uniqueResult();

        order.getOrderLineList().get(i).setGoodsShipmentLine(inOutLine);

      }
    }
  }
}