/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

(function () {

  function executeHookLogic(args, callbacks) {
    if (args.cancellation) {
      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.RevertGiftCardAmount', {
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        transaction: args.paymentToRem.get('transaction')
      }, function (result) {
        if (result && result.exception) {
          var msg = result.exception.message;
          if (msg.indexOf('GCNV_') === 0) {
            var msgsplit = msg.split(':');
            msg = OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1));
          }
          OB.MobileApp.view.waterfall('onShowPopup', {
            popup: 'GCNV_UI_Message',
            args: {
              message: msg
            }
          });
        } else if (result.success) {
          args.cancellation = false;
        }
        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
      }, function (error) {
        var msg = error.exception.message;
        if (msg.indexOf('GCNV_') === 0) {
          var msgsplit = msg.split(':');
          msg = OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1));
        }
        OB.MobileApp.view.waterfall('onShowPopup', {
          popup: 'GCNV_UI_Message',
          args: {
            message: msg
          }
        });
        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
      });
    } else {
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    }
  }

  OB.UTIL.HookManager.registerHook('OBPOS_preRemovePayment', function (args, callbacks) {
    if (args.cancellation) {
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    } else {
      args.cancellation = ((args.paymentToRem.get('kind') === 'OBPOS_payment.giftcard') ? true : (args.paymentToRem.get('kind') === 'GCNV_payment.creditnote' && !args.receipt.getPaymentStatus().isNegative && !args.paymentToRem.get('isPrePayment')));
      executeHookLogic(args, callbacks);
    }
  });

  OB.UTIL.HookManager.registerHook('OBPOS_preRemovePaymentMultiOrder', function (args, callbacks) {
    if (args.cancellation) {
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    } else {
      args.cancellation = ((args.paymentToRem.get('kind') === 'OBPOS_payment.giftcard') ? true : (args.paymentToRem.get('kind') === 'GCNV_payment.creditnote' && !args.paymentToRem.get('isPrePayment')));
      executeHookLogic(args, callbacks);
    }
  });
}());