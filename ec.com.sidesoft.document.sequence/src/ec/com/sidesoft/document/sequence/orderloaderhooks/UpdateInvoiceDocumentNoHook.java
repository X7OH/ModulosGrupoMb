package ec.com.sidesoft.document.sequence.orderloaderhooks;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.retail.posterminal.OrderLoaderHook;

@ApplicationScoped
public class UpdateInvoiceDocumentNoHook implements OrderLoaderHook {

  @Override
  public void exec(JSONObject jsonorder, Order order, ShipmentInOut shipment, Invoice invoice)
      throws Exception {

    if (jsonorder.has("invoiceDocumentNo") && invoice != null) {
      String invoiceDocumentNo = jsonorder.getString("invoiceDocumentNo");
      String validationCode = jsonorder.getString("validationCode");

      TriggerHandler.getInstance().disable();
      try {
        invoice.setDocumentNo(invoiceDocumentNo);
        invoice.setEeiCodigo(validationCode);
        OBDal.getInstance().save(invoice);
        OBDal.getInstance().flush();
      } finally {
        if (TriggerHandler.getInstance().isDisabled()) {
          TriggerHandler.getInstance().enable();
        }
      }
    }
  }
}
