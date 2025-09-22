/*
 ************************************************************************************
 * Copyright (C) 2016-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global, Backbone, _ */

//Render of Options in Discount List
(function () {
  OB.UTIL.HookManager.registerHook('OBPOS_preDiscountChangeHook', function (args, callbacks) {
    var discountsContainer = args.discountsContainer;
    var rule = OB.Model.Discounts.discountRules[discountsContainer.model.get('discountType')];
    discountsContainer.allLinesDiscount = rule.allLinesDiscount;
    discountsContainer.requiresQty = !rule.isFixed;
    if (rule.allLinesDiscount) {
      args.hideLineSelectionOptions = true;
    }
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
  });

  //ticket lines checked, enable only if discount is not a total Discount
  var DiscountsUIPrototype = OB.OBPOSPointOfSale.UI.Discounts.prototype;
  var discountsUITicketLineChecked = DiscountsUIPrototype.ticketLineChecked;

  DiscountsUIPrototype.ticketLineChecked = _.wrap(DiscountsUIPrototype.ticketLineChecked, function (wrapped, inSender, inEvent) {
    var discountsContainer = this.$.discountsContainer;
    if (!discountsContainer.allLinesDiscount) {
      (_.bind(discountsUITicketLineChecked, this, inSender, inEvent))();
    }
  });
}());