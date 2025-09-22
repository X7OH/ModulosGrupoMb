OB.UTIL.HookManager.registerHook('OBPOS_TerminalLoadedFromBackend', function (args, c) {

  var isDomicilie = OB.POS.modelterminal.get('terminal').isdomicilie;
  var terminalID = OB.POS.modelterminal.get('terminal').id;

  var reproduceErrorSoundDelivery = function () {
    var error_sound = new Audio('../ec.com.sidesoft.retail.delivery.options/sounds/delivery-bell.mp3');
    error_sound.play();
  };

  if(isDomicilie){

    process = new OB.DS.Process('ec.com.sidesoft.retail.delivery.options.OrdersDomicilie');

    // EL INTERVALO SE EJECUTA CADA 2 MINUTOS (120000 MILISEGUNDOS)
    setInterval(function(){

      process.exec({
        posid: terminalID
      }, function (data) {

        if (data) {
          // SI EXISTEN ORDENES TIPO RESERVA Y QUE NO ESTEN PAGADAS PARA ESTA CAJA
          if(data > 0){
            OB.MobileApp.keyPressProcessed = true;
            reproduceErrorSoundDelivery();
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SRDPO_Information'), OB.I18N.getLabel('SRDPO_PendingOrders'));
            OB.MobileApp.model.hookManager.callbackExecutor(args, c);
          }
        }

      });

    }, 120000);

  }
  OB.MobileApp.model.hookManager.callbackExecutor(args, c);

});