/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, _, $ */


(function () {

  OB.UTIL.HookManager.registerHook('OBMOBC_TerminalLoaded', function (args, callback) {
    var giftvoucherButton = OB.OBPOSPointOfSale.UI.ToolbarPayment.prototype.sideButtons.find(function (sideButton) {
      return sideButton && sideButton.command === 'PaymentGiftVoucher';
    });

    if (OB.UTIL.isNullOrUndefined(giftvoucherButton) && OB.MobileApp.model.get('terminal').terminalType.gcnvShowgiftcardBtn) {
      OB.OBPOSPointOfSale.UI.ToolbarPayment.prototype.sideButtons.push({
        command: 'PaymentGiftVoucher',
        i18nLabel: 'GCNV_BtnPayGiftVoucher',
        permission: 'GCNV_PaymentGiftVoucher',
        stateless: true,
        action: function (keyboard, txt) {
          if (OB.GCNV.usingStandardMode === false) {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('GCNV_UnsupportedPaymentMethod'), OB.I18N.getLabel('GCNV_multiorders_gv_notallowed'));
            return;
          }
          keyboard.doShowPopup({
            popup: 'GCNV_UI_ModalGiftCards',
            args: {
              keyboard: keyboard,
              header: OB.I18N.getLabel('GCNV_LblDialogGiftVoucher'),
              giftcardtype: 'BasedOnVoucher',
              // Gift Voucher
              action: function (dialog, consumeOK, consumeFail) {
                var successCallback = function () {
                    consumeOK();
                    },
                    errorCallback = function (errorMessage) {
                    consumeFail(null, errorMessage);
                    };
                // Pay with a gift voucher
                OB.UI.GiftCardUtils.checkIfExpiredVoucherAndConsume(keyboard, keyboard.receipt, dialog.args.giftcard.searchKey, successCallback, errorCallback, {
                  cardType: 'V'
                });
              }
            }
          });
        }
      });
    }
    OB.UTIL.HookManager.callbackExecutor(args, callback);
  });

}());