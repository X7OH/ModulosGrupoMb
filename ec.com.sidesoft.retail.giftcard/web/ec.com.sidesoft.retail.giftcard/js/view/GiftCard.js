enyo.kind({
  name: 'SRGC_.UI.GiftCardConnector',
  events: {
    onShowPopup: '',
    onHideThisPopup: '',
  },  
  components: [
    {
      components: [
        /*{
        classes: 'row-fluid',
        components: [{
          classes: 'span6',
          name: 'LblPaymentType'
        }, {
          name: 'paymenttype',
          classes: 'span6',
          style: 'font-weight: bold;'
        }]
      }, */
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
    },
  ],

  voidTransaction: function (callback, receipt, removedPayment) {
    //callback(false, 'Pago removido');
    callback(false);
    // This function will be executed when the payment is removed using the payments UI
    // In this point we should communicate with servers to remove the payment
    // The callback function has the following signature:
    // callback(true, 'Error message'); // Cannot remove the payment, The error message in the 
    //parameter will be displayed to the user
    //callback(false); // The payment has been voided successfully and the payment will be removed.
  },
  
  confirmPayment: function (inSender, inEvent) {
 	  OB.UI.GiftCardUtilsRetail.consumeGiftCard(this, this.paymentAmount, 'BasedOnProductGiftCard');
    this.mainPopup.hide();
  },
  
  closedModal: function(){
    this.mainPopup.hide();
  },
  
  initComponents: function() {
  	this.inherited(arguments);
    //this.$.paymenttype.setContent(this.paymentType);
    //this.$.LblPaymentType.setContent(OB.I18N.getLabel('SRDFP_LblPaymentType'));
    this.$.paymentamount.setContent(this.paymentAmount);
    this.$.LblAmount.setContent(OB.I18N.getLabel('SRDFP_LblAmount'));
    this.$.LblCancel.setContent(OB.I18N.getLabel('SRDFP_LblCancel'));
    this.$.LblPay.setContent(OB.I18N.getLabel('SRDFP_LblPay'));
  }

});