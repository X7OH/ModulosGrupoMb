/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone, _ */

(function () {

  OB.UTIL.HookManager.registerHook('OBPOS_AddButtonToCashManagement', function (args, callbacks) {
    var cashMgmtGiftCardsEvents = OB.MobileApp.model.get('cashMgmtGiftCardsEvents'),
        reimburseEvent = _.find(cashMgmtGiftCardsEvents, function (event) {
        return event.eventType === 'GCNV_reimbursed';
      });

    if (reimburseEvent) {

      // Add a button GiftCard Reimbursed
      args.buttons.push({
        idSufix: 'GiftCard',
        command: 'OBPOS_payment.giftcard',
        label: OB.I18N.getLabel('GCNV_BtnGiftCardCashMng'),
        definition: {
          stateless: true,
          permission: 'OBPOS_PaymentGiftCard',
          action: function (keyboard, txt) {
            args.context.doShowPopup({
              popup: 'GCNV_UI_ModalGiftCards',
              args: {
                keyboard: args.context,
                header: OB.I18N.getLabel('GCNV_HeaderGiftCardCashMng'),
                notDefaultAction: true,
                giftcardtype: ['BasedOnCreditNote', 'BasedOnGLItem'],
                action: function (dialog, consumeOK, consumeFail) {
                  OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.FindGiftCard', {
                    giftcard: dialog.args.giftcard.searchKey
                  }, function (result) {
                    var giftcard = result.data,
                        criteria = {},
                        cashPayment;

                    var successCallback = function (successMessage) {
                        consumeOK(null, successMessage);
                        };

                    var errorCallback = function (errorMessage) {
                        consumeFail(null, errorMessage);
                        };

                    // Verify is closed
                    if (giftcard.alertStatus === "C") {
                      errorCallback({
                        message: 'GCNV_ErrorGiftCardClosed',
                        params: [giftcard.searchKey],
                        keyboard: args.context
                      });
                      return;
                    }

                    if (giftcard.type !== 'BasedOnCreditNote') {
                      // Verify is BasedOnGLItem
                      if (giftcard.type !== 'BasedOnGLItem') {
                        errorCallback({
                          message: 'GCNV_ErrorGiftCardNotCertificate',
                          params: [giftcard.searchKey],
                          keyboard: args.context
                        });
                        return;
                      }

                      // Verify it can be reimbursed
                      var reasonType = _.find(OB.MobileApp.model.get('gcnvGiftcardReason'), function (reason) {
                        return reason.id === giftcard.category;
                      });
                      if (!reasonType) {
                        errorCallback({
                          message: 'GCNV_ErrorGiftCardNotFoundCategory',
                          params: [giftcard.searchKey],
                          keyboard: args.context
                        });
                        return;
                      }
                      if (!reasonType.reimbursed) {
                        errorCallback({
                          message: 'GCNV_ErrorGiftCardNotReimbursed',
                          params: [giftcard.searchKey],
                          keyboard: args.context
                        });
                        return;
                      }
                    }

                    // Find Cash payment method for GiftCard payment method
                    var cashPayments = _.filter(OB.MobileApp.model.get('payments'), function (p) {
                      return p.paymentMethod.iscash && p.paymentMethod.allowdrops;
                    }),
                        cashupMng = args.context.owner.owner.owner;

                    function convertAmount(amount, fromCurrency, toCurrency) {
                      var converter = OB.UTIL.currency.findConverter(fromCurrency, toCurrency);
                      if (fromCurrency === toCurrency) {
                        return amount;
                      } else if (converter) {
                        return OB.DEC.mul(amount, converter.rate);
                      } else {
                        converter = OB.UTIL.currency.findConverter(fromCurrency, OB.UTIL.currency.webPOSDefaultCurrencyId());
                        if (converter) {
                          return OB.UTIL.currency.toForeignCurrency(toCurrency, converter.getFinancialAmountOf(amount));
                        } else {
                          return;
                        }
                      }
                    }

                    function getCurrentCashOf(payment) {
                      var cashmgmtPayment = cashupMng.model.get('payments').find(function (pm) {
                        return payment.payment.id === pm.get('paymentmethod_id');
                      });
                      if (cashmgmtPayment) {
                        return cashmgmtPayment.get('total');
                      } else {
                        return payment.currentCash;
                      }
                    }

                    var amount = giftcard.currentamount ? giftcard.currentamount : giftcard.amount;
                    var availablePaymentMethods = _.map(cashPayments, function (cp) {
                      return {
                        currency: cp.paymentMethod.currency,
                        availableCash: OB.UTIL.currency.toForeignCurrency(cp.paymentMethod.currency, getCurrentCashOf(cp)),
                        giftcardAmount: convertAmount(amount, giftcard.currency, cp.paymentMethod.currency),
                        payment: cp,
                        isDefaultPayment: giftcard.currency === cp.paymentMethod.currency
                      };
                    });

                    //Keep only Payment Methods with enough cash
                    availablePaymentMethods = availablePaymentMethods.filter(function (cur) {
                      return cur.availableCash >= cur.giftcardAmount;
                    });

                    if (availablePaymentMethods.length === 0) {
                      errorCallback({
                        message: 'GCNV_ErrorGiftCardNotCash',
                        params: [giftcard.searchKey],
                        keyboard: args.context
                      });
                      return;
                    }

                    var apm = availablePaymentMethods.find(function (pay) {
                      return pay.isDefaultPayment;
                    });
                    apm = apm || availablePaymentMethods[0];
                    cashPayment = apm.payment;

                    // Check if card are already reimbursed
                    var isError = false;
                    cashupMng.model.depsdropstosave.each(function (pay) {
                      if (pay.get('gcnvGiftCardId') === giftcard.id) {
                        isError = true;
                      }
                    });
                    if (isError) {
                      errorCallback({
                        message: 'GCNV_ErrorCashMngAlreadyReimbursed',
                        params: [giftcard.searchKey],
                        keyboard: args.context
                      });
                      return;
                    }

                    criteria.bpartner = giftcard.businessPartner;
                    if (OB.MobileApp.model.hasPermission('OBPOS_remote.customer', true)) {
                      var bPartnerId = {
                        columns: ['bpartner'],
                        operator: 'equals',
                        value: giftcard.businessPartner,
                        isId: true
                      };
                      var remoteCriteria = [bPartnerId];
                      criteria.remoteFilters = remoteCriteria;
                    }
                    OB.Dal.find(OB.Model.BPLocation, criteria, function (dataBps) {
                      if (dataBps.models[0]) {
                        giftcard.address = dataBps.models[0];
                      }
                      // Request approval
                      OB.UTIL.Approval.requestApproval(cashupMng.model, 'GCNV_PaymentGiftCardReimbursed', function (approved) {
                        if (approved) {
                          // Add to Cash Management
                          cashupMng.currentPayment = {
                            allowopendrawer: cashPayment.paymentMethod.allowopendrawer,
                            amount: apm.giftcardAmount,
                            destinationKey: cashPayment.payment.searchKey,
                            glItem: cashPayment.paymentMethod.gLItemForDrops,
                            id: cashPayment.payment.id,
                            identifier: cashPayment.payment._identifier,
                            iscash: true,
                            isocode: cashPayment.isocode,
                            rate: cashPayment.rate,
                            type: "drop",
                            defaultProcess: 'N',
                            extendedType: 'GCNV_reimbursed',
                            extendedProp: {
                              gcnvGiftCardId: giftcard.id,
                              giftcard: giftcard
                            }
                          };

                          cashupMng.model.depsdropstosave.trigger('paymentDone', new Backbone.Model({
                            id: reimburseEvent.id,
                            name: reimburseEvent.name
                          }), cashupMng.currentPayment);

                          successCallback({
                            message: 'GCNV_ReimbursedSuccess',
                            params: [OB.I18N.formatCurrencyWithSymbol(apm.giftcardAmount, cashPayment.symbol, cashPayment.currencySymbolAtTheRight)],
                            keyboard: args.context
                          });
                        }
                      });
                    }, function (tx, error) {
                      OB.UTIL.showError("OBDAL error: " + error);
                    });
                  }, function (error) {
                    var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
                    args.context.doShowPopup({
                      popup: 'GCNV_UI_Message',
                      args: {
                        message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
                      }
                    });
                  });
                }
              }
            });
          }
        }
      });
    }
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
  });

}());