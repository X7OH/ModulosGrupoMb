/*
 ************************************************************************************
 * Copyright (C) 2013-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo */

enyo.kind({
  kind: 'OB.UI.Popup',
  name: 'OB.OBPOSPointOfSale.UI.combo.popup',
  classes: 'modal modal-poup',
  topPosition: '125px',
  autoDismiss: false,
  style: 'height: 400px;',
  events: {
    onShowPopup: ''
  },
  initComponents: function () {
    this.inherited(arguments);
    this.$.header.createComponent(this.header);
    this.$.body.createComponent(this.body);
  },
  components: [{
    style: 'padding: 10px;',
    components: [{
      name: 'header'
    }, {
      name: 'body',
      classes: 'modal-body'
    }]
  }],
  header: {
    kind: 'OB.OBPOSPointOfSale.UI.combo.popup.header',
    name: 'comboHeader'
  },
  body: {
    kind: 'OB.OBPOSPointOfSale.UI.combo.popup.body',
    name: 'comboBody',
    style: 'margin-top: 15px;'
  },
  headerText: 'Header',
  published: {
    cStatus: {}
    // Object with the following structure:
    //   cFamilyId : {
    //     qty: X, (qty needed for this family)
    //     cProductId-1: X, (qty already selected for this product)
    //     cProductId-n: X, (qty already selected for this product)
    //   }
    // If any product has no quantity selected, it is not present in the object
  },
  cFamilyButtons: {},
  selectedCFamily: null,
  countCFamilySelectedCProducts: function (cFamilyId) {
    // Returns how many combo products have been selected within this family
    var cProductId, cFamilyStatus = this.cStatus[cFamilyId],
        count = 0;
    for (cProductId in cFamilyStatus) {
      if (cFamilyStatus.hasOwnProperty(cProductId)) {
        if (cProductId !== 'qty') {
          count = count + cFamilyStatus[cProductId];
        }
      }
    }
    return count;
  },
  setCProductQty: function (cProductId, cFamilyId, qty) {
    var cFamilyButton, isComplete = false;
    if (!qty) {
      delete this.cStatus[cFamilyId][cProductId];
    } else {
      this.cStatus[cFamilyId][cProductId] = qty;
    }
    this.cFamilyButtons[cFamilyId].setCurrentQty(this.countCFamilySelectedCProducts(cFamilyId));
    for (cFamilyButton in this.cFamilyButtons) {
      if (this.cFamilyButtons.hasOwnProperty(cFamilyButton)) {
        if (this.cFamilyButtons[cFamilyButton].isComplete) {
          isComplete = true;
        } else {
          isComplete = false;
          break;
        }
      }
    }
    if (isComplete) {
      this.$.header.$.comboHeader.$.applyButton.setDisabled(false);
    } else {
      this.$.header.$.comboHeader.$.applyButton.setDisabled(true);
    }
  },
  getCProductQty: function (cProductId) {
    var cFamilyId, qty = 0;
    for (cFamilyId in this.cStatus) {
      if (this.cStatus.hasOwnProperty(cFamilyId)) {
        if (this.cStatus[cFamilyId][cProductId]) {
          qty = this.cStatus[cFamilyId][cProductId];
          break;
        }
      }
    }
    return qty;
  },
  addCProductsToOrder: function (attrs) {
    // Adds all the selected combo products to the order/ticket
    var me = this,
        cFamilyId, cProductId;

    function errorCallback(tx, error) {
      OB.UTIL.showError('OBDAL error: ' + error);
    }

    function addCProductToOrder(cProductId, qty) {
      OB.Dal.find(OB.Model.ComboProduct, {
        id: cProductId
      }, function (dataCProducts) {
        var criteria;
        if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
          criteria = {
            'id': dataCProducts.models[0].get('product')
          };
        } else {
          criteria = {};
          var remoteCriteria = [];
          var productId = {
            columns: ['id'],
            operator: 'equals',
            value: dataCProducts.models[0].get('product'),
            isId: true
          };
          remoteCriteria.push(productId);
          criteria.remoteFilters = remoteCriteria;
        }

        OB.Dal.find(OB.Model.Product, criteria, function (dataProducts) {
          if (dataProducts.length === 1) {
            var cAttrs = attrs ? JSON.parse(JSON.stringify(attrs)) : attrs,
                options = {
                comboId: me.args.comboId
                };
            me.args.receipt.addProduct(dataProducts.models[0], qty, options, cAttrs);
          }
        }, errorCallback);
      }, errorCallback);
    }

    for (cFamilyId in this.cStatus) {
      if (this.cStatus.hasOwnProperty(cFamilyId)) {
        for (cProductId in this.cStatus[cFamilyId]) {
          if (this.cStatus[cFamilyId].hasOwnProperty(cProductId)) {
            if (cProductId !== 'qty') {
              addCProductToOrder(cProductId, this.cStatus[cFamilyId][cProductId]);
            }
          }
        }
      }
    }
  },
  init: function () {
    var me = this;
    this.inherited(arguments);

    this.comboFamilies = new OB.Collection.ComboFamilyList();
    this.$.body.$.comboBody.$.leftBar.setCollection(this.comboFamilies);
    this.comboFamilies.on('selected', function (comboFamily) {
      if (comboFamily) {
        this.selectedCFamily = comboFamily.get('id');
        this.loadProducts(comboFamily);
      }
    }, this);
    this.comboFamilies.on('reset', function (comboFamily) {
      if (comboFamily.length === 0) {
        me.setCStatus({});
        return;
      }
      comboFamily.models.forEach(function (model) {
        me.cStatus[model.get('id')] = {};
        me.cStatus[model.get('id')].qty = model.get('quantity');

        // An special case should be handled: if a Family has only one product,
        // this product should be automatically added any times as it be needed.

        function errorCallback(tx, error) {
          OB.UTIL.showError('OBDAL error: ' + error);
        }

        function successCallback(dataProducts) {
          var cFamilyId, cProductId, qty;
          if (dataProducts.length === 1) {
            cFamilyId = dataProducts.models[0].get('obcomboFamily');
            cProductId = dataProducts.models[0].get('id');
            qty = me.cStatus[cFamilyId].qty;
            me.setCProductQty(cProductId, cFamilyId, qty);
            if (me.selectedCFamily === cFamilyId) {
              // If the selected cFamily is affected, it should be reloaded to show
              // the selected quanitity in the cProduct button
              me.loadProducts(cFamilyId);
            }
          }
        }

        OB.Dal.find(OB.Model.ComboProduct, {
          obcomboFamily: model.get('id')
        }, successCallback, errorCallback);
      });
    }, this);

    this.comboProducts = new OB.Collection.ComboProductList();
    this.$.body.$.comboBody.$.rightBar.setCollection(this.comboProducts);
  },
  loadFamilies: function (combo) {
    var me = this,
        comboId;

    me.setCStatus({});

    if (typeof combo === 'string') {
      comboId = combo;
    } else {
      comboId = combo.id;
      this.headerText = combo.get('_identifier');
      this.$.header.$.comboHeader.$.headerMessage.setContent(this.headerText);
    }

    function errorCallback(tx, error) {
      OB.UTIL.showError('OBDAL error: ' + error);
    }

    function successCallbackFamilies(dataFamilies) {
      me.comboProducts.reset(null); // Erase previous existing displayed 'Products'
      if (dataFamilies.length > 0) {
        me.comboFamilies.reset(dataFamilies.models);
      } else {
        me.comboFamilies.reset(null);
      }
    }

    OB.Dal.find(OB.Model.ComboFamily, {
      priceAdjustment: comboId,
      _orderByClause: '_identifier ASC'
    }, successCallbackFamilies, errorCallback);
  },
  loadProducts: function (comboFamily) {
    var me = this,
        cFamilyId;

    if (typeof comboFamily === 'string') {
      cFamilyId = comboFamily;
    } else {
      cFamilyId = comboFamily.id;
      if (this.headerText) {
        this.$.header.$.comboHeader.$.headerMessage.setContent(this.headerText + ' - ' + comboFamily.get('name'));
      }
    }

    function errorCallback(tx, error) {
      OB.UTIL.showError('OBDAL error: ' + error);
    }

    function successCallbackProducts(dataProducts) {
      if (dataProducts.length > 0) {
        me.comboProducts.reset(dataProducts.models);
      } else {
        me.comboProducts.reset(null);
      }
    }

    OB.Dal.find(OB.Model.ComboProduct, {
      obcomboFamily: cFamilyId,
      _orderByClause: '_identifier ASC'
    }, successCallbackProducts, errorCallback);
  }
});


enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.combo.popup.header',
  classes: 'subwindowheader',
  components: [{
    name: 'closebutton',
    tag: 'div',
    style: 'float: right;',
    components: [{
      kind: 'OB.OBPOSPointOfSale.UI.combo.popup.header.button',
      name: 'applyButton',
      i18nLabel: 'OBMOBC_LblApply',
      disabled: true,
      tap: function () {
        var me = this;
        OB.UTIL.HookManager.executeHooks('OBPOS_PreTapComboApplyButton', {
          context: this.owner.owner.owner,
          comboId: this.owner.owner.owner.args.comboId,
          receipt: this.owner.owner.owner.args.receipt
        }, function (args) {
          if (args && args.cancelOperation) {
            return;
          }
          me.owner.owner.owner.addCProductsToOrder(args.attrs);
          me.doHideThisPopup();
        });
      }
    }, {
      kind: 'OB.OBPOSPointOfSale.UI.combo.popup.header.button',
      name: 'cancelButton',
      i18nContent: 'OBMOBC_LblCancel',
      tap: function () {
        this.doHideThisPopup();
      }
    }]
  }, {
    classes: 'subwindowheadertext',
    name: 'headerMessage'
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.$.closebutton.headerContainer = this.$.closebutton.parent;
    this.$.closebutton.tap = this.onTapCloseButton;
  }
});

enyo.kind({
  kind: 'OB.UI.SmallButton',
  name: 'OB.OBPOSPointOfSale.UI.combo.popup.header.button',
  style: 'width: 100px; height: 30px; margin: 0px 0px 10px 10px;',
  events: {
    onHideThisPopup: ''
  },
  classes: 'btnlink-yellow btn-icon-small'
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.combo.popup.body',
  components: [{
    style: 'text-align: center; float: left; width: 39%;',
    components: [{
      kind: 'OB.UI.ScrollableTable',
      name: 'leftBar',
      listStyle: 'list',
      scrollAreaMaxHeight: '340px',
      renderHeader: 'OB.UI.ScrollableTableHeader',
      renderEmpty: 'OB.UI.RenderEmpty',
      renderLine: 'OB.OBPOSPointOfSale.UI.combo.popup.body.cFamilyButton'
    }]
  }, {
    style: 'float: left; width: 60%;',
    components: [{
      kind: 'OB.UI.ScrollableTable',
      name: 'rightBar',
      listStyle: 'list',
      scrollAreaMaxHeight: '340px',
      renderHeader: 'OB.UI.ScrollableTableHeader',
      renderEmpty: 'OB.UI.RenderEmpty',
      renderLine: 'OB.OBPOSPointOfSale.UI.combo.popup.body.cProductButton'
    }]
  }, {
    style: 'clear: both'
  }]
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.combo.popup.body.cFamilyButton',
  kind: 'OB.UI.SelectButton',
  classes: 'btnlink-left-toolbar combo-family-button',
  components: [{
    name: 'lblLeft',
    classes: 'combo-family-button-label',
    content: ''
  }, {
    name: 'lblRight',
    classes: 'combo-family-button-units',
    content: ''
  }],
  published: {
    currentQty: null,
    totalQty: null,
    isComplete: false
  },
  currentQtyChanged: function (inOldValue) {
    this.setQtyLabel();
  },
  totalQtyChanged: function (inOldValue) {
    this.setQtyLabel();
  },
  setQtyLabel: function () {
    var currentQty = this.getCurrentQty(),
        totalQty = this.getTotalQty();

    this.$.lblRight.setContent(currentQty + ' / ' + totalQty);
    if (currentQty === totalQty) {
      this.addClass('completed');
      this.removeClass('exceded');
      this.setIsComplete(true);
    } else if (currentQty > totalQty) {
      this.removeClass('completed');
      this.addClass('exceded');
      this.setIsComplete(false);
    } else {
      this.removeClass('completed');
      this.removeClass('exceded');
      this.setIsComplete(false);
    }
  },
  initComponents: function () {
    this.inherited(arguments);
    this.$.lblLeft.setContent(this.model.get('name'));
    this.setCurrentQty(0);
    this.setTotalQty(this.model.get('quantity'));
    this.owner.owner.owner.owner.owner.owner.cFamilyButtons[this.model.get('id')] = this; // Store each button in the popup to be able later to direct manipulation
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.combo.popup.body.cProductButton',
  style: 'border-bottom: 1px solid #cccccc;',
  events: {
    onHidePopup: ''
  },
  components: [{
    style: 'float: left;',
    components: [{
      kind: 'OB.UI.SelectButton',
      style: 'display: table; width: 220px; border: 0px;',
      tap: function () {
        var currentValue = this.owner.$.input.getValue();
        currentValue = currentValue || 0;
        currentValue = parseInt(currentValue, 10) + 1;
        this.owner.$.countAndRemove.updateCount(currentValue);
        this.owner.model.trigger('click', this.owner.model);
      },
      components: [{
        style: 'display: table-cell; float: left; width: 25%',
        components: [{
          kind: 'OB.UI.Thumbnail',
          name: 'thumbnail'
        }]
      }, {
        name: 'cont',
        style: 'display: table-cell;',
        components: [{
          name: 'identifier',
          style: 'float: left; padding: 0px 0px 0px 10px;'
        }, {
          style: 'clear:both;'
        }, {
          name: 'price',
          style: 'float: left; font-weight:bold; padding: 10px 0px 0px 10px;'
        }]
      }]
    }]
  }, {
    name: 'countAndRemove',
    style: 'float: right; padding: 15px 0px 0px 10px;',
    updateCount: function (value, avoidPropagation) {
      // First we update the count input and its visualization state
      var me = this;
      if (typeof value === 'undefined' || value === null) {
        // If there is no value passed as first argument, probably it has been called from a keydown
        // so there is a timeout to ensure the value is refreshed
        setTimeout(function () {
          me.updateCount(me.owner.$.input.getValue(), avoidPropagation);
        }, 100);
        return;
      }
      if (value !== '') {
        value = parseInt(value, 10);
        if (isNaN(value)) {
          value = 0;
        }
      }

      if (value === 0 || value === '0') {
        this.applyStyle('display', 'none');
      } else {
        this.applyStyle('display', 'block');
      }
      if (this.owner.$.input.getValue().toString() !== value.toString()) {
        this.owner.$.input.setValue(value);
      }

      // Then we call super 'setCProductQty' to propagate the change
      if (!avoidPropagation && value !== '') {
        this.owner.owner.owner.owner.owner.owner.owner.setCProductQty(this.owner.model.get('id'), this.owner.model.get('obcomboFamily'), parseInt(value, 10));
      }
    },
    getCount: function () {
      var count = this.owner.$.input.getValue();
      if (!count) {
        count = 0;
      }
      count = parseInt(count, 10);
      if (isNaN(count)) {
        count = 0;
      }
      return count;
    },
    components: [{
      kind: 'enyo.Input',
      type: 'text',
      name: 'input',
      classes: 'input-login',

      handlers: {
        onkeydown: 'keydownHandler',
        onchange: 'changeHandler'
      },
      keydownHandler: function (inSender, inEvent) {
        var charCode = inEvent.which || inEvent.keyCode;
        if ((charCode < 48 || (charCode > 57 && charCode < 96) || charCode > 105) && charCode !== 46 && charCode !== 8 && charCode !== 37 && charCode !== 39 && charCode !== 35 && charCode !== 36) {
          //Only allow numbers / Del / Backspace / arrows / Start / End
          inEvent.preventDefault();
          return false;
        }
        this.owner.$.countAndRemove.updateCount();
      },
      changeHandler: function (inSender, inEvent) {
        // To avoid empty or invalid input values be in the input once the input blurs
        this.owner.$.countAndRemove.updateCount(this.owner.$.countAndRemove.getCount());
      },

      style: 'width: 30px; text-align: center; height: 27px; margin: 0px 0px 10px 0px;'
    }, {
      kind: 'OB.UI.SmallButton',
      name: 'removeButton',
      label: '-',
      style: 'width: 42px; height: 38px; padding: 0px; margin: 0px 0px 10px 10px; border: 1px solid #cccccc;',
      classes: 'btnlink-white btn-icon-small btn-icon-substract',
      tap: function () {
        var currentValue = this.owner.$.input.getValue();
        currentValue = currentValue || 0;
        currentValue = parseInt(currentValue, 10) - 1;
        if (currentValue < 0) {
          currentValue = 0;
        }
        this.owner.$.countAndRemove.updateCount(currentValue);
        this.owner.model.trigger('click', this.owner.model);
      }
    }]
  }, {
    style: 'clear: both;'
  }],

  initComponents: function () {
    this.inherited(arguments);
    var me = this,
        productId = this.model.get('product');

    function errorCallback(tx, error) {
      OB.UTIL.showError('OBDAL error: ' + error);
    }

    function successCallbackProduct(dataProducts) {
      if (dataProducts.length === 1) {
        var model = dataProducts.models[0],
            name = model.get('_identifier'),
            price = OB.I18N.formatCurrency(model.get('standardPrice')),
            image;

        //Issue 28397: If the user changes very fast between families, it may happen
        //that the callback is executed after the button has been destroyed. In that
        //case all components have been destroyed and therefore we should do nothing
        if (me.destroyed) {
          return;
        }
        me.$.identifier.setContent(name);
        me.$.price.setContent(price);
        if (OB.MobileApp.model.get('permissions')['OBPOS_retail.productImages']) {
          image = OB.UTIL.getImageURL(model.get('id'));
          me.$.thumbnail.setImgUrl(image);
        } else {
          image = model.get('img');
          me.$.thumbnail.setImg(image);
        }
      } else {
        me.doHidePopup({
          popup: 'OBCOMBO_Popup'
        });
        OB.UTIL.showError(OB.I18N.getLabel('OBCOMBO_PRODUCT_NOTIN_ASSORTMENT'));
      }
    }
    var criteria;
    if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
      criteria = {
        'id': productId
      };
    } else {
      criteria = {};
      var remoteCriteria = [];
      var product = {
        columns: ['id'],
        operator: 'equals',
        value: productId,
        isId: true
      };
      remoteCriteria.push(product);
      criteria.remoteFilters = remoteCriteria;
    }

    OB.Dal.find(OB.Model.Product, criteria, successCallbackProduct, errorCallback);

    this.$.countAndRemove.updateCount(this.owner.owner.owner.owner.owner.owner.getCProductQty(this.model.get('id')), true);
  }
});


OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
  kind: 'OB.OBPOSPointOfSale.UI.combo.popup',
  name: 'OBCOMBO_Popup'
});