/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 *
 * Author Yogas Karnik
 *
 */
/*global OB, moment, enyo */

enyo.kind({
  name: 'OB.UI.ModalScrollerVerifiedReturn.ReturnLine',
  classes: 'flexContainer',
  components: [{
    classes: 'properties-label',
    components: [{
      name: 'productName',
      type: 'text',
      style: 'font-size: 17px;',
      classes: 'modal-dialog-receipt-properties-label',
      content: ''
    }]
  }, {
    classes: 'properties-component',
    components: [{
      name: 'newAttribute',
      classes: 'modal-dialog-receipt-properties-text',
      components: [{
        kind: 'OB.UI.renderTextProperty',
        name: 'valueAttribute',
        style: 'color: white',
        maxlength: '70',
        handlers: {
          oninput: 'blur'
        },
        blur: function () {
          this.bubble('onFieldChanged');
        },
        placeholder: 'Scan attribute'
      }]
    }]
  }, {
    name: 'productId',
    type: 'text',
    style: 'font-size: 17px'
  }, {
    style: 'clear: both'
  }]
});

enyo.kind({
  kind: 'OB.UI.ModalAction',
  name: 'OB.UI.ModalProductAttributeVerifiedReturns',
  style: 'width: 700px;',
  autoDismiss: false,
  bodyContent: {
    kind: 'Scroller',
    maxHeight: '225px',
    style: 'background-color: #ffffff;',
    thumb: true,
    horizontal: 'hidden',
    components: [{
      name: 'verifiedReturns'
    }]
  },
  handlers: {
    onFieldChanged: 'fieldChanged'
  },
  bodyButtons: {
    components: [{
      kind: 'OB.UI.ModalDialogButton',
      i18nContent: 'OBMOBC_LblOk',
      tap: function () {
        this.owner.owner.validateAction();
      }
    }, {
      kind: 'OB.UI.ModalDialogButton',
      i18nContent: 'OBPOS_LblClear',
      tap: function () {
        this.owner.owner.clearAction();
      }
    }, {
      kind: 'OB.UI.ModalDialogButton',
      i18nContent: 'OBMOBC_LblCancel',
      tap: function () {
        this.owner.owner.cancelAction();
      }
    }]
  },
  header: {

  },
  clearAction: function () {
    var me = this,
        i, line = me.args.line;
    for (i = 0; i < line.length; i++) {
      me.$.bodyContent.$.verifiedReturns.$['returnLine' + i].$.valueAttribute.setValue(null);
      me.$.bodyContent.$.verifiedReturns.$['returnLine' + i].$.valueAttribute.addStyles('background-color: none;');
    }
    me.$.bodyButtons.$.modalDialogButton.setDisabled(true);
    return;
  },
  cancelAction: function () {
    this.hide();
    return;
  },
  validateAction: function () {
    var me = this;
    me.args.returnLinesPopup.callbackExecutor();
    this.hide();
    return;
  },
  fieldChanged: function (inSender, inEvent) {
    var me = this,
        line = me.args.line,
        validAttribute, orderlineAttribute, orderlineProduct, inpProduct, inpAttribute, lineIndex, focusIndex, transformedAttValue;
    lineIndex = 0;
    line.forEach(function (theLine) {
      validAttribute = false;
      orderlineProduct = theLine.id;
      orderlineAttribute = theLine.attSetInstanceDesc;
      inpProduct = me.$.bodyContent.$.verifiedReturns.$['returnLine' + lineIndex].$.productId.getContent();
      inpAttribute = me.$.bodyContent.$.verifiedReturns.$['returnLine' + lineIndex].$.valueAttribute.getValue();
      inpAttribute = inpAttribute.replace(/\s+/, "");
      if (inpAttribute) {
        if ((orderlineAttribute !== inpAttribute) && (orderlineProduct === inpProduct)) {
          me.$.bodyContent.$.verifiedReturns.$['returnLine' + lineIndex].$.valueAttribute.addStyles('background-color: red;');
          validAttribute = false;
        } else {
          me.$.bodyContent.$.verifiedReturns.$['returnLine' + lineIndex].$.valueAttribute.addStyles('background-color: #6cb33f;');
          validAttribute = true;
          focusIndex = line.length === 0 ? 0 : lineIndex + 1;
          if (focusIndex < line.length) {
            me.$.bodyContent.$.verifiedReturns.$['returnLine' + focusIndex].$.valueAttribute.focus();
          }
        }
      }
      lineIndex++;
    });
    if (validAttribute) {
      me.$.bodyButtons.$.modalDialogButton.setDisabled(false);
    }
    return true;
  },
  executeOnShow: function () {
    var me = this,
        line = me.args.line,
        documentno = me.args.documentno,
        i;
    me.$.header.$.headerTitle.setContent(OB.I18N.getLabel('OBRETUR_ProductAttributeValueVerifiedReturnsDesc'));
    me.$.header.$.headerTitle.addStyles('font-size: 24px');
    me.$.header.$.documentno.setContent(documentno);
    me.$.header.$.documentno.addStyles('font-size: 24px');
    i = 0;
    me.$.bodyContent.$.verifiedReturns.destroyComponents();
    line.forEach(function (theLine) {
      var returnLine = me.$.bodyContent.$.verifiedReturns.createComponent({
        kind: 'OB.UI.ModalScrollerVerifiedReturn.ReturnLine',
        name: 'returnLine' + i
      });
      returnLine.$.valueAttribute.focus();
      returnLine.$.productName.setContent(theLine.name);
      returnLine.$.productId.setContent(theLine.id);
      returnLine.$.productId.hide();
      i++;
    });
    me.$.bodyButtons.$.modalDialogButton.setDisabled(true);
    this.$.headerCloseButton.hide();
    me.$.bodyContent.render();
  },
  initComponents: function () {
    this.inherited(arguments);
    this.$.header.createComponent({
      components: [{
        name: 'headerTitle',
        type: 'text'
      }, {
        name: 'documentno',
        type: 'text'
      }]
    });
  }
});

OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
  kind: 'OB.UI.ModalProductAttributeVerifiedReturns',
  name: 'modalProductAttributeVerifiedReturns'
});