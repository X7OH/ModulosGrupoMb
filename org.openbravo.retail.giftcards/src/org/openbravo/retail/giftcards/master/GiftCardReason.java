/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards.master;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.retail.posterminal.term.QueryTerminalProperty;

public class GiftCardReason extends QueryTerminalProperty {

  @Override
  protected boolean isAdminMode() {
    return true;
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    return Arrays.asList(new String[] { //
        "select gcr.id as id, gcr.name as name, gcr.onlyOrg as onlyOrg, gcr.useOneTime as useOneTime, "
            + "gcr.reimbursed as reimbursed, gcr.paymentMethod.id as paymentMethod, gcr.hasOwner as hasOwner, "
            + "gcr.printcard as printCard, template.templatePath as printTemplate, template.ispdf as templateIsPdf, template.printer as templatePrinter "
            + "from GCNV_GiftcardReason as gcr "
            + "left outer join gcr.printtemplate as template "
            + "where gcr.$naturalOrgCriteria and gcr.$readableSimpleClientCriteria and gcr.$activeCriteria "
            + "order by gcr.name, gcr.id" });
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

  @Override
  public String getProperty() {
    return "gcnvGiftcardReason";
  }

  @Override
  public boolean returnList() {
    return false;
  }
}
