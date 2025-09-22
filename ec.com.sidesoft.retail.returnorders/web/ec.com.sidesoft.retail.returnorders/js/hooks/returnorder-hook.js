/* ********************************************************************** */
/*    SE INSERTAN LOS PAGOS PROCESADOS PARA UN PEDIDO QUE ES DEVOLUCION   */
/*                SI NO ESTA EN LA TABLA SE INSERTA                       */
/* ********************************************************************** */
OB.UTIL.HookManager.registerHook('OBRETUR_ReturnFromOrig', function (args, c) {

  // EL PEDIDO ES DEVOLUCION
  if(args.context.filters){

    if(args.context.filters.isReturn){

      var orderPOSId = args.context.model.get('order').id,
      paymentsOrder = [],
      giftcardPaymentId = null,
      criteria = {};
    
      _.each(args.order.receiptPayments, function (model, indexModel) {
          if(model.kind === 'SRGC_GiftCard.Tarjetas'){
            giftcardPaymentId = model.paymentId;
          }
          paymentsOrder.push(model);
      });	    

      if(paymentsOrder.length > 0){

        criteria._whereClause="where id='" +orderPOSId + "'";
        OB.Dal.find(OB.Model.ReturnOrders, criteria, function (data) {      

          if (data.models && data.models.length == 0) {

            var modelOrderReturns = new OB.Model.ReturnOrders();
            modelOrderReturns.set('id',orderPOSId);
            modelOrderReturns.set('isprocess','0');
            modelOrderReturns.set('payments', JSON.stringify(paymentsOrder));
            modelOrderReturns.set('giftcardPaymentId', giftcardPaymentId);
            OB.Dal.save(modelOrderReturns, null, null, true); 

            OB.UTIL.HookManager.callbackExecutor(args, c);

          }else{
            OB.UTIL.HookManager.callbackExecutor(args, c);
          }

        }, function () {
          OB.UTIL.HookManager.callbackExecutor(args, c);
        });        

      }else{
        OB.UTIL.HookManager.callbackExecutor(args, c);
      }

    }else{
      OB.UTIL.HookManager.callbackExecutor(args, c);
    }

  }else{
      OB.UTIL.HookManager.callbackExecutor(args, c);
  }

});
// ***********************************************************************
// ***********************************************************************

/* ********************************************************************* */
/*              SE VERIFICAN LOS PAGOS PARA EL PEDIDO DEVOLUCION         */
/* ********************************************************************* */
OB.UTIL.HookManager.registerHook('OBPOS_preAddPayment', function (args, c) {
	
  // EL PEDIDO ES DEVOLUCION
  if(args.receipt.getPaymentStatus().isNegative){

    var resultReturn = null,
        amount = args.paymentToAdd.get('amount'),
        paymentName = args.paymentToAdd.get('name'),
        target = args.paymentToAdd.get('kind'),
        orderid = args.receipt.id,
        criteria = {};

    criteria._whereClause="where id='" +orderid + "'";
    OB.Dal.find(OB.Model.ReturnOrders, criteria, function (data) {      

      if (data.models && data.models.length > 0) {

        var recordOrderReturn = data.models[0],
            paymentsReturn = recordOrderReturn.get('payments'),
            paymentsParseReturn = JSON.parse(paymentsReturn);        

        resultReturn = _.find(paymentsParseReturn, function (categ) {
          if(target === categ.kind){
            return true;
          }
        }, this);  
        
        if (resultReturn) {

          if(amount === resultReturn.amount){
            OB.UTIL.HookManager.callbackExecutor(args, c);
          }else{
            args.cancellation = true;
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SSRROP_ErrorPaymentMethod'), OB.I18N.getLabel('SSRROP_PaymentMethodReturn', [paymentName, OB.I18N.formatCurrency(resultReturn.amount)]), [{
              label: OB.I18N.getLabel('OBMOBC_LblOk'),
              action: function() {
                OB.UTIL.HookManager.callbackExecutor(args, c);
              }
            }], {
              onHideFunction: function() {
              }
            });           
          }
        } else {
          args.cancellation = true;
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SSRROP_ErrorPaymentMethod'), OB.I18N.getLabel('SSRROP_PaymentMethodNotAllowed', [paymentName]), [{
            label: OB.I18N.getLabel('OBMOBC_LblOk'),
            action: function() {
              OB.UTIL.HookManager.callbackExecutor(args, c);
            }
          }], {
            onHideFunction: function() {
            }
          });  
        }  
        
      }else{
        OB.UTIL.HookManager.callbackExecutor(args, c);
      }            

    }, function () {
      OB.UTIL.HookManager.callbackExecutor(args, c);
    });     

  }else{
    OB.UTIL.HookManager.callbackExecutor(args, c);
  } 

});
// ***********************************************************************
// ***********************************************************************


// ***********************************************************************
// SE INSERTA EL ID DE LA TRANSACCION AL PAGO CUANDO ES GIFTCARD
// ***********************************************************************
OB.UTIL.HookManager.registerHook('OBPOS_PreOrderSave', function (args, c) {
  
  // EL PEDIDO ES DEVOLUCION
  if(args.receipt.getPaymentStatus().isNegative){

    var modeloPagos = args.receipt.get('payments').models,
        orderid = args.receipt.id,
        criteria = {};

    function processResult(tx) {
      window.console.log(tx);
    }

    function processError(tx) {
      window.console.error();
    }         

    _.forEach(modeloPagos, function (payment) {
      if(payment.get('kind') === 'SRGC_GiftCard.Tarjetas'){

        criteria._whereClause="where id='" +orderid + "'";
        OB.Dal.find(OB.Model.ReturnOrders, criteria, function (data) {      
    
          if (data.models && data.models.length > 0) {
            
            var recordOrderReturn = data.models[0],
                giftcardPaymentId = recordOrderReturn.get('giftcardPaymentId'),
                isProcessed = recordOrderReturn.get('isprocess'),
                comm="'";
            
            // SETEO LA TRANSACCION AL PAGO CON GIFTCARD    
            payment.set('transaction',giftcardPaymentId);

            if (isProcessed == '0'){
              //Actualizo la orden a procesada
              var sqlupdate = 'update returnorders set isprocess = true where id = ' + comm + orderid + comm;
              OB.Data.localDB.transaction(function(tx) {
                tx.executeSql(sqlupdate,null, processResult, processError);
              });
              OB.UTIL.HookManager.callbackExecutor(args, c);
            }else{
              OB.UTIL.HookManager.callbackExecutor(args, c);
            }           
          }else{
            OB.UTIL.HookManager.callbackExecutor(args, c);
          }            
        }, function () {
          OB.UTIL.HookManager.callbackExecutor(args, c);
        }); 


      }else{
        OB.UTIL.HookManager.callbackExecutor(args, c);          
      }
    });  

  }else{
    OB.UTIL.HookManager.callbackExecutor(args, c);
  }    

});
// ***********************************************************************
// ***********************************************************************