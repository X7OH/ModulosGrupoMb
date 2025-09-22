/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, $, _ */


(function () {

  OB.UTIL.HookManager.registerHook('OBPOS_PreAddProductToOrder', function (args, callbacks) {
    var affectedLine = _.filter(args.receipt.get('lines').models, function (line) {
      return line.get('product') === args.productToAdd;
    });
    if (affectedLine.length > 0 && !affectedLine[0].get('isEditable')) {
      args.cancelOperation = true;
      OB.UTIL.showError(OB.I18N.getLabel('OBRETUR_CannotChangeQty'));
    }

    OB.UTIL.HookManager.callbackExecutor(args, callbacks);

    return;
  });

}());