/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global */

(function () {

  OB.UTIL.HookManager.registerHook('OBPOS_PreSaveCashManagements', function (args, callback) {
    function processDropsDeposits(dropsdeps, index) {
      if (index === dropsdeps.length) {
        OB.UTIL.HookManager.callbackExecutor(args, callback);
      } else {
        if (dropsdeps.at(index).get('extendedType') === 'GCNV_reimbursed') {
          OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.CloseGiftCard', {
            _executeInOneServer: true,
            _tryCentralFromStore: true,
            giftcard: dropsdeps.at(index).get('giftcard').id
          }, function (result) {
            dropsdeps.at(index).set('transactionId', result.transaction.id);
            OB.UI.GiftCardUtils.getPaymentMethodCashUp(dropsdeps.at(index).get('giftcard').type, function (payMthd) {
              var deps = payMthd.get('totalDeposits') + OB.DEC.div(dropsdeps.at(index).get('origAmount'), payMthd.get('rate'));
              payMthd.set('totalDeposits', deps);
              OB.UI.GiftCardUtils.updatePaymentMethodCashUp(payMthd, dropsdeps.at(index).get('giftcard').searchKey, "GiftCardCertificate.reimbursed", function () {
                processDropsDeposits(dropsdeps, index + 1);
              });
            });
          }, function () {
            OB.UI.GiftCardUtils.getPaymentMethodCashUp(dropsdeps.at(index).get('giftcard').type, function (payMthd) {
              var deps = payMthd.get('totalDeposits') + OB.DEC.div(dropsdeps.at(index).get('origAmount'), payMthd.get('rate'));
              payMthd.set('totalDeposits', deps);
              OB.UI.GiftCardUtils.updatePaymentMethodCashUp(payMthd, dropsdeps.at(index).get('giftcard').searchKey, "GiftCardCertificate.reimbursed", function () {
                processDropsDeposits(dropsdeps, index + 1);
              });
            });
          });
        } else {
          processDropsDeposits(dropsdeps, index + 1);
        }
      }
    }
    processDropsDeposits(args.dropsdeps, 0);
  });

}());