/*
 ************************************************************************************
 * Copyright (C) 2013-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, enyo, _ */

// Free items per total amount
OB.Model.Discounts.discountRules['4755A35B4DA34F6CB08F15462BA123CF'] = {
  byTotalDiscount: true,
  async: true,
  discountedUnits: {},
  implementation: function (discountRule, receipt, line, listener) {
    if (!OB.Model.Discounts.DISCT.doProcess()) {
      // nothing to do for now
      listener.trigger('completed');
      return;
    }

    OB.Dal.find(OB.Model.FreeProduct, {
      'promotionDiscount': discountRule.get('id')
    }, function (freeProducts) {
      OB.Model.Discounts.DISCT.preprocess(discountRule, receipt, line, freeProducts, function (preprocess) {
        var i, shouldApply = false,
            dr = OB.Model.Discounts.discountRules['4755A35B4DA34F6CB08F15462BA123CF'];
        //console.log('total::', preprocess.total, discountRule.get('_identifier'), isFreeProduct?true:false);
        if (preprocess.total >= discountRule.get('disctTotalreceipt')) {
          // total is bigger than min, let's check if there are free items
          // in the ticket to apply the promo
          for (i = 0; i < preprocess.linesToCompute.length; i++) {
            if (OB.Model.Discounts.DISCT.isFreeProduct(preprocess.linesToCompute[i].line.get('product'), freeProducts) && (preprocess.total - preprocess.linesToCompute[i].total >= discountRule.get('disctTotalreceipt'))) {
              shouldApply = true;
              break;
            }
          }

          if (!shouldApply) {
            // no free products, so do not apply this rule
            listener.trigger('completed');
            return;
          }

          // promotion can be applied, do it for lines with free products
          _.each(preprocess.linesToCompute, function (lc) {
            var unitsToDiscount, fp, fpId, l = lc.line,
                discountApplied = false;


            fp = OB.Model.Discounts.DISCT.isFreeProduct(l.get('product'), freeProducts);
            if (fp) {
              fpId = fp.get('product');

              // a product can be in more than one line, keep count of how many units
              // have already been discounted
              dr.discountedUnits[fpId] = dr.discountedUnits[fpId] || 0;
              if ((fp.get('quantity') - dr.discountedUnits[fpId]) > l.get('qty')) {
                unitsToDiscount = l.get('qty') - dr.discountedUnits[fpId];
              } else {
                unitsToDiscount = fp.get('quantity') - dr.discountedUnits[fpId];
              }
              dr.discountedUnits[fpId] += unitsToDiscount;

              _.each(l.get('promotions'), function (p) {
                if (p.discountType === discountRule.get('discountType')) {
                  discountApplied = true;
                  return;
                }
              });

              if (unitsToDiscount > 0 && !discountApplied) {
                discountRule.set('qtyOffer', unitsToDiscount);
                receipt.addPromotion(l, discountRule, {
                  amt: OB.DEC.mul(l.get('discountedLinePrice') || l.get('price'), unitsToDiscount)
                });
              }
            }
          });
        }
        listener.trigger('completed');
      }, false);
    }, function (tx, error) {
      OB.UTIL.showError('OBDAL error: ' + error);
      listener.trigger('completed');
    });
  }
};