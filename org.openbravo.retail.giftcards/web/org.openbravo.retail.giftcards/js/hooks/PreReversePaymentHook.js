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
  OB.UTIL.HookManager.registerHook('OBPOS_PreAddReversalPayment', function (args, callbacks) {
    if (args.paymentToReverse.get('kind').indexOf('GCNV_payment.creditnote') === 0 || args.paymentToReverse.get('kind').indexOf('OBPOS_payment.giftcard') === 0) {

      var isReturn = args.receipt.getOrderType() === 1,
          receipt = args.receipt,
          payment = args.paymentToReverse,
          reversalPayment = args.reversalPayment,
          amount = payment.get('amount'),
          hasPaymentMethod;

      hasPaymentMethod = _.find(OB.MobileApp.model.get('payments'), function (payment) {
        return payment.payment.searchKey === args.paymentToReverse.get('kind');
      });

      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.ConsumeGiftCardAmount', {
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        paymentToReverse: payment.get("paymentId"),
        amount: OB.DEC.sub(0, amount),
        isReturn: isReturn,
        hasPaymentMethod: !OB.UTIL.isNullOrUndefined(hasPaymentMethod)
      }, function (result) {
        reversalPayment.set('transaction', result.transaction.id);
        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
      }, function (error) {
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        OB.MobileApp.view.$.containerWindow.getRoot().doShowPopup({
          popup: 'GCNV_UI_Message',
          args: {
            message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
          }
        });
        args.cancelOperation = true;
        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
      });
    } else {
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    }
  });

}());