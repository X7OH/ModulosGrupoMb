/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, GCNV, $ */

(function () {

  enyo.kind({
    name: 'GCNV.UI.SearchHeader',
    kind: 'OB.UI.ScrollableTableHeader',
    events: {
      onSearchAction: '',
      onClearAction: ''
    },
    components: [{
      style: 'padding: 10px;',
      components: [{
        style: 'display: table;',
        components: [{
          style: 'display: table-cell; width: 100%;',
          components: [{
            kind: 'OB.UI.SearchInput',
            name: 'filterText',
            style: 'width: 100%'
          }]
        }, {
          style: 'display: table-cell;',
          components: [{
            kind: 'OB.UI.SmallButton',
            name: 'clearButton',
            classes: 'btnlink-gray btn-icon-small btn-icon-clear',
            style: 'width: 100px; margin: 0px 5px 8px 19px;',
            ontap: 'clearAction'
          }]
        }, {
          style: 'display: table-cell;',
          components: [{
            kind: 'OB.UI.SmallButton',
            name: 'searchButton',
            classes: 'btnlink-yellow btn-icon-small btn-icon-search',
            style: 'width: 100px; margin: 0px 0px 8px 5px;',
            ontap: 'searchAction'
          }]
        }]
      }]
    }],
    clearAction: function () {
      this.$.filterText.setValue('');
      this.doClearAction();
    },
    disableFilterButtons: function (value) {
      this.$.searchButton.setDisabled(value);
      this.$.clearButton.setDisabled(value);
    },
    searchAction: function () {
      this.doSearchAction({
        filter: '%' + this.$.filterText.getValue() + '%'
      });
    }
  });


  enyo.kind({
    name: 'GCNV.UI.RenderGiftCard',
    kind: 'OB.UI.SelectButton',
    components: [{
      name: 'line',
      style: 'line-height: 23px;width: 100%;',
      components: [{
        components: [{
          style: 'float: left; text-align: left; width: 30%;',
          name: 'searchkey'
        }, {
          style: 'float: left; text-align:left; width: 70%;',
          name: 'businesspartner'
        }, {
          style: 'clear:both;'
        }]
      }, {
        style: 'color: #888888',
        name: 'product'
      }, {
        style: 'clear: both;'
      }]
    }],
    initComponents: function () {
      this.inherited(arguments);
      this.$.searchkey.setContent(this.model.get('searchKey'));
      this.$.businesspartner.setContent(this.model.get('businessPartner$_identifier'));
      if (this.model.get('type') === 'BasedOnGLItem') {
        this.$.product.setContent(this.model.get('category$_identifier'));
      } else if (this.model.get('type') === 'BasedOnGLItem') {
        this.$.product.setContent(this.model.get('product$_identifier'));
      }
    }
  });

  enyo.kind({
    name: 'GCNV.UI.SearchDialog',
    kind: 'OB.UI.Modal',
    topPosition: '125px',
    events: {
      onHideThisPopup: '',
      onShowPopup: '',
      onAddProduct: ''
    },
    handlers: {
      onSearchAction: 'searchAction',
      onClearAction: 'clearAction',
      onChangePaidReceipt: 'changePaidReceipt'
    },
    changedParams: function (value) {},
    body: {
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [{
          style: 'border-bottom: 1px solid #cccccc;',
          classes: 'row-fluid',
          components: [{
            classes: 'span12',
            components: [{
              name: 'listgiftcards',
              kind: 'OB.UI.ScrollableTable',
              scrollAreaMaxHeight: '400px',
              renderHeader: 'GCNV.UI.SearchHeader',
              renderLine: 'GCNV.UI.RenderGiftCard',
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
      }]
    },
    clearAction: function (inSender, inEvent) {
      this.$.body.$.listgiftcards.$.theader.$.searchHeader.$.filterText.clear();
      this.prsList.reset();
      return true;
    },
    disableFilters: function (value) {
      this.$.body.$.listgiftcards.$.theader.$.searchHeader.disableFilterButtons(value);
    },
    searchAction: function (inSender, inEvent) {

      var me = this;

      me.disableFilters(true);

      this.$.body.$.listgiftcards.$.tempty.hide();
      this.$.body.$.listgiftcards.$.tbody.hide();
      this.$.body.$.listgiftcards.$.tlimit.hide();
      this.$.body.$.renderLoading.show();

      OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.ListGiftCard', {
        filter: inEvent.filter,
        _limit: GCNV.Model.GiftCard.prototype.dataLimit + 1
      }, function (result) {
        me.$.body.$.renderLoading.hide();
        me.prsList.reset(result);
        me.disableFilters(false);
      }, function (error) {
        me.$.body.$.renderLoading.hide();
        me.gcsList.reset();
        me.$.body.$.listgiftcards.$.tempty.show();
        me.disableFilters(false);
        var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
        me.doShowPopup({
          popup: 'GCNV_UI_Message',
          args: {
            message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
          }
        });
      });

      return true;
    },
    changePaidReceipt: function (inSender, inEvent) {
      this.model.get('orderList').addPaidReceipt(inEvent.newPaidReceipt);
      return true;
    },
    executeOnShow: function () {

      this.clearAction();
      return true;
    },
    showGiftCard: function (giftcardid, giftcardmodel) {

      var me = this;

      OB.UTIL.HookManager.executeHooks('GCNV_PreFindGiftCard', {
        giftcardid: giftcardid,
        giftcard: giftcardmodel
      }, function (args) {
        if (args && args.cancellation) {
          return;
        }
        OB.UI.GiftCardUtils.service('org.openbravo.retail.giftcards.FindGiftCard', {
          giftcard: giftcardid
        }, function (result) {
          var giftcard = result.data;

          OB.UTIL.HookManager.executeHooks('GCNV_PostFindGiftCard', {
            giftcard: giftcard
          }, function (args) {
            if (args && args.cancellation) {
              return;
            }

            me.doHideThisPopup();
            me.doShowPopup({
              popup: 'GCNV_UI_Details',
              args: {
                giftcard: args.giftcard,
                view: me,
                receipt: me.model.get('order')
              }
            });
          });

        }, function (error) {
          var msgsplit = (error.exception.message || 'GCNV_ErrorGenericMessage').split(':');
          me.doShowPopup({
            popup: 'GCNV_UI_Message',
            args: {
              message: OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1))
            }
          });
        });
      });
    },
    init: function (model) {
      this.model = model;

      this.prsList = new OB.Collection.GiftCardList();
      this.$.body.$.listgiftcards.setCollection(this.prsList);


      this.prsList.on('click', function (model) {
        this.showGiftCard(model.get('searchKey'), model);
      }, this);
    },
    initComponents: function () {
      this.header = OB.I18N.getLabel('GCNV_LblGiftCards');
      this.inherited(arguments);
    }
  });

  // Register modal in the menu...
  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'GCNV.UI.SearchDialog',
    name: 'GCNV_UI_SearchDialog'
  });

}());