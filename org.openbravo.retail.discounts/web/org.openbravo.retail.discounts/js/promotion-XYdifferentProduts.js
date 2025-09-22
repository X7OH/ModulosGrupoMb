/*
 ************************************************************************************
 * Copyright (C) 2012-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone*/

// Buy X pay Y of different product
OB.Model.Discounts.discountRules['312D41071ED34BA18B748607CA679F44'] = {
  async: false,
  implementation: function (discountRule, receipt, line, ruleListener, candidates) {
    var qty = BigDecimal.prototype.ZERO,
        offered = BigDecimal.prototype.ZERO,
        priority = discountRule.get('priority') || null,
        priorityLine, x, y, chunks, i, j, ln, price, subtype = discountRule.get('oBDISCSubtype') || 'MOSTEXPENSIVE',
        totalPrice = BigDecimal.prototype.ZERO,
        distributed, amt, distributionAmt = BigDecimal.prototype.ZERO,
        distributionQty = 0,
        distribution = [],
        distributionQtyOffer = [],
        discountRuleLine, applyingToLinesCollection = Backbone.Collection.extend({
        comparator: function (line) {
          if (subtype === 'CHEAPEST') {
            // To group sort by price desc, so we generate the best (for client) possible group
            return -(line.get('discountedLinePrice') || line.get('price'));
          } else {
            return (line.get('discountedLinePrice') || line.get('price'));
          }
        }
      }),
        rule, rest, lRest, candidate, promotion, topay, togift, group, groupInv, lQty, lQtyOffer, checkedQty, chunksInThisGroup, alerts, unitsToDiscount, unitsToCheck, discountedQty, avgPrice, applyingToLines = new applyingToLinesCollection();


    // Stablish the rule that there could be a better discount if more lines are added
    receipt.get('lines').forEach(function (l) {
      if (l.get('promotions') && l.get('promotions').length > 0) {
        for (i = 0; i < l.get('promotions').length; i++) {
          // Delete promotion with a lower priority (Priority number higher)
          promotion = l.get('promotions')[i];
          if (promotion.priority && priority !== null) {
            priorityLine = promotion.priority;
            if (priority < priorityLine) {
              for (j = 0; j < candidates.length; j++) {
                if (promotion.ruleId === candidates[j]) {
                  receipt.removePromotion(l, {
                    id: candidates[j]
                  });
                }
              }
            } else {
              receipt.removePromotion(l, discountRule);
            }
          } else {
            if ((discountRule.get('discountType') === promotion.discountType) && (discountRule.id === promotion.ruleId)) {
              receipt.removePromotion(l, discountRule);
            }
          }
        }
      }
    });

    // Count the qty of products we have available for this discount
    receipt.get('lines').forEach(function (l) {
      offered = BigDecimal.prototype.ZERO;
      rest = BigDecimal.prototype.ZERO;
      if (l.get('promotions') && l.get('promotions').length > 0) {
        for (i = 0; i < l.get('promotions').length; i++) {
          promotion = l.get('promotions')[i];
          if (promotion.qtyOffer) {
            offered = offered.add(OB.DEC.toBigDecimal(promotion.qtyOffer));
          }
        }
      }
      if (l.get('promotionCandidates')) {
        l.get('promotionCandidates').forEach(function (candidateRule) {
          // If there is any line to apply the promotion, we add it
          if (candidateRule === discountRule.id) {
            if (OB.DEC.toBigDecimal(l.get('qty')).subtract(OB.DEC.toBigDecimal(offered)) > 0) {
              applyingToLines.add(l);
              rest = OB.DEC.toBigDecimal(l.get('qty')).subtract(OB.DEC.toBigDecimal(offered));
              qty = qty.add(OB.DEC.toBigDecimal(rest));
            }
          }
        });
      }
    });

    // apply the rule
    x = discountRule.get('oBDISCX');
    y = discountRule.get('oBDISCY');

    if (OB.DEC.toNumber(qty) >= x) {
      // Enforce stop cascade after applying this rule
      discountRule.set('applyNext', false, {
        silent: true
      });

      chunks = OB.DEC.toNumber(qty.divideInteger(OB.DEC.toBigDecimal(x)));
      // Do the group with the products to pay
      unitsToCheck = chunks * y;
      group = new applyingToLinesCollection();
      for (i = 0; i < applyingToLines.length; i++) {
        ln = applyingToLines.at(i);
        offered = BigDecimal.prototype.ZERO;
        if (ln.get('promotions') && ln.get('promotions').length > 0) {
          for (j = 0; j < ln.get('promotions').length; j++) {
            offered = offered.add(OB.DEC.toBigDecimal(ln.get('promotions')[j].qtyOffer));
          }
        }
        if (unitsToCheck > 0) {
          if ((ln.get('qty') - OB.DEC.toNumber(offered)) > unitsToCheck) {
            discountedQty = unitsToCheck;
          } else {
            discountedQty = ln.get('qty') - OB.DEC.toNumber(offered);
          }
          unitsToCheck -= discountedQty;
          // Total Price is only used for AVG discount
          totalPrice = totalPrice.add(OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price')).multiply(OB.DEC.toBigDecimal(discountedQty)));
          ln.set('qtyToApplyDisc', discountedQty);
          group.push(ln);
        }
      }
      // Do the group with the products to gift
      groupInv = new applyingToLinesCollection();
      applyingToLines.models.reverse();
      unitsToCheck = chunks * (x - y);
      for (i = 0; i < applyingToLines.length; i++) {
        ln = applyingToLines.at(i);
        offered = BigDecimal.prototype.ZERO;
        if (ln.get('promotions') && ln.get('promotions').length > 0) {
          for (j = 0; j < ln.get('promotions').length; j++) {
            offered = offered.add(OB.DEC.toBigDecimal(ln.get('promotions')[j].qtyOffer));
          }
        }
        if (unitsToCheck > 0) {
          if ((ln.get('qty') - OB.DEC.toNumber(offered)) > unitsToCheck) {
            discountedQty = unitsToCheck;
          } else {
            discountedQty = ln.get('qty') - OB.DEC.toNumber(offered);
          }
          unitsToCheck -= discountedQty;
          // Total Price is only used for AVG discount
          totalPrice = totalPrice.add(OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price')).multiply(OB.DEC.toBigDecimal(discountedQty)));
          ln.set('qtyToApplyDisc', discountedQty);
          groupInv.push(ln);
        }
      }
      groupInv.models.reverse();

      distributed = discountRule.get('oBDISCDistribute');

      // Start calculating discounts
      if (subtype === 'CHEAPEST' || subtype === 'MOSTEXPENSIVE') {
        // First Step: Search products that the client must pay
        topay = y * chunks;
        for (i = 0; i < group.length; i++) {
          ln = group.at(i);
          discountRuleLine = discountRule.clone();
          price = OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price'));
          lQty = OB.DEC.toBigDecimal(ln.get('qty'));
          lQtyOffer = 0;
          if (ln.get('promotions') && ln.get('promotions').length > 0) {
            for (j = 0; j < ln.get('promotions').length; j++) {
              lQtyOffer = lQtyOffer + OB.DEC.toNumber(ln.get('promotions')[j].qtyOffer);
            }
          }
          if (topay > 0) {
            lRest = OB.DEC.toNumber(lQty) - lQtyOffer;
            if (lRest > 0) {
              if (lRest >= topay) {
                if (distributed && (subtype === 'CHEAPEST')) {
                  distributionQtyOffer.push(topay);
                } else {
                  discountRuleLine.set('qtyOffer', OB.DEC.toNumber(topay));
                  receipt.addPromotion(ln, discountRuleLine, {
                    actualAmt: 0,
                    hidden: true,
                    chunks: chunks
                  });
                }
                topay = 0;
              } else {
                topay -= lRest;
                if (distributed && (subtype === 'CHEAPEST')) {
                  distributionQtyOffer.push(lRest);
                } else {
                  discountRuleLine.set('qtyOffer', OB.DEC.toNumber(lRest));
                  receipt.addPromotion(ln, discountRuleLine, {
                    actualAmt: 0,
                    hidden: true,
                    chunks: chunks
                  });
                }
              }
            }
          }
        }

        // Second Step: Search products to gift to the client 
        togift = (x - y) * chunks;
        for (i = 0; i < groupInv.length; i++) {
          ln = groupInv.at(i);
          discountRuleLine = discountRule.clone();
          price = OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price'));
          lQty = OB.DEC.toBigDecimal(ln.get('qty'));
          lQtyOffer = 0;
          if (ln.get('promotions') && ln.get('promotions').length > 0) {
            for (j = 0; j < ln.get('promotions').length; j++) {
              lQtyOffer = lQtyOffer + OB.DEC.toNumber(ln.get('promotions')[j].qtyOffer);
            }
          }
          if (togift > 0) {
            lRest = OB.DEC.toNumber(lQty) - lQtyOffer;
            if (lRest > 0) {
              if (lRest >= togift) {
                amt = price.multiply(OB.DEC.toBigDecimal(togift));
                if (distributed && (subtype === 'CHEAPEST')) {
                  distributionAmt = distributionAmt.add(amt);
                  distribution[i] = OB.DEC.toNumber(amt);
                  distributionQtyOffer.push(togift);
                } else {
                  discountRuleLine.set('qtyOffer', togift);
                  receipt.addPromotion(ln, discountRuleLine, {
                    amt: OB.DEC.toNumber(amt),
                    chunks: chunks
                  });
                }
                togift = 0;
              } else {
                togift -= lRest;
                amt = price.multiply(OB.DEC.toBigDecimal(lRest));
                if (distributed && (subtype === 'CHEAPEST')) {
                  distributionAmt = distributionAmt.add(amt);
                  distribution[i] = OB.DEC.toNumber(amt);
                  distributionQtyOffer.push(lRest);
                } else {
                  discountRuleLine.set('qtyOffer', lRest);
                  receipt.addPromotion(ln, discountRuleLine, {
                    amt: OB.DEC.toNumber(amt),
                    chunks: chunks
                  });
                }
              }
            }
          }
        }
      } else { // Discount AVG
        var totalDisc = BigDecimal.prototype.ZERO,
            groupAvg = new applyingToLinesCollection();
        totalPrice = BigDecimal.prototype.ZERO;
        unitsToCheck = chunks * x;
        groupAvg = new applyingToLinesCollection();
        for (i = 0; i < applyingToLines.length; i++) {
          ln = applyingToLines.at(i);
          offered = BigDecimal.prototype.ZERO;
          if (ln.get('promotions') && ln.get('promotions').length > 0) {
            for (j = 0; j < ln.get('promotions').length; j++) {
              offered = offered.add(OB.DEC.toBigDecimal(ln.get('promotions')[j].qtyOffer));
            }
          }
          if (unitsToCheck > 0) {
            if ((ln.get('qty') - OB.DEC.toNumber(offered)) > unitsToCheck) {
              discountedQty = unitsToCheck;
            } else {
              discountedQty = ln.get('qty') - OB.DEC.toNumber(offered);
            }
            unitsToCheck -= discountedQty;
            // Total Price is only used for AVG discount
            totalPrice = totalPrice.add(OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price')).multiply(OB.DEC.toBigDecimal(discountedQty)));
            ln.set('qtyToApplyDisc', discountedQty);
            groupAvg.push(ln);
          }
        }
        groupAvg.models.reverse();

        togift = chunks * (x - y);
        for (i = 0; i < groupAvg.length; i++) {
          if (togift > 0) {
            ln = groupAvg.at(i);
            price = OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price'));
            discountedQty = OB.DEC.toBigDecimal(ln.get('qtyToApplyDisc'));
            if (OB.DEC.toNumber(discountedQty) > togift) {
              totalDisc = totalDisc.add(price.multiply(OB.DEC.toBigDecimal(togift)));
              togift = 0;
            } else {
              totalDisc = totalDisc.add(price.multiply(discountedQty));
              togift -= discountedQty;
            }
          }
        }

        for (i = 0; i < groupAvg.length; i++) {
          ln = groupAvg.at(i);
          discountRuleLine = discountRule.clone();
          price = OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price'));
          discountedQty = OB.DEC.toBigDecimal(ln.get('qtyToApplyDisc'));
          discountRuleLine.set('qtyOffer', discountedQty);
          receipt.addPromotion(ln, discountRuleLine, {
            amt: OB.DEC.toNumber((totalDisc.multiply(price).multiply(discountedQty)).divide(totalPrice)),
            chunks: chunks
          });
        }
      }

      // Distribute the discount among all lines, but display to the user the same info as it was not distributed
      // distribution is just to be internally managed
      if (distributed && (subtype === 'CHEAPEST')) {
        var totalQty = chunks * y,
            pendingQty = chunks * y,
            qtyOffer = 0,
            partialAmt = 0;
        for (i = 0; i < group.length; i++) {
          ln = group.at(i);
          discountRuleLine = discountRule.clone();
          if (OB.DEC.toNumber(ln.get('qty')) >= totalQty && totalQty > 0) {
            qty = totalQty;
            totalQty = 0;
          } else if (totalQty > 0) {
            qty = OB.DEC.toNumber(ln.get('qty'));
            totalQty -= OB.DEC.toNumber(ln.get('qty'));
          } else {
            qty = 0;
          }
          price = OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price'));
          amt = OB.DEC.toNumber((OB.DEC.toBigDecimal(distributionQtyOffer[i]).multiply(price).multiply(OB.DEC.toBigDecimal(distributionAmt))).divide(totalPrice));
          partialAmt = OB.DEC.add(partialAmt, amt);

          discountRuleLine.set('qtyOffer', distributionQtyOffer[i]);
          receipt.addPromotion(ln, discountRuleLine, {
            actualAmt: amt,
            hidden: true,
            preserve: true,
            chunks: chunks
          });
        }

        totalQty = chunks * (x - y);
        pendingQty = chunks * (x - y);
        for (i = 0; i < groupInv.length; i++) {
          var position = i + group.length;
          ln = groupInv.at(i);
          discountRuleLine = discountRule.clone();
          if (OB.DEC.toNumber(ln.get('qty')) >= totalQty && totalQty > 0) {
            qty = totalQty;
            totalQty = 0;
          } else if (totalQty > 0) {
            qty = OB.DEC.toNumber(ln.get('qty'));
            totalQty -= OB.DEC.toNumber(ln.get('qty'));
          } else {
            qty = 0;
          }
          price = OB.DEC.toBigDecimal(ln.get('discountedLinePrice') || ln.get('price'));
          amt = OB.DEC.toNumber((OB.DEC.toBigDecimal(distributionQtyOffer[position]).multiply(price).multiply(OB.DEC.toBigDecimal(distributionAmt))).divide(totalPrice));
          if ((i === groupInv.length - 1) && ((partialAmt + amt) !== distributionAmt)) {
            amt = OB.DEC.toNumber(distributionAmt.subtract(OB.DEC.toBigDecimal(partialAmt)));
          } else {
            partialAmt = OB.DEC.add(partialAmt, amt);
          }

          discountRuleLine.set('qtyOffer', distributionQtyOffer[position]);
          receipt.addPromotion(ln, discountRuleLine, {
            actualAmt: amt,
            amt: distribution[i],
            chunks: chunks
          });
        }

      }
    }

    if (OB.DEC.toNumber(qty) % x !== 0) {
      alerts = OB.I18N.getLabel('OBPOS_DiscountAlert', [line.get('product').get('_identifier'), discountRule.get('printName') || discountRule.get('name')]);
    }

    return {
      alerts: alerts
    };
  }
};