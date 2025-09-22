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

public class Product extends Family {
  public static final String productComboPropertyExtension = "OBCOMBO_Product";
  @Inject
  @Any
  @Qualifier(productComboPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<String> prepareQuery(JSONObject jsonsent) throws JSONException {
    HQLPropertyList regularProductComboHQLProperties = ModelExtensionUtils
        .getPropertyExtensions(extensions);
    String hql = "select" + regularProductComboHQLProperties.getHqlSelect()
        + "from OBCOMBO_Product cp where ((cp.$incrementalUpdateCriteria) "
        + jsonsent.get("operator") + " (cp.obcomboFamily.$incrementalUpdateCriteria) "
        + jsonsent.get("operator")
        + " (cp.obcomboFamily.priceAdjustment.$incrementalUpdateCriteria))";

    hql += " and exists (select 1 ";
    hql += "from OBCOMBO_Family fp where fp.active =true and fp.priceAdjustment.active = true";

    hql += " and exists (select 1 " + getPromotionsHQL(jsonsent, false);
    hql += "              and fp.priceAdjustment = p)";
    hql += "              and cp.obcomboFamily = fp)";

    return Arrays.asList(new String[] { hql });
  }
}
