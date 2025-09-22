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
  OB.UTIL.HookManager.registerHook('OBPOS_PreDeleteLine', function (args, callbacks) {
    var receipt = args.order,
        selectedReceiptLines = args.selectedLines,
        deleteGiftCardCount = 0,
        transactionList = [];
    _.each(selectedReceiptLines, function (line) {
      if (line.get('product').get('giftCardTransaction')) {
        deleteGiftCardCount++;
      }
    });

    if (selectedReceiptLines.length !== deleteGiftCardCount) {
      receipt.get('lines').each(function (line) {
        var transaction = line.get('product').get('giftCardTransaction');
        if (transaction) {
          transactionList.push(transaction);
        }
      });
      if (transactionList.length > 0) {
        OB.UTIL.showConfirmation.display('', OB.I18N.getLabel('GCNV_RemoveReceiptLineWithGiftCard'));
        args.cancelOperation = true;
      }
    }
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    return;
  });
}());