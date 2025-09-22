/*
 ************************************************************************************
 * Copyright (C) 2013-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, enyo, _ */

// Discount per total amount
OB.Model.Discounts.discountRules['4183C8EB7CDA472D9E64521DC2504B15'] = {
  byTotalDiscount: true,
  async: false,
  implementation: function (discountRule, receipt, line) {
    OB.Model.Discounts.DISCT.preprocess(discountRule, receipt, line, null, function (preprocess) {
      if (preprocess.total >= discountRule.get('disctTotalreceipt')) {
        // Add promotion in all lines 
        var accumdiscount = 0;
        var totaldiscount = discountRule.get('disctTotalamountdisc');
        _.each(preprocess.linesToCompute, function (l, index, list) {
          var discount;
          // distributing  discount in all lines based on total of each one
          // if PIT, in gross
          // if no PIT, in net
          if (index < list.length - 1) {
            discount = OB.DEC.toNumber(OB.DEC.toBigDecimal(l.total).multiply(OB.DEC.toBigDecimal(totaldiscount).divide(OB.DEC.toBigDecimal(preprocess.totalNet || preprocess.total), 20, OB.DEC.getRoundingMode())));
          } else {
            discount = OB.DEC.sub(totaldiscount, accumdiscount);
          }
          accumdiscount = OB.DEC.add(accumdiscount, discount);
          receipt.addPromotion(l.line, discountRule, {
            amt: discount
          });
        });
      }
    }, false);
  }
};