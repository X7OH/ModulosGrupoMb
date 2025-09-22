(function () {

  var superThis;
  /* ******************************************************************* */ 	
  /* MODAL PRINCIPAL QUE ES LLAMADO DESDE GiftsCardUtils.consumeGiftCard */
  /* ******************************************************************* */	
  enyo.kind({
    name: 'SRGC.UI.ModalGiftCard',
    kind: 'OB.UI.ModalSelector',
    topPosition: '75px',
    modalClass: 'modal-bpdialog',
    getFilterSelectorTableHeader: function () {
      return this.$.body.$.giftcardsretail.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector;
    },
    getAdvancedFilterBtn: function () {
      return this.$.body.$.giftcardsretail.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.advancedFilterWindowButtonGC;
    },
    getAdvancedFilterDialog: function () {
      return 'modalAdvancedFilterGC';
    },
    executeOnShow: function () {
      superThis = this;

      if (!this.initialized || (this.args && _.keys(this.args).length > 0)) {
        this.selectorHide = false;
        this.initialized = true;
        this.initializedArgs = this.args;
        var column = _.find(OB.Model.GiftCardFilterRetail.getProperties(), function (prop) {
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
        this.$.body.$.giftcardsretail.gcsList.reset();
        this.$.body.$.giftcardsretail.$.stGCAssignToReceipt.$.tempty.show();
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

    i18nHeader: 'SRGC_LblCreditNote',
    clearResult: true,
    
    /* ************************************************ */ 	
    /* CUERPO DE MODAL PRINCIPAL SRGC.UI.ListGiftCards  */
    /* ************************************************ */    
    body: {
      kind: 'SRGC.UI.ListGiftCards',
      name: 'giftcardsretail'
    }
    /* ************************************************* */ 	
    /* ************************************************* */
    
  });
  /* ******************************************************************* */ 	
  /* ******************************************************************* */  

  /* ********************************************************************* */
  /*               scrollable table (body of modal)                       */
  /* CUERPO DEL MODAL PRINCIPAL DONDE SE HACE LA BUSQUEDA DE LAS TARJETAS */
  /* ********************************************************************* */
  enyo.kind({
    name: 'SRGC.UI.ListGiftCards',
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
            renderHeader: 'SRGC.UI.ModalGcScrollableHeader',
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
      var longText = me.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector.$.entityFilterText.getValue().length;

      if(longText > 11){

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
                  filter += ' / ' + OB.I18N.getLabel('SRGC_LblCreditNote');
                } else if (gc.get('type') === 'BasedOnGLItem') {
                  filter += ' / ' + OB.I18N.getLabel('SRGC_LblGiftCardsCertificate');
                } else if (gc.get('type') === 'BasedOnProductGiftCard') {
                  filter += ' / ' + OB.I18N.getLabel('SRGC_LblDialogGiftCard');
                } else if (gc.get('type') === 'BasedOnVoucher') {
                  filter += ' / ' + OB.I18N.getLabel('SRGC_LblDialogGiftVoucher');
                }
              }
              _.each(inEvent.filters, function (flt, index) {
                if (flt.column !== 'gci.searchKey') {
                  var column = _.find(OB.Model.GiftCardFilterRetail.getProperties(), function (col) {
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

        var criteria = this.owner.owner.getRemoteCriteria(OB.Model.GiftCardFilterRetail, inEvent.filters, inEvent.orderby);
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

        OB.Dal.find(OB.Model.GiftCardFilterRetail, criteria, successCallbackGCs, errorCallback, undefined, this);

        return true;        
      } 
    },
    showDetails: function (model) {
      var me = this;
      OB.UI.GiftCardUtilsRetail.service('ec.com.sidesoft.retail.giftcard.FindGiftCard', {
        giftcard: model.get('searchKey')
      }, function (result) {

        var giftcard = result.data,
            gcSelector = me.owner.owner;

        me.doHideSelector();
        gcSelector.initializedArgs.keyboard.doShowPopup({
        	popup: 'SRGC_UI_Details',	
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
        var msgsplit = (error.exception.message || 'SRGC_ErrorGenericMessage').split(':');
        me.owner.owner.initializedArgs.keyboard.doShowPopup({
          popup: 'SRGC_UI_Message',
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
            OB.UTIL.showError(OB.I18N.getLabel('SRGC_ClosedGiftCard'));
          }
        }
      }, this);
      this.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector.$.entityFilterText.minLengthToSearch = 5000;
    }
  });

  enyo.kind({
      name: 'SRGC.UI.ModalGcScrollableHeader',
      kind: 'OB.UI.ScrollableTableHeader',
      components: [{
        style: 'padding: 10px;',
        kind: 'OB.UI.FilterSelectorTableHeader',
        name: 'filterSelector',
        filters: OB.Model.GiftCardFilterRetail.getProperties()
      }, {
        style: 'padding: 7px;',
        showing: true,
        handlers: {
          onSetShow: 'setShow',
          onClosedModal: 'closedModal'
        },
        setShow: function (inSender, inEvent) {
          this.setShowing(inEvent.visibility);
        },
        components: [{
          style: 'display: table; width: 100%',
          components: [{
            style: 'display: table-cell; text-align: center; ',
            components: [{
              kind: 'OB.UI.Button',
              classes: 'btnlink btnlink-small btnlink-yellow',
              content: 'Deslice Tarjeta',
              name: 'LblCancel',
              tap: function () {
                var str1 = "";
                var str2 = "";
                var lon = 0;
                var lon = 0;

                superThis.$.body.$.giftcardsretail.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector.$.entityFilterText.setValue('');
                
                var fn = function(event){
                  str1 += event.key;
                  var lon = (str1.match(/_/g)||[]).length
                  if (event.key === "_" && lon == 1) {
                    str1 = str1.slice(1, -1);
                    lon = 0;
                    document.removeEventListener("keypress", fn, true);
                    superThis.$.body.$.giftcardsretail.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector.$.entityFilterText.setValue(str1);
                    str1 = "";
                    event.preventDefault();
                    superThis.$.body.$.giftcardsretail.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector.$.entityFilterText.setDisabled(false);
                    superThis.$.body.$.giftcardsretail.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector.$.entitySearchBtn.tap();
                  }else{
                    event.preventDefault();
                  }
                }

                superThis.$.body.$.giftcardsretail.$.stGCAssignToReceipt.$.theader.$.modalGcScrollableHeader.$.filterSelector.$.entityFilterText.setDisabled(true);
                document.addEventListener("keypress", fn,true);                              
        
              }
            }]
          }]
        }]
      }]
    });

  enyo.kind({
    name: 'SRGC.UI.AdvancedFilterWindowButtonGC',
    kind: 'OB.UI.ButtonAdvancedFilter',
    dialog: 'modalAdvancedFilterGC'
  });

  enyo.kind({
    name: 'SRGC.UI.ModalAdvancedFilterGC',
    kind: 'OB.UI.ModalAdvancedFilters',
    initComponents: function () {
      this.inherited(arguments);
      this.setFilters(OB.Model.GiftCardFilterRetail.getProperties());
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
    kind: 'SRGC.UI.ModalGiftCard',
    name: 'SRGC_UI_ModalGiftCard'
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSCashMgmt.UI.CashManagement', {
	    kind: 'SRGC.UI.ModalGiftCard',
	    name: 'SRGC_UI_ModalGiftCard'
  });
  
  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
	    kind: 'SRGC.UI.ModalAdvancedFilterGC',
	    name: 'modalAdvancedFilterGC'
	  });

  OB.UI.WindowView.registerPopup('OB.OBPOSCashMgmt.UI.CashManagement', {
    kind: 'SRGC.UI.ModalAdvancedFilterGC',
    name: 'modalAdvancedFilterGC'
  });  

}());
