/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, _ */


(function () {

  // Register event to show dialog if a gift card...
  OB.OBPOSPointOfSale.UI.PointOfSale.prototype.classModel.on('removedLine', function (instwindow, line) {
    var transaction = line.get('product').get('giftCardTransaction');
    if (transaction) {
      // cancel transaction
      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.CancelGiftCardTransaction', {
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        transaction: transaction
      }, function (result) {
        OB.UTIL.showSuccess(OB.I18N.getLabel('GCNV_MsgGiftCardCancelled'));
      }, function (error) {
        // FAIL
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        instwindow.doShowPopup({
          popup: 'GCNV_UI_Message',
          args: {
            message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
          }
        });
      });
    }
  }, this);

  OB.UTIL.HookManager.registerHook('OBPOS_PreDeleteCurrentOrder', function (args, callbacks) {
    var receipt = args.receipt,
        transactionList = [];
    receipt.get('lines').each(function (line) {
      var transaction = line.get('product').get('giftCardTransaction');
      if (transaction) {
        transactionList.push(transaction);
      }
    });
    _.each(_.uniq(transactionList), function (transaction) {
      // cancel transaction
      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.CancelGiftCardTransaction', {
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        transaction: transaction
      }, function (result) {
        OB.UTIL.showSuccess(OB.I18N.getLabel('GCNV_MsgGiftCardCancelled'));
      }, function (error) {
        // FAIL
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        args.context.doShowPopup({
          popup: 'GCNV_UI_Message',
          args: {
            message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
          }
        });
      });
    });
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
  });
}());