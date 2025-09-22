OB.MobileApp.model.hookManager.registerHook('OBPOS_PreCustomerSave', function (args, c) {

//recoge el valor del campo TaxId nuevo
var cedula 		= args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.getValue();
//recoge el valor del campo TaxId de base si es que existe
var cedula_old	= args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.attributes.value;
//recoge el valor del campo TypetaxId
var tipo_id 	= args.meObject.$.customerAttributes.$.line_taxidtypecombo.$.newAttribute.$.taxidtypecombo.$.customerCombo.getValue();

// flag RUC
var flagvalidate=false;

if(cedula_old!="" || args.meObject.customer)
{
	var tipo_id_old	= args.meObject.customer.attributes.sswhTaxidtype;
}
//select
var sql='SELECT c_bpartner.taxID FROM c_bpartner WHERE c_bpartner.taxID=?';
//Validacion cambios de  identificador 
	//Si el identificador esta vacio
if(cedula_old==="" && args.meObject.customer===undefined)
{
	if(tipo_id==="D")
	{
		validaCedula(cedula);
	}
	else
	{
		if (tipo_id==="R") 
		{
			//validaRuc(cedula);	
			
			flagvalidate = validaRuc22(cedula);
			if (flagvalidate == false){
				OB.UTIL.showConfirmation.display('!! RUC incorrecto');
				 args.passValidation=false;
				 OB.MobileApp.model.hookManager.callbackExecutor(args, c);
			}else{
				//args.meObject.saveCustomer(args.inSender, args.inEvent);
			}
		}
		else 
		{
			if (tipo_id==="P") 
			{
				validaPasaporteExiste(cedula);
			}

			//pasaporte agrega el Customer
			/*if (args.passValidation) 
			{
				args.meObject.saveCustomer(args.inSender, args.inEvent);
			} 
			else 
			{
				OB.UTIL.showError(args.error);
			} */
		}
	}
	//Si el identificador esta vacio
}
else
{
	//Validacion si se cambia el tipo de identificador
	if(tipo_id != tipo_id_old)
	{
		if(tipo_id==="D")
		{
			validaCedula(cedula);
		}
		else
		{
			if (tipo_id==="R") 
			{
				//validaRuc(cedula);	
				
				flagvalidate = validaRuc22(cedula);
				if (flagvalidate == false){
					OB.UTIL.showConfirmation.display('!! RUC incorrecto');
					 args.passValidation=false;
					 OB.MobileApp.model.hookManager.callbackExecutor(args, c);
				}else{
					//args.meObject.saveCustomer(args.inSender, args.inEvent);
				}
			}
			else
			{
				if (tipo_id==="P") 
				{
					validaPasaporteExiste(cedula);
				}
				//pasaporte agrega el Customer
				/*if (args.passValidation) 
				{
					args.meObject.saveCustomer(args.inSender, args.inEvent);
				} 
				else 
				{
					OB.UTIL.showError(args.error);
				} */
			}
		}
	}
	else
	{
		//Validacion si se mantiene el tipo de identificador pero cambia el valor del identificador
		if(cedula_old != cedula)
		{
			if(tipo_id==="D")
			{
				validaCedula(cedula);
			}
			else
			{
				if (tipo_id==="R") 
				{
					//validaRuc(cedula);	
					
					flagvalidate = validaRuc22(cedula);
					if (flagvalidate == false){
						OB.UTIL.showConfirmation.display('!! RUC incorrecto');
						 args.passValidation=false;
						 OB.MobileApp.model.hookManager.callbackExecutor(args, c);
					}else{
					//args.meObject.saveCustomer(args.inSender, args.inEvent);
					}
				}
				else
				{

					if (tipo_id==="P") 
					{
						validaPasaporteExiste(cedula);
					}					
					//pasaporte agrega el Customer
					/*if (args.passValidation) 
					{
						args.meObject.saveCustomer(args.inSender, args.inEvent);
					} 
					else 
					{
						OB.UTIL.showError(args.error);
					} */
				}
			}
		}
		else
		{
			//No se realizo ningun cambio en el identificador
			if (args.passValidation) 
			{
				args.meObject.saveCustomer(args.inSender, args.inEvent);
			} 
			else 
			{
				OB.UTIL.showError(args.error);
			} 
		}
		//Validacion si se mantiene el tipo de identificador pero cambia el valor del identificador
	}
	//Validacion si se cambia el tipo de identificador
}
//Validacion cambios de  identificador 


function validaRuc(cedula){

 if (!/^([0-9])*$/.test(cedula) || cedula.length<13 || cedula.length>13){

     OB.UTIL.showConfirmation.display('!! RUC incorrecto');
	 args.passValidation=false;
	 OB.MobileApp.model.hookManager.callbackExecutor(args, c);
      

 }else{

	numero = cedula;
	var suma = 0;
	var residuo = 0;
	var pri = false;
	var pub = false;
	var nat = false;
	var modulo = 11;

	//Aqui almacenamos los digitos de la cedula en variables. 
	d1 = numero.substr(0,1);
	d2 = numero.substr(1,1);
	d3 = numero.substr(2,1);
	d4 = numero.substr(3,1);
	d5 = numero.substr(4,1);
	d6 = numero.substr(5,1);
	d7 = numero.substr(6,1);
	d8 = numero.substr(7,1);
	d9 = numero.substr(8,1);
	d10 = numero.substr(9,1);

	if (d3==7 || d3==8){

 		OB.UTIL.showConfirmation.display('!! RUC incorrecto');
		//return false;
 		args.passValidation=false;
 		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
	}

	//Solo para personas naturales (modulo 10) 
	if (d3 < 6){
		nat = true;
		p1 = d1 * 2; if (p1 >= 10) p1 -= 9;
		p2 = d2 * 1; if (p2 >= 10) p2 -= 9;
		p3 = d3 * 2; if (p3 >= 10) p3 -= 9;
		p4 = d4 * 1; if (p4 >= 10) p4 -= 9;
		p5 = d5 * 2; if (p5 >= 10) p5 -= 9;
		p6 = d6 * 1; if (p6 >= 10) p6 -= 9;
		p7 = d7 * 2; if (p7 >= 10) p7 -= 9;
		p8 = d8 * 1; if (p8 >= 10) p8 -= 9;
		p9 = d9 * 2; if (p9 >= 10) p9 -= 9;
		modulo = 10;

	}else if(d3 == 6){
		pub = true;
		p1 = d1 * 3;
		p2 = d2 * 2;
		p3 = d3 * 7;
		p4 = d4 * 6;
		p5 = d5 * 5;
		p6 = d6 * 4;
		p7 = d7 * 3;
		p8 = d8 * 2;
		p9 = 0;

	}else if(d3 == 9) { //Solo para entidades privadas (modulo 11) 
		pri = true;
		p1 = d1 * 4;
		p2 = d2 * 3;
		p3 = d3 * 2;
		p4 = d4 * 7;
		p5 = d5 * 6;
		p6 = d6 * 5;
		p7 = d7 * 4;
		p8 = d8 * 3;
		p9 = d9 * 2;
	}

	suma = p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
	residuo = suma % modulo;

	//Si residuo=0, dig.ver.=0, caso contrario 10 - residuo
	digitoVerificador = residuo==0 ? 0: modulo - residuo;

	//ahora comparamos el elemento de la posicion 10 con el dig. ver.
	if (pub==true){
		if (digitoVerificador != d9){

		 	OB.UTIL.showConfirmation.display('!! RUC incorrecto');
			//return false;
		 	args.passValidation=false;
     		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		}
		// El ruc de las empresas del sector publico terminan con 0001
		if ( numero.substr(9,4) != '0001' ){
	 		OB.UTIL.showConfirmation.display('!! RUC incorrecto');
			//return false;
	 		args.passValidation=false;
     		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		}

	}else if(pri == true){

		if (digitoVerificador != d10){
		 	OB.UTIL.showConfirmation.display('!! RUC incorrecto');
			//return false;
		 	args.passValidation=false;
     		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		}
		if ( numero.substr(10,3) != '001' ){
		 	OB.UTIL.showConfirmation.display('!! RUC incorrecto');
			//return false;
		 	args.passValidation=false;
     		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		}

	}else if(nat == true){

		if (digitoVerificador != d10){
			OB.UTIL.showConfirmation.display('!! RUC incorrecto');
			//return false;
			args.passValidation=false;
     		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		}
		if (numero.length >13 && numero.substr(10,3) != '001' ){
		 	OB.UTIL.showConfirmation.display('!! RUC incorrecto');
			//return false;
			args.passValidation=false;
     		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		}
	}

	if (OB.Data.localDB) 
	{
        // websql
        OB.Data.localDB.readTransaction(function (tx) 
		{
          tx.executeSql(sql, [cedula], function (tr, result) 
		  {
            if (result.rows.length >0) 
			{
			   args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.setValue(cedula);
               OB.UTIL.showConfirmation.display('!! El RUC introducido existe');
               args.passValidation=false;
	     	   OB.MobileApp.model.hookManager.callbackExecutor(args, c);
               
          	} 
			else 
			{
        		//cedula correcta agrega el Customer
				if (args.passValidation) 
				{
					args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.setValue([cedula]);
					args.meObject.saveCustomer(args.inSender, args.inEvent);
      			} 
				else 
				{
					OB.UTIL.showError(args.error);
      			} 
            }
          })
        });
  	}

 	}
};

function validaCedula(cedula){
	//Algoritmo de validacion de cedula  
	array = cedula.split( "" );
  	num = array.length;
  	if(cedula == "0000000000"){

		OB.UTIL.showConfirmation.display('!! Identificador incorrecto');
		args.passValidation=false;
 		OB.MobileApp.model.hookManager.callbackExecutor(args, c);  		

  	}else if ( num == 10 ){
	    total = 0;
	    digito = (array[9]*1);
    	for( i=0; i < (num-1); i++ ){
      		mult = 0;
      		if ( ( i%2 ) != 0 ) {

        		total = total + ( array[i] * 1 );
      		}else{
        		mult = array[i] * 2;
        		if ( mult > 9 ){

          			total = total + ( mult - 9 );

        		}else{

		          	total = total + mult;	
        		}
      		}
    	}
    	decena = total / 10;
    	decena = Math.floor( decena );
    	decena = ( decena + 1 ) * 10;
    	final = ( decena - total );
    	if ( ( final == 10 && digito == 0 ) || ( final == digito ) ) 
		{
    	
			if (OB.Data.localDB) 
			{
				// websql
				OB.Data.localDB.readTransaction(function (tx) 
				{
				  tx.executeSql(sql, [cedula], function (tr, result) 
				  {
					if (result.rows.length >0) 
					{
					   args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.setValue(cedula);
					   OB.UTIL.showConfirmation.display('!! El identificador introducido existe'); 
					   args.passValidation=false;
			     	   OB.MobileApp.model.hookManager.callbackExecutor(args, c);
					   //continue;
					} 
					else 
					{
						//cedula correcta agrega el Customer
						if (args.passValidation) 
						{
							args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.setValue([cedula]);
							args.meObject.saveCustomer(args.inSender, args.inEvent);
						} 
						else 
						{
							OB.UTIL.showError(args.error);
						} 
					}
				  })
				});
			}

    	}
		else
		{
			
			OB.UTIL.showConfirmation.display('!! Identificador incorrecto');
     		args.passValidation=false;
     		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
     		
		}
  	}
	else //Validacion 10 digitos
	{
	 
		OB.UTIL.showConfirmation.display('!! Identificador incorrecto');
		args.passValidation=false;
 		OB.MobileApp.model.hookManager.callbackExecutor(args, c);
  	}
};

function validaPasaporteExiste(cedula){
	//Algoritmo de validacion de cedula  
	array = cedula.split( "" );
  	num = array.length;

  	if(cedula == "0000000000"){

		OB.UTIL.showConfirmation.display('!! Identificador incorrecto');
		args.passValidation=false;
 		OB.MobileApp.model.hookManager.callbackExecutor(args, c);  		

  	}else {
		if (OB.Data.localDB) 
		{
			// websql
			OB.Data.localDB.readTransaction(function (tx) 
			{
			  tx.executeSql(sql, [cedula], function (tr, result) 
			  {
				if (result.rows.length >0) 
				{
				   args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.setValue(cedula);
				   OB.UTIL.showConfirmation.display('!! El identificador introducido existe'); 
				   args.passValidation=false;
		     	   OB.MobileApp.model.hookManager.callbackExecutor(args, c);
				   //continue;
				} 
				else 
				{
					//cedula correcta agrega el Customer
					if (args.passValidation) 
					{
						args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.setValue([cedula]);
						args.meObject.saveCustomer(args.inSender, args.inEvent);
					} 
					else 
					{
						OB.UTIL.showError(args.error);
					} 
				}
			  })
			});
		}
  	}
};
	
	
	
	function validaRuc22(cedula){

		 if (!/^([0-9])*$/.test(cedula) || cedula.length<13 || cedula.length>13){

			 return false;
		      

		 }else{

			numero = cedula;
			var suma = 0;
			var residuo = 0;
			var pri = false;
			var pub = false;
			var nat = false;
			var modulo = 11;

			//Aqui almacenamos los digitos de la cedula en variables. 
			d1 = numero.substr(0,1);
			d2 = numero.substr(1,1);
			d3 = numero.substr(2,1);
			d4 = numero.substr(3,1);
			d5 = numero.substr(4,1);
			d6 = numero.substr(5,1);
			d7 = numero.substr(6,1);
			d8 = numero.substr(7,1);
			d9 = numero.substr(8,1);
			d10 = numero.substr(9,1);

			if (d3==7 || d3==8){

		 		//OB.UTIL.showConfirmation.display('!! RUC incorrecto');
				return false;
		 		//args.passValidation=false;
		 		//OB.MobileApp.model.hookManager.callbackExecutor(args, c);
			}

			//Solo para personas naturales (modulo 10) 
			if (d3 < 6){
				nat = true;
				p1 = d1 * 2; if (p1 >= 10) p1 -= 9;
				p2 = d2 * 1; if (p2 >= 10) p2 -= 9;
				p3 = d3 * 2; if (p3 >= 10) p3 -= 9;
				p4 = d4 * 1; if (p4 >= 10) p4 -= 9;
				p5 = d5 * 2; if (p5 >= 10) p5 -= 9;
				p6 = d6 * 1; if (p6 >= 10) p6 -= 9;
				p7 = d7 * 2; if (p7 >= 10) p7 -= 9;
				p8 = d8 * 1; if (p8 >= 10) p8 -= 9;
				p9 = d9 * 2; if (p9 >= 10) p9 -= 9;
				modulo = 10;

			}else if(d3 == 6){
				pub = true;
				p1 = d1 * 3;
				p2 = d2 * 2;
				p3 = d3 * 7;
				p4 = d4 * 6;
				p5 = d5 * 5;
				p6 = d6 * 4;
				p7 = d7 * 3;
				p8 = d8 * 2;
				p9 = 0;

			}else if(d3 == 9) { //Solo para entidades privadas (modulo 11) 
				pri = true;
				p1 = d1 * 4;
				p2 = d2 * 3;
				p3 = d3 * 2;
				p4 = d4 * 7;
				p5 = d5 * 6;
				p6 = d6 * 5;
				p7 = d7 * 4;
				p8 = d8 * 3;
				p9 = d9 * 2;
			}

			suma = p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
			residuo = suma % modulo;

			//Si residuo=0, dig.ver.=0, caso contrario 10 - residuo
			digitoVerificador = residuo==0 ? 0: modulo - residuo;

			//ahora comparamos el elemento de la posicion 10 con el dig. ver.
			if (pub==true){
				if (digitoVerificador != d9){

					return false;
				}
				// El ruc de las empresas del sector publico terminan con 0001
				if ( numero.substr(9,4) != '0001' ){
					return false;
				}

			}else if(pri == true){

				if (digitoVerificador != d10){
					return false;
				}
				if ( numero.substr(10,3) != '001' ){
					return false;
				}

			}else if(nat == true){

				if (digitoVerificador != d10){
					//OB.UTIL.showConfirmation.display('!! RUC incorrecto');
					return false;
				}
				if (numero.substr(10,3) != '001' ){
					return false;
				}
			}

			if (OB.Data.localDB) 
			{
		        // websql
		        OB.Data.localDB.readTransaction(function (tx) 
				{
		          tx.executeSql(sql, [cedula], function (tr, result) 
				  {
		            if (result.rows.length >0) 
					{
					   args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.setValue(cedula);
					   OB.UTIL.showConfirmation.display('!! El identificador introducido existe');
					   args.passValidation=false;
					   OB.MobileApp.model.hookManager.callbackExecutor(args, c);
					   
					   
		          	} 
					else 
					{
		        		//cedula correcta agrega el Customer
						if (args.passValidation) 
						{
							args.meObject.$.customerAttributes.$.line_customerTaxId.$.newAttribute.$.customerTaxId.setValue([cedula]);
							//args.meObject.saveCustomer(args.inSender, args.inEvent);
							args.passValidation = true;
							OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		      			} 
						else 
						{
							OB.UTIL.showError(args.error);
		      			} 
		            }
		          })
		        });
		  	}

		 	}
//		 return true;
		};
	//args.passValidation = true;
	//OB.MobileApp.model.hookManager.callbackExecutor(args, c);

});
