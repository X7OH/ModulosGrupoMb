/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

window.OBPOSSV = window.OBPOSSV || {};
enyo.kind({
  name: 'OBPSSV.UI.dummyControl',
  showing: false,
  init: function (model) {
    var me = this;
    this.model = model;

    function executeCallToServer(changedModel) {
      var qtyvalidate=false;

      var serverCall = new OB.DS.Process('ec.com.sidesoft.retail.custom.stockvalidation.ExtStock.ExtStockChequer');
      var statusMessage = OB.UTIL.showStatus(OB.I18N.getLabel('OBPOSSV_GettingStockFromServer'));
      serverCall.exec({
        orderLine: changedModel,
        organization: OB.POS.modelterminal.get('terminal').organization
      }, function (data, message) {
        statusMessage.hide();
        if(data.qty>0 && data.qty<1){
          qtyvalidate=true;
        }
        if (data.allowSell === false) {
          if (OB.MobileApp.model.get('obpossv_negativeStockAllowed')) {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOSSV_modalHeader'), OB.I18N.getLabel('OBPOSSV_NoEnoughStockInfo', [data.qty, changedModel.get('qty')]), [{
              label: OB.I18N.getLabel('OBPOSSV_LblProceed'),
              action: function () {
                return true;
              }
            }, {
              label: OB.I18N.getLabel('OBMOBC_LblCancel')
            }], {
              onHideFunction: function (popup) {
                if (me.model.get('order').get('undo') && data.qty ) {
                  me.model.get('order').get('undo').undo();
                } else {
                  if (data.qty <= 0) {
                    me.model.get('order').deleteLine(changedModel);
                  } else {
                    changedModel.set('qty', data.qty);
                  }
                }
              },
              style: 'background-color: #EBA001'
            });
          } else {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOSSV_modalHeader'), OB.I18N.getLabel('OBPOSSV_NoEnoughStock', [data.qty, changedModel.get('qty')]), null, {
              onHideFunction: function (popup) {
                if (me.model.get('order').get('undo') && qtyvalidate===false) {
                  me.model.get('order').get('undo').undo();
                } else {
                  if (data.qty <= 0) {
                    me.model.get('order').deleteLine(changedModel);
                  } else {
                    changedModel.set('qty', data.qty);
                  }
                }
              }
            });
          }
        }
      }, function (error) {
        statusMessage.hide();
        OB.UTIL.showError(OB.I18N.getLabel('OBPOSSV_ErrorGettingStockFromServer'));
      });
    }

    this.model.get('order').get('lines').on('add', function (addedModel) {
      if (!addedModel.get('product').get('stocked') || this.model.get('order').get('orderType') === 1 || this.model.get('order').get('isPaid') === true || this.model.get('order').get('isQuotation') === true) {
        return;
      }
      if (OB.POS.modelterminal.get('connectedToERP')) {
        executeCallToServer(addedModel);
      } else {
        OB.UTIL.showWarning(OB.I18N.getLabel('OBPOSSV_CannotCheckOffline'));
      }
      addedModel.on('change:qty', function (changedModel) {
        if (!addedModel.get('product').get('stocked') || this.model.get('order').get('orderType') === 1 || this.model.get('order').get('isPaid') === true || this.model.get('order').get('isQuotation') === true) {
          return;
        }
        if (OB.POS.modelterminal.get('connectedToERP')) {
          executeCallToServer(changedModel);
        } else {
          OB.UTIL.showWarning(OB.I18N.getLabel('OBPOSSV_CannotCheckOffline'));
        }
      }, this);
    }, this);
  }
})

OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
  kind: 'OBPSSV.UI.dummyControl',
  name: 'OBPOSSV_dummyControl'
});