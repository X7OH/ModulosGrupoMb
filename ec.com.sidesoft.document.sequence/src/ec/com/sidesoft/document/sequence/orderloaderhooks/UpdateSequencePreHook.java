package ec.com.sidesoft.document.sequence.orderloaderhooks;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.retail.posterminal.OrderLoaderPreProcessHook;
import org.openbravo.retail.posterminal.POSUtils;
import org.openbravo.retail.posterminal.PreOrderLoaderPrioritizedHook;

import ec.com.sidesoft.document.sequence.PoinOfSaleSequenceLine;
import ec.com.sidesoft.document.sequence.PointOfSaleSeq;

@ApplicationScoped
public class UpdateSequencePreHook extends PreOrderLoaderPrioritizedHook
    implements OrderLoaderPreProcessHook {

  @Override
  public void exec(JSONObject jsonOrder) throws Exception {
    int orderType = jsonOrder.getInt("orderType");

    String bpId = jsonOrder.getJSONObject("bp").getString("id");
    BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class, bpId);

    if (jsonOrder.has("invoiceDocumentNo")) {

      String documentNo = jsonOrder.getString("invoiceDocumentNo");
      boolean isReturn = jsonOrder.has("isReturn") ? jsonOrder.getBoolean("isReturn") : false;
      OBPOSApplications terminal = POSUtils.getTerminalById(jsonOrder.getString("posTerminal"));
      List<PointOfSaleSeq> sequenceList = terminal.getECSDSPointOfSaleSeqList();

      if (!isReturn) {
        // Actualizo la secuencia de invoice
        for (PointOfSaleSeq seq : sequenceList) {
          if (seq.isInvoiceSeq()) {
            this.updateSequenceList(seq, documentNo);
          }
        }
      } else if (isReturn) {
        // actualizo la secuencia de credit note
        for (PointOfSaleSeq seq : sequenceList) {
          if (!seq.isInvoiceSeq()) {
            this.updateSequenceList(seq, documentNo);
          }
        }
      }
    }
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
      if (seqLine != null) {
        seqLine.setUsed(true);
        OBDal.getInstance().save(seqLine);
        OBDal.getInstance().flush();
        SessionHandler.getInstance().commitAndStart();
      }
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  @Override
  public int getPriority() {
    // TODO Auto-generated method stub
    return 101;
  }

}
