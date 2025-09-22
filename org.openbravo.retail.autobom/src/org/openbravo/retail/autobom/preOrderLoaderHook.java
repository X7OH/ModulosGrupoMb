/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.autobom;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.retail.posterminal.OrderLoaderPreProcessHook;

@ApplicationScoped
public class preOrderLoaderHook implements OrderLoaderPreProcessHook {

  @Override
  public void exec(JSONObject jsonorder) throws Exception {
    BOMProcess bomProcess = new BOMProcess();
    bomProcess.createProcessBOM(jsonorder);
    if (bomProcess.getProductionList().length() > 0) {
      jsonorder.put("bomIds", bomProcess.getProductionList());
    }
  }

}
