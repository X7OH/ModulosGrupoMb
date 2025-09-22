package ec.com.sidesoft.document.sequence;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class GenerateSequence extends BaseProcessActionHandler {
  private static final Logger log = Logger.getLogger(GenerateSequence.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      JSONObject contentObject = new JSONObject(content);
      String sequenceId = contentObject.getString("Ecsds_Psale_Seq_ID");

      PointOfSaleSeq sequence = OBDal.getInstance().get(PointOfSaleSeq.class, sequenceId);

      Long fromNumber = sequence.getSequenceFrom();
      Long toNumber = sequence.getSequenceTo();

      for (long i = fromNumber; i <= toNumber; i++) {
        PoinOfSaleSequenceLine line = new PoinOfSaleSequenceLine();
        line.setSequence(i);
        line.setEcsdsPsaleSeq(sequence);
        OBDal.getInstance().save(line);
      }

      sequence.setGenerated(true);
      OBDal.getInstance().save(sequence);
      OBDal.getInstance().flush();
      return createResult(MessageType.SUCCESS,
          OBMessageUtils.getI18NMessage("ECSDS_process_success", null));
    } catch (Exception e) {
      log.error("Error in process", e);
      return createResult(MessageType.ERROR,
          OBMessageUtils.getI18NMessage("ECSDS_process_error", null));
    }
  }

  private JSONObject createResult(MessageType messageType, String text) {
    return getResponseBuilder().showMsgInProcessView(messageType, "Message Title", text)
        .refreshGrid().build();
  }
}