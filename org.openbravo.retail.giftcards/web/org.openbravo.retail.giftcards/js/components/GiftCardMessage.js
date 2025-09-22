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

  enyo.kind({
    kind: 'OB.UI.ModalAction',
    name: 'GCNV.UI.Message',
    closeOnAcceptButton: true,
    header: 'label',
    events: {
      onHideThisPopup: ''
    },
    handlers: {
      onAcceptButton: 'acceptButton'
    },
    bodyContent: {
      name: 'bodymessage',
      content: 'label'
    },
    bodyButtons: {
      components: [{
        kind: 'GCNV.UI.AcceptMessageButton'
      }, {
        kind: 'OB.UI.CancelDialogButton',
        name: 'cancelButton'
      }]
    },
    executeOnShow: function () {
      this.$.header.setContent(this.args.header || OB.I18N.getLabel('GCNV_LblGiftCardHeader'));
      this.$.bodyContent.$.bodymessage.setContent(this.args.message);
      this.$.bodyButtons.$.cancelButton.setShowing(this.args.cancelButton);
    },
    acceptButton: function (inSender, inEvent) {
      this.doHideThisPopup();
      if (this.args.callback) {
        this.args.callback();
      }
    }
  });

  enyo.kind({
    name: 'GCNV.UI.AcceptMessageButton',
    kind: 'OB.UI.ModalDialogButton',
    classes: 'btnlink btnlink-gray modal-dialog-button',
    isDefaultAction: true,
    events: {
      onHideThisPopup: '',
      onAcceptButton: ''
    },
    tap: function () {
      this.doAcceptButton();
    },
    initComponents: function () {
      this.inherited(arguments);
      this.setContent(OB.I18N.getLabel('OBMOBC_LblOk'));
    }
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'GCNV.UI.Message',
    name: 'GCNV_UI_Message'
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSCashMgmt.UI.CashManagement', {
    kind: 'GCNV.UI.Message',
    name: 'GCNV_UI_Message'
  });

}());