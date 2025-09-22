/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards.process;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;

public class CancelGiftCardProcessHandler extends BaseProcessActionHandler {

  static Logger log = Logger.getLogger(CancelGiftCardProcessHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject selectedObject = null;
    String giftcardInstId = null;
    JSONObject result = new JSONObject();
    JSONArray actions = new JSONArray();
    JSONObject msg = new JSONObject();
    JSONObject showMsgInView = new JSONObject();
    GiftCardInst terminal = null;
    try {
      selectedObject = new JSONObject(content);
      giftcardInstId = selectedObject.getString("inpgcnvGiftcardInstId");
      terminal = OBDal.getInstance().get(GiftCardInst.class, giftcardInstId);

      GiftCardGLItemUtils.close(terminal, null, null);
      terminal.setCancelGiftCard(true);

      msg.put("msgType", "success");
      msg.put("msgTitle", OBMessageUtils.messageBD("GCNV_SuccessCloseCard_Title"));
      msg.put(
          "msgText",
          OBMessageUtils.getI18NMessage("GCNV_SuccessCloseCard_Text",
              new String[] { terminal.getSearchKey() }));
      showMsgInView.put("showMsgInView", msg);
      actions.put(showMsgInView);
      result.put("responseActions", actions);

    } catch (Exception e) {
      try {
        log.debug(OBMessageUtils.messageBD("GCNV_ErrorCloseCard_Msg"));
        msg.put("msgType", "error");
        msg.put("msgTitle", OBMessageUtils.messageBD("GCNV_ErrorCloseCard_Title"));
        msg.put(
            "msgText",
            OBMessageUtils.getI18NMessage("GCNV_ErrorCloseCard_Msg",
                new String[] { terminal.getSearchKey() }));
        showMsgInView.put("showMsgInView", msg);
        actions.put(showMsgInView);
        result.put("responseActions", actions);
        return result;
      } catch (JSONException e1) {
        // won't happen
      }
    }
    return result;
  }
}
