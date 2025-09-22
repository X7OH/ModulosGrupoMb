/*
 ************************************************************************************
 * Copyright (C) 2014-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, _ */

(function () {

  enyo.kind({
    name: 'OB.UI.CheckboxButtonAll',
    kind: 'OB.UI.CheckboxButton',
    classes: 'modal-dialog-btn-check span1',
    style: 'width: 8%',
    events: {
      onCheckedAll: ''
    },
    handlers: {
      onAllSelected: 'allSelected'
    },
    allSelected: function (inSender, inEvent) {
      if (inEvent.allSelected) {
        this.check();
      } else {
        this.unCheck();
      }
      return true;
    },
    tap: function () {
      this.inherited(arguments);
      this.doCheckedAll({
        checked: this.checked
      });
    }
  });

  enyo.kind({
    name: 'OB.UI.CheckboxButtonReturn',
    kind: 'OB.UI.CheckboxButton',
    classes: 'modal-dialog-btn-check span1',
    style: 'width: 8%',
    handlers: {
      onCheckAll: 'checkAll'
    },
    events: {
      onLineSelected: ''
    },
    isGiftCard: false,
    checkAll: function (inSender, inEvent) {
      if (this.isGiftCard) {
        return;
      }
      if (inEvent.checked && this.parent.$.quantity.value > 0 && !this.parent.newAttribute.notReturnable) {
        this.check();
      } else {
        this.unCheck();
      }
      if (this.parent.$.quantity.value !== 0) {
        this.parent.$.quantity.setDisabled(!inEvent.checked);
        this.parent.$.qtyplus.setDisabled(!inEvent.checked);
        this.parent.$.qtyminus.setDisabled(!inEvent.checked);
      }
      if (this.parent.newAttribute.productType === 'S') {
        this.parent.$.quantity.setDisabled(true);
        this.parent.$.qtyplus.setDisabled(true);
        this.parent.$.qtyminus.setDisabled(true);
      }
    },
    tap: function () {
      if (this.isGiftCard || this.parent.$.quantity.value === 0) {
        OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_LineCanNotBeSelected'));
        return;
      }
      this.inherited(arguments);
      this.parent.$.quantity.setDisabled(!this.checked);
      this.parent.$.qtyplus.setDisabled(!this.checked);
      this.parent.$.qtyminus.setDisabled(!this.checked);

      if (this.checked) {
        this.parent.$.quantity.focus();
      }
      this.doLineSelected({
        selected: this.checked
      });
      if (this.parent.newAttribute.productType === 'S') {
        this.parent.$.quantity.setDisabled(true);
        this.parent.$.qtyplus.setDisabled(true);
        this.parent.$.qtyminus.setDisabled(true);
      }
    }
  });

  enyo.kind({
    name: 'OB.UI.EditOrderLine',
    style: 'border-bottom: 1px solid #cccccc; text-align: center; color: black; padding-top: 9px;',
    handlers: {
      onApplyChange: 'applyChange'
    },
    events: {
      onCorrectQty: ''
    },
    isGiftCard: false,
    applyChange: function (inSender, inEvent) {
      var me = this;
      var model = inEvent.model;
      var index = inEvent.lines.indexOf(this.newAttribute);
      var line, promotionsfactor;
      if (index !== -1) {
        if (this.$.checkboxButtonReturn.checked) {
          var initialQty = inEvent.lines[index].quantity;
          var qty = this.$.quantity.getValue();
          var orderList = OB.MobileApp.model.orderList;
          enyo.forEach(orderList.models, function (order) {
            enyo.forEach(order.get('lines').models, function (l) {
              if (l.get('originalOrderLineId')) {
                if (l.get('product').id === me.newAttribute.id && l.get('originalOrderLineId') === me.newAttribute.lineId) {
                  qty = qty - l.get('qty');
                }
              }
            });
          });
          if (qty > inEvent.lines[index].remainingQuantity) {
            OB.UTIL.showWarning(OB.I18N.getLabel('OBRETUR_ExceedsQuantity') + ' ' + me.newAttribute.name);
            inEvent.lines[index].exceedsQuantity = true;
          }
          inEvent.lines[index].remainingQuantity = inEvent.lines[index].remainingQuantity - this.$.quantity.getValue();
          inEvent.lines[index].selectedQuantity = this.$.quantity.getValue();
          // update promotions amount to the quantity returned
          enyo.forEach(this.newAttribute.promotions, function (p) {
            if (!OB.UTIL.isNullOrUndefined(p)) {
              p.amt = OB.DEC.mul(p.amt, (me.$.quantity.getValue() / initialQty));
              p.actualAmt = OB.DEC.mul(p.actualAmt, (me.$.quantity.getValue() / initialQty));
              p.displayedTotalAmount = OB.DEC.mul(p.displayedTotalAmount, (me.$.quantity.getValue() / initialQty));
            }
          });
        } else {
          inEvent.lines.splice(index, 1);
        }
      }
    },
    components: [{
      kind: 'OB.UI.CheckboxButtonReturn',
      name: 'checkboxButtonReturn'
    }, {
      name: 'product',
      classes: 'span4',
      style: 'line-height: 35px; font-size: 17px; width: 180px;'
    }, {
      name: 'attribute',
      classes: 'span4',
      style: 'line-height: 35px; font-size: 17px; width: 180px; text-align: left;'
    }, {
      name: 'totalQuantity',
      classes: 'span2',
      style: 'line-height: 35px; font-size: 17px; width: 85px;'
    }, {
      name: 'remainingQuantity',
      classes: 'span2',
      style: 'line-height: 35px; font-size: 17px; width:85px;'
    }, {
      name: 'fullyReturned',
      classes: 'btn-returned span2',
      showing: false
    }, {
      name: 'qtyminus',
      kind: 'OB.UI.SmallButton',
      style: 'width: 35px; color: #666666; height: 35px; margin-top: 0px; font-size: 30px; padding-right: 4px;padding-left: 4px;padding-top: 10px;padding-bottom: 10px;',
      classes: 'btnlink-gray btnlink-cashup-edit span1',
      ontap: 'subUnit'
    }, {
      kind: 'enyo.Input',
      type: 'text',
      classes: 'input span1',
      style: 'margin-right: 2px; text-align: center; width: 45px; height: 25px;',
      name: 'quantity',
      isFirstFocus: true,
      selectOnFocus: true,
      autoKeyModifier: 'num-lock',
      onchange: 'validate'
    }, {
      name: 'qtyplus',
      kind: 'OB.UI.SmallButton',
      style: 'width: 35px; color: #666666; height: 35px; margin-top: 0px; font-size: 30px; padding-right: 4px;padding-left: 4px;padding-top: 10px;padding-bottom: 10px;',
      classes: 'btnlink-gray btnlink-cashup-edit span1',
      ontap: 'addUnit'
    }, {
      name: 'price',
      classes: 'span2',
      style: 'line-height: 35px; font-size: 17px; width: 100px; margin-left: 15px;'
    }, {
      style: 'clear: both;'
    }],
    addUnit: function (inSender, inEvent) {
      var units = parseInt(this.$.quantity.getValue(), 10);
      if (!isNaN(units) && units < this.$.quantity.getAttribute('max')) {
        this.$.quantity.setValue(units + 1);
        this.validate();
      }

    },
    subUnit: function (inSender, inEvent) {
      var units = parseInt(this.$.quantity.getValue(), 10);
      if (!isNaN(units) && units > this.$.quantity.getAttribute('min')) {
        this.$.quantity.setValue(units - 1);
        this.validate();
      }
    },
    validate: function () {
      var value, maxValue;
      value = this.$.quantity.getValue();
      try {
        value = parseFloat(this.$.quantity.getValue());
      } catch (e) {
        this.addStyles('background-color: red');
        this.doCorrectQty({
          correctQty: false
        });
        return true;
      }
      maxValue = OB.DEC.toNumber(OB.DEC.toBigDecimal(this.$.quantity.getAttribute('max')));

      if (!_.isNumber(value) || _.isNaN(value)) {
        this.addStyles('background-color: red');
        this.doCorrectQty({
          correctQty: false
        });
        return true;
      }

      value = OB.DEC.toNumber(OB.DEC.toBigDecimal(value));
      this.$.quantity.setValue(value);


      if (value > maxValue || value <= 0) {
        this.addStyles('background-color: red');
        this.doCorrectQty({
          correctQty: false
        });
      } else {
        this.addStyles('background-color: white');
        this.doCorrectQty({
          correctQty: true
        });
        return true;
      }
    },
    markAsGiftCard: function () {
      this.isGiftCard = true;
      this.$.checkboxButtonReturn.isGiftCard = this.isGiftCard;
    },
    tap: function () {
      if (this.isGiftCard === true) {
        OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_LineCanNotBeSelected'));
      }
    },
    initComponents: function () {
      var movementQty = OB.DEC.Zero,
          me = this;
      _.each(this.newAttribute.shipmentlines, function (shipment) {
        movementQty += shipment.remainingQty;
      });
      this.newAttribute.remainingQuantity = movementQty;
      this.inherited(arguments);
      this.$.qtyminus.setContent(OB.I18N.getLabel('OBMOBC_Character')[3]);
      this.$.qtyplus.setContent(OB.I18N.getLabel('OBMOBC_Character')[4]);
      if (this.newAttribute.shipmentlines.length === 0) {
        this.$.checkboxButtonReturn.setDisabled(true);
        this.$.checkboxButtonReturn.removeClass('btn-icon-check');
        this.$.checkboxButtonReturn.addClass('btn-icon-canceled');
        this.$.quantity.hide();
        this.$.qtyplus.hide();
        this.$.qtyminus.hide();
        this.$.fullyReturned.setContent(OB.I18N.getLabel('OBPOS_Cancelled'));
        this.$.fullyReturned.addStyles('color: #CC9999');
        this.$.fullyReturned.show();
      } else if (!this.newAttribute.returnable) {
        this.$.checkboxButtonReturn.setDisabled(true);
        this.$.checkboxButtonReturn.removeClass('btn-icon-check');
        this.$.checkboxButtonReturn.addClass('btn-icon-notreturnable');
        this.$.quantity.hide();
        this.$.qtyplus.hide();
        this.$.qtyminus.hide();
        this.$.fullyReturned.setContent(OB.I18N.getLabel('OBRETUR_notreturnable'));
        this.$.fullyReturned.addStyles('color: #C80000');
        this.$.fullyReturned.show();
      } else if (this.newAttribute.remainingQuantity <= 0) {
        this.$.checkboxButtonReturn.setDisabled(true);
        this.$.checkboxButtonReturn.removeClass('btn-icon-check');
        this.$.checkboxButtonReturn.addClass('btn-icon-returned');
        this.$.quantity.hide();
        this.$.qtyplus.hide();
        this.$.qtyminus.hide();
        this.$.fullyReturned.setContent(OB.I18N.getLabel('OBRETUR_FullyReturned'));
        this.$.fullyReturned.show();
      }
      this.$.product.setContent(this.newAttribute.name);
      if (OB.MobileApp.model.hasPermission('OBPOS_EnableSupportForProductAttributes', true)) {
        if (this.newAttribute.attributeValue && _.isString(this.newAttribute.attributeValue)) {
          var processedAttValues = OB.UTIL.AttributeUtils.generateDescriptionBasedOnJson(this.newAttribute.attributeValue);
          if (processedAttValues && processedAttValues.keyValue && _.isArray(processedAttValues.keyValue) && processedAttValues.keyValue.length > 0) {
            _.each(processedAttValues.keyValue, function (item) {
              me.$.attribute.createComponent({
                content: item
              });
            });
            this.newAttribute.attSetInstanceDesc = processedAttValues.description;
          } else {
            this.$.attribute.setContent('NA');
          }
        } else {
          this.$.attribute.setContent('NA');
        }
      } else {
        this.$.attribute.hide();
      }
      this.$.remainingQuantity.setContent(this.newAttribute.remainingQuantity);
      this.$.totalQuantity.setContent(this.newAttribute.quantity);
      this.$.quantity.setDisabled(true);
      this.$.qtyplus.setDisabled(true);
      this.$.qtyminus.setDisabled(true);
      this.$.quantity.setValue(this.newAttribute.remainingQuantity);
      this.$.quantity.setAttribute('max', this.newAttribute.remainingQuantity);
      this.$.quantity.setAttribute('min', OB.DEC.One);
      this.$.price.setContent(this.newAttribute.priceIncludesTax ? this.newAttribute.unitPrice : this.newAttribute.baseNetUnitPrice);

      if (this.newAttribute.promotions.length > 0) {
        this.$.quantity.addStyles('margin-bottom:0px');
        enyo.forEach(this.newAttribute.promotions, function (d) {
          if (d.hidden) {
            // continue
            return;
          }
          this.createComponent({
            style: 'display: block; color:gray; font-size:13px; line-height: 20px;  width: 150px;',
            components: [{
              content: '-- ' + d.name,
              attributes: {
                style: 'float: left; width: 155%; margin-top: -8px;padding-bottom: 15px;'
              }
            }, {
              content: OB.I18N.formatCurrency(-d.amt),
              attributes: {
                style: 'float: left; width: 40%; padding-left: 390%; margin-top:-35px; padding-bottom: 15px;'
              }
            }, {
              style: 'clear: both;'
            }]
          });
        }, this);

      }
      // shipment info
      if (!OB.UTIL.isNullOrUndefined(this.newAttribute.shiplineNo)) {
        this.createComponent({
          style: 'display: block; color:gray; font-size:13px; line-height: 20px;',
          components: [{
            content: this.newAttribute.shipment + ' - ' + this.newAttribute.shiplineNo,
            attributes: {
              style: 'float: left; width: 60%;'
            }
          }, {
            style: 'clear: both;'
          }]
        });
      }
    }
  });

  enyo.kind({
    kind: 'OB.UI.ModalDialogButton',
    name: 'OB.UI.ReturnReceiptDialogApply',
    events: {
      onApplyChanges: '',
      onCallbackExecutor: '',
      onCheckQty: '',
      onValidateAttributeValue: ''
    },
    tap: function () {
      if (this.doCheckQty()) {
        return true;
      }
      if (this.doApplyChanges()) {
        this.doValidateAttributeValue();
        this.doHideThisPopup();
      }
    },
    initComponents: function () {
      this.inherited(arguments);
      this.setContent(OB.I18N.getLabel('OBMOBC_LblApply'));
    }
  });

  enyo.kind({
    kind: 'OB.UI.ModalDialogButton',
    name: 'OB.UI.ReturnReceiptDialogCancel',
    tap: function () {
      this.doHideThisPopup();
    },
    initComponents: function () {
      this.inherited(arguments);
      this.setContent(OB.I18N.getLabel('OBMOBC_LblCancel'));
    }
  });

  enyo.kind({
    name: 'OB.UI.ModalReturnReceipt',
    kind: 'OB.UI.ModalAction',
    classes: 'modal-dialog',
    style: 'width: 700px;',
    correctQty: true,
    handlers: {
      onApplyChanges: 'applyChanges',
      onCallbackExecutor: 'callbackExecutor',
      onCheckedAll: 'checkedAll',
      onCheckQty: 'checkQty',
      onCorrectQty: 'changeCorrectQty',
      onValidateAttributeValue: 'validateAttributeValue',
      onLineSelected: 'lineSelected'
    },
    lineShouldBeIncludedFunctions: [],
    bodyContent: {
      kind: 'Scroller',
      maxHeight: '225px',
      style: 'background-color: #ffffff;margin-top: -7px;',
      thumb: true,
      horizontal: 'hidden',
      components: [{
        name: 'attributes'
      }]
    },
    bodyButtons: {
      components: [{
        kind: 'OB.UI.ReturnReceiptDialogApply'
      }, {
        kind: 'OB.UI.ReturnReceiptDialogCancel'
      }]
    },
    applyChanges: function (inSender, inEvent) {
      this.waterfall('onApplyChange', {
        lines: this.args.args.order.receiptLines,
        model: this.args.args.context.model
      });
      return true;
    },
    validateAttributeValue: function (inSender, inEvent) {
      var i, productWithAttributeValue = [],
          lines = this.args.args.order.receiptLines,
          productHasAttribute = false;
      lines.forEach(function (theLine) {
        if (OB.UTIL.isNullOrUndefined(theLine.attributeValue) === false) {
          productWithAttributeValue.push(theLine);
          productHasAttribute = true;
        }
      });
      if (OB.MobileApp.model.hasPermission('OBPOS_EnableSupportForProductAttributes', true) && productHasAttribute) {
        OB.MobileApp.view.waterfall('onShowPopup', {
          popup: 'modalProductAttributeVerifiedReturns',
          args: {
            line: productWithAttributeValue,
            returnLinesPopup: this,
            documentno: this.args.args.order.documentNo
          }
        });
      } else {
        this.callbackExecutor();
      }
    },
    callbackExecutor: function (inSender, inEvent) {
      var me = this,
          i;
      if (me.args.args.order.receiptLines.length === 0) {
        OB.UTIL.showLoading(false);
        return;
      }
      var nameLocation = "";
      var oldbp = me.args.args.context.model.get('order').get('bp');
      var bpLoc = me.args.args.order.bpLocId;
      var bpBillLoc;
      var noFoundProduct = true,
          NoFoundCustomer = true,
          isLoadedPartiallyFromBackend = false;

      var findBusinessPartner = function (bp) {
          if (OB.MobileApp.model.hasPermission('OBPOS_remote.discount.bp', true)) {
            var bpFilter = {
              columns: ['businessPartner'],
              operator: 'equals',
              value: OB.MobileApp.model.get('businessPartner').id
            };
            var remoteCriteria = [bpFilter];
            var criteriaFilter = {};
            criteriaFilter.remoteFilters = remoteCriteria;
            OB.Dal.find(OB.Model.DiscountFilterBusinessPartner, criteriaFilter, function (discountsBP) {
              _.each(discountsBP.models, function (dsc) {
                OB.Dal.saveIfNew(dsc, function () {}, function () {
                  OB.error(arguments);
                });
              });
            }, function () {
              OB.error(arguments);
            });
          }
          var locationForBpartner = function (bpLoc, billLoc) {
              var finishOrderCreation = function () {
                  me.args.args.context.model.get('order').calculateReceipt(function () {
                    me.args.args.context.model.get('order').save(function () {
                      me.args.args.context.model.get('orderList').saveCurrent();
                      OB.UTIL.showLoading(false);
                    });
                  });
                  },
                  finishOrder = _.after(me.args.args.order.receiptLines.length, finishOrderCreation),
                  createOrderLines = function (ignoreReturnApproval) {
                  _.each(me.args.args.order.receiptLines, function (line) {
                    if (!line.exceedsQuantity) {
                      var createLineFunction = function (prod) {
                          line.originalOrderLineId = line.lineId;
                          line.originalDocumentNo = me.args.args.order.documentNo;
                          line.skipApplyPromotions = true;
                          delete line.id;
                          delete line.lineId;
                          delete line.deliveredQuantity;
                          var order = me.args.args.context.model.get('order');
                          var qty = line.selectedQuantity;
                          if (order.get('orderType') !== 1) {
                            qty = qty ? -qty : -1;
                          }
                          prod.set('ignorePromotions', true);
                          prod.set('standardPrice', line.priceIncludesTax ? line.unitPrice : line.baseNetUnitPrice);
                          prod.set('ignoreReturnApproval', ignoreReturnApproval);
                          order.addProductToOrder(prod, qty, {
                            isVerifiedReturn: true,
                            isEditable: false,
                            blockAddProduct: true,
                            originalOrder: me.args.args.order,
                            // Plain JS Object, no Backbone Order
                            originalLine: line
                            // Plain JS Object, no Backbone Order
                          }, line, function () {
                            me.args.args.cancelOperation = true;
                            OB.UTIL.HookManager.callbackExecutor(me.args.args, me.args.callbacks);
                            finishOrder();
                          });
                          };

                      _.each(line.promotions, function (promotion) {
                        promotion.amt = -promotion.amt;
                        promotion.actualAmt = -promotion.actualAmt;
                        promotion.displayedTotalAmount = -promotion.displayedTotalAmount;
                      });
                      OB.Dal.get(OB.Model.Product, line.id, function (product) {
                        createLineFunction(product);
                      }, null, function () {
                        new OB.DS.Request('org.openbravo.retail.posterminal.master.LoadedProduct').exec({
                          productId: line.id
                        }, function (data) {
                          createLineFunction(OB.Dal.transform(OB.Model.Product, data[0]));
                        }, function () {
                          if (noFoundProduct) {
                            noFoundProduct = false;
                            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBRETUR_InformationTitle'), OB.I18N.getLabel('OBRETUR_NoReturnLoadedText'), [{
                              label: OB.I18N.getLabel('OBPOS_LblOk'),
                              isConfirmButton: true
                            }]);
                          }
                        });
                      });
                    } else {
                      me.args.args.cancelOperation = true;
                      OB.UTIL.HookManager.callbackExecutor(me.args.args, me.args.callbacks);
                      finishOrder();
                    }
                  });
                  },
                  approvalNeeded = false,
                  currentDate = new Date(),
                  approvalList = [],
                  servicesToApprove = '',
                  servicesToApproveArr = [],
                  overdueServicesToApprove = '',
                  overdueServicesToApproveArr = [];

              bp.set('shipLocName', bpLoc.get('name'));
              bp.set('shipLocId', bpLoc.get('id'));
              bp.set('shipPostalCode', bpLoc.get('postalCode'));
              bp.set('shipCityName', bpLoc.get('cityName'));
              bp.set('shipCountryName', bpLoc.get('countryName'));
              bp.set('shipCountryId', bpLoc.get('countryId'));
              bp.set('shipRegionId', bpLoc.get('regionId'));
              bp.set('locName', bpLoc.get('name'));
              bp.set('locId', bpLoc.get('id'));
              bp.set('locationModel', bpLoc);
              if (billLoc) {
                bp.set('locName', billLoc.get('name'));
                bp.set('locId', billLoc.get('id'));
                bp.set('postalCode', billLoc.get('postalCode'));
                bp.set('cityName', billLoc.get('cityName'));
                bp.set('countryName', billLoc.get('countryName'));
                bp.set('locationBillModel', billLoc);
              }


              me.nameLocation = bpLoc.get('name');
              //If we do not let user to select qty to return of shipment lines, we do it automatically
              if (!OB.MobileApp.model.hasPermission("OBPOS_SplitLinesInShipments", true)) {
                me.args.args.order.receiptLines = me.autoSplitShipmentLines(me.args.args.order.receiptLines);
              }
              me.args.args.context.model.get('order').setBPandBPLoc(bp, false, true, function () {
                currentDate.setHours(0);
                currentDate.setMinutes(0);
                currentDate.setSeconds(0);
                currentDate.setMilliseconds(0);
                for (i = 0; i < me.args.args.order.receiptLines.length; i++) {
                  var line = me.args.args.order.receiptLines[i];
                  if (!line.notReturnable) {
                    servicesToApprove += '<br>· ' + line.name;
                    servicesToApproveArr.push(' * ' + line.name);
                    approvalNeeded = true;
                    if (line.overdueReturnDays < 0 || ((currentDate.getTime() - line.overdueReturnDays * 86400000) > (new Date(me.args.args.order.orderDate)).getTime())) {
                      overdueServicesToApprove += '<br>· ' + line.name;
                      overdueServicesToApproveArr.push(' * ' + line.name);
                    }
                  }
                }

                if (approvalNeeded) {
                  if (!OB.MobileApp.model.hasPermission('OBPOS_approval.returnService', true)) {
                    approvalList.push({
                      approval: 'OBPOS_approval.returnService',
                      message: 'OBPOS_approval.returnService',
                      params: [servicesToApprove]
                    });
                  }
                  if (!OB.MobileApp.model.hasPermission('OBRETUR_approval.overdueService', true) && overdueServicesToApprove.length > 0) {
                    approvalList.push({
                      approval: 'OBRETUR_approval.overdueService',
                      message: 'OBRETUR_returnService_OutOfDate_warning',
                      params: [overdueServicesToApprove]
                    });
                  }

                  OB.UTIL.showLoading(false);
                  OB.UTIL.Approval.requestApproval(
                  OB.MobileApp.view.$.containerWindow.getRoot().model, approvalList, function (approved, supervisor) {
                    if (approved) {
                      if (supervisor.id === OB.POS.terminal.terminal.usermodel.id && overdueServicesToApprove.length > 0) {
                        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_LblWarning'), OB.I18N.getLabel('OBRETUR_returnService_OutOfDate_warning', [overdueServicesToApproveArr]));
                      }
                      OB.UTIL.showLoading(true);
                      createOrderLines(true);
                    }
                  });
                } else {
                  createOrderLines();
                }
              });
              };
          if (isLoadedPartiallyFromBackend) {
            locationForBpartner(bpLoc, bpBillLoc);
          } else {
            OB.Dal.get(OB.Model.BPLocation, me.args.args.order.bpLocId, function (bpLoc) {
              if (me.args.args.order.bpBillLocId) {
                OB.Dal.get(OB.Model.BPLocation, me.args.args.order.bpBillLocId, function (billLoc) {
                  locationForBpartner(bpLoc, billLoc);
                }, function () {
                  // TODO: Report errors properly
                });
              } else {
                locationForBpartner(bpLoc);
              }

            }, function () {
              // TODO: Report errors properly
            });
          }
          };
      //Check businesspartner
      OB.Dal.get(OB.Model.BusinessPartner, this.args.args.order.bp, function (bp) {
        findBusinessPartner(bp);
      }, null, function () {
        var criteria = {
          bpartnerId: me.args.args.order.bp,
          bpLocationId: me.args.args.order.bpLocId
        };
        if (me.args.args.order.bpLocId !== me.args.args.order.bpBillLocId) {
          criteria.bpBillLocationId = me.args.args.order.bpBillLocId;
        }
        new OB.DS.Request('org.openbravo.retail.posterminal.master.LoadedCustomer').exec(criteria, function (data) {
          isLoadedPartiallyFromBackend = true;
          bpLoc = OB.Dal.transform(OB.Model.BPLocation, data[1]);
          //If we do not have a bill address we have an address for both
          if (data.length === 3) {
            bpBillLoc = OB.Dal.transform(OB.Model.BPLocation, data[2]);
          } else {
            bpBillLoc = OB.Dal.transform(OB.Model.BPLocation, data[1]);
          }
          findBusinessPartner(OB.Dal.transform(OB.Model.BusinessPartner, data[0]));
        }, function () {
          if (NoFoundCustomer) {
            NoFoundCustomer = false;
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOS_InformationTitle'), OB.I18N.getLabel('OBPOS_NoReceiptLoadedText'), [{
              label: OB.I18N.getLabel('OBPOS_LblOk'),
              isConfirmButton: true
            }]);
          }
        });
      });
    },
    checkedAll: function (inSender, inEvent) {
      if (inEvent.checked) {
        this.selectedLines = this.numberOfLines;
        this.allSelected = true;
      } else {
        this.selectedLines = 0;
        this.allSelected = false;
      }
      this.waterfall('onCheckAll', {
        checked: inEvent.checked
      });

      return true;
    },
    checkQty: function (inSender, inEvent) {
    	var veriReturn = false;
    	
    	if (!this.correctQty) {
    		veriReturn = true;
        }
    	
    	var currentDate = new Date();
    	currentDate.setHours(0);
        currentDate.setMinutes(0);
        currentDate.setSeconds(0);
        currentDate.setMilliseconds(0);
    	var dateOrder = new Date(this.args.args.order.orderDate);
    	dateOrder.setHours(0);
    	dateOrder.setMinutes(0);
    	dateOrder.setSeconds(0);
    	dateOrder.setMilliseconds(0);
        
    	if(currentDate.getTime() === dateOrder.getTime()){
    		veriReturn = false;
    	} else {
    		OB.UTIL.showError("La fecha del pedido no puede ser menor a la fecha actual");
    		veriReturn = true;
    	}
    	
    	/*OB.UTIL.HookManager.executeHooks('POSVAL_CheckVerifiedReturns', {
    		orderdate: this.args.args.order.orderDate,
    		flag: false
          }, function (args) {
        	if(!args.flag){
        		this.verifiedReturn = true;
        		alert ("entro la validacion")
        	} else {
        		this.verifiedReturn = false;
        	}  
        	
        });*/
    	
    	if(veriReturn) {
    		return true
    	} 
    },
    changeCorrectQty: function (inSender, inEvent) {
      this.correctQty = inEvent.correctQty;
    },
    lineSelected: function (inSender, inEvent) {
      if (inEvent.selected) {
        this.selectedLines += 1;
      } else {
        this.selectedLines -= 1;
      }
      if (this.selectedLines === this.numberOfLines) {
        this.allSelected = true;
        this.waterfall('onAllSelected', {
          allSelected: this.allSelected
        });
        return true;
      } else {
        if (this.allSelected) {
          this.allSelected = false;
          this.waterfall('onAllSelected', {
            allSelected: this.allSelected
          });
        }
      }
    },
    splitShipmentLines: function (lines) {
      var newlines = [];

      enyo.forEach(lines, function (line) {
        if (line.shipmentlines && line.shipmentlines.length > 1) {
          enyo.forEach(line.shipmentlines, function (sline) {
            var attr, splitline = {};
            for (attr in line) {
              if (line.hasOwnProperty(attr) && !OB.UTIL.isNullOrUndefined(line[attr]) && typeof line[attr] !== 'object') {
                splitline[attr] = line[attr];
              }
            }
            splitline.compname = line.lineId + sline.shipLineId;
            splitline.quantity = sline.qty;
            splitline.shiplineNo = sline.shipmentlineNo;
            splitline.shipment = sline.shipment;
            splitline.shipmentlineId = sline.shipLineId;
            splitline.remainingQuantity = sline.remainingQty;
            // delete confusing properties
            delete splitline.linegrossamount;
            delete splitline.warehouse;
            delete splitline.warehousename;
            // split promotions
            splitline.promotions = [];
            splitline.shipmentlines = [];
            splitline.shipmentlines.push(sline);
            if (line.promotions.length > 0) {
              enyo.forEach(line.promotions, function (p) {
                if (!OB.UTIL.isNullOrUndefined(p)) {
                  var attr, splitpromo = {};
                  for (attr in p) {
                    if (p.hasOwnProperty(attr) && !OB.UTIL.isNullOrUndefined(p[attr]) && typeof p[attr] !== 'object') {
                      splitpromo[attr] = p[attr];
                    }
                  }
                  splitpromo.amt = OB.DEC.mul(p.amt, (splitline.remainingQuantity / line.quantity));
                  splitpromo.actualAmt = OB.DEC.mul(p.actualAmt, (splitline.remainingQuantity / line.quantity));
                  splitpromo.displayedTotalAmount = OB.DEC.mul(p.displayedTotalAmount, (splitline.remainingQuantity / line.quantity));
                  splitline.promotions.push(splitpromo);
                }
              });
            }

            newlines.push(splitline);
          }, this);
        } else {
          line.compname = line.lineId;
          if (line.shipmentlines && line.shipmentlines.length === 1) {
            line.shipmentlineId = line.shipmentlines[0].shipLineId;
          }
          // delete confusing properties
          delete line.linegrossamount;
          delete line.warehouse;
          delete line.warehousename;
          newlines.push(line);
        }
      }, this);
      return newlines;
    },
    autoSplitShipmentLines: function (lines) {
      var newlines = [];

      enyo.forEach(lines, function (line) {
        if (line.shipmentlines && line.shipmentlines.length > 1) {
          enyo.forEach(line.shipmentlines, function (sline) {
            var attr, splitline = {};
            if (line.selectedQuantity > 0 && sline.remainingQty > 0) {
              for (attr in line) {
                if (line.hasOwnProperty(attr) && !OB.UTIL.isNullOrUndefined(line[attr]) && typeof line[attr] !== 'object') {
                  splitline[attr] = line[attr];
                }
              }
              splitline.compname = line.lineId + sline.shipLineId;
              splitline.quantity = sline.qty;
              splitline.shiplineNo = sline.shipmentlineNo;
              splitline.shipment = sline.shipment;
              splitline.shipmentlineId = sline.shipLineId;
              splitline.selectedQuantity = parseInt(splitline.selectedQuantity, 10);
              if (sline.remainingQty < splitline.selectedQuantity) {
                splitline.selectedQuantity = sline.remainingQty;
              }
              line.selectedQuantity = OB.DEC.sub(line.selectedQuantity, splitline.selectedQuantity);
              // delete confusing properties
              delete splitline.linegrossamount;
              delete splitline.warehouse;
              delete splitline.warehousename;
              // split promotions
              splitline.promotions = [];
              if (line.promotions.length > 0) {
                enyo.forEach(line.promotions, function (p) {
                  if (!OB.UTIL.isNullOrUndefined(p)) {
                    var attr, splitpromo = {};
                    for (attr in p) {
                      if (p.hasOwnProperty(attr) && !OB.UTIL.isNullOrUndefined(p[attr]) && typeof p[attr] !== 'object') {
                        splitpromo[attr] = p[attr];
                      }
                    }
                    splitpromo.amt = OB.DEC.mul(p.amt, (splitline.selectedQuantity / line.quantity));
                    splitpromo.actualAmt = OB.DEC.mul(p.actualAmt, (splitline.selectedQuantity / line.quantity));
                    splitpromo.displayedTotalAmount = OB.DEC.mul(p.displayedTotalAmount, (splitline.selectedQuantity / line.quantity));
                    splitline.promotions.push(splitpromo);
                  }
                });
              }

              newlines.push(splitline);
            }
          }, this);
        } else {
          line.compname = line.lineId;
          if (line.shipmentlines && line.shipmentlines.length === 1) {
            line.shipmentlineId = line.shipmentlines[0].shipLineId;
          }
          // delete confusing properties
          delete line.linegrossamount;
          delete line.warehouse;
          delete line.warehousename;
          newlines.push(line);
        }
      }, this);
      return newlines;
    },
    executeOnShow: function () {
      var me = this,
          lineNum = 0;
      OB.UTIL.showLoading(false);
      this.$.bodyContent.$.attributes.destroyComponents();
      this.$.header.destroyComponents();
      this.$.header.createComponent({
        name: 'CheckAllHeaderDocNum',
        style: 'text-align: center; color: white;',
        components: [{
          content: me.args.args.order.documentNo,
          name: 'documentNo',
          classes: 'span12',
          style: 'line-height: 5px; font-size: 24px;'
        }, {
          style: 'clear: both;'
        }]
      });
      if (!this.$.header.$.checkboxButtonAll) {
        this.$.header.addStyles('padding-bottom: 0px; margin: 0px; height: 102px;');

        this.$.header.createComponent({
          name: 'CheckAllHeader',
          style: 'overflow: hidden; padding-top: 5px; border-bottom: 3px solid #cccccc; text-align: center; color: black; margin-top: 30px; padding-top: 7px; padding-bottom: 7px;  font-weight: bold; background-color: white; height:46px;',
          components: [{
            kind: 'OB.UI.CheckboxButtonAll',
            name: 'checkboxButtonAll'
          }, {
            content: OB.I18N.getLabel('OBRETUR_LblProductName'),
            name: 'productNameLbl',
            classes: 'span4',
            style: 'line-height: 25px; font-size: 17px;  width: 179px; padding-top: 10px;'
          }, {
            content: OB.I18N.getLabel('OBRETUR_LblAttributeValue'),
            name: 'attributeValueLbl',
            classes: 'span2',
            style: 'line-height: 25px; font-size: 17px;  width: 179px; padding-top: 10px;'
          }, {
            name: 'totalQtyLbl',
            content: OB.I18N.getLabel('OBRETUR_LblTotalQty'),
            classes: 'span2',
            style: 'line-height: 25px; font-size: 17px; width: 85px; padding-top: 10px;'
          }, {
            name: 'remainingQtyLbl',
            content: OB.I18N.getLabel('OBRETUR_LblRemainingQty'),
            classes: 'span2',
            style: 'line-height: 25px; font-size: 17px; width: 85px; padding-top: 10px;'
          }, {
            content: OB.I18N.getLabel('OBRETUR_LblQty'),
            name: 'qtyLbl',
            classes: 'span3',
            style: 'line-height: 25px; font-size: 17px; width: 155px; padding-top: 10px;'
          }, {
            content: OB.I18N.getLabel('OBRETUR_LblPrice'),
            name: 'priceLbl',
            classes: 'span2',
            style: 'line-height: 25px; font-size: 17px; width: 110px; padding-top: 10px;'
          }, {
            style: 'clear: both;'
          }]
        });
      } else {
        this.$.header.$.checkboxButtonAll.unCheck();
      }
      if (OB.MobileApp.model.hasPermission("OBPOS_SplitLinesInShipments", true)) {
        this.args.args.order.receiptLines = this.splitShipmentLines(this.args.args.order.receiptLines);
      }
      this.numberOfLines = 0;
      this.selectedLines = 0;
      this.allSelected = false;
            

      enyo.forEach(this.args.args.order.receiptLines, function (line) {
        lineNum++;
        var isSelectableLine = true;
        _.each(this.lineShouldBeIncludedFunctions, function (f) {
          isSelectableLine = isSelectableLine && f.isSelectableLine(line);
        });
        var lineEnyoObject = this.$.bodyContent.$.attributes.createComponent({
          kind: 'OB.UI.EditOrderLine',
          name: 'line' + lineNum,
          newAttribute: line
        });
        if (!isSelectableLine) {
          lineEnyoObject.markAsGiftCard();
        }
        this.numberOfLines += 1;
        if (!line.returnable) {
          line.notReturnable = true;
        }
      }, this);

      this.$.bodyContent.$.attributes.render();
      if (!OB.MobileApp.model.hasPermission('OBPOS_EnableSupportForProductAttributes', true)) {
        this.$.header.$.attributeValueLbl.hide();
      } else {
        this.addStyles('width: 900px');
      }
      this.$.header.render();
      // Set correctQty to true onShow the popup.
      this.waterfall('onCorrectQty', {
        correctQty: true
      });
    },

    initComponents: function () {
      this.inherited(arguments);
      this.attributeContainer = this.$.bodyContent.$.attributes;
    }
  });
  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'OB.UI.ModalReturnReceipt',
    name: 'modalReturnReceipt'
  });
}());