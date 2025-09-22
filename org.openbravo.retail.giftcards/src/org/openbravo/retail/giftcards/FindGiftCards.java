/*
 ************************************************************************************
 * Copyright (C) 2016-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.retail.posterminal.ProcessHQLQuery;

public class FindGiftCards extends ProcessHQLQuery {

  public static final String findGiftCardsPropertyExtension = "GCNV_FindGiftCardsExtension";

  @Inject
  @Any
  @Qualifier(findGiftCardsPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<HQLPropertyList> getHqlProperties(JSONObject jsonsent) {
    // Get Product Properties
    List<HQLPropertyList> propertiesList = new ArrayList<HQLPropertyList>();
    Map<String, Object> args = new HashMap<String, Object>();
    HQLPropertyList characteristicsHQLProperties = ModelExtensionUtils.getPropertyExtensions(
        extensions, args);
    propertiesList.add(characteristicsHQLProperties);

    return propertiesList;
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    List<String> hqlQueries = new ArrayList<String>();

    HQLPropertyList gcHQLProperties = ModelExtensionUtils.getPropertyExtensions(extensions);

    final StringBuilder hql = new StringBuilder();

    hql.append("SELECT " + gcHQLProperties.getHqlSelect() + " FROM GCNV_GiftCardInst AS gci ");
    hql.append("WHERE $filtersCriteria ");
    boolean isFilterdByStatus = false;
    JSONArray remoteFilters = jsonsent.getJSONArray("remoteFilters");
    for (int i = 0; i < remoteFilters.length(); i++) {
      if (jsonsent.getJSONArray("remoteFilters").getJSONObject(i).getJSONArray("columns")
          .getString(0).equals("alertStatus")) {
        isFilterdByStatus = true;
        break;
      }
    }
    if (!isFilterdByStatus) {
      hql.append("AND gci.alertStatus <> 'C' ");
    }
    hql.append("AND gci.$readableSimpleClientCriteria AND gci.$naturalOrgCriteria ");

    if (jsonsent.has("orderByClause") && jsonsent.get("orderByClause") != JSONObject.NULL) {
      hql.append("$orderByCriteria");
    }

    hqlQueries.add(hql.toString());
    return hqlQueries;
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

}
