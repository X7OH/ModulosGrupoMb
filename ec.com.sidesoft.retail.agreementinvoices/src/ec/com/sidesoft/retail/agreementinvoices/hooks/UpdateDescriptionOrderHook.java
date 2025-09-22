package ec.com.sidesoft.retail.agreementinvoices.hooks;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.retail.posterminal.OrderLoaderHook;

@ApplicationScoped
public class UpdateDescriptionOrderHook implements OrderLoaderHook {

  @Override
  public void exec(JSONObject jsonorder, Order order, ShipmentInOut shipment, Invoice invoice)
      throws Exception {

    // TODO Auto-generated method stub

    if (jsonorder.has("descriptionnew")) {
      String strdescription = jsonorder.getString("descriptionnew");

      if (!strdescription.equals("")) {

        // Order order = OBDal.getInstance().get(Order.class, jsonorder.getString("id"));

        String[] strDescriptionNew = new String[5];
        try {
          String[] strDescriptionNew2 = strdescription.split("PAG:");
          strDescriptionNew = strDescriptionNew2;
        } catch (Exception e1) {

        }

        if (strDescriptionNew[0] != null) {
          strdescription = String.valueOf(strDescriptionNew[0]);
        }

        TriggerHandler.getInstance().disable();
        try {

          order.set(Order.PROPERTY_DESCRIPTION, strdescription);
          if(order.getBusinessPartner().isSscmbIsagreement() == true) {          
            order.set(Order.PROPERTY_OBPOSNOTINVOICEONCASHUP, true);
          }
          OBDal.getInstance().save(order);

          try {
            shipment.set(ShipmentInOut.PROPERTY_DESCRIPTION, ".");
            OBDal.getInstance().save(shipment);
          } catch (Exception e) {
          }

          try {
            invoice.set(Invoice.PROPERTY_DESCRIPTION, ".");
            OBDal.getInstance().save(invoice);
          } catch (Exception e) {
          }

          OBDal.getInstance().flush();
        } finally {
          if (TriggerHandler.getInstance().isDisabled()) {
            TriggerHandler.getInstance().enable();
          }
        }
      }else {
        if(order.getBusinessPartner().isSscmbIsagreement() == true) {
          TriggerHandler.getInstance().disable();
          try {
              order.set(Order.PROPERTY_OBPOSNOTINVOICEONCASHUP, true);
              OBDal.getInstance().save(order);
              OBDal.getInstance().flush();            
          } finally {
            if (TriggerHandler.getInstance().isDisabled()) {
              TriggerHandler.getInstance().enable();
            }
          }          
        }        
        
      }

    }

  }

}