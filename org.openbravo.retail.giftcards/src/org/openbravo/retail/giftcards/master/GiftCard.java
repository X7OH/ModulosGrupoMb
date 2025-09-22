/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.giftcards.master;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.retail.posterminal.ProcessHQLQuery;

public class GiftCard extends ProcessHQLQuery {

  public static final String giftCardPropertyExtension = "GCNV_GiftCardExtension";

  @Inject
  @Any
  @Qualifier(giftCardPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<HQLPropertyList> getHqlProperties(JSONObject jsonsent) {
    // Get Product Properties
    List<HQLPropertyList> propertiesList = new ArrayList<HQLPropertyList>();
    Map<String, Object> args = new HashMap<String, Object>();
    HQLPropertyList giftcardsHQLProperties = ModelExtensionUtils.getPropertyExtensions(extensions,
        args);
    propertiesList.add(giftcardsHQLProperties);

    return propertiesList;
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    HQLPropertyList giftCardProperties = ModelExtensionUtils.getPropertyExtensions(extensions);

    return Arrays
        .asList(new String[] { "SELECT "
            + giftCardProperties.getHqlSelect()
            + " from Product p "
            + "where p.$readableSimpleClientCriteria and $filtersCriteria and $hqlCriteria and (p.$incrementalUpdateCriteria) and "
            + "(p.gcnvGiftcardtype = 'G' or p.gcnvGiftcardtype = 'V')" });
  }
}
