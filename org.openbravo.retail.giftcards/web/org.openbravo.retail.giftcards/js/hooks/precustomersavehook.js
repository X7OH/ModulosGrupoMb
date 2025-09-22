/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

(function () {
  OB.UTIL.HookManager.registerHook('OBPOS_BeforeCustomerSave', function (args, callbacks) {
    args.customer.set('uniqueCreditNote', true);
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    return;
  });
}());