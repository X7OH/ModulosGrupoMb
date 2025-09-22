/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone, moment, _ */

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'GCNV.UI.Certificate_btnApply',
  isDefaultAction: true,
  events: {
    onApplyChanges: ''
  },
  disabled: false,
  putDisabled: function (state) {
    if (state === false) {
      this.setDisabled(false);
      this.removeClass('disabled');
      this.setAttribute('disabled', null);
      this.disabled = false;
    } else {
      this.setDisabled();
      this.addClass('disabled');
      this.setAttribute('disabled', 'disabled');
      this.disabled = true;
    }
  },
  tap: function () {
    if (this.disabled) {
      return;
    }
    this.putDisabled(true);
    if (this.doApplyChanges()) {
      var me = this,
          model = this.owner.owner.model,
          serverCallGiftCardCertificate = new OB.DS.Process.FailOver('org.openbravo.retail.giftcards.GiftCardCertificate');
      OB.MobileApp.model.runSyncProcess(function () {
        model.set('businessPartnerId', OB.MobileApp.model.receipt.get('bp').get('id'));
        model.set('businessPartnerName', OB.MobileApp.model.receipt.get('bp').get('_identifier'));
        me.doHideThisPopup();
        serverCallGiftCardCertificate.exec({
          _executeInOneServer: true,
          _tryCentralFromStore: true,
          model: me.owner.owner.model,
          businessPartner: model.get('giftcardCBpartner') ? model.get('giftcardCBpartner') : OB.MobileApp.model.receipt.get('bp').get('id'),
          cashupId: OB.MobileApp.model.get('terminal').cashUpId
        }, function (data) {
          if (data && data.exception) {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_Error'), OB.I18N.getLabel(data.exception.message), [{
              label: OB.I18N.getLabel('OBMOBC_LblOk'),
              action: function () {
                me.bubble('onShowPopup', {
                  popup: 'GCNV_UI_Certificate',
                  args: {
                    callback: null,
                    showOnly: true
                  }
                });
                return true;
              }
            }]);
            me.putDisabled(false);
          } else if (data && !data.exception) {
            // Update CashUp
            model.set('giftcardNumber', data.data.cardNumber);
            OB.UI.GiftCardUtils.getPaymentMethodCashUp(undefined, function (payMthd) {
              var drop = payMthd.get('totalDrops') + model.get('giftcardAmount');
              payMthd.set('totalDrops', drop);
              OB.UI.GiftCardUtils.updatePaymentMethodCashUp(payMthd, model.get('giftcardNumber'), "GiftCardCertificate.create", function () {
                // Print and close dialog
                me.printGiftCard();
                OB.UTIL.showAlert.display(OB.I18N.getLabel('GCNV_MsgGiftCardCertificateCreated', [model.get('giftcardNumber'), OB.I18N.formatCurrency(model.get('giftcardAmount'))]), OB.I18N.getLabel('OBMOBC_LblSuccess'), 'alert-success', false, 'GCNV_MsgGiftCardCertificateCreated');
                me.putDisabled(false);
              });
            });
          }
        });
      }, function () {
        OB.UTIL.showError(OB.I18N.getLabel('GCNV_ErrorWhenSynchronizing'));
        me.putDisabled(false);
      });
    } else {
      this.putDisabled(false);
    }
  },

  printGiftCard: function () {
    var me = this,
        model = this.owner.owner.model,
        gctemplateresource, giftCardData = new Backbone.Model();

    // Find Gift Card Reason in Terminal Properties
    var reasonType = _.find(OB.MobileApp.model.get('gcnvGiftcardReason'), function (reason) {
      return reason.id === model.get('giftcardCategory');
    });

    if (reasonType.printCard && reasonType.printTemplate) {
      gctemplateresource = new OB.DS.HWResource(reasonType.printTemplate);

      // Set properties to giftCardData in order to print it
      giftCardData.set('giftCardId', model.get('giftcardNumber'));
      giftCardData.set('expirationDate', model.get('giftcardExpirationDate'));
      giftCardData.set('businessPartnerId', model.get('businessPartnerId'));
      giftCardData.set('businessPartnerName', model.get('businessPartnerName'));
      giftCardData.set('gcOwnerId', model.get('giftcardCBpartner'));
      giftCardData.set('gcOwnerName', model.get('giftcardCBpartnerName'));
      giftCardData.set('amount', model.get('giftcardAmount'));
      giftCardData.set('currentamount', model.get('giftcardAmount'));
      giftCardData.set('category', model.get('giftcardCategory'));
      giftCardData.set('categoryName', reasonType.name);
      giftCardData.set('type', 'BasedOnGLItem');

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
            });
          });
        } else {
          // If there is no printer defined show error
          OB.UTIL.showError(OB.I18N.getLabel('OBPGC_NoPrinter'));
        }
      } else {
        OB.POS.hwserver.print(gctemplateresource, {
          giftCardData: giftCardData
        });
      }

    } else {
      // If there is no template defined show error
      OB.UTIL.showError(OB.I18N.getLabel('OBPGC_NoTemplate'));
    }
  },

  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBPOS_LblApplyButton'));
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'GCNV.UI.Certificate_btnCancel',
  isDefaultAction: false,
  i18nContent: 'OBMOBC_LblCancel',
  tap: function () {
    this.doHideThisPopup();
  }
});

enyo.kind({
  kind: 'OB.UI.ModalAction',
  name: 'GCNV.UI.Certificate',
  i18nHeader: 'GCNV_LblGiftCardsCertificate',
  topPosition: '60px',
  events: {
    onAddProduct: ''
  },
  handlers: {
    onClearErrors: 'clearErrors',
    onApplyChanges: 'applyChanges',
    onShowHideCustomer: 'showHideCustomer'
  },
  //body of the popup
  bodyContent: {
    kind: 'Scroller',
    style: 'background-color: #ffffff; overflow: hidden;',
    thumb: true,
    horizontal: 'hidden',
    components: [{
      name: 'attributes'
    }]
  },

  //buttons of the popup
  bodyButtons: {
    components: [{
      name: 'error',
      content: '',
      style: 'color: red;'
    }, {
      kind: 'GCNV.UI.Certificate_btnApply',
      name: 'btnApply'
    }, {
      kind: 'GCNV.UI.Certificate_btnCancel'
    }]
  },

  newAttributes: [{
    kind: 'OB.UI.renderComboProperty',
    name: 'giftcardCategory',
    modelProperty: 'giftcardCategory',
    mandatory: true,
    i18nLabel: 'GCNV_LblGiftCardsCategory',
    retrievedPropertyForValue: 'id',
    retrievedPropertyForText: 'name',
    collection: new Backbone.Collection(),
    handlers: {
      onchange: 'selectChanged'
    },
    fetchDataFunction: function (args) {
      if (this.destroyed) {
        return;
      }
      var gcnvGiftcardReason = OB.MobileApp.model.get('gcnvGiftcardReason');
      if (gcnvGiftcardReason) {
        var giftcardReason = new Backbone.Collection();
        _.each(gcnvGiftcardReason, function (p) {
          giftcardReason.add(new Backbone.Model(p));
        });
        this.dataReadyFunction(giftcardReason, args);
        this.bubble('onShowHideCustomer', {
          show: gcnvGiftcardReason.length > 0 ? gcnvGiftcardReason[0].hasOwner : false
        });
      } else {
        OB.UTIL.showError(OB.I18N.getLabel('GCNV_ErrorGettingGiftcardReason'));
        this.dataReadyFunction(null, args);
        this.bubble('onShowHideCustomer', {
          show: false
        });
      }
    },
    getValue: function () {
      return this.$.renderCombo.getValue();
    },
    selectChanged: function (inSender, inEvent) {
      var id = inSender.getValue(),
          reason = _.find(this.collection.models, function (r) {
          return r.id === id;
        });
      if (reason) {
        this.bubble('onShowHideCustomer', {
          show: reason.get('hasOwner')
        });
      }
    }
  }, {
    kind: 'OB.UI.renderTextProperty',
    name: 'giftcardCreationDate',
    modelProperty: 'giftcardCreationDate',
    readOnly: true,
    i18nLabel: 'GCNV_LblGiftCardsCreationDate'
  }, {
    kind: 'OB.UI.SmallButton',
    name: 'customer',
    classes: 'btnlink-gray obrcifp-btn',
    modelProperty: 'giftcardCBpartner',
    mandatory: true,
    i18nLabel: 'GCNV_LblGiftCardsCustomer',
    style: 'margin-top: 0px; margin-bottom: 0px;',
    handlers: {
      onLoadValue: 'loadValue',
      onChangeBPartner: 'changeBPartner',
      onCloseCancelSelector: 'closeCancelSelector'
    },
    changeBPartner: function (inSender, inEvent) {
      if (inEvent.target === 'giftCardCertificate') {
        this.showDialog();
        this.businessPartner = inEvent.businessPartner;
        this.renderCustomer(inEvent.businessPartner.get('_identifier'));
        this.bubble('onClearErrors');
      }
    },
    closeCancelSelector: function (inSender, inEvent) {
      if (inEvent.target === 'giftCardCertificate') {
        this.showDialog();
      }
    },
    showDialog: function () {
      this.bubble('onShowPopup', {
        popup: 'GCNV_UI_Certificate',
        args: {
          callback: null,
          showOnly: true
        }
      });
    },
    loadValue: function (inSender, inEvent) {},
    renderCustomer: function (newCustomer) {
      this.setContent(newCustomer);
      this.name = newCustomer;
    },
    tap: function () {
      this.bubble('onHideThisPopup');
      this.bubble('onShowPopup', {
        popup: 'modalcustomer',
        args: {
          target: 'giftCardCertificate'
        }
      });
    },
    getValue: function () {
      return this.businessPartner ? this.businessPartner.get('id') : null;
    }
  }, {
    kind: 'OB.UI.DatePicker',
    name: 'giftcardExpirationDate',
    modelProperty: 'giftcardExpirationDate',
    mandatory: true,
    i18nLabel: 'GCNV_LblGiftCardsExpirationDate',
    handlers: {
      onLoadValue: 'loadValue',
      onModified: 'modifiedValidate'
    },
    style: 'overflow: hidden;',
    loadValue: function (inSender, inEvent) {
      if (this.modelProperty === inEvent.modelProperty) {
        this.setLocale(OB.MobileApp.model.get('terminal').language_string);
        if (inEvent.model && inEvent.model.get(this.modelProperty)) {
          this.setValue(new Date(inEvent.model.get(this.modelProperty)));
        } else {
          this.setValue('');
        }
      }
    },
    modifiedValidate: function (inSender, inEvent) {
      this.bubble('onClearErrors');
    },
    getPropValue: function () {
      return OB.I18N.formatDate(this.getValue());
    }
  }, {
    kind: 'OB.UI.renderTextProperty',
    name: 'giftcardAmount',
    modelProperty: 'giftcardAmount',
    mandatory: true,
    i18nLabel: 'GCNV_LblGiftCardsAmount',
    handlers: {
      oninput: 'inputChange'
    },
    isValid: function () {
      var val = this.getNodeProperty("value", this.value).trim();
      if (val.match(/^\d*\.{0,1}\d*$/) !== null || val.match(/^\d*\,{0,1}\d*$/) !== null) {
        val = this.getFloatValue(val);
        return !isNaN(val);
      } else {
        return false;
      }
    },
    getFloatValue: function (value) {
      value = value.replace(',', '.');
      return parseFloat(value);
    },
    getValue: function () {
      return this.getFloatValue(this.getNodeProperty("value", this.value));
    },
    inputChange: function (inSender, inEvent) {
      this.bubble('onClearErrors');
    }
  }],

  loadValue: function (mProperty) {
    this.waterfall('onLoadValue', {
      model: this.model,
      modelProperty: mProperty
    });
  },

  clearErrors: function (inSender, inEvent) {
    this.$.bodyButtons.$.error.setContent('');
    _.each(this.properties, function (comp) {
      var property = comp.$.newAttribute.children[0];
      if (comp.getShowing() && !property.readOnly) {
        if (property.modelProperty === 'giftcardCBpartner') {
          property.parent.removeClass('gcnv-edit-error');
        } else {
          property.removeClass('gcnv-edit-error');
        }
      }
    }, this);
  },

  applyChanges: function (inSender, inEvent) {
    var hasError = false,
        expDate = false;
    this.clearErrors(inSender, inEvent);
    _.each(this.properties, function (comp) {
      var property = comp.$.newAttribute.children[0];
      if (comp.getShowing() && !property.readOnly) {
        var value = typeof (property.getPropValue) === "function" ? property.getPropValue() : property.getValue();
        if ((property.mandatory && !value) || (typeof (property.isValid) === "function" && !property.isValid())) {
          hasError = true;
          if (property.modelProperty === 'giftcardCBpartner') {
            property.parent.addClass('gcnv-edit-error');
          } else {
            property.addClass('gcnv-edit-error');
          }
        } else if (property.name === 'giftcardExpirationDate' && moment(value, OB.Format.date.toUpperCase()).format("YYYY-MM-DD") < moment().format("YYYY-MM-DD")) {
          hasError = true;
          expDate = true;
          property.addClass('gcnv-edit-error');
        }
        this.model.set(property.modelProperty, value);
        if (property.modelProperty === 'giftcardCBpartner') {
          this.model.set('giftcardCBpartnerName', property.businessPartner !== '' ? property.businessPartner.get('_identifier') : '');
        }
      }
    }, this);
    if (!hasError) {
      return true;
    } else {
      this.$.bodyButtons.$.error.setContent(OB.I18N.getLabel(expDate ? 'GCNV_ErrorExpirationDate' : 'OBPOS_ErrorMadatoryField'));
      return false;
    }
  },

  showHideCustomer: function (inSender, inEvent) {
    _.each(this.properties, function (comp) {
      var property = comp.$.newAttribute.children[0];
      if (property.modelProperty === 'giftcardCBpartner') {
        comp.setShowing(inEvent.show);
      }
    }, this);
  },

  executeOnShow: function () {
    if (!this.args.showOnly) {
      this.model = new Backbone.Model({
        giftcardNumber: '',
        giftcardCreationDate: OB.I18N.formatDate(new Date()),
        giftcardCBpartner: null,
        giftcardCBpartnerName: null,
        giftcardExpirationDate: null,
        giftcardAmount: 0,
        giftcardCategory: null
      });
      _.each(this.properties, function (comp) {
        var property = comp.$.newAttribute.children[0];
        if (property.modelProperty === 'giftcardCBpartner') {
          property.parent.removeClass('gcnv-edit-error');
          property.businessPartner = '';
          property.setContent(OB.I18N.getLabel('GCNV_LblEmpty'));
        } else {
          property.removeClass('gcnv-edit-error');
        }
        this.loadValue(property.modelProperty);
      }, this);
      this.$.bodyContent.$.attributes.render();
      this.$.bodyButtons.$.error.setContent('');
    }
  },

  initComponents: function () {
    this.inherited(arguments);
    this.properties = [];
    enyo.forEach(this.newAttributes, function (natt) {
      this.properties.push(this.$.bodyContent.$.attributes.createComponent({
        kind: 'OB.UI.PropertyEditLine',
        name: 'line_' + natt.name,
        newAttribute: natt
      }));
    }, this);
  }

});

OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
  kind: 'GCNV.UI.Certificate',
  name: 'GCNV_UI_Certificate'
});