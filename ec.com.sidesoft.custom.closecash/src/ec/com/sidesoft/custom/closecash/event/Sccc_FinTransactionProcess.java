package ec.com.sidesoft.custom.closecash.event;

import java.util.HashMap;

import javax.enterprise.event.Observes;
import javax.servlet.ServletException;

import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

public class Sccc_FinTransactionProcess extends EntityPersistenceEventObserver {

	  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
	      FIN_FinaccTransaction.ENTITY_NAME) };

	  @Override
	  protected Entity[] getObservedEntities() {
	    return entities;
	  }

	  
	  public void onSave(@Observes EntityNewEvent event) {
	    if (!isValidEvent(event)) {
	      return;
	    }
	    System.out.println("1"); 
	    final FIN_FinaccTransaction objFinaccTransaction = (FIN_FinaccTransaction) event.getTargetInstance();
	    
//	    objCashclousure.getId();
	    
	    System.out.println("2");
	    FIN_TransactionProcess transactionprocess = new FIN_TransactionProcess();
	    
	    if(objFinaccTransaction.isScccIscashclousure()) {
	    ConnectionProvider conn = new DalConnectionProvider(false);
	    
	  //  private OBError processTransaction(VariablesSecureApp vars, ConnectionProvider conn,
	    //    String strAction, FIN_FinaccTransaction transaction) throws Exception {
	    System.out.println("3");
	    try {
	      System.out.println("4");
	    VariablesSecureApp vars = new VariablesSecureApp(null);
	      ProcessBundle pb = new ProcessBundle("F68F2890E96D4D85A1DEF0274D105BCE", vars).init(conn);
	      HashMap<String, Object> parameters = new HashMap<String, Object>();
	      parameters.put("action", "P");
	      parameters.put("Fin_FinAcc_Transaction_ID", objFinaccTransaction.getId());
	      pb.setParams(parameters);
	      OBError myMessage = null;
	     new FIN_TransactionProcess().execute(pb);
	  
	    myMessage = (OBError) pb.getResult();
	    System.out.println(myMessage);
	    }catch(Exception e) {
	      throw new OBException("Error "+e.getMessage());
	    }
	    }
	    System.out.println("5");
	    
	    
	    
	    
	    

	  }
	  public void onUpdate(@Observes EntityUpdateEvent event) throws ServletException{
		    if (!isValidEvent(event)) {
		      return;
		    }
		    
	  }
	  /*
	    String msg = "";
	    try {
	      OBContext.setAdminMode(false);
	      if (strAction.equals("P") && !transaction.isProcessed()) {
	        // ***********************
	        // Process Transaction
	        // ***********************

	        boolean orgLegalWithAccounting = FIN_Utility.periodControlOpened(
	            FIN_FinaccTransaction.TABLE_NAME, transaction.getId(), FIN_FinaccTransaction.TABLE_NAME
	                + "_ID", "LE");
	        boolean documentEnabled = getDocumentConfirmation(transaction.getId());
	        if (documentEnabled
	            && !FIN_Utility.isPeriodOpen(transaction.getClient().getId(),
	                AcctServer.DOCTYPE_FinAccTransaction, transaction.getOrganization().getId(),
	                OBDateUtils.formatDate(transaction.getDateAcct())) && orgLegalWithAccounting) {
	          msg = OBMessageUtils.messageBD("PeriodNotAvailable");
	          throw new OBException(msg);
	        }

	        final FIN_FinancialAccount financialAccount = transaction.getAccount();
	        financialAccount.setCurrentBalance(financialAccount.getCurrentBalance().add(
	            transaction.getDepositAmount().subtract(transaction.getPaymentAmount())));
	        transaction.setProcessed(true);
	        FIN_Payment payment = transaction.getFinPayment();
	        if (payment != null) {
	          if (transaction.getBusinessPartner() == null) {
	            transaction.setBusinessPartner(payment.getBusinessPartner());
	          }
	          AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
	          if (StringUtils.equals(payment.getStatus(), dao.PAYMENT_STATUS_AWAITING_EXECUTION)
	              && dao.isAutomatedExecutionPayment(financialAccount, payment.getPaymentMethod(),
	                  payment.isReceipt())) {
	            msg = OBMessageUtils.messageBD("APRM_AutomaticExecutionProcess");
	            throw new OBException(msg);
	          }

	          payment.setStatus(payment.isReceipt() ? "RDNC" : "PWNC");
	          transaction.setStatus(payment.isReceipt() ? "RDNC" : "PWNC");
	          if (transaction.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
	            transaction.setTransactionType(TRXTYPE_BPWithdrawal);
	          } else {
	            transaction.setTransactionType(TRXTYPE_BPDeposit);
	          }
	          OBDal.getInstance().save(payment);
	          if (transaction.getDescription() == null || "".equals(transaction.getDescription())) {
	            transaction.setDescription(payment.getDescription());
	          }
	          Boolean invoicePaidold = false;
	          for (FIN_PaymentDetail pd : payment.getFINPaymentDetailList()) {
	            for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
	              invoicePaidold = psd.isInvoicePaid();
	              if (!invoicePaidold) {
	                if ((FIN_Utility.invoicePaymentStatus(payment).equals(payment.getStatus()))) {
	                  psd.setInvoicePaid(true);
	                }
	                if (psd.isInvoicePaid()) {
	                  FIN_Utility.updatePaymentAmounts(psd);
	                  FIN_Utility.updateBusinessPartnerCredit(payment);
	                }
	                OBDal.getInstance().save(psd);
	              }
	            }
	          }

	          if (!StringUtils.equals(transaction.getCurrency().getId(), payment.getCurrency().getId())) {
	            transaction.setForeignCurrency(payment.getCurrency());
	            transaction.setForeignConversionRate(payment.getFinancialTransactionConvertRate());
	            transaction.setForeignAmount(payment.getAmount());
	          }

	        } else {
	          transaction.setStatus(transaction.getDepositAmount().compareTo(
	              transaction.getPaymentAmount()) > 0 ? "RDNC" : "PWNC");
	        }
	        if (transaction.getForeignCurrency() != null
	            && !transaction.getCurrency().equals(transaction.getForeignCurrency())
	            && getConversionRateDocument(transaction).size() == 0) {
	          insertConversionRateDocument(transaction);
	        }
	        transaction.setAprmProcessed("R");
	        OBDal.getInstance().save(financialAccount);
	        OBDal.getInstance().save(transaction);

	      } 
	  
	  */
	
	}
