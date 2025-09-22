/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global _ */

// Buy X pay Y of same product
OB.Model.Discounts.discountRules.E08EE3C23EBA49358A881EF06C139D63 = {
  async: false,
  implementation: function (discountRule, receipt, line) {
    var alerts, qty, x, y, mod, chunks, price, finalPrice, totalQty = 0,
        totalToGift = 0,
        totalToPay = 0,
        totalChunks, product, applyToLines = [],
        clonedDiscountRule, discountRulePay, discountRuleGift, topay, togift, totalMod;

    x = discountRule.get('oBDISCX');
    y = discountRule.get('oBDISCY');

    if (!x || !y || x === 0) {
      window.console.warn('Discount incorrectly defined, missing x or y', discountRule);
    }
    qty = line.get('qty');

    mod = qty % x;

    //If Product(Non-Grouped) Enforces To Apply Promotions Each Line Then Find Such Product and Calculate and Apply Promotions To 
    //Each Line which has The same Product till AppliedPromotions Lines Count Reaches The TotalToGift of Respective Product .
    //calculate the totalQty and totaltogift if product is non-grouped to Apply Promotions Each Unit Line Level .
    receipt.get('lines').forEach(function (l) {
      if (line.get('product').id === l.get('product').id && l.get('price') === line.get('price')) {
        applyToLines.push(l);
        totalQty += l.get('qty');
        if (totalQty >= x) {
          totalChunks = Math.floor(totalQty / x);
          totalToGift = (x - y) * totalChunks;
          totalToPay = y * totalChunks;
          totalMod = totalQty % x;
        }
      }
    });
    if (totalQty >= x) {
      if (totalMod !== 0) {
        alerts = OB.I18N.getLabel('OBPOS_DISCAlertXYSameProduct', [x - mod, line.get('product').get('_identifier'), discountRule.get('printName') || discountRule.get('name')]);
      }
      clonedDiscountRule = discountRule.clone();
      price = line.get('discountedLinePrice') || line.get('price');
      _.forEach(applyToLines, function (l) {
        if (totalToGift > 0) {
          if (totalToGift - l.get('qty') >= 0) {
            clonedDiscountRule.set('qtyOffer', l.get('qty'));
            receipt.addPromotion(l, clonedDiscountRule, {
              amt: price * l.get('qty'),
              hidden: false,
              chunks: totalChunks
            });
            totalToGift -= l.get('qty');
          } else {
            clonedDiscountRule.set('qtyOffer', totalToGift);
            receipt.addPromotion(l, clonedDiscountRule, {
              amt: price * totalToGift,
              hidden: false,
              chunks: totalChunks
            });

            if (totalToPay - (l.get('qty') - totalToGift) >= 0) {
              clonedDiscountRule.set('qtyOffer', l.get('qty') - totalToGift);
              receipt.addPromotion(l, clonedDiscountRule, {
                amt: 0,
                hidden: true,
                chunks: totalChunks
              });
              totalToPay -= l.get('qty') - totalToGift;
            } else {
              clonedDiscountRule.set('qtyOffer', totalToPay);
              receipt.addPromotion(l, clonedDiscountRule, {
                amt: 0,
                hidden: true,
                chunks: totalChunks
              });
              totalToPay = 0;
            }
            totalToGift = 0;
          }
        } else if (totalToPay > 0) {
          if (totalToPay - l.get('qty') >= 0) {
            clonedDiscountRule.set('qtyOffer', l.get('qty'));
            receipt.addPromotion(l, clonedDiscountRule, {
              amt: 0,
              hidden: true,
              chunks: totalChunks
            });
            totalToPay -= l.get('qty');
          } else {
            clonedDiscountRule.set('qtyOffer', totalToPay);
            receipt.addPromotion(l, clonedDiscountRule, {
              amt: 0,
              hidden: true,
              chunks: totalChunks
            });
            totalToPay = 0;
          }
        }
      });
    }
    return {
      alerts: alerts
    };
  }
};