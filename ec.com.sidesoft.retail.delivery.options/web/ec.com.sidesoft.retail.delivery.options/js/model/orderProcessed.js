// Creamos el modelo de la tabla Sqllite
var OrderProcessed = OB.Data.ExtensibleModel.extend({
  modelName: 'OrderProcessed',
  tableName: 'orderprocessed',
  entityName: 'OrderProcessed',
  source: '',
  local: true
 });

// Añadimos las propiedades para el modelo Serve Option
OrderProcessed.addProperties([{
   name: 'id',
   column: 'id',
   primaryKey: true,
   type: 'TEXT'
 },{
   name: 'isprocess',
   column: 'isprocess',
   type: 'TEXT'
 }, {
  name: 'payments',
  column: 'payments',
  type: 'TEXT'
}]);

 // Registro del modelo en el POS (Sqllite)
 OB.Data.Registry.registerModel(OrderProcessed);

/* ******************************************************************************* */
/* VERIFICO SI LA ORDEN QUE SE ESTA ABRIENDO YA ESTA EN LA TABLA ORDERPROCESSED    */
/*                    SI NO ESTA EN LA TABLA SE INSERTA                            */
/* ******************************************************************************* */
OB.UTIL.HookManager.registerHook('OBRETUR_ReturnFromOrig', function (args, c) {
  
  var isDomicilie = OB.POS.modelterminal.get('terminal').isdomicilie; 

  if(isDomicilie){

    var orderPOSId = args.order.orderid;
    var orderPOSdocumentNo = args.order.documentNo;
    var orderPOSLocId = args.order.bpLocId;
    var criteria = {};

    function verfiedLocation(orderPOSLocId) {

      var criteriaLocation = { 
        id: {
          operator: OB.Dal.EQ,
          value: orderPOSLocId
        },
        '_orderByClause': 'c_bpartner_location_id desc'
      };      

      OB.Dal.find(OB.Model.BPLocation, criteriaLocation, function (data) {      

        if (data.models && data.models.length == 0) {

          //LA DIRECCION DEL TERCERO NO EXISTE Y SE RECARGA EL NAVEGADOR
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SRDPO_Information'), OB.I18N.getLabel('SRDPO_BPNotExistInCahe'), 
            [{
            label: OB.I18N.getLabel('SRDPO_Refresh'),
            action: function () {
              //SE ENVIA EL PROCESO DE SYNCRONIZACION INCREMENTAL
              OB.MobileApp.model.loadModels(null, true);
              OB.UTIL.startLoadingSteps();
              OB.MobileApp.model.set('isLoggingIn', true);
              OB.UTIL.showLoading(true);
              OB.MobileApp.model.on('incrementalModelsLoaded', function () {
                OB.MobileApp.model.off('incrementalModelsLoaded');
                OB.UTIL.showLoading(false);
                OB.MobileApp.model.set('isLoggingIn', false);
                OB.MobileApp.model.runSyncProcess();
                OB.MobileApp.model.hookManager.callbackExecutor(args, c);
              });              
            }
          }]);

        }else{
          OB.MobileApp.model.hookManager.callbackExecutor(args, c);
        }

      });

    }    
    
    criteria._whereClause="where id='" +orderPOSId + "'";
    OB.Dal.find(OB.Model.OrderProcessed, criteria, function (data) {      
      if (data.models && data.models.length == 0) {

        var modelOrder = new OB.Model.OrderProcessed();
        var serverCall = new OB.DS.Process('ec.com.sidesoft.retail.delivery.options.OrderPayment');
        serverCall.exec({
          documentNo: orderPOSdocumentNo
        }, function (dataPayment, message) {
          if(dataPayment.length > 0){
            var paymentCall = JSON.stringify(dataPayment);
            modelOrder.set('id',orderPOSId);
            modelOrder.set('isprocess','0');
            modelOrder.set('payments', paymentCall);
            OB.Dal.save(modelOrder, null, null, true);            
          }else{
            modelOrder.set('id',orderPOSId);
            modelOrder.set('isprocess','0');
            OB.Dal.save(modelOrder, null, null, true);             
          }
        }, function (error) {
          modelOrder.set('id',orderPOSId);
          modelOrder.set('isprocess','0');
          OB.Dal.save(modelOrder, null, null, true);          
        });        

        // SE VERIFICA QUE LA DIRECCION DEL PEDIDO EXISTA EN LA CACHE
        verfiedLocation(orderPOSLocId);

      }else{

        var recordOrder = data.models[0];
        var isProcessed = recordOrder.get('isprocess');        
        if(isProcessed === '1'){
          processedOrderPopupOrigin(args, c, orderPOSdocumentNo);
          return;
        }else{
          // SE VERIFICA QUE LA DIRECCION DEL PEDIDO EXISTA EN LA CACHE
          verfiedLocation(orderPOSLocId);          
        }
      
      }

    }, function () {
      OB.MobileApp.model.hookManager.callbackExecutor(args, c);
    });           

  }else{
    OB.MobileApp.model.hookManager.callbackExecutor(args, c);
  }
  
});

/* ******************************************************************************* */
/*      VERIFICO QUE LOS PRODUCTOS QUE NO SON LDM TENGAN STOCK DISPONIBLE          */
/* VERIFICO SI LA ORDEN QUE SE ESTA ABRIENDO YA ESTA EN LA TABLA ORDERPROCESSED    */
/*                         Y ESTA ISPROCESSED = TRUE                               */
/* ******************************************************************************* */
OB.UTIL.HookManager.registerHook('OBPOS_PreOrderSave', function (args, c) {
  
  var isDomicilie = OB.POS.modelterminal.get('terminal').isdomicilie; 

  if(isDomicilie){

    var comm="'";
    var orderPOSId = args.context.context.attributes.order.attributes.id;
    var orderPOSDocumentno = args.context.context.attributes.order.attributes.documentNo;
    var esConvenio = args.context.context.attributes.order.attributes.bp.attributes.sscmbIsagreement;
    var criteria = {};

    function processResult(tx) {
      window.console.log(tx);
    }

    function processError(tx) {
      window.console.error();
    }      

    if(esConvenio){
      criteria._whereClause="where id='" +orderPOSId + "'";
      OB.Dal.find(OB.Model.OrderProcessed, criteria, function (data) {
        if (data.models && data.models.length > 0) {

          var recordOrder = data.models[0];
          var isProcessed = recordOrder.get('isprocess');
          
          if (isProcessed == '0'){
            //Actualizo la orden a procesada
            var sqlupdate = 'update orderprocessed set isprocess = true where id = ' + comm + orderPOSId + comm;
            OB.Data.localDB.transaction(function(tx) {
              tx.executeSql(sqlupdate,null, processResult, processError);
            });
          }else{
            // SI LA ORDER ESTA EN PROCESSED TRUE ENVIAR MENSAJE PARA PO DEJAR PROCESAR LA ORDEN
            processedOrderPopup(args, c, orderPOSDocumentno);
            return
          }
        }
        OB.MobileApp.model.hookManager.callbackExecutor(args, c);
      });

    }

    OB.MobileApp.model.hookManager.callbackExecutor(args, c);
  } else {
    OB.MobileApp.model.hookManager.callbackExecutor(args, c);  
  }
}); 

var processedOrderPopup = function (args, c, orderPOSDocumentno) {
  OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_Error'), OB.I18N.getLabel('SRDPO_ErrorOrderProcessed', [orderPOSDocumentno]));
  OB.UTIL.showError('El pedido ' + orderPOSDocumentno + ' ya ha sido procesado.');
  args.cancellation = true;
  //OB.UTIL.HookManager.callbackExecutor(args, c);
  OB.MobileApp.model.hookManager.callbackExecutor(args, c);
};

var processedOrderPopupOrigin = function (args, c, orderPOSDocumentno) {
  OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_Error'), OB.I18N.getLabel('SRDPO_ErrorOrderProcessed', [orderPOSDocumentno]));
  OB.UTIL.showError('El pedido ' + orderPOSDocumentno + ' ya ha sido procesado.');
  args.cancelOperation = true;
  OB.MobileApp.model.hookManager.callbackExecutor(args, c);
};

OB.UTIL.HookManager.registerHook('OBPOS_preAddPayment', function (args, c) {
	
  var isDomicilie = OB.POS.modelterminal.get('terminal').isdomicilie,
      orderid = args.receipt.get('orderid'),
      esConvenio = args.receipt.attributes.bp.attributes.isagreements;
      criteria = {};

  if(isDomicilie){ 

    if(!args.receipt.getPaymentStatus().isNegative){
      var result = null,
      amount = args.paymentToAdd.get('amount'),
      paymentName = args.paymentToAdd.get('name'),
      target = args.paymentToAdd.get('kind');    

      criteria._whereClause="where id='" +orderid + "'";
      OB.Dal.find(OB.Model.OrderProcessed, criteria, function (data) {      

        if (data.models && data.models.length > 0) {

          var recordOrder = data.models[0],
          payments = recordOrder.get('payments'),
          paymentsParse = JSON.parse(payments),
          mensaje;

          if(paymentsParse === null) {
            OB.MobileApp.model.hookManager.callbackExecutor(args, c);
          }else{
            result = _.find(paymentsParse, function (categ) {
              if(target === categ.searchKey){
                return true;
              }
            }, this);
            
            if (result) {
              if(amount === result.monto){
                OB.MobileApp.model.hookManager.callbackExecutor(args, c);
              }else{
                if(!esConvenio){
                  args.cancellation = true;
                  OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SRDPO_ErrorPaymentMethod'), OB.I18N.getLabel('SRDPO_PaymentMethod', [paymentName, OB.I18N.formatCurrency(result.monto)]), [{
                    label: OB.I18N.getLabel('OBMOBC_LblOk'),
                    action: function() {
                      OB.MobileApp.model.hookManager.callbackExecutor(args, c);
                    }
                  }], {
                    onHideFunction: function() {
                    }
                  });
                }else{
                  OB.MobileApp.model.hookManager.callbackExecutor(args, c);    
                }           
              }
            } else {
              if(!esConvenio){
                args.cancellation = true;
                OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SRDPO_ErrorPaymentMethod'), OB.I18N.getLabel('SRDPO_PaymentMethodNotAllowed', [paymentName]), [{
                  label: OB.I18N.getLabel('OBMOBC_LblOk'),
                  action: function() {
                    OB.MobileApp.model.hookManager.callbackExecutor(args, c);
                  }
                }], {
                  onHideFunction: function() {
                  }
                });        
              }else{
                OB.MobileApp.model.hookManager.callbackExecutor(args, c);    
              }
            }

          }

        }else{
          OB.MobileApp.model.hookManager.callbackExecutor(args, c);
        }            
      }, function () {
        OB.MobileApp.model.hookManager.callbackExecutor(args, c);
      });      

    }else{
      OB.MobileApp.model.hookManager.callbackExecutor(args, c);  
    }

  }else{
    OB.MobileApp.model.hookManager.callbackExecutor(args, c);
  }

});

