/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone */

OB.OBCOMBO = {
  comboRuleId: '7899A7A4204749AD92881133C4EE7A57',
  discountType: {
    fixPrice: 'FIXPRICE',
    percentage: 'PERCENTAGE',
    fixDiscount: 'FIXDISC'
  }
};

OB.Data.Registry.registerModel(Backbone.Model.extend({
  modelName: 'ComboFamily',
  generatedStructure: true,
  entityName: 'OBCOMBO_Family',
  source: 'org.openbravo.retail.discounts.combo.master.Family'
}));

OB.Data.Registry.registerModel(Backbone.Model.extend({
  modelName: 'ComboProduct',
  generatedStructure: true,
  entityName: 'OBCOMBO_Product',
  source: 'org.openbravo.retail.discounts.combo.master.Product'
}));

OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push({
  modelName: 'ComboFamily',
  generatedModel: true
}, {
  modelName: 'ComboProduct',
  generatedModel: true
});


// extending standard discounts filter in order to take into account
// combo definition
OB.Model.Discounts.standardFilter += "\n" //
+ "and (M_Offer.M_Offer_Type_ID != '" + OB.OBCOMBO.comboRuleId + "'" //
+ " or exists (select 1" //
+ "              from obcombo_family cf, obcombo_product cp" //
+ "             where cf.obcombo_family_id = cp.obcombo_family_id" //
+ "               and cf.M_Offer_ID = M_Offer.M_Offer_ID" //
+ "               and cp.M_Product_ID = ?))";

OB.Model.DiscountsExecutor.prototype.criteriaParams.push('productId');