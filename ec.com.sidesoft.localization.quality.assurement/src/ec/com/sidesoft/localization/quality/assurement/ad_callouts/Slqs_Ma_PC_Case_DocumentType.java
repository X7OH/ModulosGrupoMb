package ec.com.sidesoft.localization.quality.assurement.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class Slqs_Ma_PC_Case_DocumentType extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String documentTypeId = info.getStringParameter("inpemSlqsDocumentType", null);
    DocumentType docObj = OBDal.getInstance().get(DocumentType.class, documentTypeId);
    if (docObj.getDocumentSequence() != null) {
      Sequence seq = docObj.getDocumentSequence();

      info.addResult("inpemSlqsDocumentno",
          "<" + (seq.getPrefix() != null ? seq.getPrefix() : "")
              + seq.getNextAssignedNumber().toString()
              + (seq.getSuffix() != null ? seq.getSuffix() : "") + ">");
    }
  }
}
