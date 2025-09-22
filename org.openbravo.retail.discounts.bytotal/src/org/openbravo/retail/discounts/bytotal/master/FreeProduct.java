/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.discounts.bytotal.master;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.retail.posterminal.master.Discount;

/**
 * @author adrianromero
 * 
 */
public class FreeProduct extends Discount {
  public static final String freeProductPropertyExtension = "DISCT_FREEPRODUCT";

  @Inject
  @Any
  @Qualifier(freeProductPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<String> prepareQuery(JSONObject jsonsent) throws JSONException {
    HQLPropertyList regularFreeProductHQLProperties = ModelExtensionUtils
        .getPropertyExtensions(extensions);
    String hql = "select" + regularFreeProductHQLProperties.getHqlSelect()
        + "from DISCT_FREEPRODUCT fp where ((fp.$incrementalUpdateCriteria) "
        + jsonsent.get("operator")
        + " (fp.promotionDiscount.$incrementalUpdateCriteria)) and exists (select 1 "
        + getPromotionsHQL(jsonsent, false) + " and fp.promotionDiscount = p)";

    return Arrays.asList(new String[] { hql });
  }
}
