/*
 ************************************************************************************
 * Copyright (C) 2012-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, _, $, GCNV */


(function () {

  var isGiftCard;

  OB.OBGCNE = OB.OBGCNE || {};
  OB.OBGCNE.Utils = OB.OBGCNE.Utils || {};
  OB.OBGCNE.Utils.PrintCreditNoteTemplate = '../org.openbravo.retail.giftcards/res/creditnote.xml';

  var findProductModel = function (id, callback) {
      var criteria;
      if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
        criteria = {
          'id': id
        };
      } else {
        criteria = {};
        var remoteCriteria = [];
        var productId = {
          columns: ['id'],
          operator: 'equals',
          value: id,
          isId: true
        };
        remoteCriteria.push(productId);
        criteria.remoteFilters = remoteCriteria;
      }

      OB.Dal.find(OB.Model.Product, criteria, function successCallbackProducts(dataProducts) {
        if (dataProducts && dataProducts.length > 0) {
          callback(dataProducts.at(0));
        } else {
          callback(null);
        }
      }, function errorCallback(tx, error) {
        OB.UTIL.showError("OBDAL error: " + error);
        callback(null);
      });
      };

  var findGiftCardModel = function (id, callback) {
      var criteria;
      if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
        criteria = {
          'id': id
        };
      } else {
        criteria = {};
        var remoteCriteria = [],
            productId = {
            columns: ['id'],
            operator: 'equals',
            value: id,
            isId: true
            };
        remoteCriteria.push(productId);
        criteria.remoteFilters = remoteCriteria;
      }
      OB.Dal.find(OB.Model.GiftCard, criteria, function successCallbackGiftCard(dataGiftCard) {
        if (dataGiftCard && dataGiftCard.length > 0) {
          callback(dataGiftCard.at(0));
        } else {
          callback(null);
        }
      }, function errorCallback(tx, error) {
        OB.UTIL.showError("OBDAL error: " + error);
        callback(null);
      });
      };

  var service = function (source, dataparams, callback, callbackError) {
      var process = new OB.DS.Process(source);
      process.exec(dataparams, function (data) {
        if (data && data.exception) {
          callbackError(data);
        } else if (data) {
          callback(data);
        }
      }, callbackError);
      };

  var cancelGiftCard = function (keyboard, receipt, card, success, fail) {

      if (!receipt.get('isEditable')) {
        keyboard.doShowPopup({
          popup: 'modalNotEditableOrder'
        });
        if (fail) {
          fail();
        }
        return;
      }

      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.CancelGiftCard', {
        giftcard: card,
        _tryCentralFromStore: true,
        _executeInOneServer: true
      }, function (result) {
        OB.UI.GiftCardUtils.findProductModel(result.product.id, function (transactionproduct) {

          // Add properties to product.
          transactionproduct.set('giftCardTransaction', result.transaction.id);
          transactionproduct.set('isEditablePrice', false);
          transactionproduct.set('isEditableQty', false);
          transactionproduct.set('standardPrice', result.transaction.amount);
          transactionproduct.set('ignorePromotions', true);

          keyboard.doAddProduct({
            product: transactionproduct,
            ignoreStockTab: true
          });
        });
        if (success) {
          success();
        }
      }, function (error) {
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        keyboard.doShowPopup({
          popup: 'GCNV_UI_Message',
          args: {
            message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
          }
        });
        if (fail) {
          fail();
        }
      });

      };

  var consumeGiftCardAmount = function (keyboard, receipt, card, amount, includingTaxes, success, fail) {

      if (!receipt.get('isEditable') && !receipt.get('cancelLayaway') && card.type !== 'BasedOnGLItem') {
        keyboard.doShowPopup({
          popup: 'modalNotEditableOrder'
        });
        if (fail) {
          fail();
        }
        return;
      }

      // For verified returns and line returns the total amount of the line where gift card is consumed must be negative and the transaction in the ERP also will be negative
      // For classic returns (Return this receipt) the total amount of the line where gift card is consumed (in this case we are adding money instead of consume) should be positive, however the transaction in the ERP should be negative (we are increasing the balance) so this value will be processed in the backend to create a proper transaction and then proccessed back again in the client to have the desired value (negative)
      var isMultiOrder = false,
          isReturn = false;
      if (receipt.get('multiOrdersList')) {
        isMultiOrder = true;
      } else if (receipt.getOrderType() === 1) {
        isReturn = true;
      }

      if (receipt.getPaymentStatus().isNegative && !isMultiOrder && !isReturn) {
        amount = -1 * amount;
      }

      var payment = _.find(receipt.get('payments').models, function (p) {
        return p.get('kind') === 'OBPOS_payment.giftcard:' + card.searchKey;
      });

      var giftCardPaymentMethod = _.find(OB.MobileApp.model.get('payments'), function (payment) {
        return payment.payment.searchKey === 'OBPOS_payment.giftcard';
      });

      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.ConsumeGiftCardAmount', {
        giftcard: card.searchKey,
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        amount: amount,
        isReturn: isReturn,
        transaction: payment ? payment.get('transaction') : null,
        hasPaymentMethod: giftCardPaymentMethod !== undefined && giftCardPaymentMethod !== null
      }, function (result) {

        if (result.product) {
          OB.UI.GiftCardUtils.findProductModel(result.product.id, function (transactionproduct) {

            // Add properties to product.
            transactionproduct.set('giftCardTransaction', result.transaction.id);
            transactionproduct.set('isEditablePrice', false);
            transactionproduct.set('isEditableQty', false);
            transactionproduct.set('standardPrice', (result.transaction.amount * (isReturn ? 1 : -1)));
            transactionproduct.set('ignorePromotions', true);
            transactionproduct.set('currentamt', result.currentamt);

            keyboard.doAddProduct({
              product: transactionproduct,
              ignoreStockTab: true,
              callback: function (success) {
                if (success === false) {
                  OB.UI.GiftCardUtils.cancelGiftCardTransaction(keyboard, result.transaction.id);
                }
              }
            });
          });
        } else {
          var p = _.find(OB.MobileApp.model.get('payments'), function (payment) {
            return result.paymentMethod.id === payment.paymentMethod.paymentMethod;
          });

          // Add Payment
          var modelToApply;
          if (keyboard.model.get('leftColumnViewManager').isOrder()) {
            modelToApply = keyboard.model.get('order');
          } else {
            modelToApply = keyboard.model.get('multiOrders');
          }
          modelToApply.addPayment(new OB.Model.PaymentLine({
            kind: p.payment.searchKey,
            paymentData: {
              card: card.searchKey,
              voidTransaction: function (callback) {
                callback(false, null);
              },
              voidConfirmation: false
            },
            name: OB.I18N.getLabel('GCNV_LblGiftCardsCertificate') + ' ' + card.searchKey,
            amount: OB.DEC.abs(result.realamnt),
            rate: p.rate,
            mulrate: p.mulrate,
            isocode: p.isocode,
            isCash: p.paymentMethod.isCash,
            allowOpenDrawer: p.paymentMethod.allowOpenDrawer,
            openDrawer: p.paymentMethod.openDrawer,
            printtwice: p.paymentMethod.printtwice,
            transaction: result.transaction.id
          }));
        }
        if (success) {
          success();
        }
      }, function (error) {
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        if (fail) {
          var errorMessage = {
            message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1)),
            keyboard: keyboard
          };
          fail(errorMessage);
        }
      });
      };

  var checkIfExpiredGiftCardAndConsume = function (keyboard, receipt, card, amount, includingTaxes, success, fail) {
      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.FindGiftCard', {
        giftcard: card.searchKey,
        giftcardtype: 'G'
      }, function (result) {
        if (result && result.data && result.data.obgcneExpirationdate && OB.OBGCNE.Utils.isInThePast(result.data.obgcneExpirationdate)) {
          var errorMessage = {
            message: OB.I18N.getLabel('OBGCNE_GiftCardExpired', [card.searchKey]),
            keyboard: keyboard
          };
          fail(errorMessage);
        } else {
          consumeGiftCardAmount(keyboard, receipt, card, amount, includingTaxes, success, fail);
        }
      }, function (error) {
        consumeGiftCardAmount(keyboard, receipt, card, amount, includingTaxes, success, fail);
      });
      };

  var consumeGiftCard = function (keyboard, amount, giftCardType) {
      var header = giftCardType === 'BasedOnProductGiftCard' ? 'GCNV_HeaderGiftCard' : 'GCNV_HeaderGiftCertificate';
      keyboard.doShowPopup({
        popup: 'GCNV_UI_ModalGiftCards',
        args: {
          keyboard: keyboard,
          header: OB.I18N.getLabel(header, [OB.I18N.formatCurrency(amount)]),
          giftcardtype: giftCardType,
          amount: amount,
          // Gift Card
          action: function (dialog, consumeOK, consumeFail) {
            var successCallback = function () {
                consumeOK();
                },
                errorCallback = function (errorMessage) {
                consumeFail(null, errorMessage);
                };
            // Pay with a gift card
            OB.UI.GiftCardUtils.checkIfExpiredGiftCardAndConsume(keyboard, keyboard.receipt, dialog.args.giftcard, amount, keyboard.receipt.get('priceIncludesTax'), successCallback, errorCallback);
          }
        }
      });
      };

  var consumeCreditNoteAmount = function (keyboard, receipt, card, amount, paymentToAdd, success, fail) {

      var isMultiOrder = false,
          isReturn = false;
      if (receipt.get('multiOrdersList')) {
        isMultiOrder = true;
      } else if (receipt.getOrderType() === 1) {
        isReturn = true;
      }

      if (receipt.getPaymentStatus().isNegative && !isMultiOrder && !isReturn) {
        amount = -1 * amount;
      }

      var payment = _.find(receipt.get('payments').models, function (p) {
        return p.get('kind') === 'GCNV_payment.creditnote:' + card;
      });

      var giftCardPaymentMethod = _.find(OB.MobileApp.model.get('payments'), function (payment) {
        return payment.payment.searchKey === 'GCNV_payment.creditnote';
      });

      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.ConsumeGiftCardAmount', {
        giftcard: card,
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        amount: amount,
        isReturn: isReturn,
        transaction: payment ? payment.get('transaction') : null,
        hasPaymentMethod: giftCardPaymentMethod !== undefined && giftCardPaymentMethod !== null
      }, function (result) {

        var p = null,
            newPayment = new Backbone.Model();
        if (giftCardPaymentMethod !== undefined && giftCardPaymentMethod !== null) {
          p = giftCardPaymentMethod;
        } else {
          p = _.find(OB.MobileApp.model.get('payments'), function (payment) {
            return result.paymentMethod.id === payment.paymentMethod.paymentMethod;
          });
        }

        // Add Payment
        var modelToApply, paymentData = paymentToAdd.get('paymentData') || {};
        if (keyboard.model.get('leftColumnViewManager').isOrder()) {
          modelToApply = keyboard.model.get('order');
        } else {
          modelToApply = keyboard.model.get('multiOrders');
        }
        OB.UTIL.clone(paymentToAdd, newPayment);
        paymentData.card = card;
        paymentData.voidTransaction = function (callback) {
          callback(false, null);
        };
        paymentData.voidConfirmation = false;
        newPayment.set('name', OB.MobileApp.model.getPaymentName(p.payment.searchKey) + ' ' + card);
        newPayment.set('amount', OB.DEC.abs(result.realamnt));
        newPayment.set('paymentData', paymentData);
        newPayment.set('transaction', result.transaction.id);
        modelToApply.addPayment(new OB.Model.PaymentLine(newPayment.attributes));
        if (success) {
          success();
        }
      }, function (error) {
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        var errorMessage = {
          message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1)),
          keyboard: keyboard
        };
        if (fail) {
          fail(errorMessage);
        }
      });
      };

  var consumeGiftCardLines = function (keyboard, receipt, card, success, fail, options) {

      if (!receipt.get('isEditable')) {
        keyboard.doShowPopup({
          popup: 'modalNotEditableOrder'
        });
        if (fail) {
          fail();
        }
        return;
      }

      // Calculate the vouchers already used in the lines.
      var currentvouchers = {};
      _.each(receipt.get('lines').models, function (line) {
        var giftvoucherproduct = line.get('product').get('giftVoucherProduct');
        if (giftvoucherproduct) {
          var giftvoucherqty = line.get('product').get('giftVoucherQuantity');
          var voucherinfo = currentvouchers[giftvoucherproduct];
          if (!voucherinfo) {
            voucherinfo = {
              product: giftvoucherproduct,
              quantity: 0
            };
            currentvouchers[giftvoucherproduct] = voucherinfo;
          }
          voucherinfo.quantity += giftvoucherqty;
        }
      }, this);

      var lines = [];
      _.each(receipt.get('lines').models, function (line) {
        var product = line.get('product').get('id');
        var quantity = line.get('qty');
        var promotions = line.get('promotions');
        var discountedPrices = 0;
        var linePrice = 0;
        var voucherinfo = currentvouchers[product];
        if (voucherinfo) {
          quantity -= voucherinfo.quantity;
        }
        _.each(promotions, function (promotion) {
          discountedPrices = discountedPrices + promotion.amt;
        });
        linePrice = (line.get('price') * quantity - discountedPrices) / quantity;
        lines.push({
          product: product,
          quantity: quantity,
          price: linePrice
        });
      }, this);

      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.ConsumeGiftCardLines', {
        giftcard: card,
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        lines: lines,
        isReturn: receipt.get('documentType') === OB.MobileApp.model.get('terminal').terminalType.documentTypeForReturns
      }, function (result) {
        var data = result.data;
        if (data && (data.exception || data.length === 0)) {
          var params = {
            message: OB.I18N.getLabel('GCNV_ErrorGiftVoucherNotApplied')
          };
          if (options && options.cardType) {
            if (options.cardType === 'V') {
              params.header = OB.I18N.getLabel('GCNV_LblDialogGiftVoucher');
            }
          }
          keyboard.doShowPopup({
            popup: 'GCNV_UI_Message',
            args: params
          });
        } else {
          _.each(data, function (item) {
            OB.UI.GiftCardUtils.findProductModel(item.product.id, function (transactionproduct) {
              // Add properties to product.
              if (item.transaction.quantity < 0) {
                //is a return but we need to add line as usual
                item.transaction.quantity = item.transaction.quantity * -1;
              }
              transactionproduct.set('giftCardTransaction', item.transaction.id);
              transactionproduct.set('giftVoucherProduct', item.transaction.product);
              transactionproduct.set('giftVoucherQuantity', item.transaction.quantity);
              transactionproduct.set('isEditablePrice', false);
              transactionproduct.set('isEditableQty', false);
              transactionproduct.set('standardPrice', -item.price);
              transactionproduct.set('ignorePromotions', true);
              transactionproduct.set('avoidSplitProduct', true);

              var index = 0,
                  receiptlines = receipt.get('lines');

              while (index < receiptlines.length) {
                if (receiptlines.at(index++).get('product').get('id') === item.transaction.product) {
                  break;
                }
              }

              keyboard.doAddProduct({
                product: transactionproduct,
                qty: item.transaction.quantity,
                ignoreStockTab: true,
                options: {
                  at: index
                }
              });
            });
          }, this);
        }

        if (success) {
          success();
        }
      }, function (error) {
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        var errorMessage = {
          message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1)),
          keyboard: keyboard
        };
        if (options && options.cardType) {
          if (options.cardType === 'V') {
            errorMessage.header = OB.I18N.getLabel('GCNV_LblDialogGiftVoucher');
            errorMessage.message = OB.I18N.getLabel('GCNV_ErrorVoucherNotFound', [msgsplit.slice(1)]);
          }
        }

        if (fail) {
          fail(errorMessage);
        }
      });
      };

  var checkIfExpiredVoucherAndConsume = function (keyboard, receipt, card, success, fail, options) {
      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.FindGiftCard', {
        giftcard: card,
        giftcardtype: 'V'
      }, function (result) {
        if (result && result.data && result.data.obgcneExpirationdate && OB.OBGCNE.Utils.isInThePast(result.data.obgcneExpirationdate)) {
          var errorMessage = {
            message: OB.I18N.getLabel('OBGCNE_VoucherExpired', [card]),
            keyboard: keyboard
          };
          fail(errorMessage);
        } else {
          consumeGiftCardLines(keyboard, receipt, card, success, fail, options);
        }
      }, function (error) {
        consumeGiftCardLines(keyboard, receipt, card, success, fail, options);
      });
      };

  var cancelGiftCardTransaction = function (context, transaction) {
      // cancel transaction
      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.CancelGiftCardTransaction', {
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        transaction: transaction
      }, function (result) {
        // OK
      }, function (error) {
        // FAIL
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        context.doShowPopup({
          popup: 'GCNV_UI_Message',
          args: {
            message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
          }
        });
      });
      };

  var getPaymentMethodCashUp = function (searchKey, callback) {
      OB.Dal.find(OB.Model.CashUp, {
        'isprocessed': 'N'
      }, function (cashUp) {
        OB.Dal.find(OB.Model.PaymentMethodCashUp, {
          'cashup_id': cashUp.at(0).get('id'),
          'searchKey': searchKey === 'BasedOnCreditNote' ? 'GCNV_payment.creditnote' : 'OBPOS_payment.giftcard'
        }, function (payMthds) { //OB.Dal.find success
          callback(payMthds.at(0));
        });
      });
      };

  var updatePaymentMethodCashUp = function (payMthd, giftcardNumber, transMsg, callback) {
      OB.Dal.save(payMthd, function () {
        OB.Dal.transaction(function (tx) {
          OB.UTIL.calculateCurrentCash(null, tx);
        }, function () {
          // the transaction failed
          OB.error("[" + transMsg + "] The transaction failed to be commited. CardNumber: " + giftcardNumber);
        }, function () {
          // success transaction...
          OB.info("[" + transMsg + "] Transaction success. CardNumber: " + giftcardNumber);
          if (callback) {
            callback();
          }
        });
      });
      };

  OB.UI.GiftCardUtils = {
    findProductModel: findProductModel,
    findGiftCardModel: findGiftCardModel,
    service: service,
    cancelGiftCard: cancelGiftCard,
    cancelGiftCardTransaction: cancelGiftCardTransaction,
    consumeGiftCardAmount: consumeGiftCardAmount,
    checkIfExpiredGiftCardAndConsume: checkIfExpiredGiftCardAndConsume,
    consumeGiftCard: consumeGiftCard,
    consumeCreditNoteAmount: consumeCreditNoteAmount,
    consumeGiftCardLines: consumeGiftCardLines,
    checkIfExpiredVoucherAndConsume: checkIfExpiredVoucherAndConsume,
    getPaymentMethodCashUp: getPaymentMethodCashUp,
    updatePaymentMethodCashUp: updatePaymentMethodCashUp
  };

  OB.OBGCNE.Utils.isGiftCard = function (product, callback) {
    if (product.get('giftCardTransaction')) {
      // Is a giftcard product BUT as a discount payment
      callback(false);
    } else {
      var criteria;
      if (product.get('gcnvGiftcardtype')) {
        callback(true, product);
      } else {
        callback(false);
      }
    }
  };

  OB.OBGCNE.Utils.isInThePast = function (dateString) {
    var date, now;

    date = new Date(!OB.UTIL.isNullOrUndefined(dateString) ? OB.I18N.parseServerDate(dateString) : dateString);
    now = new Date();
    now.setHours(0);
    now.setMinutes(0);
    now.setSeconds(0);
    now.setMilliseconds(0);

    return date.getTime() < now.getTime();
  };

}());