/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, $ */

(function () {

  enyo.kind({
    name: 'GCNV.UI.MenuGiftCard',
    kind: 'OB.UI.MenuAction',
    permission: 'GCNV_PaymentGiftCard',
    i18nLabel: 'GCNV_LblGiftCards',
    events: {
      onShowPopup: ''
    },
    tap: function () {
      if (this.disabled) {
        return true;
      }
      this.inherited(arguments); // Manual dropdown menu closure
      this.doShowPopup({
        popup: 'GCNV_UI_SearchDialog'
      });
    },
    updateVisibility: function () {
      if (OB.MobileApp.model.get('payments').length <= 0) {
        this.hide();
        return;
      }
    },
    init: function (model) {
      this.model = model;
      OB.GCNV = OB.GCNV || {};
      OB.GCNV.usingStandardMode = true;
      this.updateVisibility();
      if (this.model.get('leftColumnViewManager')) {
        OB.GCNV.usingStandardMode = this.model.get('leftColumnViewManager').isOrder();
        this.model.get('leftColumnViewManager').on('change:currentView', function (changedModel) {
          if (changedModel.isOrder()) {
            this.setDisabled(false);
            OB.GCNV.usingStandardMode = true;
            return;
          }
          if (changedModel.isMultiOrder()) {
            OB.GCNV.usingStandardMode = false;
            this.setDisabled(true);
            return;
          }
        }, this);
      }
    }
  });

  // Register the menu...
  OB.OBPOSPointOfSale.UI.LeftToolbarImpl.prototype.menuEntries.push({
    kind: 'GCNV.UI.MenuGiftCard'
  });

  enyo.kind({
    name: 'GCNV.UI.MenuGiftCardCertificate',
    kind: 'OB.UI.MenuAction',
    classes: 'disabled',
    disabled: true,      
    permission: 'GCNV_PaymentGiftCardCertificate',
    i18nLabel: 'GCNV_CreateGiftCardsCertificate',
    events: {
      onShowPopup: ''
    },
    tap: function () {
      if (this.disabled) {
        return true;
      }
      var giftcardPayment = OB.MobileApp.model.get('payments').find(function (pay) {
        return pay.payment.searchKey === 'OBPOS_payment.giftcard';
      });
      this.inherited(arguments); // Manual dropdown menu closure
      if (!giftcardPayment) {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_Error'), OB.I18N.getLabel('GCNV_GiftCardPaymentNotConfigured'));
        return;
      }
      this.doShowPopup({
        popup: 'GCNV_UI_Certificate'
      });
    },
    updateVisibility: function () {
      if (OB.MobileApp.model.get('payments').length <= 0 || !OB.MobileApp.model.hasPermission(this.permission, true)) {
        this.hide();
        return;
      }
    },
    init: function (model) {
      this.model = model;
      this.updateVisibility();
    }
  });

  // Register the menu...
  OB.OBPOSPointOfSale.UI.LeftToolbarImpl.prototype.menuEntries.push({
    kind: 'GCNV.UI.MenuGiftCardCertificate'
  });

}());