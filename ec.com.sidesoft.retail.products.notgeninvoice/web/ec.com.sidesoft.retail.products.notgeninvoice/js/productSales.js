/***
 * Extender el modelo del Producto
 * **/
OB.Model.Product.addProperties([{
    name: 'isinvoice',
    column: 'isinvoice',
    type: 'TEXT'
  }]);


/***
 * DesHABILITA EL CHECK FACTURA, SI SE SELECCIONA UN PRODUCTO TIPO 'NO GENERAR FACTURA'
 ***/


OB.UTIL.HookManager.registerHook('OBPOS_AddProductToOrder', function (args, c) {
	
	var v_isGenerateInvoice = args.productToAdd.get('isinvoice');
	
	
	if (v_isGenerateInvoice==true){
		
		  args.receipt.attributes.generateInvoice=false;
		  
	      OB.MobileApp.view.terminal.attributes.generateInvoice = false;
	      
	      OB.MobileApp.model.receipt._previousAttributes.generateInvoice = false;
	}
 	OB.MobileApp.model.hookManager.callbackExecutor(args, c);
});


/***
 * HABILITA EL CHECK FACTURA, SI YA NO EXISTE PRODUCTOS CON EL CHECK 'NO GENERAR FACTURA'
 ***/


OB.UTIL.HookManager.registerHook('OBPOS_PostDeleteLine', function (args, c) {
	
	
	var ord = args.order;
	
	var linesProd = ord.get('lines');
	
	var numObjLines= linesProd.length;
	
	var v_isGenerateInvoice = false;
	
	var i=0;
	
	var countGenerateInvoice = 0;
	
	for (i =0; i<numObjLines ; i++){
		
		var prod = linesProd.get('product');
		
		var isGenInvoice =  prod.get('isinvoice');
		if (isGenInvoice==true){
			countGenerateInvoice++;
		}
		
	}
	
	
	if (countGenerateInvoice>0){
		
		  args.order.attributes.generateInvoice=false;
		  
	      OB.MobileApp.view.terminal.attributes.generateInvoice = false;
	      
	      OB.MobileApp.model.receipt._previousAttributes.generateInvoice = false;
	      
	   	OB.MobileApp.model.hookManager.callbackExecutor(args, c);

	}else{
		  
		  args.order.attributes.generateInvoice=true;
		  
	      OB.MobileApp.view.terminal.attributes.generateInvoice = true;
	      
	      OB.MobileApp.model.receipt._previousAttributes.generateInvoice = true;
	      
	   	OB.MobileApp.model.hookManager.callbackExecutor(args, c);


	}
	
	
 	OB.MobileApp.model.hookManager.callbackExecutor(args, c);
});


