/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, _ */

(function () {
  OB.UTIL.HookManager.registerHook('OBPOS_CheckReceiptMandatoryFields', function (args, callbacks) {
    var orders = args.orders;
    var removeOrderList = _.filter(orders, function (order) {
      var isGiftCardProduct = false;
      _.each(order.get('lines').models, function (l) {
        if (l.get('product').get('gcnvGiftcardtype') === 'G' && _.isUndefined(l.get('giftcardid'))) {
          isGiftCardProduct = true;
          return;
        }
      });
      if (isGiftCardProduct) {
        return true;
      }
    });
    args.removeOrderList = removeOrderList;
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    return;
  });
}());