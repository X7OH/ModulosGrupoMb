/*
 ************************************************************************************
 * Copyright (C) 2014-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, $ */


(function () {

  /**
   * Adds a function into the lineShouldBeIncludedFunctions array of functions of OB.UI.ModalReturnReceipt
   * @return {Boolean} true: the receipt line can be selectable, false: the receipt line cannot be selected
   */
  OB.UI.ModalReturnReceipt.prototype.lineShouldBeIncludedFunctions.push({
    isSelectableLine: function (receiptLine) {
      if (!OB.UTIL.isNullOrUndefined(receiptLine.giftCardType)) {
        return false;
      }
      return true;
    }
  });

}());