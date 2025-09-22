/*
 ************************************************************************************
 * Copyright (C) 2012-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, _, $ */


(function () {

  enyo.kind({
    name: 'SRGC.UI.ReturnButton',
    kind: 'OB.UI.ModalDialogButton',
    isDefaultAction: true,
    events: {
      onHideThisPopup: '',
      onReturnButton: ''
    },
    disabled: false,
    putHide: function (state) {
      if (state === false) {
        this.show();
      } else {
        this.hide();
      }
    },
    tap: function () {
      if (this.disabled) {
        return;
      }
      this.doReturnButton();
    },
    initComponents: function () {
      this.inherited(arguments);
      this.setContent(OB.I18N.getLabel('SRGC_LblReturn'));
    }
  });

  enyo.kind({
    name: 'SRGC.UI.ApplyDialogButton',
    kind: 'OB.UI.ModalDialogButton',
    isDefaultAction: true,
    events: {
      onHideThisPopup: '',
      onAcceptButton: ''
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
      this.doAcceptButton();
    },
    initComponents: function () {
      this.inherited(arguments);
      this.setContent(OB.I18N.getLabel('SRGC_LblApply'));
    }
  });

  enyo.kind({
    name: 'SRGC.UI.PrintButton',
    kind: 'OB.UI.ModalDialogButton',
    isDefaultAction: true,
    events: {
      onHideThisPopup: '',
      onPrintButton: ''
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
      this.doPrintButton();
    },
    initComponents: function () {
      this.inherited(arguments);
      this.setContent(OB.I18N.getLabel('SRGC_Print'));
    }
  });

  enyo.kind({
    kind: 'OB.UI.ScrollableTableHeader',
    name: 'SRGC.UI.SummaryHeader',
    style: 'padding: 10px; border-bottom: 1px solid #cccccc;',
    components: [{
      style: 'line-height: 27px; font-size: 18px; font-weight: bold;',
      name: 'title',
      components: [{
        style: 'float: left; text-align: left; width: 60%;',
        name: 'LblProduct'
      }, {
        style: 'float: left; text-align:right; width: 20%;',
        name: 'LblQuantity'
      }, {
        style: 'float: left; text-align:right; width: 20%;',
        name: 'LblCurrentQuantity'
      }, {
        style: 'clear:both;'
      }]
    }],
    initComponents: function () {
      this.inherited(arguments);
      this.$.LblProduct.setContent(OB.I18N.getLabel('SRGC_LblProduct'));
      this.$.LblQuantity.setContent(OB.I18N.getLabel('SRGC_LblQuantity'));
      this.$.LblCurrentQuantity.setContent(OB.I18N.getLabel('SRGC_LblCurrentQuantity'));
    }
  });

  enyo.kind({
    name: 'SRGC.UI.SummaryRender',
    //    kind: 'OB.UI.SelectButton',
    components: [{
      style: 'float: left; text-align: left; width: 60%;',
      name: 'product'
    }, {
      style: 'float: left; text-align:right; width: 20%;',
      name: 'quantity'
    }, {
      style: 'float: left; text-align:right; width: 20%;',
      name: 'currentquantity'
    }, {
      style: 'clear:both;'
    }],
    initComponents: function () {
      this.inherited(arguments);
      this.$.product.setContent(this.model.get('product$_identifier'));
      this.$.quantity.setContent(this.model.get('quantity').toString());
      this.$.currentquantity.setContent(this.model.get('currentquantity').toString());
    }

  });

  enyo.kind({
    kind: 'OB.UI.ModalAction',
    name: 'SRGC.UI.Details',
    closeOnAcceptButton: true,
    header: '',
    events: {
      onHideThisPopup: '',
      onShowDivText: ''
    },
    handlers: {
      onAcceptButton: 'acceptButton',
      //onReturnButton: 'returnButton',
      onPrintButton: 'printButton'
    },
    style: 'min-height: 455px; overflow: hidden',
    bodyContent: {
      style: 'background-color: #ffffff;',
      components: [{
        components: [{
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'LblGiftCardID'
            }]
          }, {
            style: 'float: left; width: 342px; text-align: left; color: black; padding-top: 5px; padding-left: 5px',
            name: 'searchkey'
          }, {
            style: 'clear: both'
          }]
        }]
      }, {
        components: [{
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'LblStatus'
            }]
          }, {
            style: 'float: left; width: 342px; text-align: left; color: black; padding-top: 5px; padding-left: 5px',
            name: 'status'
          }, {
            style: 'clear: both'
          }]
        }]
      }, /* {
        name: 'productcontainer',
        components: [{
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'LblProduct'
            }]
          }, {
            style: 'float: left; width: 342px; text-align: left; color: black; padding-top: 5px; padding-left: 5px',
            name: 'product'
          }, {
            style: 'clear: both'
          }]
        }]
      },  {
        name: 'categorycontainer',
        components: [{
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'LblCategory'
            }]
          }, {
            style: 'float: left; width: 342px; text-align: left; color: black; padding-top: 5px; padding-left: 5px',
            name: 'category'
          }, {
            style: 'clear: both'
          }]
        }]
      }, {
        components: [{
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'LblBusinessPartner'
            }]
          }, {
            style: 'float: left; width: 342px; text-align: left; color: black; padding-top: 5px; padding-left: 5px',
            name: 'businesspartner'
          }, {
            style: 'clear: both'
          }]
        }]
      }, */ {
        name: 'amountcontainer',
        components: [{
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'LblAmount'
            }]
          }, {
            style: 'float: left; width: 342px; text-align: right; color: black; padding-top: 5px; padding-left: 5px',
            name: 'amount'
          }, {
            style: 'clear: both'
          }]
        }]
      }, {
        name: 'currentamountcontainer',
        components: [{
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'LblCurrentAmount'
            }]
          }, {
            style: 'float: left; width: 342px; text-align: right; color: black; padding-top: 5px; padding-left: 5px',
            name: 'currentamount'
          }, {
            style: 'clear: both'
          }]
        }]
      }, 

      {
        kind: 'OB.UI.ScrollableTable',

        maxHeight: '225px',
        thumb: true,
        horizontal: 'hidden',

        style: 'color:black;padding: 10px;',
        name: 'summary',
        scrollAreaMaxHeight: '250px',
        renderHeader: 'SRGC.UI.SummaryHeader',
        renderEmpty: 'OB.UI.RenderEmpty',
        renderLine: 'SRGC.UI.SummaryRender'
      }, 

      {
        components: [

        {
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'GCOwner'
            }]
          }, {
            style: 'float: left;width: 342px;text-align: left; color: black;',
            name: 'gcowner'
          }, {
            style: 'clear: both'
          }]
        }]
      },

      {
        name: 'expirationDatecontainer',
        components: [{
          components: [{
            style: 'border: 1px solid #F0F0F0; background-color: #E2E2E2; color: black; width: 200px; height: 40px; float: left; text-align: right;',
            components: [{
              style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
              name: 'ExpirationDate'
            }]
          }, {
            style: 'float: left;width: 342px;text-align: left; color: black;',
            name: 'expirationDate'
          }, {
            style: 'clear: both'
          }]
        }]
      }
      
      ]
    },

    bodyButtons: {
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [
        /*{
          kind: 'SRGC.UI.ReturnButton',
          name: 'returnbutton'
        },*/ 
        {
          kind: 'SRGC.UI.ApplyDialogButton',
          name: 'okbutton'
        }, 
        {
          kind: 'SRGC.UI.PrintButton',
          name: 'printbutton'
        }, {
          kind: 'OB.UI.CancelDialogButton'
        }]
      }]
    },

    /*returnButton: function (inSender, inEvent) {

      var me = this;

      if (this.args.receipt.get('orderType') === 0) {
        // must be a return order
        this.args.view.doShowPopup({
          popup: 'SRGC_UI_Message',
          args: {
            message: OB.I18N.getLabel('SRGC_MsgReturnOrder'),
            cancelButton: true,
            callback: function () {
              me.doShowDivText({
                permission: 'OBPOS_receipt.return',
                orderType: 1
              });
              me.returnExecute();
            }
          }
        });
      } else {
        this.returnExecute();
      }
    }, */

    returnExecute: function () {

      var giftcard = this.args.giftcard;
      var me = this;
      
      var consumeOK = function () {
          if (me.args.okcallback) {
            me.args.okcallback();
          }
          //me.$.bodyButtons.$.returnbutton.setAttribute('disabled', null);
          //me.$.bodyButtons.$.returnbutton.setContent(OB.I18N.getLabel('SRGC_LblReturn'));
          me.propertiesCancelled = false;
          me.doHideThisPopup();
          };

      var consumeFail = function () {
          //me.$.bodyButtons.$.returnbutton.setAttribute('disabled', null);
          //me.$.bodyButtons.$.returnbutton.setContent(OB.I18N.getLabel('SRGC_LblReturn'));
          };

      //me.$.bodyButtons.$.returnbutton.setAttribute('disabled', 'disabled');
      //me.$.bodyButtons.$.returnbutton.setContent(OB.I18N.getLabel('SRGC_LblLoading'));

      //
      OB.UI.GiftCardUtilsRetail.cancelGiftCard(this.args.view, this.args.receipt, giftcard.searchKey, consumeOK, consumeFail);
    },

    printButton: function (inSender, inEvent) {

      var giftCard = this.args.giftcard,
          giftCardData = new Backbone.Model(),
          gctemplateresource;

      if (giftCard.type === 'BasedOnCreditNote') {
        gctemplateresource = new OB.DS.HWResource(giftCard.printTemplate || OB.MobileApp.model.get('terminal').printCreditNoteTemplate || OB.OBGCNE.Utils.PrintCreditNoteTemplate);
        giftCardData.set('giftCardId', giftCard.searchKey);
        giftCardData.set('businessPartnerId', giftCard.businessPartner);
        giftCardData.set('businessPartnerName', giftCard.businessPartner$_identifier);
        giftCardData.set('amount', giftCard.amount);
        giftCardData.set('currentamount', giftCard.currentamount);
        OB.POS.hwserver.print(gctemplateresource, {
          giftCardData: giftCardData
        });
      } else {
        if (giftCard.printTemplate) {
          gctemplateresource = new OB.DS.HWResource(giftCard.printTemplate);

          // Set properties to giftCardData in order to print it
          giftCardData.set('giftCardId', giftCard.searchKey);
          giftCardData.set('expirationDate', OB.I18N.formatDate(new Date(!OB.UTIL.isNullOrUndefined(giftCard.obgcneExpirationdate) ? OB.I18N.parseServerDate(giftCard.obgcneExpirationdate) : giftCard.obgcneExpirationdate)));
          giftCardData.set('productId', giftCard.product);
          giftCardData.set('productName', giftCard.product$_identifier);
          giftCardData.set('businessPartnerId', giftCard.businessPartner);
          giftCardData.set('businessPartnerName', giftCard.businessPartner$_identifier);
          giftCardData.set('gcOwnerId', giftCard.obgcneGCOwner);
          giftCardData.set('gcOwnerName', giftCard.obgcneGCOwner$_identifier);
          giftCardData.set('amount', giftCard.amount);
          giftCardData.set('currentamount', giftCard.currentamount);
          giftCardData.set('summaryList', giftCard.gCNVGiftCardSummaryList);
          giftCardData.set('alertStatus', giftCard.alertStatus);
          giftCardData.set('type', giftCard.type);
          giftCardData.set('categoryId', giftCard.category);
          giftCardData.set('categoryName', giftCard.category$_identifier);
          giftCardData.set('reprint', true);

          if (giftCard.templateIsPdf) {
            if (giftCard.templatePrinter) {
              gctemplateresource.printer = parseInt(giftCard.templatePrinter, 10);
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
              OB.UTIL.showError(OB.I18N.getLabel('SRGC_NoPrinter'));
            }
          } else {
            OB.POS.hwserver.print(gctemplateresource, {
              giftCardData: giftCardData
            });
          }
        } else {
          // If there is no template defined show error
          OB.UTIL.showError(OB.I18N.getLabel('SRGC_NoTemplate'));
        }
      }
    },

    acceptButton: function (inSender, inEvent) {

      var giftcard = this.args.giftcard;
      var me = this;
      
      var showMessagePopup = function (message, action) {
          var keyboard = message.keyboard,
              args = {};
          args.message = message.params ? OB.I18N.getLabel(message.message, message.params) : OB.I18N.getLabel(message.message);
          if (message.header) {
            args.header = message.header;
          }
          args.callback = action;
          keyboard.doShowPopup({
            popup: 'SRGC_UI_Message',
            args: args
          });
      };  

      var consumeOK = function (callback, successMessage) {
    	  
    	  if (me.args.okcallback) {
            me.args.okcallback();
          }
    	  
          me.statusReady();
          me.propertiesCancelled = false;
          me.doHideThisPopup();

          if (successMessage) {
            showMessagePopup(successMessage);
          }

          if (callback) {
            callback();
          }
      }; 

      var consumeFail = function (callback, errorMessage) {
    	  me.statusReady();
        me.propertiesCancelled = false;

        if (errorMessage) {
          me.doHideThisPopup();
          showMessagePopup(errorMessage, function () {
            errorMessage.keyboard.doShowPopup({
              popup: 'SRGC_UI_Details',
              args: me.args
            });
          });
        }

        me.propertiesCancelled = true;
        if (callback) {
          callback();
        } 
      };    	  

      this.statusLoading();

      //****************************************************************//
      //			 SE REALIZA EL PAGO CON LA GIFT CARD				//
      //****************************************************************//
      if (this.args.action) {
        this.args.action(this, consumeOK, consumeFail);
      } else {
        if (giftcard.amount) {
          // Is a Product Gift Card
          OB.UI.GiftCardUtilsRetail.checkIfExpiredGiftCardAndConsume(this.args.view, this.args.receipt, giftcard, this.args.amount || this.args.receipt.getPending(), this.args.receipt.get('priceIncludesTax'), consumeOK, consumeFail);
        } else {
          // Is a Gift Voucher
          OB.UI.GiftCardUtilsRetail.checkIfExpiredVoucherAndConsume(this.args.view, this.args.receipt, giftcard.searchKey, consumeOK, consumeFail, {
            cardType: 'V'
          });
        }
      } 
      //****************************************************************//
      //****************************************************************//       
    },

    statusReady: function () {

      var receipt = this.model.get('order');
      
      if (receipt) {
        var applyButton = receipt.getPending() > 0,
            isCNOrGLItem;
        isCNOrGLItem = this.args.giftcard.type === 'BasedOnCreditNote' || this.args.giftcard.type === 'BasedOnGLItem' ? true : false;

        if (!applyButton) {
          applyButton = this.model.get('multiOrders') && this.model.get('multiOrders').get('multiOrdersList') && this.model.get('multiOrders').get('multiOrdersList').length > 0;
        }
        if (applyButton && isCNOrGLItem && !this.args.amount) {
          applyButton = false;
        }
        this.$.bodyButtons.$.okbutton.putDisabled(!applyButton);
        //this.$.bodyButtons.$.returnbutton.putHide(isCNOrGLItem || (!isCNOrGLItem && !OB.MobileApp.model.hasPermission('GCNV_AllowRefundGiftCardsAndVouchers', true)) || this.args.giftcard.type === 'CN' || this.args.view.kind === 'OB.OBPOSPointOfSale.UI.KeyboardOrder' || !receipt.get('isEditable') || (receipt.get('orderType') !== 1 && receipt.get('orderType') !== 0));
      } else {
        this.$.bodyButtons.$.okbutton.putDisabled(false);
        //this.$.bodyButtons.$.returnbutton.putHide(true);
      }

      this.$.bodyButtons.$.okbutton.setContent(OB.I18N.getLabel('SRGC_LblApply'));
      //this.$.bodyButtons.$.returnbutton.setContent(OB.I18N.getLabel('SRGC_LblReturn'));
      this.$.bodyButtons.$.printbutton.setContent(OB.I18N.getLabel('SRGC_Print'));

      this.setPrintingProperties();
    },

    setPrintingProperties: function () {
      var giftcard = this.args.giftcard;
      if (this.args.giftcard.type === 'BasedOnGLItem') {
        // Find Gift Card Reason in Terminal Properties
        var reasonType = _.find(OB.MobileApp.model.get('gcnvGiftcardReason'), function (reason) {
          return reason.id === giftcard.category;
        });
        if (reasonType && reasonType.printCard) {
          this.$.bodyButtons.$.printbutton.putDisabled(false);
          this.args.giftcard.printTemplate = reasonType.printTemplate;
          this.args.giftcard.templateIsPdf = reasonType.templateIsPdf;
          this.args.giftcard.templatePrinter = reasonType.templatePrinter;
        } else {
          this.$.bodyButtons.$.printbutton.putDisabled(true);
        }
      } else if (this.args.giftcard.type === 'BasedOnProductGiftCard' || this.args.giftcard.type === 'BasedOnVoucher') {
        // Find Product
        OB.Dal.get(OB.Model.Product, this.args.giftcard.product, enyo.bind(this, function (product) {
          if (product.get('printCard')) {
            this.$.bodyButtons.$.printbutton.putDisabled(false);
            this.args.giftcard.printTemplate = product.get('printTemplate');
            this.args.giftcard.templateIsPdf = product.get('templateIsPdf');
            this.args.giftcard.templatePrinter = product.get('templatePrinter');
          } else {
            this.$.bodyButtons.$.printbutton.putDisabled(true);
          }
        }), function errorCallback(tx, error) {
          OB.error(tx);
          OB.error(error);
        });
      } else if (this.args.giftcard.type === 'BasedOnCreditNote') {
        this.$.bodyButtons.$.printbutton.putDisabled(false);
        this.args.giftcard.printTemplate = OB.MobileApp.model.get('terminal').printCreditNoteTemplate;
      } else {
        this.$.bodyButtons.$.printbutton.putDisabled(true);
      }
    },

    statusLoading: function () {
      this.$.bodyButtons.$.okbutton.setAttribute('disabled', 'disabled');
      this.$.bodyButtons.$.okbutton.setContent(OB.I18N.getLabel('SRGC_LblLoading'));
      //this.$.bodyButtons.$.returnbutton.setAttribute('disabled', 'disabled');
      //this.$.bodyButtons.$.returnbutton.setContent(OB.I18N.getLabel('SRGC_LblLoading'));
    },

    executeOnHide: function () {
      if (this.args.parentDialog && this.args.cancelAction && this.propertiesCancelled) {
        this.args.cancelAction();
      }
    },

    executeOnShow: function () {
      var giftcard = this.args.giftcard;

      this.propertiesCancelled = true;
      this.statusReady();

      this.$.bodyContent.$.searchkey.setContent(giftcard.searchKey);
      this.$.bodyContent.$.status.setContent(OB.I18N.getLabel('SRGC_LblStatus-' + giftcard.alertStatus));
      if (giftcard.type === 'BasedOnGLItem') {
        this.$.bodyContent.$.productcontainer.setShowing(false);
        this.$.bodyContent.$.categorycontainer.setShowing(true);
        this.$.bodyContent.$.category.setContent(giftcard.category$_identifier);
      } else if (giftcard.type === 'BasedOnCreditNote') {
        this.$.bodyContent.$.productcontainer.setShowing(false);
        this.$.bodyContent.$.categorycontainer.setShowing(false);
        this.$.bodyContent.$.expirationDatecontainer.setShowing(false);
      } else {
        //this.$.bodyContent.$.categorycontainer.setShowing(false);
        //this.$.bodyContent.$.productcontainer.setShowing(true);
        //this.$.bodyContent.$.product.setContent(giftcard.product$_identifier);
      }
      //this.$.bodyContent.$.businesspartner.setContent(giftcard.businessPartner$_identifier);
      this.$.bodyContent.$.gcowner.setContent(giftcard.obgcneGCOwner$_identifier);
      this.$.bodyContent.$.expirationDate.setContent(!OB.UTIL.isNullOrUndefined(giftcard.obgcneExpirationdate) ? OB.I18N.formatDate(new Date(OB.I18N.parseServerDate(giftcard.obgcneExpirationdate))) : '');

      if (giftcard.obgcneExpirationdate && OB.OBGCNE.Utils.isInThePast(giftcard.obgcneExpirationdate)) {
        this.$.bodyButtons.$.okbutton.setAttribute('disabled', 'disabled');
        //this.$.bodyButtons.$.returnbutton.setAttribute('disabled', 'disabled');
        this.$.bodyContent.$.status.setContent(OB.I18N.getLabel('OBGCNE_Expired'));
      }

      if (giftcard.amount !== null) {
        // It's a gift card
        if (giftcard.type === 'BasedOnCreditNote') {
          this.$.header.setContent(OB.I18N.getLabel('SRGC_LblCreditNote'));
        } else if (giftcard.type === 'BasedOnGLItem') {
          this.$.header.setContent(OB.I18N.getLabel('SRGC_LblGiftCardsCertificate'));
        } else {
          this.$.header.setContent(OB.I18N.getLabel('SRGC_LblDialogGiftCard'));
        }
        this.$.bodyContent.$.summary.setShowing(false);
        this.$.bodyContent.$.amountcontainer.setShowing(true);
        this.$.bodyContent.$.currentamountcontainer.setShowing(true);
        this.$.bodyContent.$.amount.setContent(OB.I18N.formatCurrency(giftcard.amount));
        this.$.bodyContent.$.currentamount.setContent(OB.I18N.formatCurrency(giftcard.currentamount));
      } else {
        // It's a gift voucher
        this.$.header.setContent(OB.I18N.getLabel('SRGC_LblDialogGiftVoucher'));
        this.$.bodyContent.$.summary.setShowing(true);
        this.$.bodyContent.$.amountcontainer.setShowing(false);
        this.$.bodyContent.$.currentamountcontainer.setShowing(false);
        // summary ordered by product identifier.
        var summarylist = new Backbone.Collection();
        summarylist.comparator = function (model) {
          return model.get('product$_identifier');
        };
        summarylist.add(giftcard.gCNVGiftCardSummaryList);
        this.$.bodyContent.$.summary.setCollection(summarylist);
      }
    },
    initComponents: function () {
      this.inherited(arguments);
      this.$.bodyContent.$.LblGiftCardID.setContent(OB.I18N.getLabel('SRGC_LblGiftCardID'));
      this.$.bodyContent.$.LblStatus.setContent(OB.I18N.getLabel('SRGC_LblStatus'));
      //this.$.bodyContent.$.LblProduct.setContent(OB.I18N.getLabel('SRGC_LblProduct'));
      //this.$.bodyContent.$.LblCategory.setContent(OB.I18N.getLabel('SRGC_LblCategory'));
      //this.$.bodyContent.$.LblBusinessPartner.setContent(OB.I18N.getLabel('SRGC_LblBusinessPartner'));
      this.$.bodyContent.$.LblAmount.setContent(OB.I18N.getLabel('SRGC_LblAmount'));
      this.$.bodyContent.$.LblCurrentAmount.setContent(OB.I18N.getLabel('SRGC_LblCurrentAmount'));
      this.$.bodyContent.$.GCOwner.setContent(OB.I18N.getLabel('SRGC_GCOwner'));
      this.$.bodyContent.$.ExpirationDate.setContent(OB.I18N.getLabel('SRGC_ExpirationDate'));
    },
    init: function (model) {
      this.model = model;
      this.$.bodyButtons.$.okbutton.putDisabled(true);
    }
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'SRGC.UI.Details',
    name: 'SRGC_UI_Details'
  });

  OB.UI.WindowView.registerPopup('OB.OBPOSCashMgmt.UI.CashManagement', {
    kind: 'SRGC.UI.Details',
    name: 'SRGC_UI_Details'
  });

}());