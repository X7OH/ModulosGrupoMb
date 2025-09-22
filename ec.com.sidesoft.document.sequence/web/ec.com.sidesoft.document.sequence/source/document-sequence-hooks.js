OB.UTIL.HookManager.registerHook("OBPOS_PreOrderSave", function (args, c) {
  var receipt = args.receipt;
  var model = args.model;
  //TODO
  //crear criteria en función de si es positivo el total o negativo (invoice document o credit note document) y mirar si el tercero es facturable
  var orderType = receipt.get('orderType');

  var criteria = {};
  //TODO cambiar por el campo que ellos usan para facturar o no
  if (OB.Model.InvoiceDocumentSequence &&
    (receipt.get('bp').get('sscmbIsagreement') === false ||
      receipt.get('bp').get('sscmbIsagreement') === undefined ||
      receipt.get('bp').get('sscmbIsagreement') === null)) {
    if (SIDESOFTUTILS.isReturn(receipt) && (orderType === 1 || orderType === 0)) {
      //Se considera una devolución y hay que consumir de credit note sequence
      criteria.invoiceSeq = {
        operator: OB.Dal.EQ,
        value: false
      }
    } else if (orderType === 0) {
      //Venta normal y hay que consumir de factura
      criteria.invoiceSeq = {
        operator: OB.Dal.EQ,
        value: true
      }
    } else {
      OB.UTIL.HookManager.callbackExecutor(args, c);
      return;
    }
  } else {
    OB.UTIL.HookManager.callbackExecutor(args, c);
    return;
  }

  OB.Dal.find(OB.Model.InvoiceDocumentSequence, criteria, function (data) {
    if (data.models && data.models.length > 0) {
      var recordSequence = data.models[0];
      var newDocumentNo = recordSequence.get('currentSeq');
      var identifier = recordSequence.get('identifier');
      var inDevelopment = recordSequence.get('inDevelopment');
      var searchKey = recordSequence.get('searchKey');
      var store = recordSequence.get('store');
      var RUC = recordSequence.get('RUC');

    } else {
      sidesoftErrorPopUp(args, c);
      return;
    }

    var documentSequenceNo = SIDESOFTUTILS.generateCode(newDocumentNo, receipt.get('bp'), identifier, inDevelopment, store, RUC);
    if (documentSequenceNo === -1) {
      sidesoftErrorPopUp(args, c);
      return;
    }

    
    var isDomicilie = OB.POS.modelterminal.get('terminal').isdomicilie; 

    if(isDomicilie){

      var comm="'";
      var orderPOSId = args.receipt.id;
      var orderPOSDocumentno = args.receipt.attributes.documentNo;
      var products = [];
      var criteria = {};
      criteria.remoteFilters = [];

      function processResult(tx) {
        window.console.log(tx);
      }

      function processError(tx) {
        window.console.error();
      }  
      
      criteria._whereClause="where id='" +orderPOSId + "'";
      
      OB.Dal.find(OB.Model.OrderProcessed, criteria, function (data) {
      	if (data.models && data.models.length == 0) {

          var modelOrder = new OB.Model.OrderProcessed();
          modelOrder.set('id',orderPOSId);
          modelOrder.set('isprocess','0');
          
          OB.Dal.save(modelOrder, null, null, true);

          //Actualizo la sequencia
          OB.Dal.updateRecordColumn(recordSequence, 'currentSeq', newDocumentNo + 1, function () {
            //TODO
            //Cambiar el documentno del recibo
            receipt.set('invoiceDocumentNo', SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
            model.set('invoiceDocumentNo', SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
            receipt.set('validationCode', documentSequenceNo);
      
            OB.MobileApp.model.receipt.set('invoiceDocumentNo',SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo) );
            OB.MobileApp.model.receipt.set('invoiceDocumentNo',  SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
            OB.MobileApp.model.receipt.set('validationCode', documentSequenceNo);
            
            if (SIDESOFTUTILS.isReturn(receipt)) {
              receipt.set('isReturn', true);
              model.set('isReturn', true);
            } else {
              receipt.set('isReturn', false);
              model.set('isReturn', false);
            }
      
            OB.UTIL.HookManager.callbackExecutor(args, c);
          }, function () {
            sidesoftErrorPopUp(args, c)
          });              

      	}else{
      		var recordOrder = data.models[0];
      		var isProcessed = recordOrder.get('isprocess');

          if (isProcessed == '0'){  

            //Actualizo la orden a procesada
            var sqlupdate = 'update orderprocessed set isprocess = true where id = ' + comm + orderPOSId + comm;
            OB.Data.localDB.transaction(function(tx) {
              tx.executeSql(sqlupdate,null, processResult, processError);
            });

            //Actualizo la sequencia
            OB.Dal.updateRecordColumn(recordSequence, 'currentSeq', newDocumentNo + 1, function () {
              //TODO
              //Cambiar el documentno del recibo
              receipt.set('invoiceDocumentNo', SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
              model.set('invoiceDocumentNo', SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
              receipt.set('validationCode', documentSequenceNo);

              OB.MobileApp.model.receipt.set('invoiceDocumentNo',SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo) );
              OB.MobileApp.model.receipt.set('invoiceDocumentNo',  SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
              OB.MobileApp.model.receipt.set('validationCode', documentSequenceNo);
              
              if (SIDESOFTUTILS.isReturn(receipt)) {
                receipt.set('isReturn', true);
                model.set('isReturn', true);
              } else {
                receipt.set('isReturn', false);
                model.set('isReturn', false);
              }

              OB.UTIL.HookManager.callbackExecutor(args, c);
            }, function () {
              sidesoftErrorPopUp(args, c)
            });              
            OB.MobileApp.model.hookManager.callbackExecutor(args, c);

      		}else{

            // SI LA ORDER ESTA EN PROCESSED TRUE ENVIAR MENSAJE PARA PO DEJAR PROCESAR LA ORDEN
            processedOrderPopup(args, c, orderPOSDocumentno);
            return;

      		}

      	}

      });
      
    } else {
    
		    //Actualizo la sequencia
		    OB.Dal.updateRecordColumn(recordSequence, 'currentSeq', newDocumentNo + 1, function () {
		      //TODO
		      //Cambiar el documentno del recibo
		      receipt.set('invoiceDocumentNo', SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
		      model.set('invoiceDocumentNo', SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
		      receipt.set('validationCode', documentSequenceNo);
		
		      OB.MobileApp.model.receipt.set('invoiceDocumentNo',SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo) );
		      OB.MobileApp.model.receipt.set('invoiceDocumentNo',  SIDESOFTUTILS.concatGroupedBy3(store + newDocumentNo));
		      OB.MobileApp.model.receipt.set('validationCode', documentSequenceNo);
		      
		      if (SIDESOFTUTILS.isReturn(receipt)) {
		        receipt.set('isReturn', true);
		        model.set('isReturn', true);
		      } else {
		        receipt.set('isReturn', false);
		        model.set('isReturn', false);
		      }
		
		      OB.UTIL.HookManager.callbackExecutor(args, c);
		    }, function () {
		      sidesoftErrorPopUp(args, c)
		    });
		    
    }

  }, function () {
    errorPopUp(args, c)
  });
});


var SIDESOFTUTILS = {}


SIDESOFTUTILS.concatGroupedBy3 = function (key) {

  var newKey = '';
  var upd_acct = 0;
  for (var i = 1; i <= key.length; i++) {
		if (i % 3 === 0 && i !== key.length) {
			if (upd_acct <= 1){
				newKey += key[i - 1] + '-'
				upd_acct += 1
		  	}
			else
			{
				newKey += key[i - 1]
			}
		  console.log(newKey)
		  
		} else {
		  newKey += key[i - 1]
		  console.log(newKey)
		}
}
  return newKey;
}

SIDESOFTUTILS.isReturn = function (receipt) {
  var isReturn = true;
  receipt.get('lines').forEach(function (line) {
    isReturn = line.get('shipmentlineId') !== '' && line.get('shipmentlineId') !== undefined && isReturn
  });
  return (receipt.get('net') < 0 || receipt.get('gross') < 0) && isReturn;
}

SIDESOFTUTILS.generateCode = function (documentSequence, bp, identifier, inDevelopment, store, RUC) {
  var date = SIDESOFTUTILS.formatDate(new Date()); //Campo 1
  var identifier = identifier.substr(0, 2); //identifies Campo 2
  var rucNumber = SIDESOFTUTILS.zFill(RUC, 13) //Campo 3
  var tipoDeAmbiente = inDevelopment ? 1 : 2 //Campo 4
  var serie = SIDESOFTUTILS.zFill(parseInt(store), 6); //Campo 5
  var numComproSec = SIDESOFTUTILS.zFill(documentSequence, 9); //Campo 6
  var random8Digit = SIDESOFTUTILS.zFill(Math.floor(Math.random() * 90000000) + 10000000, 8); //Campo 7
  var tipoDeEmision = 1; //Campo 8
  var verificationCode = SIDESOFTUTILS.getVerificationCode(date + identifier + rucNumber + tipoDeAmbiente + serie + numComproSec + random8Digit + tipoDeEmision) //Campo 9

  if (verificationCode === -1) {
    return verificationCode;
  }

  return date + identifier + rucNumber + tipoDeAmbiente + serie + numComproSec + random8Digit + tipoDeEmision + verificationCode;
}

SIDESOFTUTILS.formatDate = function (d) {
  var month = String(d.getMonth() + 1);
  var day = String(d.getDate());
  var year = String(d.getFullYear());

  if (month.length < 2) month = '0' + month;
  if (day.length < 2) day = '0' + day;

  return day + month + year;
}

SIDESOFTUTILS.zFill = function (number, width) {
  var numberOutput = Math.abs(number); /* Valor absoluto del número */
  var length = number.toString().length; /* Largo del número */
  var zero = "0"; /* String de cero */

  if (width <= length) {
    if (number < 0) {
      return ("-" + numberOutput.toString());
    } else {
      return numberOutput.toString();
    }
  } else {
    if (number < 0) {
      return ("-" + (zero.repeat(width - length)) + numberOutput.toString());
    } else {
      return ((zero.repeat(width - length)) + numberOutput.toString());
    }
  }
}

SIDESOFTUTILS.getVerificationCode = function (key) {
  var mod11, result, total = 0;

  if (!/^\d{48}$/.test(key)) {
    return -1;
  }

  for (var i = key.length - 1, weight = 2; i >= 0; i--) {
    total = total + (parseInt(key.charAt(i)) * weight);
    if (weight == 7) {
      weight = 2;
    } else {
      weight++;
    }
  }
    mod11 = 11 - (total % 11);
    switch (mod11) {
      case 11:
        result = 0;
        break;
      case 10:
        result = 1;
        break;
      default:
        result = mod11;
        break;
    }
    return result;

}


var sidesoftErrorPopUp = function (args, c) {
  console.log('error')
  OB.MobileApp.view.$.containerWindow.getRoot().doShowPopup({
    popup: 'SIDESOFT.UI.ErrorSequence'
  });
  args.cancellation = true;
  OB.UTIL.HookManager.callbackExecutor(args, c);
}


var processedOrderPopup = function (args, c, orderPOSDocumentno) {
	  OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_Error'), OB.I18N.getLabel('SRDPO_ErrorOrderProcessed', [orderPOSDocumentno]));
	  OB.UTIL.showError('El pedido ' + orderPOSDocumentno + ' ya ha sido procesado.');
	  args.cancellation = true;
	  OB.MobileApp.model.hookManager.callbackExecutor(args, c);
	}
