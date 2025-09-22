/*
 ************************************************************************************
 * Copyright (C) 2013-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global  _ */

OB.Model.Discounts.DISCT = {
  isFreeProduct: function (product, freeProducts) {
    var p;
    if (!freeProducts || !freeProducts.length || !freeProducts.at) {
      return false;
    }
    for (p = 0; p < freeProducts.length; p++) {
      if (freeProducts.at(p).get('product') === product.id) {
        return freeProducts.at(p);
      }
    }
    return false;
  },

  addLineAsCandidate: function (line, discountRule) {
    var promotionCandidates = line.get('promotionCandidates') || [];
    if (promotionCandidates.indexOf(discountRule.id) === -1) {
      promotionCandidates.push(discountRule.id);
      line.set('promotionCandidates', promotionCandidates);
    }
  },

  removeAutoPromotionsFromReceipt: function (receipt) {
    var i, linePromotions;
    _.each(receipt.get('lines').models, function (line) {
      line.set('promotions', _.filter(line.get('promotions'), function (promotion) {
        return promotion.manual;
      }));
    });
  },

  doProcess: function () {
    // when working in Best Deal Case mode, by total discounts should only be applied
    // in the BDC process
    return !(OB.POS.modelterminal.get('terminal').bestDealCase && !OB.Model.Discounts.preventApplyPromotions);
  },

  preprocess: function (discountRule, receipt, checkingLine, freeProducts, preprocessCallback, isManuallyAdded, instanceId) {
    var me = this,
        priceIncludesTax = receipt.get('priceIncludesTax');

    function preprocessImpl() {
      var total = 0,
          linesToCompute = [],
          promotionCandidates, ret, isFreeProduct = OB.Model.Discounts.DISCT.isFreeProduct(checkingLine.get('product'), freeProducts);


      function getDiscountedLinePrice(line) {
        // discounted price should be set in line.discountedLinePrice, but we cannot trust
        // all rules correctly set this info
        var price;

        line.calculateGross();
        // in PIT discounts are applied to gross
        // in no PIT discounts are applied to net
        if (priceIncludesTax) {
          price = line.getGross();
        } else {
          price = line.getNet();
        }

        _.each(line.get('promotions'), function (p) {
          if (discountRule.isPercentage) {
            if (p.ruleId !== discountRule.id || (p.ruleId === discountRule.id && p.discountinstance !== instanceId)) {
              price = OB.DEC.sub(price, (p.amt || 0));
            }

          } else if (!discountRule.isAmount) {
            if (p.ruleId !== discountRule.id) {
              price = OB.DEC.sub(price, (p.amt || 0));
            }
          }
        });
        return {
          price: price
        };
      }

      if (!me.doProcess() && !OB.Model.Discounts.discountRules[discountRule.get('discountType')].isManual) {
        return {
          total: 0,
          linesToCompute: []
        };
      }
      // This line is candidate for this promotion
      me.addLineAsCandidate(checkingLine, discountRule);

      // Calculate total without my discounts...
      receipt.get('lines').each(function (l) {
        var lineCandidates, linePrice, appliedPromotions, lastPromo;
        if (l.stopApplyingPromotions()) {
          appliedPromotions = l.get('promotions');
          if (appliedPromotions) {
            lastPromo = appliedPromotions[appliedPromotions.length - 1];
          }
          if (!isManuallyAdded && (!lastPromo || (lastPromo.ruleId !== discountRule.id && lastPromo.priority < discountRule.get('priority'))) && !isFreeProduct) {
            // lines with a promotion already applied, should not be computed for total
            // nor applied new promo
            // if last promo is current one, then compute it
            return; //continue
          }
        }

        lineCandidates = l.get('promotionCandidates') || [];
        if (lineCandidates.indexOf(discountRule.id) === -1 && !isManuallyAdded) {
          // line cannot be applied with current rule, skip it
          return; //continue
        }

        if (l.get('qty') <= 0) {
          // the line is negative or has qty 0, skip it
          return; //continue
        }

        linePrice = getDiscountedLinePrice(l);

        // total for PIT includes taxes and for no PIT does not
        total = OB.DEC.add(total, linePrice.price);

        // ... but discount is applied to net price
        linesToCompute.push({
          line: l,
          total: linePrice.price
        });
      });

      if (total < discountRule.get('disctTotalreceipt')) {
        // reset this promotion in all lines
        receipt.get('lines').each(function (l) {
          receipt.removePromotion(l, discountRule);
        });
      }

      if (!isManuallyAdded) {
        // Enforce stop cascade after applying this rule
        discountRule.set('applyNext', false, {
          silent: true
        });
      }

      ret = {
        total: total,
        linesToCompute: linesToCompute
      };

      preprocessCallback(ret);
    }

    preprocessImpl();
  },

  addTotalManualPercentageDisc: function (discountRule, receipt, line, ruleFromReceipt) {
    var definition = {},
        discPercentage;

    discPercentage = ruleFromReceipt.get('rule').userAmt || discountRule.get('userAmt') || discountRule.get('disctTotalpercdisc');
    definition.lastApplied = true;
    discountRule.isPercentage = true;
    if (ruleFromReceipt) {
      // if rule is override, remove all promotions from receipt
      if (!discountRule.get('applyNext')) {
        OB.Model.Discounts.DISCT.removeAutoPromotionsFromReceipt(receipt);
      }
      OB.Model.Discounts.DISCT.preprocess(discountRule, receipt, line, null, function (preprocess) {
        // Add promotion in all lines
        _.each(preprocess.linesToCompute, function (l) {
          var alreadyCalculated = _.filter(l.line.get('promotions'), function (p) {
            return p.ruleId === ruleFromReceipt.get('rule').id && p.discountinstance === ruleFromReceipt.get('rule').discountinstance;
          });
          if (alreadyCalculated.length > 0) {
            // This promotion is already applied
            return;
          }
          var discount = OB.DEC.toBigDecimal(l.total).multiply(OB.DEC.toBigDecimal(discPercentage).divide(new BigDecimal('100'), 20, OB.DEC.getRoundingMode()));
          discount = OB.DEC.toNumber(discount);
          definition.amt = discount;
          definition.discountinstance = ruleFromReceipt.get('rule').discountinstance;
          definition.forceReplace = true;
          discountRule.set('qtyOffer', 0); // By total doesn't consume units
          receipt.addPromotion(l.line, discountRule, definition);
        });
      }, true, ruleFromReceipt.get('rule').discountinstance);
    }
  },

  addTotalManualAmountDisc: function (discountRule, receipt, line, ruleFromReceipt) {
    var definition = {},
        totaldiscount;

    definition.lastApplied = true;
    discountRule.isAmount = true;
    if (ruleFromReceipt) {
      // if rule is override, remove all promotions from receipt
      if (!discountRule.get('applyNext')) {
        OB.Model.Discounts.DISCT.removeAutoPromotionsFromReceipt(receipt);
      }

      var accumdiscount = 0;
      OB.Model.Discounts.DISCT.preprocess(discountRule, receipt, line, null, function (preprocess) {
        // Add promotion in all lines
        totaldiscount = ruleFromReceipt.get('rule').userAmt || discountRule.get('userAmt') || discountRule.get('disctTotalamountdisc');
        _.each(preprocess.linesToCompute, function (l, index, list) {
          var discount = 0;
          // distributing  discount in all lines based on total of each one
          // if PIT, in gross
          // if no PIT, in net
          if (OB.DEC.toNumber(OB.DEC.toBigDecimal(l.total)) !== 0) {
            if (index < list.length - 1) {
              discount = OB.DEC.toNumber(OB.DEC.toBigDecimal(l.total).multiply(OB.DEC.toBigDecimal(totaldiscount).divide(OB.DEC.toBigDecimal(preprocess.totalNet || preprocess.total), 20, OB.DEC.getRoundingMode())));
            } else {
              discount = OB.DEC.sub(totaldiscount, accumdiscount);
            }
            discount = (discount > l.total) ? l.total : discount;
            accumdiscount = OB.DEC.add(accumdiscount, discount);
          }
          definition.amt = discount;
          definition.discountinstance = ruleFromReceipt.get('rule').discountinstance;
          definition.userAmt = ruleFromReceipt.get('rule').userAmt;
          definition.forceReplace = true;
          discountRule.set('qtyOffer', 0); // By total doesn't consume units
          definition.extraProperties = ruleFromReceipt.get('rule').extraProperties;
          receipt.addPromotion(l.line, discountRule, definition);
        });
      }, true, ruleFromReceipt.get('rule').discountinstance);
    }
  },

  manualByTotalPromotionsExecutor: function (receipt, line) {
    if (!line.get('orderManualPromotionsAlreadyApplied')) {
      var orderManualPromotions = receipt.get('orderManualPromotions');
      _.each(orderManualPromotions.models, function (orderManualPromotion) {
        var discountRule = orderManualPromotion.get('discountRule');
        discountRule.set('applyNext', orderManualPromotion.get('rule').applyNext);
        discountRule.set('userAmt', orderManualPromotion.get('rule').userAmt);
        discountRule.set('extraProperties', orderManualPromotion.get('rule').extraProperties);
        OB.Model.Discounts.discountRules[discountRule.get('discountType')].discountCalculation(discountRule, receipt, line, orderManualPromotion);
      });
      line.set('orderManualPromotionsAlreadyApplied', true, {
        silent: true
      });
    }
  }
};