/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.discounts.combo.master;

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

public class Family extends Discount {
  public static final String familyPropertyExtension = "OBCOMBO_Family";
  @Inject
  @Any
  @Qualifier(familyPropertyExtension)
  private Instance<ModelExtension> extensions;

  protected String getFamilyHQL(JSONObject jsonsent) throws JSONException {
    return getFamilyHQL(jsonsent, false);
  }

  protected String getPromotionsHQL(JSONObject jsonsent) throws JSONException {
    return getPromotionsHQL(jsonsent, false);
  }

  protected String getFamilyHQL(JSONObject jsonsent, boolean withSelect) throws JSONException {
    HQLPropertyList regularFamilyHQLProperties = ModelExtensionUtils
        .getPropertyExtensions(extensions);
    String hql = "";
    if (withSelect == true) {
      hql += "select" + regularFamilyHQLProperties.getHqlSelect();
    }
    hql += "from OBCOMBO_Family fp where ((fp.$incrementalUpdateCriteria) "
        + jsonsent.get("operator") + " (fp.priceAdjustment.$incrementalUpdateCriteria))";

    hql += " and exists (select 1 " + getPromotionsHQL(jsonsent, false);
    hql += "              and fp.priceAdjustment = p)";
    return hql;
  }

  @Override
  protected List<String> prepareQuery(JSONObject jsonsent) throws JSONException {
    return Arrays.asList(new String[] { getFamilyHQL(jsonsent, true) });
  }
}
