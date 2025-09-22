/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.retail.posterminal.ProcessHQLQuery;

public class ListGiftCard extends ProcessHQLQuery {

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {

    String where = "where gc.alertStatus <> 'C' "
        + "and gc.$readableSimpleCriteria and gc.$activeCriteria and "
        + "gc.organization.id in ("
        + Utility.getInStrSet(new HashSet<String>(new OrganizationStructureProvider()
            .getParentList(OBContext.getOBContext().getCurrentOrganization().getId(), true)))
        + ") and (upper(gc.businessPartner.name) like upper(:filter) or upper(gc.searchKey) like upper(:filter))";

    String query = "select gc.searchKey as _identifier, gc.businessPartner.name as businessPartner$_identifier, "
        + "gc.id as id, p.name as product$_identifier, c.name as category$_identifier, "
        + "gc.searchKey as searchKey, gc.type as type " //
        + "from GCNV_GiftCardInst gc left join gc.product as p left join gc.category as c ";

    return Arrays.asList(new String[] { query + where });
  }
}
