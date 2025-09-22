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

  /*Modal definition*/
  enyo.kind({
    name: 'GCNV.UI.ModalGiftCards',
    kind: 'OB.UI.ModalSelector',
    topPosition: '75px',
    modalClass: 'modal-bpdialog',
    getFilterSelectorTableHeader: function () {
      return this.$.body.$.giftcards.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector;
    },
    getAdvancedFilterBtn: function () {
      return this.$.body.$.giftcards.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.advancedFilterWindowButtonGC;
    },
    getAdvancedFilterDialog: function () {
      return 'modalAdvancedFilterGC';
    },
    executeOnShow: function () {
      if (!this.initialized || (this.args && _.keys(this.args).length > 0)) {
        this.selectorHide = false;
        this.initialized = true;
        this.initializedArgs = this.args;
        var column = _.find(OB.Model.GiftCardFilter.getProperties(), function (prop) {
          return prop.name === 'businessPartner';
        }, this);

        var bp = this.calculateBusinessPartner();
        if (!OB.UTIL.isNullOrUndefined(bp)) {
          column.preset.id = bp.get('id');
          column.preset.name = bp.get('_identifier');
        } else {
          column.preset.id = '';
          column.preset.name = '';
        }
        this.initSelector();
        this.getFilterSelectorTableHeader().clearFilter();
        this.$.header.setContent(this.args.header);
        this.$.body.$.giftcards.gcsList.reset();
        this.$.body.$.giftcards.$.stGCAssignToReceipt.$.tempty.show();
      } else {
        this.args = this.initializedArgs;
      }
    },
    calculateBusinessPartner: function () {
      var bp;
      if (OB.MobileApp.view.currentWindow === 'retail.pointofsale') {
        var mOrderList = this.model.get('multiOrders').get('multiOrdersList');
        if (mOrderList.length) {
          var i;
          bp = mOrderList.models[0].get('bp');
          for (i = 1; i < mOrderList.models.length; i++) {
            if (mOrderList.models[i].get('bp').get('id') !== bp.get('id')) {
              bp = OB.MobileApp.model.get('businessPartner');
              break;
            }
          }
        } else {
          bp = this.model.get('order').get('bp');
        }
        return bp;
      } else {
        return null;
      }
    },

    i18nHeader: 'GCNV_LblCreditNote',
    clearResult: true,
    body: {
      kind: 'GCNV.UI.ListGiftCards',
      name: 'giftcards'
    }
  });

  /*scrollable table (body of modal)*/
  enyo.kind({
    name: 'GCNV.UI.ListGiftCards',
    classes: 'row-fluid',
    events: {
      onHideSelector: '',
      onShowSelector: ''
    },
    handlers: {
      onSearchAction: 'searchAction',
      onClearFilterSelector: 'clearFilterSelector',
      onCanHidePopup: 'canHidePopup'
    },
    components: [{
      classes: 'span12',
      components: [{
        style: 'border-bottom: 1px solid #cccccc;',
        classes: 'row-fluid',
        components: [{
          classes: 'span12',
          components: [{
            name: 'stGCAssignToReceipt',
            kind: 'OB.UI.ScrollableTable',
            classes: 'bp-scroller',
            scrollAreaMaxHeight: '400px',
            renderHeader: 'GCNV.UI.ModalGcScrollableHeader',
            renderLine: 'OB.UI.ListGCsSelectorLine',
            renderEmpty: 'OB.UI.RenderEmpty'
          }, {
            name: 'renderLoading',
            style: 'border-bottom: 1px solid #cccccc; padding: 20px; text-align: center; font-weight: bold; font-size: 30px; color: #cccccc',
            showing: false,
            initComponents: function () {
              this.setContent(OB.I18N.getLabel('OBPOS_LblLoading'));
            }
          }]
        }]
      }]
    }],
    hidePopup: true,
    clearFilterSelector: function (inSender, inEvent) {
      this.gcsList.reset();
      return true;
    },
    searchAction: function (inSender, inEvent) {
      var me = this;

      this.$.stGCAssignToReceipt.$.tempty.hide();
      this.$.stGCAssignToReceipt.$.tbody.hide();
      this.$.stGCAssignToReceipt.$.tlimit.hide();
      this.$.renderLoading.show();

      function successCallbackGCs(dataGcs) {
        var docNoAdvanceFilter, entityFilterText;

        docNoAdvanceFilter = _.find(inEvent.filters, function (filter) {
          return filter.column === 'gci.searchKey';
        });
        entityFilterText = docNoAdvanceFilter ? docNoAdvanceFilter.value : '';

        me.$.renderLoading.hide();
        if (dataGcs && dataGcs.length) {
          if (dataGcs.length === 1 && dataGcs.at(0).get('searchKey') === entityFilterText && dataGcs.at(0).get('alertStatus') !== 'C') {
            me.showDetails(dataGcs.at(0));
          }
          this.hidePopup = dataGcs.at(0).get('alertStatus') !== 'C' ? true : false;
          _.each(dataGcs.models, function (gc) {
            var filter = '';
            if (me.owner.owner.args.giftcardtype instanceof Array) {
              if (gc.get('type') === 'BasedOnCreditNote') {
                filter += ' / ' + OB.I18N.getLabel('GCNV_LblCreditNote');
              } else if (gc.get('type') === 'BasedOnGLItem') {
                filter += ' / ' + OB.I18N.getLabel('GCNV_LblGiftCardsCertificate');
              } else if (gc.get('type') === 'BasedOnProductGiftCard') {
                filter += ' / ' + OB.I18N.getLabel('GCNV_LblDialogGiftCard');
              } else if (gc.get('type') === 'BasedOnVoucher') {
                filter += ' / ' + OB.I18N.getLabel('GCNV_LblDialogGiftVoucher');
              }
            }
            _.each(inEvent.filters, function (flt, index) {
              if (flt.column !== 'gci.searchKey') {
                var column = _.find(OB.Model.GiftCardFilter.getProperties(), function (col) {
                  return col.column === flt.column;
                });
                if (column) {
                  if (column.isSelector) {
                    filter += ' / ' + (flt.caption ? flt.caption : '');
                  } else {
                    filter += ' / ' + (gc.get(column.name) ? gc.get(column.name) : '');
                  }
                }
              }
            });
            gc.set('_identifier', gc.get('searchKey'));
            gc.set('filter', filter);
          });
          me.gcsList.reset(dataGcs.models);
          me.$.stGCAssignToReceipt.$.tbody.show();
        } else {
          me.gcsList.reset();
          me.$.stGCAssignToReceipt.$.tempty.show();
        }
      }

      function errorCallback(tx, error) {
        OB.UTIL.showError("OBDAL error: " + error);
      }

      var criteria = this.owner.owner.getRemoteCriteria(OB.Model.GiftCardFilter, inEvent.filters, inEvent.orderby);
      if (this.owner.owner.args.giftcardtype) {
        criteria.remoteFilters.push({
          columns: ['type'],
          operator: 'equals',
          value: this.owner.owner.args.giftcardtype
        });
      }

      if (!criteria._orderByClause) {
        criteria._orderByClause = 'gci.searchKey desc';
      }

      OB.Dal.find(OB.Model.GiftCardFilter, criteria, successCallbackGCs, errorCallback, undefined, this);

      return true;
    },
    showDetails: function (model) {
      var me = this;
      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.FindGiftCard', {
        giftcard: model.get('searchKey')
      }, function (result) {

        var giftcard = result.data,
            gcSelector = me.owner.owner;

        me.doHideSelector();
        gcSelector.initializedArgs.keyboard.doShowPopup({
          popup: 'GCNV_UI_Details',
          args: {
            giftcard: giftcard,
            view: gcSelector.initializedArgs.keyboard,
            receipt: gcSelector.initializedArgs.keyboard.receipt,
            amount: gcSelector.initializedArgs.amount,
            notDefaultAction: gcSelector.initializedArgs.notDefaultAction,
            action: gcSelector.initializedArgs.action,
            parentDialog: gcSelector,
            cancelAction: function () {
              me.doShowSelector();
            }
          }
        });
      }, function (error) {
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        me.owner.owner.initializedArgs.keyboard.doShowPopup({
          popup: 'GCNV_UI_Message',
          args: {
            message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
          }
        });
      });
    },
    canHidePopup: function (inSender, inEvent) {
      inEvent.callback(this.hidePopup ? true : false);
    },
    gcsList: null,
    init: function (model) {
      var me = this;
      this.gcsList = new Backbone.Collection();
      this.$.stGCAssignToReceipt.setCollection(this.gcsList);
      this.gcsList.on('click', function (model) {
        if (me.target && me.target.startsWith('filterSelectorButton_')) {
          me.doChangeFilterSelector({
            selector: {
              name: me.target.substring('filterSelectorButton_'.length),
              value: model.get('searchKey'),
              text: model.id
            }
          });
        } else {
          if (model.get('alertStatus') !== 'C') {
            me.showDetails(model);
          } else {
            OB.UTIL.showError(OB.I18N.getLabel('GCNV_ClosedGiftCard'));
          }
        }
      }, this);
    }
  });

  enyo.kind({
    name: 'GCNV.UI.ModalGcScrollableHeader',
    kind: 'OB.UI.ScrollableTableHeader',
    components: [{
      style: 'padding: 10px;',
      kind: 'OB.UI.FilterSelectorTableHeader',
      name: 'filterSelector',
      filters: OB.Model.GiftCardFilter.getProperties()
    }, {
      style: 'padding: 7px;',
      showing: true,
      handlers: {
        onSetShow: 'setShow'
      },
      setShow: function (inSender, inEvent) {
        this.setShowing(inEvent.visibility);
      },
      components: [{
        style: 'display: table; width: 100%',
        components: [{
          style: 'display: table-cell; text-align: center; ',
          components: [{
            kind: 'GCNV.UI.AdvancedFilterWindowButtonGC',
            name: 'advancedFilterWindowButtonGC'
          }]
        }]
      }]
    }]
  });

  enyo.kind({
    name: 'GCNV.UI.AdvancedFilterWindowButtonGC',
    kind: 'OB.UI.ButtonAdvancedFilter',
    dialog: 'modalAdvancedFilterGC'
  });

  enyo.kind({
    name: 'GCNV.UI.ModalAdvancedFilterGC',
    kind: 'OB.UI.ModalAdvancedFilters',
    initComponents: function () {
      this.inherited(arguments);
      this.setFilters(OB.Model.GiftCardFilter.getProperties());
    }
  });

  /*items of collection*/
  enyo.kind({
    name: 'OB.UI.ListGCsSelectorLine',
    kind: 'OB.UI.FilterSelectorRenderLine',
    events: {
      onCanHidePopup: ''
    },
    canHidePopup: function () {
      this.doCanHidePopup({
        callback: function (canHide) {
          return canHide;
        }
      });
    }
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'GCNV.UI.ModalGiftCards',
    name: 'GCNV_UI_ModalGiftCards'
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSCashMgmt.UI.CashManagement', {
    kind: 'GCNV.UI.ModalGiftCards',
    name: 'GCNV_UI_ModalGiftCards'
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'GCNV.UI.ModalAdvancedFilterGC',
    name: 'modalAdvancedFilterGC'
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSCashMgmt.UI.CashManagement', {
    kind: 'GCNV.UI.ModalAdvancedFilterGC',
    name: 'modalAdvancedFilterGC'
  });

}());