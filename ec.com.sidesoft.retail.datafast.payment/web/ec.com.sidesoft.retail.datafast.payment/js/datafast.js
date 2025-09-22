/* METODO DE PAGO DATAFAST */
enyo.kind({
  name: 'SRDFP_.UI.DatafastConnector',
  components: [{
    components: [
      {
      classes: 'row-fluid',
      components: [{
        classes: 'span6',
        name: 'LblPaymentType'
      }, {
        name: 'paymenttype',
        classes: 'span6',
        style: 'font-weight: bold;'
      }]
    }, 
    {      
      classes: 'row-fluid',
      components: [{
        classes: 'span6',
        name: 'LblAmount'
      }, {
        name: 'paymentamount',
        classes: 'span6',
        style: 'font-weight: bold;'
      }]
    },
    ]
  },
  {
      kind: 'OB.UI.Button',
      classes: 'btnlink',
      style: 'float: right;',
      content: 'CANCEL',
      name: 'LblCancel',
      ontap: 'closedModal'
  },  
  {
    kind: 'OB.UI.Button',
    classes: 'btnlink',
    style: 'float: right;',
    name: 'LblPay',
    ontap: 'confirmPayment'
  }
  ],
  
  voidTransaction: function (callback, receipt, removedPayment) {
    // This function will be executed when the payment is removed using the payments UI
    // In this point we should communicate with servers to remove the payment
    // The callback function has the following signature:
    // callback(true, 'Error message'); // Cannot remove the payment, The error message in the 
    //parameter will be displayed to the user
    // callback(false); // The payment has been voided successfully and the payment will be removed.
  },
  
  getSubtotal: function(num, percent) {
    return ( Number(num) / Number(percent) );
  },  

  getIVA: function(num, percent) {
    return Number(num) - (  Number(num) / Number(percent) );
  },          

  confirmPayment: function () {

    //In this point we should retrieve the needed info to pay (credit card
    //and then connect with the payment provider.
    this.mainPopup.hide();
    OB.UTIL.showLoading(true);

    var me = this;
    var rate = 1.12;

    /*var totAmount = 0;
    var totNet = 0;
    var totNoTax = 0;
    var totSub=0;
    var taxes = me.receipt.get('taxes');

    for (var t in taxes) {
      if(taxes[t].amount == 0){
        totNet += taxes[t].net;
      }else{
        totNoTax += taxes[t].net;
      }
      totAmount += taxes[t].amount;
    } 

    totSub=totNet+totNoTax;  */    

    /* var subtotal12 = OB.I18N.formatCurrency(totNoTax);
    var subtotal0 = OB.I18N.formatCurrency(totNet);
    var iva12 = OB.I18N.formatCurrency(totAmount); */

    var subtotal12 = OB.I18N.formatCurrency(this.getSubtotal(this.paymentAmount, rate));
    var subtotal0 = OB.I18N.formatCurrency(0);
    var iva12 = OB.I18N.formatCurrency(this.getIVA(this.paymentAmount, rate));  
    
    $.ajax({
      url:'http://localhost:8090/datafast',
      data: { 
        monto: this.paymentAmount,
        subtotal12: subtotal12,
        subtotal0: subtotal0,
        iva12: iva12
      },
      dataType:'json',
      type:'POST',     
      success: function (data){

        if (!data.resultado || data.timeout || me.isEmpty(data) ) {
          if(me.isEmpty(data)){
            me.mainPopup.hide();
            OB.UTIL.showLoading(false);
            OB.UTIL.showConfirmation.display("No se pudo obtener respuesta de la transaccion");
          }else if(!data.resultado || data.timeout){
            me.mainPopup.hide();
            OB.UTIL.showLoading(false);
            OB.UTIL.showConfirmation.display(data.message);              
          }else{
            me.mainPopup.hide();
            OB.UTIL.showLoading(false);
            OB.UTIL.showConfirmation.display("No se pudo obtener respuesta de la transaccion");              
          }
        }else{
          var infoTrasaccion = data.datosrecepcion;

          var paymentresult = {
            TransactionID: infoTrasaccion[0].numeroAprobacion,
            ApprovedAmount: me.paymentAmount,
            CardNumberMasked: infoTrasaccion[0].numeroTarjeta,
            //CardLogo: data.Card.CardLogo,
            // Void Confirmation indicates whether or not to show a confirmation dialog when the user taps on the
            // button that removes this payment before executing the voidTransaction function.
            // By default is true
            voidConfirmation: true,          
            // Void transaction will be executed when the payment line is removed.
            // VERY IMPORTANT TO REMOVE PAYMENTS
            voidTransaction: me.voidTransaction
          }; 

          var newPayment = new OB.Model.PaymentLine({
           'kind': me.key,
           'name': me.name,
           'amount': parseFloat(me.paymentAmount),
           'paymentData': paymentresult
          });                   

          me.receipt.addPayment(newPayment);      

          me.mainPopup.hide();
          OB.UTIL.showLoading(false);
          OB.UTIL.showConfirmation.display(data.message); 
        }          
          
      },
      error: function (error){
        me.mainPopup.hide();
        OB.UTIL.showLoading(false);
        OB.UTIL.showConfirmation.display("No se pudo obtener respuesta de la transaccion");
      }
    });
  },

  isEmpty: function(obj) {
    for(var key in obj) {
      if(obj.hasOwnProperty(key))
        return false;
    }
    return true;
  },

  closedModal: function(){
    this.mainPopup.hide();
  },
  
  initComponents: function() {
    this.inherited(arguments);
    this.$.paymenttype.setContent(this.paymentType);
    this.$.paymentamount.setContent(this.paymentAmount);
    this.$.LblPaymentType.setContent(OB.I18N.getLabel('SRDFP_LblPaymentType'));
    this.$.LblAmount.setContent(OB.I18N.getLabel('SRDFP_LblAmount'));
    this.$.LblCancel.setContent(OB.I18N.getLabel('SRDFP_LblCancel'));
    this.$.LblPay.setContent(OB.I18N.getLabel('SRDFP_LblPay'));
  }
  
});