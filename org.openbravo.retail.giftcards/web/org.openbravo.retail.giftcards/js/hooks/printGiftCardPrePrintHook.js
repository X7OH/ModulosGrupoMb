/*
 ************************************************************************************
 * Copyright (C) 2015-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, _, $ */


(function () {

  var printGiftCards, printPaymentGiftCards;

  printGiftCards = function (args, iteration, callbacks) {

    if (args.order.get('lines').models.length - 1 >= iteration) {
      var line = args.order.get('lines').models[iteration],
          gctemplateresource, giftCardData = new Backbone.Model();

      if (line.get('giftcardid') && line.get('product').get('printCard') && line.get('product').get('printTemplate')) {
        gctemplateresource = new OB.DS.HWResource(line.get('product').get('printTemplate'));

        // Set properties to giftCardData in order to print it
        giftCardData.set('giftCardId', line.get('giftcardid'));
        giftCardData.set('expirationDate', (line.get('giftcardexpirationdate') || line.get('giftcardobgcneExpirationdate')));
        giftCardData.set('productId', line.get('product').id);
        giftCardData.set('productName', line.get('product').get('_identifier'));
        giftCardData.set('businessPartnerId', args.order.get('bp').id);
        giftCardData.set('businessPartnerName', args.order.get('bp').get('_identifier'));
        giftCardData.set('gcOwnerId', (line.get('gcowner') || line.get('giftcardobgcneGCOwner')));
        giftCardData.set('gcOwnerName', line.get('gcowner_name'));

        // Find GiftCard in local database
        OB.UI.GiftCardUtils.findGiftCardModel(line.get('product').get('id'), function (giftcardproduct) {
          // Add giftcard properties to giftCardData
          giftCardData.set('giftCardType', giftcardproduct.get('giftCardType'));
          giftCardData.set('amount', giftcardproduct.get('amount'));

          if (line.get('product').get('templateIsPdf')) {
            if (line.get('product').get('templatePrinter')) {
              gctemplateresource.printer = parseInt(line.get('product').get('templatePrinter'), 10);
              gctemplateresource.dateFormat = OB.Format.date;
              gctemplateresource.subreports = [];
              gctemplateresource.getData(function () {
                OB.POS.hwserver._printPDF({
                  param: JSON.parse(JSON.stringify(giftCardData.toJSON())),
                  mainReport: gctemplateresource,
                  subReports: gctemplateresource.subreports
                }, printGiftCards(args, iteration + 1, callbacks));
              });
            } else {
              // If there is no printer defined show error
              OB.UTIL.showError(OB.I18N.getLabel('OBPGC_NoPrinter'));
              printGiftCards(args, iteration + 1, callbacks);
            }
          } else {
            OB.POS.hwserver.print(gctemplateresource, {
              giftCardData: giftCardData
            }, printGiftCards(args, iteration + 1, callbacks));
          }
        });

      } else {
        printGiftCards(args, iteration + 1, callbacks);
      }

    } else {
      printPaymentGiftCards(args, 0, callbacks);
    }
  };

  printPaymentGiftCards = function (args, iteration, callbacks) {
    var templateCreditNote = OB.MobileApp.model.get('terminal').printCreditNoteTemplate;

    if (args.order.get('payments').models.length - 1 >= iteration) {
      var payment = args.order.get('payments').models[iteration],
          gctemplateresource, giftCardData = new Backbone.Model(),
          giftCard;
      if (payment.get('kind') === 'OBPOS_payment.giftcard' && !payment.get('isPrePayment')) {
        OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.FindGiftCard', {
          giftcard: payment.get('paymentData').card
        }, function (result) {

          giftCard = result.data;

          // Find Gift Card Reason in Terminal Properties
          var reasonType = _.find(OB.MobileApp.model.get('gcnvGiftcardReason'), function (reason) {
            return reason.id === giftCard.category;
          });

          if (reasonType.printCard && reasonType.printTemplate && giftCard.currentamount > 0) {
            gctemplateresource = new OB.DS.HWResource(reasonType.printTemplate);

            // Set properties to giftCardData in order to print it
            giftCardData.set('giftCardId', giftCard.searchKey);
            giftCardData.set('expirationDate', OB.I18N.formatDate(new Date(!OB.UTIL.isNullOrUndefined(giftCard.obgcneExpirationdate) ? OB.I18N.parseServerDate(giftCard.obgcneExpirationdate) : giftCard.obgcneExpirationdate)));
            giftCardData.set('businessPartnerId', giftCard.businessPartner);
            giftCardData.set('businessPartnerName', giftCard.businessPartner$_identifier);
            giftCardData.set('gcOwnerId', giftCard.obgcneGCOwner);
            giftCardData.set('gcOwnerName', giftCard.obgcneGCOwner$_identifier);
            giftCardData.set('amount', giftCard.amount);
            giftCardData.set('currentamount', giftCard.currentamount);
            giftCardData.set('alertStatus', giftCard.alertStatus);
            giftCardData.set('type', giftCard.type);
            giftCardData.set('categoryId', giftCard.category);
            giftCardData.set('categoryName', giftCard.category$_identifier);

            if (reasonType.templateIsPdf) {
              if (reasonType.templatePrinter) {
                gctemplateresource.printer = parseInt(reasonType.templatePrinter, 10);
                gctemplateresource.dateFormat = OB.Format.date;
                gctemplateresource.subreports = [];
                gctemplateresource.getData(function () {
                  OB.POS.hwserver._printPDF({
                    param: JSON.parse(JSON.stringify(giftCardData.toJSON())),
                    mainReport: gctemplateresource,
                    subReports: gctemplateresource.subreports
                  }, printPaymentGiftCards(args, iteration + 1, callbacks));
                });
              } else {
                // If there is no printer defined show error
                OB.UTIL.showError(OB.I18N.getLabel('OBPGC_NoPrinter'));
                printPaymentGiftCards(args, iteration + 1, callbacks);
              }
            } else {
              OB.POS.hwserver.print(gctemplateresource, {
                giftCardData: giftCardData
              }, printPaymentGiftCards(args, iteration + 1, callbacks));
            }
          } else {
            printPaymentGiftCards(args, iteration + 1, callbacks);
          }

        }, function (error) {
          var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
          OB.MobileApp.view.$.containerWindow.getRoot().doShowPopup({
            popup: 'GCNV_UI_Message',
            args: {
              message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
            }
          });
        });

      } else if (payment.get('kind') === 'GCNV_payment.creditnote' && !payment.get('isPrePayment')) {
        var printCreditNote, currentAmt = null;
        gctemplateresource = new OB.DS.HWResource(templateCreditNote || OB.OBGCNE.Utils.PrintCreditNoteTemplate);

        printCreditNote = function (creditNote, currentAmount) {
          giftCardData.set('giftCardId', creditNote.searchKey);
          giftCardData.set('businessPartnerId', creditNote.businessPartnerId);
          giftCardData.set('businessPartnerName', creditNote.businessPartnerName);
          giftCardData.set('amount', creditNote.amount);
          giftCardData.set('currentamount', currentAmount);

          OB.POS.hwserver.print(gctemplateresource, {
            giftCardData: giftCardData
          }, printPaymentGiftCards(args, iteration + 1, callbacks));
        };

        if (args.order.get('bp').get('uniqueCreditNote') && OB.MobileApp.model.get('terminal').businessPartner !== args.order.get('bp').get('id')) {
          var serverCall = new OB.DS.Process('org.openbravo.retail.giftcards.FindCreditNote');
          serverCall.exec({
            bp: args.order.get('bp')
          }, function (data) {
            if (data && data.exception) {
              OB.UTIL.showConfirmation.display('', data.exception.message);
            } else if (data.searchKey) {
              printCreditNote(data, data.currentAmount);
            } else {
              if (!OB.UTIL.isNullOrUndefined(payment.get('paymentData').creditNote.currentamount)) {
                currentAmt = OB.DEC.sub(payment.get('paymentData').creditNote.currentamount, payment.get('origAmount'));
              }
              printCreditNote(payment.get('paymentData').creditNote, currentAmt);
            }
          });
        } else {
          if (!OB.UTIL.isNullOrUndefined(payment.get('paymentData').creditNote.currentamount)) {
            currentAmt = OB.DEC.sub(payment.get('paymentData').creditNote.currentamount, payment.get('origAmount'));
          }
          printCreditNote(payment.get('paymentData').creditNote, currentAmt);
        }

      } else {
        printPaymentGiftCards(args, iteration + 1, callbacks);
      }
    } else {
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    }
  };

  OB.UTIL.HookManager.registerHook('OBPRINT_PrePrint', function (args, callbacks) {
    printGiftCards(args, 0, callbacks);
  });

}());