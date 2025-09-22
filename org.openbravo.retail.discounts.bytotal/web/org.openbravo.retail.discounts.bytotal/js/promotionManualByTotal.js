/*
 ************************************************************************************
 * Copyright (C) 2016-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, enyo, _ */
// Manual discounts by Total
// Variable Percentage Discount per Total Amount
OB.Model.Discounts.discountRules.ADF7E7F91B3A49869FB3F89B8C5A325E = {
  isAutoCalculated: true,
  byTotalDiscount: true,
  allLinesDiscount: true,
  isManual: true,
  isFixed: false,
  isAmount: false,
  async: false,
  implementation: function (discountRule, receipt, line) {
    OB.Model.Discounts.DISCT.manualByTotalPromotionsExecutor(receipt, line);
  },
  addManual: function (receipt, line, promotion) {},
  getIdentifier: function (rule, discount) {
    return (discount.name || rule.get('printName') || rule.get('name')) + ' - ' + (rule.get('userAmt') || rule.get('disctTotalpercdisc')) + '%';
  },
  getAmountProperty: function () {
    return 'disctTotalpercdisc';
  },
  discountCalculation: function (discountRule, receipt, line, orderManualPromotion, callback) {
    OB.Model.Discounts.DISCT.addTotalManualPercentageDisc(discountRule, receipt, line, orderManualPromotion, callback);
  }
};

//Fixed Percentage Discount per Total Amount
OB.Model.Discounts.discountRules['096984DC2B944C85A9162C66C37EE7A3'] = {
  isAutoCalculated: true,
  byTotalDiscount: true,
  allLinesDiscount: true,
  isManual: true,
  isFixed: true,
  isAmount: false,
  async: false,
  implementation: function (discountRule, receipt, line) {
    OB.Model.Discounts.DISCT.manualByTotalPromotionsExecutor(receipt, line);
  },
  addManual: function (receipt, line, promotion) {},
  getIdentifier: function (rule, discount) {
    return (discount.name || rule.get('printName') || rule.get('name')) + ' - ' + rule.get('disctTotalpercdisc') + ' %';
  },
  getAmountProperty: function () {
    return 'disctTotalpercdisc';
  },
  discountCalculation: function (discountRule, receipt, line, orderManualPromotion, callback) {
    OB.Model.Discounts.DISCT.addTotalManualPercentageDisc(discountRule, receipt, line, orderManualPromotion, callback);
  }
};

// Fixed Discount per Total Amount
OB.Model.Discounts.discountRules['971642418DD24DE5BD860D63EF57D5F6'] = {
  isAutoCalculated: true,
  byTotalDiscount: true,
  allLinesDiscount: true,
  isManual: true,
  isFixed: true,
  isAmount: true,
  async: false,
  implementation: function (discountRule, receipt, line) {
    OB.Model.Discounts.DISCT.manualByTotalPromotionsExecutor(receipt, line);
  },
  addManual: function (receipt, line, promotion) {},
  getIdentifier: function (rule, discount) {
    return (discount.name || rule.get('printName') || rule.get('name')) + ' - ' + rule.get('disctTotalamountdisc') + ' ' + OB.MobileApp.model.get('terminal').currency$_identifier;
  },
  getAmountProperty: function () {
    return 'disctTotalamountdisc';
  },
  discountCalculation: function (discountRule, receipt, line, orderManualPromotion, callback) {
    OB.Model.Discounts.DISCT.addTotalManualAmountDisc(discountRule, receipt, line, orderManualPromotion, callback);
  }
};

// Variable Discount per Total Amount
OB.Model.Discounts.discountRules['00535FB65D9941AE9575546FBAF11B95'] = {
  isAutoCalculated: true,
  byTotalDiscount: true,
  allLinesDiscount: true,
  isManual: true,
  isFixed: false,
  isAmount: true,
  async: false,
  implementation: function (discountRule, receipt, line) {
    OB.Model.Discounts.DISCT.manualByTotalPromotionsExecutor(receipt, line);
  },
  addManual: function (receipt, line, promotion) {},
  getIdentifier: function (rule, discount) {
    return (discount.name || rule.get('printName') || rule.get('name')) + ' - ' + (discount.userAmt || rule.get('userAmt') || rule.get('disctTotalamountdisc')) + ' ' + OB.MobileApp.model.get('terminal').currency$_identifier;
  },
  getAmountProperty: function () {
    return 'disctTotalamountdisc';
  },
  discountCalculation: function (discountRule, receipt, line, orderManualPromotion, callback) {
    OB.Model.Discounts.DISCT.addTotalManualAmountDisc(discountRule, receipt, line, orderManualPromotion, callback);
  }
};