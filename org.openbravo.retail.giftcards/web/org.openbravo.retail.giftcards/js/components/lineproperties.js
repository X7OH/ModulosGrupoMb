/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone, $, _  */


(function () {

  var GiftCardID = {
    kind: 'OB.UI.renderTextProperty',
    name: 'giftcardid',
    modelProperty: 'giftcardid',
    i18nLabel: 'GCNV_LblGiftCardID',
    applyValue: function (orderline) {
      this.doSetLineProperty({
        line: orderline,
        property: this.modelProperty,
        value: this.getValue()
      });
      return true;
    },
    showProperty: function (orderline, callback) {
      // Show the property only for giftcards...
      this.lineId = orderline.id;
      OB.OBGCNE.Utils.isGiftCard(orderline.get('product'), callback);
    }
  };

  // Register the new property
  OB.UI.ModalReceiptLinesPropertiesImpl.prototype.newAttributes.unshift(GiftCardID);

  // Register event to show dialog if a gift card...
  OB.OBPOSPointOfSale.UI.PointOfSale.prototype.classModel.on('createdLine', function (instwindow, line) {
    OB.OBGCNE.Utils.isGiftCard(line.get('product'), function (result, giftcardproduct) {
      if (result) {
        line.get('product').set('isEditablePrice', true);
        line.get('product').set('isEditableQty', false);
        if (!line.get('giftcardid')) {
          instwindow.editLine(null, {
            args: {
              autoDismiss: false,
              requiredFiedls: ['giftcardid'],
              requiredFieldNotPresentFunction: function (line, field) {
                OB.MobileApp.model.receipt.deleteLine(line);
              }
            }
          });
        }
      }
    });
  }, this);

  OB.UTIL.HookManager.registerHook('OBPOS_PreAddProductToOrder', function (args, callbacks) {

    var receipt = args.receipt;
    var product = args.productToAdd;

    if (product.get('giftCardTransaction')) {
      product.set('updatePriceFromPricelist', false);
    }

    OB.OBGCNE.Utils.isGiftCard(product, function (result, giftcardproduct) {
      if (result) {
        if (receipt.get('orderType') === 1) {
          // cancel
          args.context.doShowPopup({
            popup: 'GCNV_UI_Message',
            args: {
              message: OB.I18N.getLabel('GCNV_MsgCannotAddGiftCardToReturn', [product.get('_identifier')])
            }
          });
          args.cancelOperation = true;
          OB.UTIL.HookManager.callbackExecutor(args, callbacks);
        } else {
          if (product.get('gcnvGiftcardtype') === 'G') {
            // Create an empty line to prevent errors
            var line = new OB.Model.OrderLine();
            // Verify the taxes of gift card
            OB.DATA.OrderFindTaxes(receipt, line, product.get('taxCategory')).then(function (coll) {
              var tax = coll.at(0);
              if (!tax.get('taxExempt')) {
                // cancel not exempt taxes
                args.context.doShowPopup({
                  popup: 'GCNV_UI_Message',
                  args: {
                    message: OB.I18N.getLabel('GCNV_MsgCannotAddGiftCardNotTaxExempt', [product.get('_identifier'), tax.get('name')])
                  }
                });
                args.cancelOperation = true;
              }
              OB.UTIL.HookManager.callbackExecutor(args, callbacks);
            });
          } else {
            OB.UTIL.HookManager.callbackExecutor(args, callbacks);
          }
        }
      } else {
        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
      }
    });
  });


  var origTap = OB.UI.ReceiptPropertiesDialogApply.prototype.tap;
  OB.UI.ReceiptPropertiesDialogApply.prototype.tap = _.wrap(OB.UI.ReceiptPropertiesDialogApply.prototype.tap, function (wrapped) {

    var tap = _.bind(origTap, this),
        serverCallCheckDuplicity = new OB.DS.Process('org.openbravo.retail.giftcards.CheckDuplicityOfID'),
        giftid = this.owner.owner.propertycomponents ? this.owner.owner.propertycomponents.giftcardid : false,
        giftidShow = this.owner.owner.propertycomponents ? this.owner.owner.propertycomponents.giftcardid.owner.owner.showing : false,
        i;
    if (giftidShow) {
      if (giftid.getValue()) {
        for (i = 0; i < OB.MobileApp.model.receipt.get('lines').length; i++) {
          if (OB.MobileApp.model.receipt.get('lines').at(i).get('giftcardid') === giftid.getValue() && giftid.lineId !== OB.MobileApp.model.receipt.get('lines').at(i).id) {
            giftid.propertiesDialog.owner.doShowPopup({
              popup: 'GCNV_UI_Message',
              args: {
                message: OB.I18N.getLabel('GCNV_DuplicatedID')
              }
            });
            return false;
          }
        }

        return serverCallCheckDuplicity.exec({
          gcid: giftid.getValue()
        }, function (data) {
          if (data && data.exception) {
            giftid.propertiesDialog.owner.doShowPopup({
              popup: 'GCNV_UI_Message',
              args: {
                message: data.exception.message
              }
            });
            return false;
          } else if (data && !data.exception) {
            if (data.used) { // if gc ID is in used by another gc
              giftid.propertiesDialog.owner.doShowPopup({
                popup: 'GCNV_UI_Message',
                args: {
                  message: OB.I18N.getLabel('GCNV_DuplicatedID')
                }
              });
              return false;
            } else {
              tap();
            }
          }
        });
      } else {
        giftid.propertiesDialog.owner.doShowPopup({
          popup: 'GCNV_UI_Message',
          args: {
            message: OB.I18N.getLabel('GCNV_LblGiftCardInvalid')
          }
        });
        return false;
      }
    } else {
      tap();
    }

  });
  OB.UTIL.HookManager.registerHook('OBPOS_LineSelected', function (args, callbacks) {
    var hasGiftCard = false;
    if (args.line && args.line.get('product').get('giftCardTransaction')) {
      hasGiftCard = true;
    } else if (args.selectedLines && args.selectedLines.length > 0) {
      hasGiftCard = _.filter(args.selectedLines, function (line) {
        return line.get('product').get('giftCardTransaction');
      }).length > 0;
    }
    if (hasGiftCard) {
      args.context.waterfall('onHideReturnLineButton', {
        hide: true
      });
    }
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
  });

}());