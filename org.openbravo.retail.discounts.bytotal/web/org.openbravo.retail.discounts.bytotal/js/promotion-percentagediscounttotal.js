/*
 ************************************************************************************
 * Copyright (C) 2013-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, enyo, _ */

// Percentage discount per total amount
OB.Model.Discounts.discountRules['9707DE71F91549DB80CCB2F094E951EA'] = {
  byTotalDiscount: true,
  async: false,
  implementation: function (discountRule, receipt, line) {
    OB.Model.Discounts.DISCT.preprocess(discountRule, receipt, line, null, function (preprocess) {
      if (preprocess.total >= discountRule.get('disctTotalreceipt')) {
        // Add promotion in all lines
        _.each(preprocess.linesToCompute, function (l) {
          var discount = OB.DEC.toBigDecimal(l.total).multiply(OB.DEC.toBigDecimal(discountRule.get('disctTotalpercdisc')).divide(new BigDecimal('100'), 20, OB.DEC.getRoundingMode()));
          discount = OB.DEC.toNumber(discount);

          receipt.addPromotion(l.line, discountRule, {
            amt: discount
          });
        });
      }
    }, false);
  }
};