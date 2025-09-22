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
    var giftcardButton = OB.OBPOSPointOfSale.UI.ToolbarPayment.prototype.sideButtons.find(function (sideButton) {
      return sideButton && sideButton.command === 'PaymentGiftCard';
    });

    if (OB.UTIL.isNullOrUndefined(giftcardButton) && OB.MobileApp.model.get('terminal').terminalType.gcnvShowgiftcardBtn) {
      OB.OBPOSPointOfSale.UI.ToolbarPayment.prototype.sideButtons.push({
        command: 'PaymentGiftCard',
        i18nLabel: 'GCNV_BtnPayGiftCard',
        permission: 'GCNV_PaymentGiftCard',
        stateless: false,
        action: function (keyboard, txt) {
          var amount = OB.DEC.number(OB.I18N.parseNumber(txt || ''));
          if (!OB.GCNV.usingStandardMode) {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('GCNV_UnsupportedPaymentMethod'), OB.I18N.getLabel('GCNV_multiorders_gc_notallowed'));
            return;
          }
          amount = _.isNaN(amount) ? keyboard.receipt.getPending() : amount;
          if (amount > keyboard.receipt.getPending()) {
            keyboard.doShowPopup({
              popup: 'GCNV_UI_Message',
              args: {
                message: OB.I18N.getLabel('GCNV_ErrorCannotConsumeAmount')
              }
            });
            return;
          }
          OB.UI.GiftCardUtils.consumeGiftCard(keyboard, amount, 'BasedOnProductGiftCard');
        }
      });
    }
    OB.UTIL.HookManager.callbackExecutor(args, callback);
  });

}());