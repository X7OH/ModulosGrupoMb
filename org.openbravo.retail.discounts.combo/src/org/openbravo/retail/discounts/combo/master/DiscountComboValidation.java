/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.discounts.combo.master;

import org.codehaus.jettison.json.JSONException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.process.SimpleQueryBuilder;
import org.openbravo.retail.posterminal.CustomInitialValidation;
import org.openbravo.retail.posterminal.OBPOSApplications;

public class DiscountComboValidation extends CustomInitialValidation {

  public String promotionTypeCombo = "7899A7A4204749AD92881133C4EE7A57";

  @Override
  public void validation(OBPOSApplications posTerminal) throws JSONException {

    String hqlDiscount = "select p.id " + new Family().getPromotionsHQL(null)
        + " and p.discountType.id = '" + promotionTypeCombo + "'"
        + " and not exists (select 1 from OBCOMBO_Family fp where fp.priceAdjustment = p)";

    SimpleQueryBuilder discount = new SimpleQueryBuilder(hqlDiscount, OBContext.getOBContext()
        .getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId(),
        null, null, null);

    Query discountQuery = discount.getDalQuery();
    ScrollableResults scrollableResults = discountQuery.scroll(ScrollMode.FORWARD_ONLY);
    if (scrollableResults.next()) {
      throw new JSONException("OBCOMBO_COMBOFAMILY_NOTADDED");
    }
  }
}