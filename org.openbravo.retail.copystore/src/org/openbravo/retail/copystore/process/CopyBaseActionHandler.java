/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore.process;

import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.retail.copystore.process.CopyStoreProcess.ProcessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CopyBaseActionHandler extends BaseProcessActionHandler {
  private static final Logger log = LoggerFactory.getLogger(BaseProcessActionHandler.class);
  @Inject
  @Any
  protected Instance<PropertyHandler> propertyHandlers;

  @Inject
  @Any
  protected Instance<FKPropertyHandler> fkPropertyHandlers;

  @Inject
  @Any
  protected Instance<BlankProperties> blankProperties;

  @Inject
  @Any
  protected Instance<PostProcessHandler> postProcessHandlers;

  @Override
  public JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject params = null;
    JSONObject jsonContent = null;

    try {
      jsonContent = new JSONObject(content);
      params = jsonContent.getJSONObject("_params");
    } catch (JSONException ignore) {
      log.error("There was an error getting params for content:{}", content, ignore);
    }
    long t = System.currentTimeMillis();
    CopyStoreProcess copyProcess = new CopyStoreProcess(params, propertyHandlers,
        fkPropertyHandlers, blankProperties, postProcessHandlers);
    executeProcess(copyProcess, jsonContent);
    log.debug("Copy {} took {} ms", copyProcess.type, System.currentTimeMillis() - t);

    JSONObject result = new JSONObject();
    try {

      result.put("retryExecution", copyProcess.type == ProcessType.copyTerminal
          || copyProcess.hasErrors);
      result.put("showResultsInProcessView", true);
      JSONArray actions = new JSONArray();

      JSONObject msg = new JSONObject();
      if (!copyProcess.isValidated()) {
        msg.put("msgType", "error");
        msg.put("msgTitle", OBMessageUtils.messageBD("OBPOSCS_PrevalidationsFailedTitle"));
        msg.put("msgText", OBMessageUtils.messageBD("OBPOSCS_PrevalidationsFailed"));
      } else if (!copyProcess.hasErrors) {
        msg.put("msgType", "success");
        msg.put("msgTitle", OBMessageUtils
            .messageBD(copyProcess.type == ProcessType.copyStore ? "OBPOSCS_CopyStoreSuccessTitle"
                : "OBPOSCS_CopyTerminalSuccessTitle"));
        msg.put("msgText", OBMessageUtils
            .messageBD(copyProcess.type == ProcessType.copyStore ? "OBPOSCS_CopyStoreSuccessMsg"
                : "OBPOSCS_CopyTerminalSuccessMsg"));
      } else {
        msg.put("msgType", "error");
        msg.put("msgTitle", OBMessageUtils.messageBD("Failure"));
        msg.put("msgText", OBMessageUtils.messageBD("OBPOSCS_CopyStoreFailMsg"));
      }

      JSONObject showLog = new JSONObject();
      showLog.put("msg", msg);
      showLog.put("log", copyProcess.getLog());
      showLog.put("disableFields", copyProcess.type == ProcessType.copyTerminal
          && !copyProcess.hasErrors);

      JSONObject shoLogAction = new JSONObject();
      shoLogAction.put("showCopyStoreLog", showLog);

      actions.put(shoLogAction);
      result.put("responseActions", actions);
    } catch (JSONException e) {
      log.error("Error generating response", e);
    }
    return result;
  }

  protected abstract void executeProcess(CopyStoreProcess copyStore, JSONObject jsonContent);
}
