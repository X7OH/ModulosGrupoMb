enyo.kind({
  kind: 'OB.UI.ModalAction',
  name: 'SIDESOFT.UI.ErrorSequence',
  i18nHeader: 'OBMOBC_Error',
  bodyContent: {
    //content: OB.I18N.getLabel('ECSDS_ErrorSequence')
  },
  bodyButtons: {
    components: [{
      kind: 'OBPOS_LblApplyButton'
    }]
  },
  initComponents: function () {
    this.bodyContent.content = OB.I18N.getLabel('ECSDS_ErrorSequence');
    this.inherited(arguments);
  }
});


OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
  kind: 'SIDESOFT.UI.ErrorSequence',
  name: 'SIDESOFT.UI.ErrorSequence'
});