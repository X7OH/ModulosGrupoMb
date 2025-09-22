/* global enyo, $
 * Credit Card
 * No. tarjeta, 
 * nombre y 
 * fecha caducidad
 */

enyo.kind({
	events: {
	    onHideThisPopup: ''
	},
	name : 'SSPOS_.UI.ConnectorCC',
	components : [ {
		components : [ {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Payment type'
			}, {
				name : 'paymenttype',
				classes : 'span6',
				style : 'font-weight: bold;'
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'No. de Tarjeta'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'creditCard',
				modelProperty : 'creditcard',
				style : 'width: 40%; height: 20px; margin:0;',
				onkeypress : 'handleKeyPress',
				onkeydown : 'handleKeyDown'
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Titular de la cuenta'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'principalowner',
				modelProperty : 'principalowner',
				style : 'width: 40%; height: 20px; margin:0;'
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Fecha de Expiracion'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'expirationdate',
				modelProperty : 'expirationdate',
				style : 'width: 40%; height: 20px; margin:0;'
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Monto a descontar:'
			}, {
				name : 'paymentamount',
				classes : 'span6',
				style : 'font-weight: bold;'
			} ]
		} ]
	}, {
		kind : 'OB.UI.Button',
		classes : 'btnlink',
		style : 'float: right;',
		content : 'OK',
		ontap : 'confirmPayment'
	} ],
	confirmPayment : function() {
       //Se crea objeto datosCreditcar con la informacion de la tarjeta de Credito--(hcurbelo).
                       var datosCreditcar = {
			POwner : this.$.principalowner.getValue(),
			ExpDate : this.$.expirationdate.getValue(),
			TransactionID : this.$.creditCard.getValue()
			 }
     //Se valida que el objeto datosCreditcar no este vacio al hacer la transaccion--(hcurbelo).
                if(datosCreditcar.TransactionID!="" && datosCreditcar.POwner!="" && datosCreditcar.ExpDate!="" ){
		var paymentresult = {
			POwner : this.$.principalowner.getValue(),
			ExpDate : this.$.expirationdate.getValue(),
			TransactionID : this.$.creditCard.getValue(),
			ApprovedAmount : this.paymentAmount,
			CardNumberMasked : this.$.creditCard.getValue(),
		// CardLogo: data.Card.CardLogo
		// Void transaction will be excuted when the payment line is removed.
		// VERY IMPORTANT TO REMOVE PAYMENTS
			 voidTransaction: function (callback) {
				 callback(false, 'Pago removido');
			 }

		};
		var newPayment = new OB.Model.PaymentLine({
			'kind' : this.key,
			'name' : this.paymentType,
			'amount' : parseFloat(paymentresult.ApprovedAmount),
			'paymentData' : paymentresult
		});

		this.receipt.addPayment(newPayment);
		// close the popup
		this.doHideThisPopup();
      //Si el objeto datosCreditcar esta vacio retorna un error informativo--(hcurbelo).
	     }else{
	    alert('ERROR: Campos Vacios');
          }
	},
	initComponents : function() {
		this.inherited(arguments);
		var me = this;
		window.setTimeout(function () {
			me.$.creditCard.focus();
		}, 1000);

    	this._carddata = '';
		this.$.paymenttype.setContent(this.paymentType);
		this.$.paymentamount.setContent(this.paymentAmount);
		// this.$.configvalue.setContent(this.paymentMethod.cuwpmConfigfield);
	},
	handleKeyDown : function(inSender, inEvent) {
		return true;
	},

	handleKeyPress : function(inSender, inEvent) {
		if (inEvent) {
			if (inEvent.charCode === 13) {
				var dump=this.processPayment(this.$.creditCard.getValue());
				this._carddata = '';
			} else {
			}
		}
		return true;
	},
	parseCardData: function (strParse){
		// member variables 
		this.input_trackdata_str = strParse;
		this.account_name = null;
		this.surname = null;
		this.firstname = null;
		this.acccount = null;
		this.exp_month = null;
		this.exp_year = null;
		this.track1 = null;
		this.track2 = null;
		this.hasTrack1 = false;
		this.hasTrack2 = false;
		
		sTrackData = this.input_trackdata_str;     //--- Get the track data
		
	  if ( strParse != '' )
	  {
	    //--- Determine the presence of special characters
	    nHasTrack1 = strParse.indexOf("&");
	    nHasTrack2 = strParse.indexOf("¿");

	    //--- Set boolean values based off of character presence
	    this.hasTrack1 = bHasTrack1 = false;
	    this.hasTrack2 = bHasTrack2 = false;
	    if (nHasTrack1 > 0) { this.hasTrack1 = bHasTrack1 = true; }
	    if (nHasTrack2 > 0) { this.hasTrack2 = bHasTrack2 = true; }  

	    //--- Initialize
	    bTrack1_2  = false;
	    bTrack1    = false;
	    bTrack2    = false;

	    //--- Determine tracks present
	    if (( bHasTrack1) && ( bHasTrack2)) { bTrack1_2 = true; }
	    if (( bHasTrack1) && (!bHasTrack2)) { bTrack1   = true; }
	    if ((!bHasTrack1) && ( bHasTrack2)) { bTrack2   = true; }

	    //--- Initialize alert message on error
	    bShowAlert = false;
	    if (bTrack1_2)
	    { 
	      strCutUpSwipe = '' + strParse + ' ';
	      arrayStrSwipe = new Array(4);
	      arrayStrSwipe = strCutUpSwipe.split("&");
	  
	      var sAccountNumber, sName, sShipToName, sMonth, sYear;
	  
	      if ( arrayStrSwipe.length > 2 )
	      {
	        this.account = stripAlpha( arrayStrSwipe[0].substring(1,arrayStrSwipe[0].length) );
	        this.account_name          = arrayStrSwipe[1];
	        this.exp_month         = arrayStrSwipe[2].substring(2,4);
	        this.exp_year          = '20' + arrayStrSwipe[2].substring(0,2); 
	        
	        //--- Different card swipe readers include or exclude the % in the front of the track data - when it's there, there are
	        //---   problems with parsing on the part of credit cards processor - so strip it off
	        if ( sTrackData.substring(0,1) == '%' ) {
	        	sTrackData = sTrackData.substring(1,sTrackData.length);
	        }

	    	var track2sentinel = strParse.indexOf("Ñ");
	    	if(track2sentinel==-1){
	    		track2sentinel = strParse.indexOf("ñ");
	        }
	       	if( track2sentinel != -1 ){
	       		this.track1 = sTrackData.substring(0, track2sentinel);
	       		this.track2 = sTrackData.substring(track2sentinel);
	       	}

			//--- parse name field into first/last names
			var nameDelim = this.account_name.indexOf("/");
			if( nameDelim != -1 ){
				this.surname = this.account_name.substring(0, nameDelim);
				this.firstname = this.account_name.substring(nameDelim+1);
			}
	      }
	      else  //--- for "if ( arrayStrSwipe.length > 2 )"
	      { 
	        bShowAlert = true;  //--- Error -- show alert message
	      }
	    }

	    if (bTrack1)
	    {
	      strCutUpSwipe = '' + strParse + ' ';
	      arrayStrSwipe = new Array(4);
	      arrayStrSwipe = strCutUpSwipe.split("&");
	  
	      var sAccountNumber, sName, sShipToName, sMonth, sYear;
	  
	      if ( arrayStrSwipe.length > 2 )
	      {
	        this.account = sAccountNumber = stripAlpha( arrayStrSwipe[0].substring( 1,arrayStrSwipe[0].length) );
	        this.account_name = sName	= arrayStrSwipe[1];
	        this.exp_month = sMonth	= arrayStrSwipe[2].substring(2,4);
	        this.exp_year = sYear	= '20' + arrayStrSwipe[2].substring(0,2); 
	  
	        if ( sTrackData.substring(0,1) == '%' ) { 
	        	this.track1 = sTrackData = sTrackData.substring(1,sTrackData.length);
	        }

			this.track2 = ';' + sAccountNumber + '=' + sYear.substring(2,4) + sMonth + '111111111111?';
	        sTrackData = sTrackData + this.track2;

			var nameDelim = this.account_name.indexOf("/");
			if( nameDelim != -1 ){
				this.surname = this.account_name.substring(0, nameDelim);
				this.firstname = this.account_name.substring(nameDelim+1);
			}

	      }
	      else  //--- for "if ( arrayStrSwipe.length > 2 )"
	      { 
	        bShowAlert = true;  //--- Error -- show alert message
	      }
	    }
 
	    if (bTrack2)
	    {

	      nSeperator  = strParse.indexOf("=");
	      sCardNumber = strParse.substring(1,nSeperator);
	      sYear       = strParse.substr(nSeperator+1,2);
	      sMonth      = strParse.substr(nSeperator+3,2);

	      this.account = sAccountNumber = stripAlpha(sCardNumber);
	      this.exp_month = sMonth		= sMonth;
	      this.exp_year = sYear			= '20' + sYear; 

	      if ( sTrackData.substring(0,1) == '%' ) {
			sTrackData = sTrackData.substring(1,sTrackData.length);
	      }
	  
	    }

	    if (((!bTrack1_2) && (!bTrack1) && (!bTrack2)) || (bShowAlert))
	    {
	      //alert('Difficulty Reading Card Information.\n\nPlease Swipe Card Again.');
	    }

	  } //--- end "if ( strParse != '' )"

		var dump = {
	              Name: this.account_name,
	              ApprovalNumber: this.account,
	              Exp_month: this.exp_month,
	              Exp_year: this.exp_year
		}

		function stripAlpha(sInput){
			if( sInput == "" )	return '';
			return sInput.replace(/[^0-9]/g, '');
		}
		return dump;

	},
	processPayment: function (xmlcarddata) {
	    var usedTransaction;
	    var me = this;
	    var document, card;

	    try {
	      var carddata = this.parseCardData(xmlcarddata);
	      var xmltrack;
    	  var diasMes=new Array(31,28,31,30,31,30,31,31,30,31,30,31);
    	  var fecha=new Date();
    	  var diames=fecha.getDate();
    	  var diasemana=fecha.getDay();
    	  var mes=fecha.getMonth() + 1 ;
    	  var ano=fecha.getFullYear();
    	  if(ano%4 == 0){
    		  diasMes[1]=29;
    	  }
	      if (carddata.Exp_year == ano) {
	        if(carddata.Exp_month <= mes){
		    	  alert('Tarjeta valida');
					this.$.principalowner.setContent(carddata.Name);
					this.$.expirationdate.setContent(carddata.Exp_year+'-'+carddata.Exp_month);
					this.$.creditCard.setValue(carddata.ApprovalNumber);
	        }else{
	        	alert('Tarjeta caducada');
	        	this.$.creditCard.setContent('');
	        }
	      }else if(carddata.Exp_year > ano){
	    	  alert('Tarjeta valida');
				this.$.principalowner.setValue(carddata.Name);
				this.$.expirationdate.setValue(carddata.Exp_year+'-'+carddata.Exp_month);
				this.$.creditCard.setValue(carddata.ApprovalNumber);
	      } else {
	    	  alert('Tarjeta caducada');
	    	  this.$.creditCard.setValue('');
	      } 
	    } catch (e) {
	    }
	  }
});


/*global enyo, $ 
/* Forma de cheque se debe requerir:  
 *  No. de cta,
 *  no. de cheque, 
 *  titular de las cuentas, 
 *  No. de telefono, 
 *  Banco del cheque.
*/
enyo.kind({
	events: {
	    onHideThisPopup: ''
	},
	name : 'SSPOS_.UI.ConnectorCheque',
	components : [ {
		components : [ {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Payment type'
			}, {
				name : 'paymenttype',
				classes : 'span6',
				style : 'font-weight: bold;'
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'No. de Cuenta'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'noaccount',
				modelProperty : 'noaccount',
				style : 'width: 40%; height: 20px; margin:0;',
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'No. de Cheque'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'nocheck',
				modelProperty : 'nocheck',
				style : 'width: 40%; height: 20px; margin:0;',
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Titular de la cuenta'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'principalowner',
				modelProperty : 'principalowner',
				style : 'width: 40%; height: 20px; margin:0;',
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'No. de telefono'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'phonenumber',
				modelProperty : 'phonenumber',
				style : 'width: 40%; height: 20px; margin:0;',
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Banco:'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'bank',
				modelProperty : 'bank',
				style : 'width: 40%; height: 20px; margin:0;',
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Monto a descontar:'
			}, {
				name : 'paymentamount',
				classes : 'span6',
				style : 'font-weight: bold;'
			} ]
		}]
	}, {
		kind : 'OB.UI.Button',
		classes : 'btnlink',
		style : 'float: right;',
		content : 'OK',
		ontap : 'confirmPayment'
	}],
	/*
	 * Forma de cheque se debe requerir: No. de cta, no. de cheque, titular de
	 * las cuentas, No. de telefono, Banco del cheque.
	 */
	confirmPayment : function() {
		var paymentresult = {
			NoAccount : this.$.noaccount.getValue(),
			NoCheck : this.$.nocheck.getValue(),
			POwner : this.$.principalowner.getValue(),
			NoPhone : this.$.phonenumber.getValue(),
			Bank : this.$.bank.getValue(),
			TransactionID : this.$.nocheck.getValue(),
			ApprovedAmount : this.paymentAmount,
			CardNumberMasked : this.$.nocheck.getValue(),
		// CardLogo: data.Card.CardLogo
		// Void transaction will be excuted when the payment line is removed.
		// VERY IMPORTANT TO REMOVE PAYMENTS
			 voidTransaction: function (callback) {
				 callback(false, 'Pago removido');
			 }

		};
		var newPayment = new OB.Model.PaymentLine({
			'kind' : this.key,
			'name' : this.paymentType,
			'amount' : parseFloat(paymentresult.ApprovedAmount),
			'paymentData' : paymentresult
		});

		this.receipt.addPayment(newPayment);
		// close the popup
		this.doHideThisPopup();
	},
	initComponents : function() {
		this.inherited(arguments);
		this.$.paymenttype.setContent(this.paymentType);
		this.$.paymentamount.setContent(this.paymentAmount);
	}
});

/* global enyo, $
 * Credit Card
 * No. tarjeta, 
 * nombre y 
 * fecha caducidad
 */

enyo.kind({
	events: {
	    onHideThisPopup: ''
	},
	name : 'SSPOS_.UI.ConnectorWH',
	components : [ {
		components : [ {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Payment type'
			}, {
				name : 'paymenttype',
				classes : 'span6',
				style : 'font-weight: bold;'
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'No. de Retencion'
			}, {
				kind : 'OB.UI.CustomerTextProperty',
				name : 'creditCard',
				modelProperty : 'creditcard',
				style : 'width: 40%; height: 20px; margin:0;',
			} ]
		}, {
			classes : 'row-fluid',
			components : [ {
				classes : 'span6',
				content : 'Monto a descontar:'
			}, {
				name : 'paymentamount',
				classes : 'span6',
				style : 'font-weight: bold;'
			} ]
		} ]
	}, {
		kind : 'OB.UI.Button',
		classes : 'btnlink',
		style : 'float: right;',
		content : 'OK',
		ontap : 'confirmPayment'
	} ],
	confirmPayment : function() {
		var paymentresult = {
			TransactionID : this.$.creditCard.getValue(),
			ApprovedAmount : this.paymentAmount,
			CardNumberMasked : this.$.creditCard.getValue(),
		// CardLogo: data.Card.CardLogo
		// Void transaction will be excuted when the payment line is removed.
		// VERY IMPORTANT TO REMOVE PAYMENTS

		};
		var newPayment = new OB.Model.PaymentLine({
			'kind' : this.key,
			'name' : this.paymentType,
			'amount' : parseFloat(paymentresult.ApprovedAmount),
			'paymentData' : paymentresult
		});

		this.receipt.addPayment(newPayment);
		// close the popup
		this.doHideThisPopup();
	},
	initComponents : function() {
		this.inherited(arguments);
		this.$.paymenttype.setContent(this.paymentType);
		this.$.paymentamount.setContent(this.paymentAmount);
	}
});


 OB.UTIL.HookManager.registerHook('OBPOS_PostAddProductToOrder', function(args, callbacks) {
     
		
		var numberLines = args.receipt.get('lines').models.length;
		
		if (    ((numberLines>0  && numberLines<40)
			|| (numberLines>=40 && numberLines<50)
			|| (numberLines>=50 && numberLines<60))
			&& OB.MobileApp.model.get('connectedToERP')
		   )
		{
		
			
			//OB.Cache.resetCacheForModel(OB.Model.Order);
			//OB.Cache.resetCacheForModel(OB.Model.Discounts);
			//OB.Cache.resetCacheForModel(OB.Model.modelLoaders);

			//OB.Dal.remove(OB.Model.ComboFamily, null, null);
			//OB.Dal.remove(OB.Model.ComboProduct, null, null);
			//OB.Dal.removeAll(OB.Model[model]);
			//OB.Dal.removeAll(OB.Model.ComboProduct);
			//OB.Dal.removeAll(OB.Model.ComboFamily);
			OB.Cache.resetCacheForModel(OB.Model.ComboProduct);
			OB.Cache.resetCacheForModel(OB.Model.ComboFamily);

			//OB.MobileApp.model.receipt.save();
			//OB.MobileApp.model.orderList.saveCurrent();
			args.cancelOperation = false;			
			OB.UTIL.HookManager.callbackExecutor(args, callbacks);
		
		}else{
			args.cancelOperation = false;			
			OB.UTIL.HookManager.callbackExecutor(args, callbacks);
		}
    });