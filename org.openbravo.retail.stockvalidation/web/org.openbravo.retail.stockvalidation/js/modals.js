/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, _*/

window.OBPOSSV = window.OBPOSSV || {};
enyo.kind({
  name: 'OBPSSV.UI.dummyControl',
  showing: false,
  init: function (model) {
    var me = this;
    this.model = model;

    function calculateProductQuantity(changedModel) {
      var qty = changedModel.get('qty');
      var orderList = OB.MobileApp.model.orderList;
      enyo.forEach(orderList.models, function (order) {
        enyo.forEach(order.get('lines').models, function (l) {
          if (l.get('product').id === changedModel.get('product').id && l.id !== changedModel.id) {
            qty = qty + l.get('qty');
          }
        });
      });
      return qty;
    }

    function executeCallToServer(changedModel) {
      var serverCall = new OB.DS.Process('org.openbravo.retail.stockvalidation.StockChecker');
      var statusMessage = OB.UTIL.showStatus(OB.I18N.getLabel('OBPOSSV_GettingStockFromServer'));
      var quantity = calculateProductQuantity(changedModel);
      OB.UTIL.showLoading(true);
      serverCall.exec({
        orderLine: changedModel,
        qty: quantity,
        organization: OB.POS.modelterminal.get('terminal').organization
      }, function (data, message) {
        statusMessage.hide();
        OB.UTIL.showLoading(false);
        if (data.exception) {
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_Error'), data.exception.message);
          if (me.model.get('order').get('undo')) {
            me.model.get('order').get('undo').undo(me.model);
          } else {
            if (data.qty <= 0) {
              me.model.get('order').deleteLinesFromOrder([changedModel]);
            } else {
              changedModel.set('qty', data.qty);
            }
          }
          return true;
        }
        if (data.allowSell === false) {
          if (data.allowNegativeStock) {
            changedModel.set('overissueStoreBin', data.overissueStoreBin, {
              silent: true
            });
            changedModel.set('overissueQty', data.overissueQty, {
              silent: true
            });
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOSSV_modalHeader'), OB.I18N.getLabel('OBPOSSV_NoEnoughStockInfo', [data.qty, quantity]), [{
              label: OB.I18N.getLabel('OBPOSSV_LblProceed'),
              action: function () {
                return true;
              }
            }, {
              label: OB.I18N.getLabel('OBMOBC_LblCancel')
            }], {
              autoDismiss: false,
              onHideFunction: function (popup) {
                if (me.model.get('order').get('undo')) {
                  me.model.get('order').get('undo').undo(me.model);
                } else {
                  if (data.qty <= 0) {
                    me.model.get('order').deleteLinesFromOrder([changedModel]);
                  } else {
                    changedModel.set('qty', data.qty);
                  }
                }
              },
              style: 'background-color: #EBA001'
            });
          } else {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOSSV_modalHeader'), OB.I18N.getLabel('OBPOSSV_NoEnoughStock', [data.qty, quantity]), null, {
              autoDismiss: false,
              onHideFunction: function (popup) {
                if (me.model.get('order').get('undo')) {
                  me.model.get('order').get('undo').undo(me.model);
                } else {
                  if (data.qty <= 0) {
                    me.model.get('order').deleteLinesFromOrder([changedModel]);
                  } else {
                    changedModel.set('qty', data.qty);
                  }
                }
              }
            });
          }
        }
      }, function (error) {
        OB.UTIL.showLoading(false);
        statusMessage.hide();
        OB.UTIL.showError(OB.I18N.getLabel('OBPOSSV_ErrorGettingStockFromServer'));
      }, true, 3000);
    }

    function executeCallToServerForNewOrderFromQuotation(data) {
      var serverCall = new OB.DS.Process('org.openbravo.retail.stockvalidation.OrderFromQuotationStockChecker');
      var statusMessage = OB.UTIL.showStatus(OB.I18N.getLabel('OBPOSSV_GettingStockFromServer'));
      var modalMessage = "";
      var forbiddenLines = false;
      var allowedLines = false;
      serverCall.exec({
        ticketLines: data
      }, function (data, message) {
        statusMessage.hide();
        if (data && data.exception) {
          OB.UTIL.showError(OB.I18N.getLabel('OBPOSSV_ErrorGettingStockFromServer'));
        } else if (data && data.length > 0) {
          var notValidLines = _.filter(data, function (curLine) {
            return curLine.allowSell === false;
          }, this);
          if (notValidLines && notValidLines.length > 0) {
            var allowNegativeStockLines = _.filter(notValidLines, function (curLine) {
              return curLine.allowNegativeStock;
            });
            var forbidNegativeStockLines = _.filter(notValidLines, function (curLine) {
              return !curLine.allowNegativeStock;
            });
            var i;
            // Exists at least one store bin that allows negative stock
            if (allowNegativeStockLines && allowNegativeStockLines.length > 0) {
              allowedLines = true;
              _.each(allowNegativeStockLines, function (curLine) {
                modalMessage = modalMessage + OB.I18N.getLabel('OBPOSSV_stockInfoLine', [curLine.productIdentifier, curLine.availableQty, curLine.originalQty]);
                modalMessage = modalMessage + '\n';
              }, this);
              modalMessage = modalMessage + '\n';
              modalMessage = modalMessage + OB.I18N.getLabel('OBPOSSV_NoEnoughStockQuestion');
              for (i = 0; i < allowNegativeStockLines.length; i++) {
                var allowNegativeLine = allowNegativeStockLines[i];
                me.model.get('order').get('lines').at(allowNegativeLine.index).set('overissueStoreBin', allowNegativeLine.overissueStoreBin, {
                  silent: true
                });
                me.model.get('order').get('lines').at(allowNegativeLine.index).set('overissueQty', allowNegativeLine.overissueQty, {
                  silent: true
                });
              }
            }
            // Exists at least one store bin that forbids negative stock
            if (forbidNegativeStockLines && forbidNegativeStockLines.length > 0) {
              forbiddenLines = true;
              _.each(forbidNegativeStockLines, function (curLine) {
                modalMessage = modalMessage + OB.I18N.getLabel('OBPOSSV_stockInfoLine', [curLine.productIdentifier, curLine.availableQty, curLine.originalQty]);
                modalMessage = modalMessage + '\n';
              }, this);
              modalMessage = modalMessage + '\n';
              modalMessage = modalMessage + OB.I18N.getLabel('OBPOSSV_notAllowedNegativeStock');
            }
            // All lines are forbidden to be added
            if (forbiddenLines && !allowedLines) {
              OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOSSV_modalHeader'), modalMessage, null, {
                onHideFunction: function (popup) {
                  var linesToDelete = [];
                  _.each(forbidNegativeStockLines, function (curLine) {
                    if (curLine.availableQty <= 0) {
                      linesToDelete.push(me.model.get('order').get('lines').at(curLine.index));
                    } else {
                      me.model.get('order').get('lines').at(curLine.index).set('qty', curLine.availableQty);
                    }
                  }, this);
                  me.model.get('order').deleteLinesFromOrder(linesToDelete);
                }
              });
            } else {
              OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOSSV_modalHeader'), modalMessage, [{
                label: OB.I18N.getLabel('OBPOSSV_LblProceed'),
                action: function () {
                  var linesToDelete = [];
                  _.each(forbidNegativeStockLines, function (curLine) {
                    if (curLine.availableQty <= 0) {
                      linesToDelete.push(me.model.get('order').get('lines').at(curLine.index));
                    } else {
                      me.model.get('order').get('lines').at(curLine.index).set('qty', curLine.availableQty);
                    }
                  }, this);
                  me.model.get('order').deleteLinesFromOrder(linesToDelete, function () {
                    return true;
                  });
                }
              }, {
                label: OB.I18N.getLabel('OBMOBC_LblCancel')
              }], {
                onHideFunction: function (popup) {
                  var linesToDelete = [];
                  _.each(allowNegativeStockLines.concat(forbidNegativeStockLines), function (curLine) {
                    if (curLine.availableQty <= 0) {
                      linesToDelete.push(me.model.get('order').get('lines').at(curLine.index));
                    } else {
                      me.model.get('order').get('lines').at(curLine.index).set('qty', curLine.availableQty);
                    }
                  }, this);
                  me.model.get('order').deleteLinesFromOrder(linesToDelete);
                },
                style: 'background-color: #EBA001'
              });
            }
          }
        }
      }, function (error) {
        OB.UTIL.showLoading(false);
        statusMessage.hide();
        OB.UTIL.showError(OB.I18N.getLabel('OBPOSSV_ErrorGettingStockFromServer'));
      });
    }

    this.model.get('order').get('lines').on('add', function (addedModel) {
      if (!OB.MobileApp.model.hasPermission('OBPOSSV_EnableStockValidation', true)) {
        return;
      }
      addedModel.on('change:qty', function (changedModel) {
        if (!addedModel.get('product').get('stocked') || addedModel.get('qty') < 0 || this.model.get('order').get('orderType') === 1 || this.model.get('order').get('isPaid') === true || this.model.get('order').get('isQuotation') === true) {
          return;
        }
        executeCallToServer(changedModel);
      }, this);
      if (!addedModel.get('product').get('stocked') || addedModel.get('qty') < 0 || this.model.get('order').get('orderType') === 1 || this.model.get('order').get('isPaid') === true || this.model.get('order').get('isQuotation') === true) {
        return;
      }
      executeCallToServer(addedModel);
    }, this);

    this.model.get('order').on('orderCreatedFromQuotation', function () {
      var dataToSend = [];
      _.each(this.model.get('order').get('lines').models, function (curLine, index) {
        var lineToSend = {};
        if (curLine.get('product').get('stocked')) {
          lineToSend.productid = curLine.get('product').id;
          lineToSend.productidentifier = curLine.get('product').get('_identifier');
          lineToSend.qty = curLine.get('qty');
          lineToSend.index = index;
          lineToSend.stocked = curLine.get('product').get('stocked');
          if (curLine.get('product').get('hasAttributes') && _.isString(curLine.get('attributeValue')) && curLine.get('attributeValue').length > 0) {
            lineToSend.attributeValue = curLine.get('attributeValue');
          }
          dataToSend.push(lineToSend);
        }
      }, this);
      if (dataToSend.length > 0) {
        executeCallToServerForNewOrderFromQuotation(dataToSend);
      }
    }, this);
  }
});

OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
  kind: 'OBPSSV.UI.dummyControl',
  name: 'OBPOSSV_dummyControl'
});