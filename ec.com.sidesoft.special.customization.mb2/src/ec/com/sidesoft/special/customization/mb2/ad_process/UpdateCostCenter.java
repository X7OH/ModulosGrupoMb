package ec.com.sidesoft.special.customization.mb2.ad_process;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

public class UpdateCostCenter extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(UpdateCostCenter.class);
  private static final String language = OBContext.getOBContext().getLanguage().getLanguage();
  private static final ConnectionProvider conn = new DalConnectionProvider(false);

  @Override
  protected void doExecute(ProcessBundle bundle) {
    OBContext.setAdminMode();

    TriggerHandler.getInstance().disable();

    try
	{
		ProcessLogger logger = bundle.getLogger();
		String message;

		// update the orders
		Integer updatedOrders = updateOrders();
		message = String.format(Utility.messageBD(conn, "SCMBA_UpdatedOrders", language),
			updatedOrders);
		logger.logln(message);

		// update the inouts
		Integer updatedShipmentInOuts = updateShipmentInOuts();
		message = String.format(Utility.messageBD(conn, "SCMBA_UpdatedShipmentInOut", language),
			updatedShipmentInOuts);
		logger.logln(message);

		// update the invoices
		Integer updatedInvoices = updateInvoices();
		message = String.format(Utility.messageBD(conn, "SCMBA_UpdatedInvoices", language),
			updatedInvoices);
		logger.logln(message);

		// update the invoices
		Integer updatedFINPayments = updatePayments();
		message = String.format(Utility.messageBD(conn, "SCMBA_UpdatedFINPayments", language),
			updatedFINPayments);
		logger.logln(message);
	}
	finally
	{
	    TriggerHandler.getInstance().enable();
		OBContext.restorePreviousMode();
	}
  }

  private Integer updateOrders() {
    Integer updateRecords = 0;

    final StringBuilder orderWhereClause = new StringBuilder();
    orderWhereClause.append(" as ord join ord.");
    orderWhereClause.append(Order.PROPERTY_ORGANIZATION);
    orderWhereClause.append(" as o where ord.");
    orderWhereClause.append(Order.PROPERTY_SALESTRANSACTION);
    orderWhereClause.append("=true and (ord.");
    orderWhereClause.append(Order.PROPERTY_COSTCENTER);
    orderWhereClause.append(" is null and o.");
    orderWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    orderWhereClause.append(" is not null or o.");
    orderWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    orderWhereClause.append(" is not null and ord.");
    orderWhereClause.append(Order.PROPERTY_COSTCENTER);
    orderWhereClause.append(".id<>o.");
    orderWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    orderWhereClause.append(".id)");

    final OBQuery<Order> obqInvoice = OBDal.getInstance().createQuery(Order.class,
        orderWhereClause.toString());
    List<Order> orders = obqInvoice.list();

    for (Order order : orders) {
      Costcenter costCenter = order.getOrganization().getScmbaCostcenter();
      if (costCenter != null) {
        updateRecords++;
        order.setCostcenter(costCenter);
        Boolean updatedFactAcct = updateFactAcct(order.getId(), costCenter);
      }
    }

    OBDal.getInstance().flush();
    return updateRecords;
  }

  private Integer updateShipmentInOuts() {
    Integer updateRecords = 0;

    final StringBuilder shipmentInOutWhereClause = new StringBuilder();
    shipmentInOutWhereClause.append(" as io join io.");
    shipmentInOutWhereClause.append(ShipmentInOut.PROPERTY_ORGANIZATION);
    shipmentInOutWhereClause.append(" as o where io.");
    shipmentInOutWhereClause.append(ShipmentInOut.PROPERTY_SALESTRANSACTION);
    shipmentInOutWhereClause.append("=true and (io.");
    shipmentInOutWhereClause.append(ShipmentInOut.PROPERTY_COSTCENTER);
    shipmentInOutWhereClause.append(" is null and o.");
    shipmentInOutWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    shipmentInOutWhereClause.append(" is not null or o.");
    shipmentInOutWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    shipmentInOutWhereClause.append(" is not null and io.");
    shipmentInOutWhereClause.append(ShipmentInOut.PROPERTY_COSTCENTER);
    shipmentInOutWhereClause.append(".id<>o.");
    shipmentInOutWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    shipmentInOutWhereClause.append(".id)");

    final OBQuery<ShipmentInOut> obqShipmentInOut = OBDal.getInstance()
        .createQuery(ShipmentInOut.class, shipmentInOutWhereClause.toString());
    List<ShipmentInOut> shipmentInOuts = obqShipmentInOut.list();

    for (ShipmentInOut shipmentInOut : shipmentInOuts) {
      Costcenter costCenter = shipmentInOut.getOrganization().getScmbaCostcenter();
      if (costCenter != null) {
        updateRecords++;
        shipmentInOut.setCostcenter(costCenter);
        Boolean updatedFactAcct = updateFactAcct(shipmentInOut.getId(), costCenter);
      }
    }

    OBDal.getInstance().flush();
    return updateRecords;
  }

  private Integer updateInvoices() {
    Integer updateRecords = 0;

    final StringBuilder invoiceWhereClause = new StringBuilder();
    invoiceWhereClause.append(" as i join i.");
    invoiceWhereClause.append(ShipmentInOut.PROPERTY_ORGANIZATION);
    invoiceWhereClause.append(" as o where i.");
    invoiceWhereClause.append(ShipmentInOut.PROPERTY_SALESTRANSACTION);
    invoiceWhereClause.append("=true and (i.");
    invoiceWhereClause.append(ShipmentInOut.PROPERTY_COSTCENTER);
    invoiceWhereClause.append(" is null and o.");
    invoiceWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    invoiceWhereClause.append(" is not null or o.");
    invoiceWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    invoiceWhereClause.append(" is not null and i.");
    invoiceWhereClause.append(ShipmentInOut.PROPERTY_COSTCENTER);
    invoiceWhereClause.append(".id<>o.");
    invoiceWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    invoiceWhereClause.append(".id)");

    final OBQuery<Invoice> obqInvoice = OBDal.getInstance().createQuery(Invoice.class,
        invoiceWhereClause.toString());
    List<Invoice> invoices = obqInvoice.list();

    for (Invoice invoice : invoices) {
      Costcenter costCenter = invoice.getOrganization().getScmbaCostcenter();
      if (costCenter != null) {
        updateRecords++;
        invoice.setCostcenter(invoice.getOrganization().getScmbaCostcenter());
        Boolean updatedFactAcct = updateFactAcct(invoice.getId(), costCenter);
      }
    }

    OBDal.getInstance().flush();
    return updateRecords;
  }

  private Integer updatePayments() {
    Integer updateRecords = 0;

    final StringBuilder paymentWhereClause = new StringBuilder();
    paymentWhereClause.append(" as p join p.");
    paymentWhereClause.append(FIN_Payment.PROPERTY_ORGANIZATION);
    paymentWhereClause.append(" as o where p.");
    paymentWhereClause.append(FIN_Payment.PROPERTY_RECEIPT);
    paymentWhereClause.append("=true and (p.");
    paymentWhereClause.append(FIN_Payment.PROPERTY_COSTCENTER);
    paymentWhereClause.append(" is null and o.");
    paymentWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    paymentWhereClause.append(" is not null or o.");
    paymentWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    paymentWhereClause.append(" is not null and p.");
    paymentWhereClause.append(FIN_Payment.PROPERTY_COSTCENTER);
    paymentWhereClause.append(".id<>o.");
    paymentWhereClause.append(Organization.PROPERTY_SCMBACOSTCENTER);
    paymentWhereClause.append(".id)");

    final OBQuery<FIN_Payment> obqPayment = OBDal.getInstance().createQuery(FIN_Payment.class,
        paymentWhereClause.toString());
    List<FIN_Payment> payments = obqPayment.list();

    for (FIN_Payment payment : payments) {
      Costcenter costCenter = payment.getOrganization().getScmbaCostcenter();
      if (costCenter != null) {
        updateRecords++;
        payment.setCostCenter(payment.getOrganization().getScmbaCostcenter());
        Boolean updatedFactAcct = updateFactAcct(payment.getId(), costCenter);
      }
    }

    OBDal.getInstance().flush();
    return updateRecords;
  }

  private Boolean updateFactAcct(String id, Costcenter costCenter) {
    final StringBuilder accountingFactWhereClause = new StringBuilder();
    final List<Object> params = new ArrayList<Object>();
    accountingFactWhereClause.append(" as fa where fa.");
    accountingFactWhereClause.append(AccountingFact.PROPERTY_RECORDID);
    accountingFactWhereClause.append("=?");
    params.add(id);

    final OBQuery<AccountingFact> obqAccountingFact = OBDal.getInstance()
        .createQuery(AccountingFact.class, accountingFactWhereClause.toString(), params);
    List<AccountingFact> accountingFacts = obqAccountingFact.list();
    for (AccountingFact accountingFact : accountingFacts) {
      accountingFact.setCostcenter(costCenter);
    }

    OBDal.getInstance().flush();
    return !accountingFacts.isEmpty();
  }

}