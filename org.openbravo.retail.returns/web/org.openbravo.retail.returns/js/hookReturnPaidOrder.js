/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB */


(function () {

  OB.UTIL.HookManager.registerHook('OBRETUR_ReturnFromOrig', function (args, callbacks) {
    var order = args.order,
        params = args.params,
        context = args.context;
    if (params.isReturn) {
      if (context.model && context.model.get('order') && context.model.get('order').get('isEditable') === false) {
        context.doShowPopup({
          popup: 'modalNotEditableOrder'
        });
        return;
      }
      context.doShowPopup({
        popup: 'modalReturnReceipt',
        args: {
          args: args,
          callbacks: callbacks
        }
      });
    } else {
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    }

    return;
  });

}());