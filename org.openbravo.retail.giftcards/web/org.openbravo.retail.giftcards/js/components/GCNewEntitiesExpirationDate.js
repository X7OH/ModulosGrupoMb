/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

(function () {

  var GiftCardExpirationDate;

  GiftCardExpirationDate = {
    kind: 'OB.UI.renderTextProperty',
    name: 'giftcardobgcneExpirationdate',
    modelProperty: 'giftcardobgcneExpirationdate',
    i18nLabel: 'OBGCNE_ExpirationDate',
    disabled: true,
    isoDate: '',
    loadValue: function (inSender, inEvent) {
      if (this.modelProperty === inEvent.modelProperty) {
        var expirationDays = inEvent.model.get('product').get('expirationDays');
        if (expirationDays === null || expirationDays === undefined) {
          this.setValue('');
        } else {
          var now = new Date();
          var expirationDate = new Date();
          expirationDate.setMonth(expirationDate.getMonth() + 2);
          expirationDate.setDate(now.getDate() + expirationDays);
          this.setValue(OB.Utilities.Date.JSToOB(expirationDate, OB.Format.date));
          this.isoDate = expirationDate.toISOString();
        }
      }
    },
    applyValue: function (orderline) {
      if (this.isoDate) {
        this.doSetLineProperty({
          line: orderline,
          property: this.modelProperty,
          value: this.isoDate
        });
      }
      return true;
    },
    showProperty: function (orderline, callback) {
      // Show the property only for giftcards...  
      OB.OBGCNE.Utils.isGiftCard(orderline.get('product'), callback);
    }
  };

  OB.UI.ModalReceiptLinesPropertiesImpl.prototype.newAttributes.unshift(GiftCardExpirationDate);

}());