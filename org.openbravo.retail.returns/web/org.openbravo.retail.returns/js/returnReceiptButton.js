/*
 ************************************************************************************
 * Copyright (C) 2013-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo */

(function () {

  enyo.kind({
    name: 'OBRETUR.UI.MenuReturn',
    kind: 'OB.UI.MenuAction',
    permission: 'OBRETUR_Return',
    i18nLabel: 'OBRETUR_LblReturn',
    events: {
      onPaidReceipts: '',
      onTabChange: ''
    },
    tap: function () {
      if (this.disabled) {
        return true;
      }
      var order = OB.MobileApp.model.receipt;
      if (order.validateAllowSalesWithReturn(-1, false)) {
        return true;
      }
      this.inherited(arguments); // Manual dropdown menu closure
      if (!OB.MobileApp.model.get('connectedToERP')) {
        OB.UTIL.showError(OB.I18N.getLabel('OBPOS_OfflineWindowRequiresOnline'));
        return;
      }
      if (OB.MobileApp.model.hasPermission(this.permission)) {
        this.doTabChange({
          tabPanel: 'scan',
          keyboard: 'toolbarscan',
          edit: true
        });
        this.doPaidReceipts({
          isQuotation: false,
          isReturn: true
        });
      }
    },
    updateVisibility: function () {
      var me = this;
      if (this.receipt.get('isEditable') === false || this.receipt.get('isQuotation') || this.model.get('order').get('orderType') === 2) {
        me.setDisabled(true);
        return;
      } else {
        me.setDisabled(false);
        return;
      }
    },
    init: function (model) {
      var me = this;
      this.model = model;
      this.receipt = model.get('order');
      this.receipt.on('change:isEditable change:isQuotation change:orderType', function () {
        this.updateVisibility();
      }, this);
    }
  });

  // Register the menu...
  OB.OBPOSPointOfSale.UI.LeftToolbarImpl.prototype.menuEntries.push({
    kind: 'OBRETUR.UI.MenuReturn'
  });
}());