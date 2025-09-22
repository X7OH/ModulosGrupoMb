/***
 * Extender el modelo de BusinessPartner
 * **/
OB.Model.BusinessPartner.addProperties([{
    name: 'isagreements',
    column: 'isagreements',
    type: 'TEXT'
  }]);

/***
 * Pop up del metodo de pago Convenio
 * **/


enyo.kind({
  name: 'SPAI_.UI.AgreementsInvoiceConnector',
  components: [{
    components: [{
      classes: 'row-fluid',
      components: [{
        classes: 'span6',
        content:  'Tipo de Pago' 
      }, {
        name: 'paymenttype',
        classes: 'span6',
        style: 'font-weight: bold;'
      }]
    }]
  }, {
		 kind:'OB.UI.ModalDialogButton',
	  	 name: 'OB.OBSSPOS.confirmReserve',
	  	 i18nContent: 'OBMOBC_LblOk',
	  	 isDefaultAction: true,
    ontap: 'confirmPayment'

  }],
  voidTransaction: function (callback, receipt, removedPayment) {
    // This function will be executed when the payment is removed using the payments UI
    // In this point we should communicate with servers to remove the payment
    // The callback function has the following signature:
    // callback(true, 'Error message'); // Cannot remove the payment, The error message in the parameter will be displayed to the user
    // callback(false); // The payment has been voided successfully and the payment will be removed.
  },
  updateVisibility: function (isVisible) {
	    if (!OB.MobileApp.model.hasPermission(this.permission)) {
	      this.hide();
	      return;
	    }
	    if (!isVisible) {
	      this.hide();
	      return;
	    }
	    this.show();
	  },
  confirmPayment: function () {
      //In this point we should retrieve the needed info to pay (credit card
      //and then connect with the payment provider.
      var me = this;
      var receipt =       OB.MobileApp.model.receipt;
      var unabledInvoice =false;
      var isAgreement = receipt.attributes.bp.attributes.isagreements;
      
      if (isAgreement){
      

      
      this.receipt.attributes.generateInvoice = false;
      OB.MobileApp.model.generateInvoice= false;
      
      receipt._callbacks['change:generateInvoice', function (model) {
          //if (!model.get('generateInvoice')) {
          this.updateVisibility(unabledInvoice);
        //} else {
        //  me.updateVisibility(false);
        //}
      }, this];
      
      OB.MobileApp.model.set('change:generateInvoice', function (model) {
          //if (!model.get('generateInvoice')) {
          this.updateVisibility(unabledInvoice);
        //} else {
        //  me.updateVisibility(false);
        //}
      }, this);
      
      OB.MobileApp.view.terminal.attributes.generateInvoice = false;
      
      
      //this.$.paymenttype.owner.changes.generateInvoice = false;
      
      OB.MobileApp.model.receipt._previousAttributes.generateInvoice = false;

      /***
       * Se crea la línea de pago
       * ***/
      var newPayment = new OB.Model.PaymentLine({
			'kind' : this.key,
			'name' : this.paymentType,
			'amount' : parseFloat(OB.MobileApp.model.receipt.attributes.gross),
			'paymentData' : null
		});

		this.receipt.addPayment(newPayment);
    

	  var ordView = OB.OBPOSPointOfSale.UI.ReceiptView;
	  
	  
	  
	  this.receipt.attributes.generateInvoice=false;

	  
      this.mainPopup.hide();
      
  }else{
      //OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SPAI_ErrorTablesResources'));
	  
	  this.mainPopup.hide();
	  
      OB.UTIL.showConfirmation.display('El tercero seleccionado no es de tipo Convenio');
  }

	  //OB.Model.Terminal.prototype.initialize.call(this);

  },
  initComponents: function() {
    this.inherited(arguments);
    this.$.paymenttype.setContent(this.paymentType);
    //this.$.paymentamount.setContent(this.paymentAmount);
    //this.$.configvalue.setContent(this.paymentMethod.spcoConfigfield);
  }
});

/***
 * HABILITAR EL CHECK FACTURA, SI SE ELIMINA EL METODO DE PAGO CONVENIO
 ***/


OB.UTIL.HookManager.registerHook('OBPOS_preRemovePayment', function (args, c) {
	
	var v_payment =args.paymentToRem.attributes.kind;
	
	var sample = this;
	
	if (v_payment=='SPAI_Payments.AgreementsInvoice'){
		
		  args.receipt.attributes.generateInvoice=true;
		  
	      OB.MobileApp.view.terminal.attributes.generateInvoice = true;
	      
	      OB.MobileApp.model.receipt._previousAttributes.generateInvoice = true;
	}
 	OB.MobileApp.model.hookManager.callbackExecutor(args, c);
});


/***
 *  CAMBIAR LA PLANTILLA DE IMPRESIÓN
 ***/

OB.UTIL.HookManager.registerHook('OBPRINT_PrePrint', function (args, c) {
	
	linesSlice = args.order.get('lines').models.slice();
	//console.log("OBJETO tEMPORAL**: ");
	//console.log(linesSlice);
	

	var userPOS = OB.POS.modelterminal.get('context').user._identifier;
	var orgPOS = OB.POS.modelterminal.get('context').organization.name;
	var orgPOSID = OB.POS.modelterminal.get('context').organization.id;
	var isDomicilie = OB.POS.modelterminal.get('terminal').isdomicilie; 
	
	
	var strDescription= "";
	var strSubstractDescription= "";
    strDescription  = args.order.get('description');
  

    if(strDescription.length>0){
    	  strDescription  = OB.MobileApp.model.receipt.get('descriptionnew');
		
		    //strDescription = strDescription.replace("\n","****");   
		    var caracteresDescripcion = strDescription.split("*PAG");
		    
		    console.log("NUEVO****");
		    
		    var intCountDescription= caracteresDescripcion.length;
		    var newDescription = [];
		    var newDescriptionFinal = [];
		    var strInicioTexto = 0;
		    var contarLetra=0;
		    var contarSaltos=0;
		    var AcumLetras=0;
		
		    if(intCountDescription>0){
		        //console.log(caracteresDescripcion[0]);
		    	caracteresDescripcion = caracteresDescripcion[0].split("\n");
		    	//console.log(caracteresDescripcion);
		    }else{
		    	
		    	caracteresDescripcion = strDescription.split("\n");
		    }
		    console.log(caracteresDescripcion);

    }
	
	var jsonObjecUser = 
    {
     user:userPOS,
     org:orgPOS,
     descriptionorder:caracteresDescripcion,
     isDomicilie
    }
	
	var printType = args.order.attributes.bp.changed.isagreements;

	
	if (!printType){
		
	
		var Args =args;
		var templateBP = '../ec.com.sidesoft.retail.agreementinvoices/res/printReceipt.xml';
		var resourceBP = new OB.DS.HWResource(templateBP);
		resourceBP.resourcedata = args.order.changed.bp;
		args.forcePrint = true;

		args.template =resourceBP;
		args.forcedtemplate =resourceBP; 
		
		//args.callback = c;
		lines = args.order.get('lines').models.slice();
		for (var i = 0; i < lines.length; i++) {
			line = lines[i];
	        console.log("Lineas Temporales: "+ i);
	       console.log(line);
			
		}

	    OB.POS.hwserver.print(new OB.DS.HWResource(templateBP), {bp: args.order.changed.bp, order: args.order, user: jsonObjecUser, linesComanda: linesSlice  });
	    
		
	    
	    
	}else{
		
		var Args =args;
		var templateBP = '../ec.com.sidesoft.retail.agreementinvoices/res/printbp.xml';
		var resourceBP = new OB.DS.HWResource(templateBP);
		resourceBP.resourcedata = args.order.changed.bp;
		args.forcePrint = true;

		args.template =resourceBP;
		args.forcedtemplate =resourceBP; 
		
		//args.callback = c;

	    OB.POS.hwserver.print(new OB.DS.HWResource(templateBP), {bp: args.order.changed.bp, order: args.order, user: jsonObjecUser, linesComanda: linesSlice   });
	    
		
	}
	
	//OB.MobileApp.model.hookManager.callbackExecutor(args, c);
});







//Generate the buttons

enyo.kind({
	name:'OB.OBPOSPointOfSale.UI.ButtonTabTest',
	kind: 'OB.UI.ToolbarButtonTab',
	tabPanel: 'orderDescription',
	i18nLabel: 'SPAI_DESCRIPTION_ORDER',
	tap: function(args,c){
		
		OB.MobileApp.view.$.containerWindow.showPopup('SPAI_ModalSetInfoOrder',{
			    //Parameters
			    // Receipt model is send to the popup
			    receipt: args.receipt,
			    productToAdd: args.productToAdd,

			    // Callback will be invoked by the popup when all is ready.
			    // passing it through arguments
			    callback: function (cancel) {

				if (cancel) {
				
				    
			        args.cancelOperation = true;
				
					OB.MobileApp.model.hookManager.callbackExecutor(args, c);

				} else {
					
					args.cancelOperation = false;
					
					OB.MobileApp.model.hookManager.callbackExecutor(args, c);
					


					
				
			  		}
			    } 
			});
			
	},
	init: function (model){
		this.model = model;
	}
});


//Extend RightToolbarImpl
OB.OBPOSPointOfSale.UI.RightToolbarImpl.prototype.buttons.push({
		kind: 'OB.OBPOSPointOfSale.UI.ButtonTabTest',
		name:  'orderDescription'
});

//Modal Pop Up
enyo.kind({ 

	kind:'OB.UI.ModalAction',
	autoDismiss:false,
	name: 'OB.SPAI.UI.ModalSetInfoOrder' ,
	i18nHeader: 'SPAI_HeaderDescription',
	
	bodyContent: { 
		components:[ {
			 kind:'enyo.TextArea',
			 type:'TextArea',
			 attributes: {
				 maxlength:200000
			 },
			 style: 'width:450px; height: 180px; word-wrap: break-word;',
			 name: 'descripcionorder',
			 selectOnFocus: true,
			 isFirstFocus: true,
			 value:''
		     ,handlers: {
				        onLoadValue: 'loadValue',
				        onkeydown: 'keydownHandler'
		    },
		    events: {
		        onSetProperty: '',
		        onSetLineProperty: ''
		    },
		    loadValue: function(inSender, inEvent) {
		        if (this.modelProperty === inEvent.modelProperty) {
		            if (inEvent.model && inEvent.model.get(this.modelProperty)) {
		                this.setValue(inEvent.model.get(this.modelProperty));
		            } else {
		                this.setValue('');
		            }
		        }
		    }
			,keydownHandler: function(inSender, inEvent) {
		        if (inEvent.keyCode === 13) {
		            return true;
		        }
		    }					 
		 },{
				name:'warningLabel',
				style:'margin:15px; color: white; font-weight: bold;',
				//content:'',
				showing: true
				
			},
		 {
			 kind:'enyo.TextArea',
			 type:'TextArea',
			 attributes: {
				 maxlength:200000,
				 readOnly: true
			 },
			 style: 'width:450px; height: 180px; word-wrap: break-word;',
			 name: 'orderdetail',
			 selectOnFocus: false,
			 value:'',
			 /*initComponents: function () {
		    	    if (this.readOnly) {
		    	      this.setDisabled(true);
		    	      this.setAttribute('readonly', 'readonly');
		    	    }
		    	  }*/
			 handlers: {
			        onLoadValue: 'loadValue',
			        onkeydown: 'keydownHandler'
		     },
		     loadValue: function(inSender, inEvent) {
		        if (this.modelProperty === inEvent.modelProperty) {
		            if (inEvent.model && inEvent.model.get(this.modelProperty)) {
		                this.setValue(inEvent.model.get(this.modelProperty));
		            } else {
		                this.setValue('');
		            }
		        }
		     }
			 ,keydownHandler: function(inSender, inEvent) {
		        if (inEvent.keyCode === 13) {
		            return true;
		        }
		     }
		 }]
	},
saveDescription: function(inSender, inEvent){
 
 var me = this;
 var strDescription = this.$.bodyContent.$.descripcionorder.getValue();
 if(strDescription !== null){
	 
	 
	 
	 //var strDescription= "";
		var strSubstractDescription= "";
	    //strDescription  = args.order.get('description');

	    //strDescription = strDescription.replace("\n","****");   
	    //var caracteresDescripcion = strDescription.split("*PAG");
		var caracteresDescripcion = strDescription;
	    
	    //console.log(caracteresDescripcion.length);
	    
	    /*var intCountDescription= caracteresDescripcion.length;
	    var newDescription = [];
	    var newDescriptionFinal = [];
	    var strInicioTexto = 0;
	    var contarLetra=0;
	    var contarSaltos=0;
	    var AcumLetras=0;

	    if(intCountDescription>0){
	        //console.log(caracteresDescripcion[0]);
	    	caracteresDescripcion = caracteresDescripcion[0];
	    	//console.log(caracteresDescripcion);
	    }else{
	    	caracteresDescripcion = strDescription;
	    }*/
	    
	    
	//this.args.receipt.set('description', strDescription);
	//OB.MobileApp.model.receipt.set('description',caracteresDescripcion);
	
	OB.MobileApp.model.receipt.set('descriptionnew',caracteresDescripcion);
	
	OB.MobileApp.model.receipt.set('description', caracteresDescripcion.substr(0,200));
	console.log(OB.MobileApp.model.receipt.get('descriptionnew'));
	// this.orderdescription = true;
	 this.hide();
	 
 }else{
	 this.hide();
 }
 return true;
},
cancel: function(){
 this.hide();
 
},

bodyButtons:{
 components:[{
	 kind:'OB.UI.ModalDialogButton',
	 name: 'OB.OBCOMTR.confirmGuestQty',
	 i18nContent: 'OBMOBC_LblOk',
	 isDefaultAction: true,
	 ontap:'saveDescription'
 },{
	 kind:'OB.UI.ModalDialogButton',
	 name: 'OB.OBCOMTR.cancelGuestQty',
	 i18nContent: 'OBMOBC_LblCancel',
	 ontap:'cancel'
 }]
},
/*executeBeforeHide: function(){
	
	console.log("descripcion orden: ");
	
	console.log(OB.MobileApp.model.receipt.get('description'));
	
		this.$.bodyContent.$.descripcionorder='Sample'; 
 var me = this;
 
 var strDescription= "";
 strDescription  = OB.MobileApp.model.receipt.get('description');
 
 this.$.bodyContent.$.control.components["0"].value = strDescription;

 //this.orderdescription =  OB.MobileApp.model.receipt.get('description');
// this.$.bodyContent.$.descripcionorder.setValue(strDescription);
 
 if (strDescription!==null && strDescription!==""){
		 //this.args.callback();
  		 
  		 return true;
  		this.hide();
 }else{

	 return false;
 }
 
 return true;
},*/

executeOnShow: function(){
 var me = this;

 this.$.bodyButtons.saveDescription = function(inSender, inEvent){
	 me.saveDescription(inSender, inEvent);
 };
 this.$.bodyButtons.cancel = function(){
	 me.cancel();		 
 };

 //this.args.receipt.unset('OBCOMTR_guestAmount');
 this.orderdescription = null;
 //this.$.bodyContent.$.descripcionorder = ( OB.MobileApp.model.receipt.get('description'));
 //this.$.bodyContent.$.control.components["0"].value = "Sa,ple";
 
 if(OB.MobileApp.model.receipt.get('descriptionnew')){ 
	 
	 this.$.bodyContent.$.descripcionorder.setValue(OB.MobileApp.model.receipt.get('descriptionnew'));

 }else{
 this.$.bodyContent.$.descripcionorder.setValue(OB.MobileApp.model.receipt.get('description'));
 }
 
 
 var lines = [];      
 var  line, promotions, auxLines;
 var finalLines = [];
 
 //orderdetail
 lines = OB.MobileApp.model.receipt.get('lines').models;
 
 console.log("******* Lineas  *****");
 console.log(lines);
 
 
 var strLinedescription='';
 var comboID='';
 var comboOldID='1';
if(lines.length>0){ 
 for (var i = 0; i < lines.length; i++)
 {
  
    //line = lines.at(i);
     line = lines[i];
     
     try{
        comboID = line.get('comboId');
     }catch(err){
     }
     

    	 try{
    	     if (comboID.length==0){
    	    	 comboID=line[i].attributes.promotions["0"].ruleId;
    	     }
    	    	 

    	 }catch(err){
    	 }

     
     if ( comboID != comboOldID){
         comboOldID = comboID ;
         strLinedescription =  strLinedescription + "*************************************" + "\n";
     }
     
     if(line.get('qty')){
	     strLinedescription =  strLinedescription + line.printQty() + '    ' +line.get('product').get('_identifier') +"\n";
	     
	     if (line.get('description')){
	    	 
	    	 var strDescripcion = line.get('description');
	    	 var countdescripcion = strDescripcion.length;
	    	 if (countdescripcion>0){
	    		 strLinedescription =  strLinedescription + '    <<' + line.get('description')+ '>>' +"\n";
	    		 
	    	 }
	     }
     }
     
 }
 
 this.$.bodyContent.$.orderdetail.setValue(strLinedescription);
 
}

 
 this.$.bodyContent.$.warningLabel.setContent(OB.I18N.getLabel('SPAI_ConfirmReceipt'));
}
});

OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale',{
kind: 'OB.SPAI.UI.ModalSetInfoOrder',
name: 'SPAI_ModalSetInfoOrder'
});

/***
 *  VALIDAR tERCERO POR DEFAULT
 ***/
OB.UTIL.HookManager.registerHook('OBPOS_BeforeCustomerSave', function (customer, c) {
	

	/*
	 * OBPOS_BeforeCustomerSave
		Available from: RR17Q1
		Executed when: Before the actual saving process occurs.
		Arguments:
		customer: The customer to be saved.
		isNew: This flag indicate if new customer or existing one.
		validations: Boolean value.
		Arguments for callback:
		cancellation: If this property is added to the arguments and the value of this property is true, the customer save won't be done.
	 * 
	 * */
	

	var terminalID = OB.POS.modelterminal.get('context').organization.id;
	var bpdefault = customer;
	var bpID = bpdefault.customer.id;
	var bpIsNew= bpdefault.isNew;
	if (bpIsNew){
		
		bpdefault.cancellation = false;
		OB.MobileApp.model.hookManager.callbackExecutor(bpdefault, c);
		
	}else{ 

	
	 var sqlRev= "select idcustomer from OrganizationSales where id ='"+ terminalID + "' AND idcustomer ='" + bpID + "'";;
	   
	   OB.Data.localDB.readTransaction(function (tx) {
			  
			tx.executeSql(sqlRev, null, function (tr, result) {
	 
				if (result.rows.length ==0){

					//args.cancelOperation = true;
					//OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SLRRT_ErrorTablesResources'));
					//OB.UTIL.showError("No hay mesas disponibles");
					//OB.MobileApp.model.hookManager.callbackExecutor(args, c);
					
					bpdefault.cancellation = false;
					OB.MobileApp.model.hookManager.callbackExecutor(bpdefault, c);
				}else{
					
					var objTables = result.rows;
					var updTableID='';
					var flag ='1';

					
					for (var i = 0;i< objTables.length;i++){
					
						if(objTables[i].idcustomer == bpID){
							flag ='0';
						}
					}
					
					if (flag=='0'){
						bpdefault.cancellation = true;
					 OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SPAI_CustomerNotAllowedHeader'), OB.I18N.getLabel('SPAI_ErrorCustomer'), [{
			                label: OB.I18N.getLabel('OBMOBC_LblOk'),
			                action: function() {
			                	OB.MobileApp.model.hookManager.callbackExecutor(bpdefault, c);
			                }
			            }], {
			                onHideFunction: function() {
			                }
			            });
			            
					}

				}
			})
		});
}

});

/***
 *  VALIDAR DIRECCION POR DEFAULT
 ***/
OB.UTIL.HookManager.registerHook('OBPOS_BeforeCustomerAddrSave', function (customerAddr, c) {
	

	/*
	 *OBPOS_BeforeCustomerAddrSave
		Available from: RR17Q1
		Executed when: Before the actual saving process occurs.
		Arguments:
		customerAddr: The customer address to be saved.
		isNew: This flag indicate if new customer address or existing one.
		Arguments for callback:
		cancellation: If this property is added to the arguments and the value of this property is true, the customer address save won't be done.
	 * 
	 * */
	

	var terminalID = OB.POS.modelterminal.get('context').organization.id;
	var bpdefault = customerAddr;
	var bpID = bpdefault.customerAddr.id;
	var bpaddrIsNew= bpdefault.isNew;

	var bpartnerID= bpdefault.customerAddr.attributes.bpartner;
	var sqlRev= "select idlocation,idcustomer from OrganizationSales where id ='"+ terminalID + "'";;
	   
	   OB.Data.localDB.readTransaction(function (tx) {
			  
			tx.executeSql(sqlRev, null, function (tr, result) {
	 
				if (result.rows.length ==0){

					//args.cancelOperation = true;
					//OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SLRRT_ErrorTablesResources'));
					//OB.UTIL.showError("No hay mesas disponibles");
					//OB.MobileApp.model.hookManager.callbackExecutor(args, c);
					
					bpdefault.cancellation = false;
					OB.MobileApp.model.hookManager.callbackExecutor(bpdefault, c);
				}else{
					
					var objTables = result.rows;
					var updTableID='';
					var flag ='1';
					var flag2 ='1';

					
					for (var i = 0;i< objTables.length;i++){
					
						if(objTables[i].idlocation == bpID){
							flag ='0';
						}
					}
					
					for (var i = 0;i< objTables.length;i++){
						
						if(objTables[i].idcustomer == bpartnerID){
							flag2 ='0';
						}
					}
					
					if(flag2 =='0'){
					
						bpdefault.cancellation = true;
						 OB.UTIL.showConfirmation.display(OB.I18N.getLabel('SPAI_CustomerAddrNotAllowedHeader'), OB.I18N.getLabel('SPAI_CustomerAddr'), [{
				                label: OB.I18N.getLabel('OBMOBC_LblOk'),
				                action: function() {
				                	OB.MobileApp.model.hookManager.callbackExecutor(bpdefault, c);
				                }
				            }], {
				                onHideFunction: function() {
				                }
				            });
					 
					}else{
						if(bpaddrIsNew){
							
							bpdefault.cancellation = false;
							OB.MobileApp.model.hookManager.callbackExecutor(bpdefault, c);
							
						} 
					}

				}
			})
		});
	   



	   
});

/***
 *  VALIDAR DESCRIPCION 200 DE LONGITUD
 ***/
OB.UTIL.HookManager.registerHook('OBPOS_PreOrderSave', function (args, c) {
	

	var strDescription = OB.MobileApp.model.receipt.get('description');
	 if(strDescription !== null){
		 
		OB.MobileApp.model.receipt.set('descriptionnew',strDescription);
		
		OB.MobileApp.model.receipt.set('description', strDescription.substr(0,200));
		console.log(OB.MobileApp.model.receipt.get('descriptionnew'));
		// this.orderdescription = true;
		 
	 }
	 args.cancellation = false;
		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
	   
});

/***
 *  VALIDAR MÉTODO DE PAGO CONVENIO
 ***/
OB.UTIL.HookManager.registerHook('OBPOS_PaymentSelected', function (args, c) {
	
	var argspm = args;
try{	
	if (argspm.paymentSelected.paymentMethod){
	var methosPayment = argspm.paymentSelected.paymentMethod.paymentProvider;
	var isAgreement =	argspm.order.attributes.bp.attributes.isagreements;

    if( methosPayment!='SPAI_.UI.AgreementsInvoiceConnector' && isAgreement){
    	
    	console.log(methosPayment);
    	console.log(isAgreement);

   	 args.cancellation = true;
			 OB.UTIL.showConfirmation.display(OB.I18N.getLabel('Spai_ErrorPaymentMethod'), OB.I18N.getLabel('Spai_ErrorAgreements'), [{
	                label: OB.I18N.getLabel('OBMOBC_LblOk'),
	                action: function() {
	                	OB.MobileApp.model.hookManager.callbackExecutor(args, c);
	                }
	            }], {
	                onHideFunction: function() {
	                }
	            });
    }
	}
	
}catch(err){
}

	
	
	   
});

/***
 *  VALIDAR MÉTODO DE PAGO CONVENIO
 ***/
OB.UTIL.HookManager.registerHook('OBPOS_preAddPayment', function (args, c) {
	
	var argspm = args;
	var isAgreement = argspm.receipt.attributes.bp.attributes.isagreements;
	
	if (isAgreement){

				try{	
					
			
						var methosPayment =  argspm.paymentToAdd.attributes.kind;
			
				    if( methosPayment!='SPAI_Payments.AgreementsInvoice' && isAgreement){
				    	args.cancellation = true;
				   	
							 OB.UTIL.showConfirmation.display(OB.I18N.getLabel('Spai_ErrorPaymentMethod'), OB.I18N.getLabel('Spai_ErrorAgreements'), [{
					                label: OB.I18N.getLabel('OBMOBC_LblOk'),
					                action: function() {
					                	OB.MobileApp.model.hookManager.callbackExecutor(args, c);
					                }
					            }], {
					                onHideFunction: function() {
					                }
					            });
				    }else{
						args.cancellation = false;
						OB.MobileApp.model.hookManager.callbackExecutor(args, c);
					}	
			
					
				}catch(err){
				}
	}else{
		// args.cancellation = false;
		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		
	}
	   
});

/***
 *  VALIDAR DEVOLUCION CON MÉTODO DE PAGO CONVENIO
 ***/
OB.UTIL.HookManager.registerHook('OBPOS_PostPaymentDone', function (args, c) {
	
	var isAgreement = args.receipt.attributes.bp.attributes.isagreements;
	
	if(args.receipt.attributes.approvals.length > 0){

		if (isAgreement){
			args.receipt.attributes.generateInvoice = false;
			args.cancellation = false;
			OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		}else{
			args.cancellation = false;
			OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		}		

	}else{
		args.cancellation = false;
		OB.MobileApp.model.hookManager.callbackExecutor(args, c);		
	}

});


