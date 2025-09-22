/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone, $, _  */

OB.UTIL.HookManager.registerHook('OBPOS_PreApplyAutomaticDiscount', function (args, callbacks) {

  // Get all by total discounts registered in the application
  var byTotalDiscountIdList = [];
  var i;
  _.each(OB.Model.Discounts.discountRules, function (discountRule, ruleId) {
    if (discountRule.byTotalDiscount) {
      byTotalDiscountIdList.push(ruleId);
    }
  });

  // Get from discount list all by total promotions
  var targetDiscountRuleList = [];
  args.discountList.forEach(function (discount, index) {
    if (byTotalDiscountIdList.indexOf(discount.get('discountType')) !== -1) {
      targetDiscountRuleList.push(args.discountList.at(index));
    }
  });

  // Keep the order of by total promotions give by the local db
  targetDiscountRuleList.sort(function (a, b) {
    return a.get('_idx') - b.get('_idx');
  });

  // Remove by total promotions from discount list and push it to the end
  _.each(targetDiscountRuleList, function (targetDiscount) {
    args.discountList.remove(targetDiscount);
    args.discountList.push(targetDiscount);
  });
  OB.UTIL.HookManager.callbackExecutor(args, callbacks);

});