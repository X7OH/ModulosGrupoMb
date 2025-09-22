/*
 ************************************************************************************
 * Copyright (C) 2016-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone, $, _  */

OB.UTIL.HookManager.registerHook('OBPOS_preApplyDiscountsHook', function (args, callbacks) {
  var context = args.context,
      discountsContainer = context.$.discountsContainer;
  if (context.$.checkOverride.checked) {
    context.order.set('orderManualPromotions', new Backbone.Collection());
    context.$.checkOverride.unCheck();
  }
  if (discountsContainer.allLinesDiscount) {
    args.cancelOperation = true;
    var promotionToApply = {},
        currentOrderLineModels;
    promotionToApply.rule = {};
    promotionToApply.rule.id = discountsContainer.model.get('id');
    promotionToApply.rule.userAmt = discountsContainer.amt;
    promotionToApply.rule.obdiscAllowmultipleinstan = discountsContainer.model.get('obdiscAllowmultipleinstan');
    promotionToApply.rule.isTotalDisc = discountsContainer.allLinesDiscount;
    promotionToApply.rule.applyNext = !context.$.checkOverride.checked;
    // Set the amt to discountRule
    discountsContainer.model.set('userAmt', discountsContainer.amt);
    promotionToApply.discountRule = discountsContainer.model;
    if (discountsContainer.allLinesDiscount && !discountsContainer.amt) {
      //Show a modal pop up with the error
      context.doShowPopup({
        popup: 'modalDiscountNeedQty'
      });
      return true;
    }
    //if total discount, Then check if there are existing manual discounts added. Else add discounts in order.
    var manualPromotionsList = context.order.get('orderManualPromotions');
    if (manualPromotionsList) {
      _.each(manualPromotionsList, function (promotionObject) {
        if (promotionObject.get('rule').id === promotionToApply.rule.id) {
          manualPromotionsList.splice(manualPromotionsList.indexOf(promotionObject), 1);
        }
      });
      context.order.addManualPromotionToList(promotionToApply);
    } else {
      context.order.addManualPromotionToList(promotionToApply);
    }
    //if by Total discounts are applied with "override existing promotions", remove all promotions from all lines
    if (context.$.checkOverride.checked) {
      _.each(context.order.get('lines').models, function (lineModel) {
        lineModel.set('promotions', []);
      });
    }
    _.each(context.order.get('lines').models, function (lineModel) {
      if (lineModel.get('promotions')) {
        lineModel.get('promotions').forEach(function (promotion) {
          promotion.lastApplied = undefined;
        });
      }
    });
    // Recalculate all promotions again
    context.order.calculateReceipt(OB.UTIL.HookManager.callbackExecutor(args, callbacks));
  } else {
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
  }
});