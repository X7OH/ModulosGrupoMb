/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, _, Backbone */
OB.Model.Discounts.discountRules[OB.OBCOMBO.comboRuleId] = {
  async: true,

  isCandidate: function (discountRule, receipt, line, isCandidateCallback, isNotCandidateCallback) {
    var familyCriteria = { // Query local DB to know detail about the rule
      priceAdjustment: discountRule.id
    },
        isCandidateForNow = true;

    OB.Dal.findUsingCache('ComboFamilyQuery', OB.Model.ComboFamily, familyCriteria, function (families) {
      var familyCount = families.length;
      families.forEach(function (family) {
        var productCriteria = {
          obcomboFamily: family.id
        },
            numOfUnits = 0,
            line, i, productsInFamily = [];

        OB.Dal.findUsingCache('ComboProductQuery', OB.Model.ComboProduct, productCriteria, function (products) {
          var foundProduct = false;

          familyCount -= 1;

          products.forEach(function (product) {
            var currentLine;
            for (i = 0; i < receipt.get('lines').length; i++) {
              currentLine = receipt.get('lines').at(i);
              if (currentLine.get('product').id === product.get('product')) {
                numOfUnits += currentLine.get('qty');
              }
            }
          }); // end loop products in family
          if (family.get('quantity') > numOfUnits) {
            isCandidateForNow = false;
          }
          if (familyCount === 0) {
            if (isCandidateForNow) {
              isCandidateCallback();
            } else {
              isNotCandidateCallback();
            }

          }
        }, null, {
          modelsAffectedByCache: ['ComboProduct']
        }); // end find product
      }); // end loop family
    }, null, {
      modelsAffectedByCache: ['ComboFamily']
    }); // end find family
  },
  isCandidateFullReceipt: function (discountRule, combination, lineNo, isCandidateCallback, isNotCandidateCallback) {
    var familyCriteria = { // Query local DB to know detail about the rule
      priceAdjustment: discountRule.id
    },
        ruleId = discountRule.id,
        isCandidateForNow = true;

    OB.Dal.findUsingCache('ComboFamilyQuery', OB.Model.ComboFamily, familyCriteria, function (families) {
      var familyCount = families.length,
          theRuleId = ruleId,
          familyIdx;

      var findProduct = function (family, productCriteria, numOfUnits) {
          OB.Dal.findUsingCache('ComboProductQuery', OB.Model.ComboProduct, productCriteria, function (products) {
            var foundProduct = false;
            var ruleId = theRuleId;
            familyCount -= 1;

            products.forEach(function (product) {
              var currentLine, i;
              for (i = 0; i < combination.length; i++) {
                currentLine = combination[i].line.line;
                if (currentLine.get('product').id === product.get('product')) {
                  if (ruleId === combination[i].rule.get('id')) {
                    numOfUnits += currentLine.get('qty');
                  }
                }
              }
            }); // end loop products in family
            if (family.get('quantity') > numOfUnits) {
              isCandidateForNow = false;
            }
            if (familyCount === 0) {
              if (isCandidateForNow) {
                isCandidateCallback();
              } else {
                isNotCandidateCallback();
              }

            }
          }, null, {
            modelsAffectedByCache: ['ComboProduct']
          }); // end find product
          };

      for (familyIdx = 0; familyIdx < families.length; familyIdx++) {
        var family = families.at(familyIdx),
            productCriteria = {
            obcomboFamily: family.id
            },
            numOfUnits = 0,
            line, i, productsInFamily = [];

        findProduct(family, productCriteria, numOfUnits);
      } // end loop family
    }, null, {
      modelsAffectedByCache: ['ComboFamily']
    }); // end find family
  },

  implementation: function (discountRule, receipt, line, listener, candidates) {
    var lineCandidates = new Backbone.Collection(),
        promotionCandidates = line.get('promotionCandidates') || [],
        familyCriteria = { // Query local DB to know detail about the rule
        priceAdjustment: discountRule.id
        },
        applyingFamilies = [],
        missingFamilies = false,
        executionId, finalAmt, totalAmtCombo, totalComboAmt, lineNum, previousAmt;

    /**
     * Removes current discount rule from a given line
     */

    function removeThisCombo(line) {
      var promos = line.get('promotions'),
          newPromos = [];
      if (!promos) {
        return;
      }

      promos.forEach(function (p) {
        if (p.ruleId !== discountRule.id) {
          newPromos.push(p);
        }
      });

      line.set('promotions', newPromos);
    }

    /**
     * applyDiscount is invoked once we have selected the lines that
     * will participate in the combo
     */

    function applyDiscount() {
      var done = false,
          line, i, family, chunks, selectedLines, affectedLines = [],
          totalAmt, promotionAmt = 0,
          qty = BigDecimal.prototype.ZERO,
          offered = BigDecimal.prototype.ZERO,
          priority = discountRule.get('priority') || null,
          outOfComboAmt, selectedQty, completedFamily, canApply, unitsPerFamily, applyNTimes, totalQty, alreadyUsedPerProduct, completedArgs = null,
          applyingToLinesCollection = new Backbone.Collection(),
          applyingToLines;


      var isInvalidLine = function (line) {
          var invalidLine = false;
          _.forEach(line.get('promotions'), function (promotionApplied) {
            if (!promotionApplied.applyNext) {
              invalidLine = true;
            }
          });
          return invalidLine;
          };

      if (missingFamilies) {
        // nothing to do
        listener.trigger('completed');
        return;
      }

      discountRule.set('applyNext', false, {
        silent: true
      });


      canApply = true;
      selectedLines = [];

      receipt.get('lines').forEach(function (l) {
        var promotion, priorityLine, i, j;
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
      applyingToLines = receipt.checkAvailableUnitsPerLine(discountRule);

      // check how many times (chunks) the discount can be applied
      while (!done && canApply) {
        for (i = 0; i < applyingFamilies.length; i++) {
          totalQty = 0;
          completedFamily = false;
          family = applyingFamilies[i].family;

          while (!completedFamily) {
            var avoidLine = false;
            line = applyingFamilies[i].lines[applyingFamilies[i].position];
            if (line.get('promotions')) {
              var j, promoPriority = false,
                  promotions = line.get('promotions');
              for (j = 0; j < promotions.length; j++) {
                if (discountRule.get('priority') < promotions[j].priority) {
                  promoPriority = true;
                  break;
                }
              }
              if (!promoPriority) {
                avoidLine = isInvalidLine(line);
              }
            }
            if (!avoidLine) {
              totalQty += OB.UTIL.isNullOrUndefined(line.get('qtyAvailable')) ? line.get('qty') : line.get('qtyAvailable');

              selectedLines.push({
                family: family,
                receiptLine: line
              });
            }

            applyingFamilies[i].position += 1;

            if (applyingFamilies[i].position === applyingFamilies[i].lines.length) {
              completedFamily = true;
              done = true;

              if (totalQty < family.get('quantity')) {
                // we're done with all the products of a family but dind't get 
                // enough quantity
                canApply = false;
              }
            } else if (totalQty >= family.get('quantity')) {
              // All necessary products are satisfied
              completedFamily = true;
              done = true;
            }
          }
          applyNTimes = Math.floor(totalQty / family.get('quantity') || 1);
          if (!chunks || chunks > applyNTimes) {
            chunks = applyNTimes;
          }
        }
      }

      if (chunks !== 0 && canApply) {
        // for each line, set how many units participate in the discount
        unitsPerFamily = {};
        selectedLines.forEach(function (line) {
          var units, totalUnits, alreadyUserPerProduct, familyId = line.family.id,
              pendingUnits;
          totalUnits = line.family.get('quantity') * chunks;
          alreadyUsedPerProduct = unitsPerFamily[familyId] ? unitsPerFamily[familyId] : 0;
          pendingUnits = totalUnits - alreadyUsedPerProduct;
          if (line.receiptLine.get('qty') > pendingUnits) {
            line.units = pendingUnits;
          } else {
            line.units = line.receiptLine.get('qty');
          }
          unitsPerFamily[familyId] = (unitsPerFamily[familyId] ? unitsPerFamily[familyId] : 0) + line.units;
        });

        totalAmt = selectedLines.reduce(function (total, product) {
          var l = product.receiptLine,
              linePrice = 0,
              lineUnits = product.units;
          linePrice = l.get('discountedLinePrice') || l.get('price');
          if (lineUnits > 0) {
            total = OB.DEC.add(total, OB.DEC.mul(lineUnits, linePrice));
          }
          return total;
        }, OB.DEC.Zero);

        outOfComboAmt = selectedLines.reduce(function (total, product) {
          var l = product.receiptLine,
              comboAmt = 0,
              linePrice = 0,
              lineUnits = product.units,
              unitsOutOfCombo = OB.DEC.sub(l.get('qty'), lineUnits);
          if (lineUnits === 0) {
            linePrice = l.get('discountedLinePrice') || l.get('price');
            comboAmt = OB.DEC.add(comboAmt, OB.DEC.add(total, linePrice));
            totalComboAmt = OB.DEC.add((totalComboAmt || 0), comboAmt);
          }

          return (totalComboAmt || 0);
        }, OB.DEC.Zero);

        finalAmt = OB.DEC.mul(chunks, (discountRule.get('obcomboCombofixprice') || 0)) + outOfComboAmt;
        promotionAmt = totalAmt - finalAmt + outOfComboAmt;
        totalAmtCombo = totalAmt;

        var total = 0,
            familyCriteria = {
            priceAdjustment: discountRule.id
            };
        total = totalAmtCombo;

        lineNum = 0;
        previousAmt = 0;


        // finally, apply the discount
        _.forEach(selectedLines, function (line) {
          if (line.units === 0) {
            return; //continue
          }

          var family = line.family,
              receiptLine = line.receiptLine,
              discountType = family.get('discountType'),
              price = receiptLine.get('discountedLinePrice') || receiptLine.get('price'),
              lineUnits = line.units,
              discountAmt = 0;

          // split line, keeping in current one the units included within the combo
          // Issue 32972: from 16Q3, rule implementations don't split lines
          // if (receiptLine.get('qty') > line.units) {
          //   receipt.splitLine(receiptLine, line.units);
          // }
          if (OB.OBCOMBO.discountType.fixPrice === discountType) {
            discountAmt = OB.DEC.mul(OB.DEC.sub(price, family.get('fixedPrice')), line.units);
            if (!discountRule.get('obcomboAllowinclineprice') && price < family.get('fixedPrice')) {
              discountAmt = 0;
            }
          } else if (OB.OBCOMBO.discountType.percentage === discountType) {
            discountAmt = OB.DEC.mul(OB.DEC.div(OB.DEC.toBigDecimal(price).multiply(OB.DEC.toBigDecimal(family.get('percentage'))), 100), line.units);
          } else if (OB.OBCOMBO.discountType.fixDiscount === discountType) {
            discountAmt = OB.DEC.mul(family.get('fixedDiscount'), line.units);
          } else if (OB.OBCOMBO.comboFixPrice.discountType.comboFixPrice === discountType) {
            lineNum += 1;
            if (lineNum < selectedLines.length) {
              discountAmt = OB.DEC.mul(OB.DEC.div(OB.DEC.mul(promotionAmt, OB.DEC.mul(price, chunks)), (total)), lineUnits);
              previousAmt += discountAmt;
            } else {
              discountAmt = OB.DEC.sub(promotionAmt, previousAmt);
            }
            if (!discountRule.get('obcomboAllowinclineprice') && promotionAmt < 0) {
              discountAmt = 0;
            }
            //remove before added comboFixPrice promotions
            var removeDiscountRule = {
              id: null
            };
            _.each(receiptLine.get('promotions'), function (p) {
              if (p.discountType === discountRule.get('discountType')) {
                removeDiscountRule.id = p.ruleId;
              }
            });
            receipt.removePromotion(receiptLine, removeDiscountRule);
          } else {
            window.console.error('Discount type for combo not implemented', discountType);
          }

          affectedLines.push(receiptLine);
          discountRule.set('qtyOffer', line.units);
          receipt.addPromotion(receiptLine, discountRule, {
            amt: discountAmt,
            pack: true,
            chunks: chunks
          });
        });
      } else {
        completedArgs = {
          alerts: OB.I18N.getLabel('OBPOS_DiscountAlert', [line.get('product').get('_identifier'), discountRule.get('printName') || discountRule.get('name')])
        };
      }


      // for all the lines that are not affected by current combo, remove it
      // this can happen in case combo was previously applied and now is rearranged
      // Issue 32972: This is no longer necessary
      // receipt.get('lines').forEach(function (l) {
      //   if (l.isAffectedByPack() && l.isAffectedByPack().ruleId === discountRule.get('id') && affectedLines.indexOf(l) === -1) {
      //     removeThisCombo(l);
      //   }
      // });
      listener.trigger('completed', completedArgs);
    }

    executionId = new Date().valueOf();
    OB.debug('calculate combo', line);

    // This line is candidate for this promotion
    if (promotionCandidates.indexOf(discountRule.id) === -1) {
      promotionCandidates.push(discountRule.id);
    }
    line.set('promotionCandidates', promotionCandidates);

    if (line.get('addedBySplit')) {
      line.set('addedBySplit', false, {
        silent: true
      });
    }
    // look for other lines that are candidates to apply this same rule
    receipt.get('lines').forEach(function (l) {
      if (l.get('promotionCandidates')) {
        l.get('promotionCandidates').forEach(function (candidateRule) {
          if (candidateRule === discountRule.id) {
            lineCandidates.add(l);
          }
        });
      }
    });
    OB.Dal.findUsingCache('ComboFamilyQuery', OB.Model.ComboFamily, familyCriteria, function (families) {
      var familyCount = families.length;
      families.forEach(function (family) {
        var productCriteria = {
          obcomboFamily: family.id
        },
            line, i, productsInFamily = [];
        if (missingFamilies) {
          // already checked and cannot be applied
          return;
        }

        OB.Dal.findUsingCache('ComboProductQuery', OB.Model.ComboProduct, productCriteria, function (products) {
          var foundProduct = false;

          familyCount -= 1;

          products.forEach(function (product) {
            var currentLine;
            for (i = 0; i < lineCandidates.length; i++) {
              currentLine = lineCandidates.at(i);
              if (currentLine.get('product').id === product.get('product')) {
                productsInFamily.push(currentLine);
                foundProduct = true;
              }
            }
          }); // end loop products in family
          if (!foundProduct) {
            missingFamilies = true;
          }

          productsInFamily.sort(function (a, b) {
            // sort by product price desc
            return b.get('price') > a.get('price');
          });

          // for each of the families that participate in the combo, we keep a
          // list of all the lines with products in that family sorted by price
          // desc
          applyingFamilies.push({
            family: family,
            lines: productsInFamily,
            position: 0
          });

          if (familyCount === 0 || missingFamilies) {
            // we're done with the checking, apply discount if possible
            // and finish
            applyDiscount();


          }
        }, null, {
          modelsAffectedByCache: ['ComboProduct']
        }); // end find product
      }); // end loop family
    }, null, {
      modelsAffectedByCache: ['ComboFamily']
    }); // end find family
  },

  /**
   * Adding combo from Product browser
   */
  addProductToOrder: function (order, productToAdd) {
    var popupInstance = OB.POS.terminal.$.containerWindow.getRoot().$.OBCOMBO_Popup;
    // TODO: use getRoot instead of $.pointOfSale
    OB.POS.terminal.$.containerWindow.getRoot().doShowPopup({
      popup: 'OBCOMBO_Popup',
      args: {
        comboId: productToAdd.id,
        receipt: order
      }
    });

    popupInstance.loadFamilies(productToAdd);
  }
};
OB.Model.Discounts.discountRules[OB.OBCOMBO.comboFixPrice.comboRuleId] = OB.Model.Discounts.discountRules[OB.OBCOMBO.comboRuleId];