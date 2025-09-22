/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
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

public class CashMgmtEvents extends QueryTerminalProperty {

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    return Arrays
        .asList(new String[] { "select c.id as id, c.name as name, c.eventtype as eventType from OBRETCO_CashManagementEvents c "
            + "where  c.$orgCriteria and c.eventtype like 'GCNV_%' order by c.name, c.id  " });
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

  @Override
  public String getProperty() {
    return "cashMgmtGiftCardsEvents";
  }

  @Override
  public boolean returnList() {
    return false;
  }
}
