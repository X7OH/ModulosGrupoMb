/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, moment, _ */

(function () {

  enyo.kind({
    name: 'seachBPGiftCard',
    components: [{
      kind: 'OB.UI.BusinessPartnerGiftCard',
      name: 'businessPartnerGiftCard'
    }, {
      name: 'removeOwner',
      kind: 'OB.UI.SmallButton',
      classes: 'btnlink btnlink-small btnlink-gray',
      style: 'float: left; margin: 5px;',
      tap: function () {
        this.owner.$.businessPartnerGiftCard.setContent(OB.I18N.getLabel('OBGCNE_LblSelectOwnerGiftCard'));
        this.owner.bPartnerId = null;
        this.owner.bPartnerName = null;
      },
      initComponents: function () {
        this.setContent('x');
      }
    }]

  });

  enyo.kind({

    name: 'OB.UI.BusinessPartnerGiftCard',
    kind: 'OB.UI.SmallButton',
    classes: 'btnlink btnlink-small btnlink-gray',
    style: 'float: left; max-width: 253px; text-overflow:ellipsis; white-space: nowrap; overflow: hidden; margin: 5px;',
    published: {
      order: null
    },
    handlers: {
      onChangeBPartner: 'changeBPartner'
    },
    events: {
      onShowPopup: ''
    },
    tap: function () {
      var me = this;
      if (!this.disabled) {
        this.doShowPopup({
          popup: 'modalcustomer',
          args: {
            target: 'giftCardProduct'
          }
        });
      }
    },
    changeBPartner: function (inSender, inEvent) {
      if (inEvent.target === 'giftCardProduct') {
        this.owner.bPartnerId = inEvent.businessPartner.get('id');
        this.owner.bPartnerName = inEvent.businessPartner.get('_identifier');
        this.setContent(inEvent.businessPartner.get('_identifier'));
      }
    },
    initComponents: function () {
      this.setContent(OB.I18N.getLabel('OBGCNE_LblSelectOwnerGiftCard'));
    },
    orderChanged: function (oldValue) {
      this.setContent(OB.I18N.getLabel('OBGCNE_LblSelectOwnerGiftCard'));
    }

  });

  var giftCardOwnerSelector = {
    name: 'giftcardobgcneGCOwner',
    kind: 'seachBPGiftCard',
    modelProperty: 'giftcardobgcneGCOwner',
    modelPropertyText: 'gcowner_name',
    i18nLabel: 'OBGCNE_GCOwner',
    bPartnerId: null,
    bPartnerName: null,
    applyValue: function (orderline) {
      orderline.set(this.modelProperty, this.bPartnerId);
      orderline.set(this.modelPropertyText, this.bPartnerName);
      return true;
    },
    showProperty: function (orderline, callback) {
      // Show the property only for giftcards...  
      OB.OBGCNE.Utils.isGiftCard(orderline.get('product'), callback);
    }
  };

  var protoExecuteOnShow = OB.UI.ModalReceiptLinesPropertiesImpl.prototype.executeOnShow;
  OB.UI.ModalReceiptLinesPropertiesImpl.prototype.executeOnShow = _.wrap(OB.UI.ModalReceiptLinesPropertiesImpl.prototype.executeOnShow, function (wrapped) {
    var executeOnShow, gcowner = this.$.bodyContent.$.attributes.$.line_giftcardobgcneGCOwner.$.newAttribute.$.giftcardobgcneGCOwner;
    if (this.currentLine) {
      gcowner.bPartnerId = this.currentLine.get('giftcardobgcneGCOwner');
      gcowner.bPartnerName = this.currentLine.get('gcowner_name');
      if (this.currentLine.get('giftcardobgcneGCOwner')) {
        if (this.currentLine.get('gcowner_name').length > 25) {
          gcowner.$.businessPartnerGiftCard.setContent(this.currentLine.get('gcowner_name').substring(0, 22) + '...');
        } else {
          gcowner.$.businessPartnerGiftCard.setContent(this.currentLine.get('gcowner_name'));
        }
      } else {
        gcowner.$.businessPartnerGiftCard.setContent(OB.I18N.getLabel('OBGCNE_LblSelectOwnerGiftCard'));
      }
    }
    executeOnShow = _.bind(protoExecuteOnShow, this);
    executeOnShow();
  });

  OB.UI.ModalReceiptLinesPropertiesImpl.prototype.newAttributes.unshift(giftCardOwnerSelector);

}());