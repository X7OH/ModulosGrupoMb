package ec.com.sidesoft.process.print;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class PrintProcess extends BaseProcessActionHandler {
  private static Logger log = Logger.getLogger(PrintProcess.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    try {
      JSONObject contentObject = new JSONObject(content);

      String originalProcessId = (String) parameters.get("processId");

      String orderId = contentObject.getString("inpcOrderId");

      HashMap<String, String> printParams = new HashMap<String, String>();
      PrintProcessFromTemplate template = new PrintProcessFromTemplate();
      template.printProcess(originalProcessId, orderId, printParams);
      return createResult(MessageType.SUCCESS,
          OBMessageUtils.getI18NMessage("SSRCM_process_success", null));

    } catch (JSONException e) {
      log.error(e.getMessage());
      e.printStackTrace();
      return createResult(MessageType.ERROR, e.getMessage());
    }

  }

  private JSONObject createResult(MessageType messageType, String text) {
    return getResponseBuilder().showMsgInProcessView(messageType, "Message Title", text)
        .refreshGrid().build();
  }
}
