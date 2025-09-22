(function () {

  function executeHookLogic(args, callbacks) {
    if (args.cancellation) {
      OB.UI.GiftCardUtilsRetail.service('ec.com.sidesoft.retail.giftcard.RevertGiftCardAmount', {
        _executeInOneServer: true,
        _tryCentralFromStore: true,
        transaction: args.paymentToRem.get('transaction')
      }, function (result) {
        if (result && result.exception) {
          var msg = result.exception.message;
          if (msg.indexOf('SRGC_') === 0) {  
            var msgsplit = msg.split(':');
            msg = OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1));
          }
          OB.MobileApp.view.waterfall('onShowPopup', {
            popup: 'SRGC_UI_Message',
            args: {
              message: msg
            }
          });
        } else if (result.success) {
          args.cancellation = false;
        }
        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
      }, function (error) {
        var msg = error.exception.message;
        //if (msg.indexOf('GCNV_') === 0) {
        if (msg.indexOf('SRGC_') === 0) {  
          var msgsplit = msg.split(':');
          msg = OB.I18N.getLabel(msgsplit[0], msgsplit.slice(1));
        }
        OB.MobileApp.view.waterfall('onShowPopup', {
          popup: 'SRGC_UI_Message',
          args: {
            message: msg
          }
        });
        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
      });
    } else {
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    }
  }

  OB.UTIL.HookManager.registerHook('OBPOS_preRemovePayment', function (args, callbacks) {
    if(args.paymentToRem.get('kind') === 'SRGC_GiftCard.Tarjetas'){
      if (args.cancellation) {
        OB.UTIL.HookManager.callbackExecutor(args, callbacks);
      } else {
        args.cancellation = true;
        executeHookLogic(args, callbacks);
      }
    }else{
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    }
  });  
}());