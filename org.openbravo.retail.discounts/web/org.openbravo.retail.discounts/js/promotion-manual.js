/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, _, enyo, BigDecimal*/

// Manual discounts
(function () {
  function add(receipt, line, promotion) {
    var definition = promotion.definition,
        price, pctg, discPrice, unitDiscount, oldDiscountedLinePrice;
    definition.manual = true;
    definition._idx = -1;
    definition.lastApplied = true;
    if (definition.percentage) {
      price = line.get('price');
      price = OB.DEC.toBigDecimal(price);
      pctg = OB.DEC.toBigDecimal(definition.percentage);

      discPrice = price.multiply(new BigDecimal('100').subtract(pctg).divide(new BigDecimal('100'), 20, OB.DEC.getRoundingMode()));
      discPrice = new BigDecimal((String)(OB.DEC.toNumber(discPrice)));

      definition.amt = OB.DEC.mul(OB.DEC.abs(line.get('qty')), OB.DEC.toNumber(price.subtract(discPrice)));
    } else {
      definition.amt = definition.userAmt;

    }

    oldDiscountedLinePrice = !_.isNull(line.get('discountedLinePrice')) ? line.get('discountedLinePrice') : line.get('price');
    if (oldDiscountedLinePrice < (line.getQty() !== 0 ? OB.DEC.abs(OB.DEC.div(definition.amt, line.getQty())) : 0)) {
      definition.amt = OB.DEC.mul(oldDiscountedLinePrice, OB.DEC.abs(line.getQty()));
    }

    unitDiscount = line.get('qty') !== 0 ? OB.DEC.div(definition.amt, OB.DEC.abs(line.get('qty'))) : 0;
    // if Qty is negative, then the discount should be added instead of substract
    if (line.getQty() < BigDecimal.prototype.ZERO) {
      definition.amt = OB.DEC.mul(definition.amt, new BigDecimal("-1"));
    }
    line.set('discountedLinePrice', line.get('price') - unitDiscount);

    receipt.addPromotion(line, promotion.rule, definition);
  }

  function addPercentage(receipt, line, promotion) {
    promotion.definition.percentage = promotion.definition.userAmt;
    add(receipt, line, promotion);
  }
  // User Defined Amount
  OB.Model.Discounts.discountRules.D1D193305A6443B09B299259493B272A = {
    isManual: true,
    isFixed: false,
    isAmount: true,
    addManual: function (receipt, line, promotion) {
      add(receipt, line, promotion);
    },
    getAmountProperty: function () {
      return 'obdiscAmt';
    }
  };

  // Discretionary Discount Fixed Amount
  OB.Model.Discounts.discountRules['7B49D8CC4E084A75B7CB4D85A6A3A578'] = {
    isManual: true,
    isFixed: true,
    isAmount: true,
    addManual: function (receipt, line, promotion) {
      add(receipt, line, promotion);
    },
    getAmountProperty: function () {
      return 'obdiscAmt';
    }
  };

  // User Defined Percentage
  OB.Model.Discounts.discountRules['20E4EC27397344309A2185097392D964'] = {
    isManual: true,
    isFixed: false,
    isAmount: false,
    addManual: function (receipt, line, promotion) {
      addPercentage(receipt, line, promotion);
    },
    getIdentifier: function (rule, discount) {
      return (discount.name || rule.get('printName') || rule.get('name')) + ' - ' + discount.percentage + ' %';
    },
    getAmountProperty: function () {
      return 'obdiscPercentage';
    }
  };

  // Discretionary Discount Fixed Percentage
  OB.Model.Discounts.discountRules['8338556C0FBF45249512DB343FEFD280'] = {
    isManual: true,
    isFixed: true,
    isAmount: false,
    addManual: function (receipt, line, promotion) {
      addPercentage(receipt, line, promotion);
    },
    getAmountProperty: function () {
      return 'obdiscPercentage';
    }
  };

  OB.Model.Discounts.onLoadActions = OB.Model.Discounts.onLoadActions || [];
  OB.Model.Discounts.onLoadActions.push({
    execute: function () {

      /**
       * Checks if approval is required to pay this ticket. If required, a popup is shown requesting it.
       */
      OB.UTIL.HookManager.registerHook('OBPOS_CheckPaymentApproval', function (args, callbacks) {
        // Checking if applied discretionary discounts require approval
        var discretionaryDiscountTypes = OB.Model.Discounts.getManualPromotions(true),
            discountsToCheck = [],
            requiresApproval = false,
            i, callback, approvalType = 'OBPOS_approval.discounts';

        callback = function () {
          OB.UTIL.HookManager.callbackExecutor(args, callbacks);
          return;
        };

        if (OB.MobileApp.model.hasPermission(approvalType, true)) {
          // current user is a supervisor, no need to check further permissions
          callback();
          return;
        }
        // check discount approval already approved
        if (_.find(args.context.get('order').get('approvedList'), function (l) {
          return l.approvalType === approvalType;
        })) {
          callback();
          return;
        }

        args.context.get('order').get('lines').each(function (l) {
          var p, promotions;
          promotions = l.get('promotions');
          if (promotions) {
            for (p = 0; p < promotions.length; p++) {
              if (_.contains(discretionaryDiscountTypes, promotions[p].discountType) && !_.contains(discountsToCheck, promotions[p].ruleId)) {
                discountsToCheck.push(promotions[p].ruleId);
              }
            }
          }
        }, args.context);

        if (discountsToCheck.length > 0) {
          OB.Dal.find(OB.Model.Discount, {
            obdiscApprovalRequired: true
          }, enyo.bind(args.context, function (discountsWithApproval) {
            for (i = 0; i < discountsToCheck.length; i++) {
              if (discountsWithApproval.where({
                id: discountsToCheck[i]
              }).length > 0) {
                requiresApproval = true;
                break;
              }
            }
            if (requiresApproval) {
              args.approvals.push(approvalType);
            }
            callback();
          }));
        } else {
          callback();
          return;
        }
      });
    }
  });

}());