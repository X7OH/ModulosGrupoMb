(function () {

  enyo.kind({
    kind: 'OB.UI.ModalAction',
    name: 'SRGC.UI.Message',
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
        kind: 'SRGC.UI.AcceptMessageButton'
      }, {
        kind: 'OB.UI.CancelDialogButton',
        name: 'cancelButton'
      }]
    },
    executeOnShow: function () {
      this.$.header.setContent(this.args.header || OB.I18N.getLabel('SRGC_LblGiftCardHeader'));
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
    name: 'SRGC.UI.AcceptMessageButton',
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
    kind: 'SRGC.UI.Message',
    name: 'SRGC_UI_Message'
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSCashMgmt.UI.CashManagement', {
    kind: 'SRGC.UI.Message',
    name: 'SRGC_UI_Message'
  });

}());