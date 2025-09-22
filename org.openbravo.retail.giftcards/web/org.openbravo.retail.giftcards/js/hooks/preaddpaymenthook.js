/*
 ************************************************************************************
 * Copyright (C) 2017-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, _ */

(function () {
  OB.UTIL.HookManager.registerHook('OBPOS_preAddPayment', function (args, callbacks) {
    var order = args.receipt,
        i, gcids = [],
        serverCallCheckDuplicity = new OB.DS.Process('org.openbravo.retail.giftcards.CheckDuplicityOfID'),
        amount = args.paymentToAdd.get('amount'),
        transactionId = args.paymentToAdd.get('transaction');
    args.keyboard = OB.MobileApp.view.$.containerWindow.getRoot();
    args.keyboard.receipt = args.receipt;
    var collectGiftCards = function (order) {
        for (i = 0; i < order.get('lines').length; i++) {
          if (order.get('lines').at(i).get('product').get('giftCardType') === 'V' || order.get('lines').at(i).get('product').get('giftCardType') === 'G') {
            if (order.get('lines').at(i).get('giftcardid') === '') {
              OB.UTIL.showError(OB.I18N.getLabel('GCNV_LblGiftCardInvalid'));
              gcids = [];
              return;
            } else {
              gcids.push({
                gcid: order.get('lines').at(i).get('giftcardid')
              });
            }
          }
        }
        };
    if (order.get('multiOrdersList')) {
      order.get('multiOrdersList').each(collectGiftCards);
    } else {
      collectGiftCards(order);
    }
    //If paymentToAdd is Credit Note show popup
    var giftcardPopup = function () {
        if ('GCNV_payment.creditnote' === args.paymentToAdd.get('kind') && !transactionId && !args.paymentToAdd.get('reversedPaymentId')) {
          if (!args.receipt.getPaymentStatus().isNegative) {
            args.keyboard.doShowPopup({
              popup: 'GCNV_UI_ModalGiftCards',
              args: {
                keyboard: args.keyboard,
                header: OB.I18N.getLabel('GCNV_HeaderCreditNote', [OB.I18N.formatCurrency(amount)]),
                giftcardtype: 'BasedOnCreditNote',
                amount: amount,
                // Credit Note
                action: function (dialog, consumeOK, consumeFail) {
                  var successCallback = function () {
                      consumeOK(function () {
                        // success
                        args.cancellation = true;
                        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
                      });
                      },
                      errorCallback = function (errorMessage) {
                      consumeFail(null, errorMessage);
                      };
                  var paymentData = args.paymentToAdd.get('paymentData') || {};
                  paymentData.creditNote = {
                    searchKey: dialog.args.giftcard.searchKey,
                    businessPartnerId: dialog.args.giftcard.businessPartner,
                    businessPartnerName: dialog.args.giftcard.businessPartner$_identifier,
                    amount: dialog.args.giftcard.amount,
                    currentamount: dialog.args.giftcard.currentamount
                  };
                  args.paymentToAdd.set('paymentData', paymentData);
                  // Pay with a credit note
                  OB.UI.GiftCardUtils.consumeCreditNoteAmount(args.keyboard, args.receipt, dialog.args.giftcard.searchKey, amount, args.paymentToAdd, successCallback, errorCallback);
                }
              }
            });
          } else {
            var paymentData = args.paymentToAdd.get('paymentData') || {};
            paymentData.creditNote = {
              searchKey: args.receipt.get('documentNo'),
              businessPartnerId: args.receipt.get('bp').id,
              businessPartnerName: args.receipt.get('bp').get('_identifier'),
              amount: args.paymentToAdd.get('amount')
            };
            args.paymentToAdd.set('creditnoteId', args.receipt.get('documentNo'));
            args.paymentToAdd.set('paymentData', paymentData);
            OB.UTIL.HookManager.callbackExecutor(args, callbacks);
          }
        } else if ('OBPOS_payment.giftcard' === args.paymentToAdd.get('kind') && !transactionId) {
          OB.UI.GiftCardUtils.consumeGiftCard(args.keyboard, amount, 'BasedOnGLItem');
        } else {
          OB.UTIL.HookManager.callbackExecutor(args, callbacks);
        }
        };
    if (gcids.length > 0) {
      serverCallCheckDuplicity.exec({
        gcid: null,
        gcids: gcids
      }, function (data) {
        if (data && data.exception) {
          OB.UTIL.showError(data.exception.message);
        } else if (data && !data.exception) {
          if (data.used) { // if gc ID is in used by another gc
            OB.UTIL.showError(data.id + ' ' + OB.I18N.getLabel('GCNV_DuplicatedID'));
          } else {
            giftcardPopup();
          }
        }
      });
    } else {
      giftcardPopup();
    }
    return;

  });
}());