/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

OB.OBCOMBO.comboFixPrice = {
  comboRuleId: '71895FA82C9645949CB752564FB1389D',
  discountType: {
    comboFixPrice: 'COMBOFIXPRICE'
  }
};

// extending standard combo discounts filter in order to take into account 
// combo fix price definition
OB.Model.Discounts.standardFilter += "\n" //
+ "and (M_Offer.M_Offer_Type_ID != '" + OB.OBCOMBO.comboFixPrice.comboRuleId + "'" //
+ " or exists (select 1" //
+ "              from obcombo_family cf, obcombo_product cp" //
+ "             where cf.obcombo_family_id = cp.obcombo_family_id" //
+ "               and cf.M_Offer_ID = M_Offer.M_Offer_ID" //
+ "               and cp.M_Product_ID = ?))";

OB.Model.DiscountsExecutor.prototype.criteriaParams.push('productId');