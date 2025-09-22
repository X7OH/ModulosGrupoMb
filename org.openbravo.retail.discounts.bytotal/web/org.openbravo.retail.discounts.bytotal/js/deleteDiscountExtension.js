/*
 ************************************************************************************
 * Copyright (C) 2016-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, console, exports, Backbone, _  */

//modalDeleteDiscounts extension
(function () {
  var isTotalDiscount = function (promotionObj) {
      var discountRule = OB.Model.Discounts.discountRules[promotionObj.discountType];
      return (discountRule.byTotalDiscount === undefined ? false : discountRule.byTotalDiscount);
      };
  var deleteDiscountModalPrototype = OB.UI.ModalDeleteDiscount.prototype,
      deleteDiscountcallbackExecutor = deleteDiscountModalPrototype.callbackExecutor;

  deleteDiscountModalPrototype.callbackExecutor = _.wrap(deleteDiscountModalPrototype.callbackExecutor, function (wrapped, inSender, inEvent) {
    var manualPromotions = this.args.receipt.get('orderManualPromotions'),
        i, j;

    for (i = 0; i < this.promotionsList.length; i++) {
      if (this.promotionsList[i].deleteDiscount && isTotalDiscount(this.promotionsList[i].promotionObj)) {
        //if Total discount , delete from orderManualPromotions
        for (j = 0; j < manualPromotions.length; j++) {
          if (manualPromotions.at(j).get('rule').id === this.promotionsList[i].promotionObj.ruleId && manualPromotions.at(j).get('rule').discountinstance === this.promotionsList[i].promotionObj.discountinstance) {
            manualPromotions.remove(manualPromotions.at(j));
            break;
          }
        }
        continue;
      }
    }

    (_.bind(deleteDiscountcallbackExecutor, this, inSender, inEvent))();
  });

  var deleteDiscountLinePrototype = OB.UI.DeleteDiscountLine.prototype,
      deleteDiscountLineRenderLines = deleteDiscountLinePrototype.renderDiscountLines;

  deleteDiscountLinePrototype.renderDiscountLines = _.wrap(deleteDiscountLinePrototype.renderDiscountLines, function (wrapped) {
    if (isTotalDiscount(this.newAttribute.promotionObj)) {
      var byTotalDiscountAmt = 0,
          me = this;
      this.$.checkboxButtonDiscount.check();
      this.$.discount.setContent(this.newAttribute.promotionIdentifier);
      // get total amount of byTotalDiscount
      this.args.receipt.get('lines').models.forEach(function (line) {
        _.each(line.get('promotions'), function (linePromotions) {
          //check manual promotions
          if (me.newAttribute.promotionObj.ruleId === linePromotions.ruleId && me.newAttribute.promotionObj.discountinstance === linePromotions.discountinstance) {
            byTotalDiscountAmt += linePromotions.amt;
          }
        });
      });
      this.$.price.setContent(OB.I18N.formatCurrency(byTotalDiscountAmt * (-1)));
      this.$.discountedProducts.createComponent({
        style: 'text-align: left; color: rgb(159, 66, 66);',
        components: [{
          tag: 'li',
          content: OB.I18N.getLabel('DISCT_LblDeleteDiscountOnAllLines')
        }, {
          style: 'clear: both;'
        }]
      });
    } else {
      (_.bind(deleteDiscountLineRenderLines, this))();
    }
  });
}());