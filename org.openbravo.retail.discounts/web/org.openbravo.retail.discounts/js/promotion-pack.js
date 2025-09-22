/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, _, Backbone, console */

// Pack
OB.Model.Discounts.discountRules.BE5D42E554644B6AA262CCB097753951 = {
  async: true,
  implementation: function (discountRule, receipt, line, listener, candidates) {
    var applyingToLines = new Backbone.Collection(),
        criteria, promotionCandidates = line.get('promotionCandidates') || [],
        finalAmt, totalAmtCombo, lineNum, previousAmt, i, receiptProducts = {};

    // Query local DB to know detail about the rule (products and quantities) 
    criteria = {
      priceAdjustment: discountRule.id,
      _orderByClause: '_identifier'
    };

    OB.Dal.find(OB.Model.DiscountFilterProduct, criteria, function (products) {
      var chunks, totalAmt, promotionAmt = 0,
          outOfComboAmt;

      applyingToLines = receipt.checkAvailableUnitsPerLine(discountRule);

      applyingToLines.forEach(function (l) {
        if (receiptProducts[l.get('product').id]) {
          receiptProducts[l.get('product').id].lines.push(l);
          receiptProducts[l.get('product').id].qty += OB.UTIL.isNullOrUndefined(line.get('qtyAvailable')) ? l.getQty() : l.get('qtyAvailable');
        } else {
          receiptProducts[l.get('product').id] = {
            lines: [l],
            qty: OB.UTIL.isNullOrUndefined(line.get('qtyAvailable')) ? l.getQty() : l.get('qtyAvailable')
          };
        }
      });

      if (_.size(receiptProducts) !== products.length) {
        // rule can't be applied because not all products are matched
        listener.trigger('completed');
        return;
      }

      products.forEach(function (product) {
        var i, line, applyNtimes;
        if (chunks === 0) {
          // already decided not to apply
          return;
        }
        for (i = 0; i < applyingToLines.length; i++) {
          if (applyingToLines.at(i).get('product').id === product.get('product')) {
            line = applyingToLines.at(i);
            break;
          }
        }
        if (!line) {
          // cannot apply the rule
          chunks = 0;
          return;
        }
        product.set('receiptLine', line);
        applyNtimes = Math.floor(receiptProducts[line.get('product').id].qty / (product.get('obdiscQty') || 1));
        if (!chunks || applyNtimes < chunks) {
          chunks = applyNtimes;
        }
      });

      if (chunks === 0) {
        // Cannot apply, ensure the promotion is not applied
        applyingToLines.forEach(function (ln) {
          receipt.removePromotion(ln, discountRule);
        });
        listener.trigger('completed', {
          alerts: OB.I18N.getLabel('OBPOS_DiscountAlert', [line.get('product').get('_identifier'), discountRule.get('printName') || discountRule.get('name')])
        });
        return;
      }

      // There are products enough to apply the rule
      // Enforce stop cascade after applying this rule
      discountRule.set('applyNext', false, {
        silent: true
      });

      totalAmt = products.reduce(function (total, product) {
        var l = product.get('receiptLine');
        return OB.DEC.add(total, OB.DEC.mul(receiptProducts[l.get('product').id].qty, l.get('discountedLinePrice') || l.get('price')));
      }, OB.DEC.Zero);

      outOfComboAmt = products.reduce(function (total, product) {
        var l = product.get('receiptLine'),
            unitsOutOfCombo = receiptProducts[l.get('product').id].qty - (chunks * (product.get('obdiscQty') || 1));

        return OB.DEC.add(total, OB.DEC.mul(unitsOutOfCombo, l.get('discountedLinePrice') || l.get('price')));
      }, OB.DEC.Zero);

      finalAmt = OB.DEC.mul(chunks, discountRule.get('obdiscPrice').toFixed(OB.MobileApp.model.get('currency').obposPosprecision || OB.MobileApp.model.get('currency').pricePrecision)) + outOfComboAmt;
      promotionAmt = totalAmt - finalAmt;
      totalAmtCombo = totalAmt - outOfComboAmt;

      // first loop calculated the total discount, now let's apply it
      if (promotionAmt > 0) {
        lineNum = 0;
        previousAmt = 0;
        products.forEach(function (product) {
          var l = product.get('receiptLine'),
              price = l.get('discountedLinePrice') || l.get('price'),
              actualAmt;
          // discountRuleLine = discountRule.clone();
          lineNum += 1;
          if (lineNum < products.length) {
            actualAmt = OB.DEC.div(OB.DEC.mul(OB.DEC.mul(((l.get('product').get('groupProduct')) ? product.get('obdiscQty') : l.get('qty')) * chunks || l.get('qty'), price), promotionAmt), (totalAmtCombo));
            previousAmt += (l.get('product').get('groupProduct')) ? actualAmt : OB.DEC.mul(actualAmt, receiptProducts[l.get('product').id].qty);
          } else {
            // last line with discount: calculate discount based on pending amt to be discounted
            actualAmt = OB.DEC.sub(promotionAmt, previousAmt);
          }
          discountRule.set('qtyOffer', OB.DEC.toNumber(product.get('obdiscQty') || 1) * chunks);
          _.each(receiptProducts[l.get('product').id].lines, function (lp) {
            receipt.addPromotion(lp, discountRule, {
              amt: actualAmt,
              pack: true,
              chunks: chunks
            });
          });
        });
      }
      listener.trigger('completed');
    }, function () {
      window.console.error('Error querying for products', discountRule, receipt, line, arguments);
      listener.trigger('completed');
    });
  },

  addProductToOrder: function (order, productToAdd, attrs) {
    var criteria;
    var count;

    function errorCallback(error) {
      console.error('OBDAL error: ' + error, arguments);
    }

    OB.Dal.get(OB.Model.Discount, productToAdd.get('id'), function (pack) {
      if (pack.get('endingDate') && pack.get('endingDate').length > 0) {
        var objDate = new Date(pack.get('endingDate'));
        var now = new Date();
        var nowWithoutTime = new Date(now.toISOString().split('T')[0]);
        if (nowWithoutTime > objDate) {
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOS_PackExpired_header'), OB.I18N.getLabel('OBPOS_PackExpired_body', [pack.get('_identifier'), objDate.toLocaleDateString()]));
          return;
        }
      }
      criteria = {
        priceAdjustment: pack.get('id'),
        _orderByClause: '_identifier'
      };

      OB.Dal.find(OB.Model.DiscountFilterProduct, criteria, function (productsOfPromotion) {
        var addProductsAndCalculateDiscounts;
        addProductsAndCalculateDiscounts = function (products, index, callback, errorCallback) {
          if (index === products.size()) {
            return callback();
          }
          OB.Dal.get(OB.Model.Product, products.at(index).get('product'), function (product) {
            if (product) {
              order.addProduct(product, products.at(index).get('obdiscQty'), {
                belongsToPack: true,
                blockAddProduct: true
              }, attrs, function () {
                addProductsAndCalculateDiscounts(products, index + 1, callback, errorCallback);
              });
            }
          }, errorCallback);
        };
        order.set('skipApplyPromotions', true);
        addProductsAndCalculateDiscounts(productsOfPromotion, 0, function () {
          order.set('skipApplyPromotions', false);
          order.calculateReceipt();
        }, errorCallback);
      });
    }, function () {

    });
  }
};