	/* Forma de cheque se debe requerir:  
	 *  No. de cta,
	 *  no. de cheque, 
	 *  titular de las cuentas, 
	 *  No. de telefono, 
	 *  Banco del cheque.
	*/
var PaymentLine = Backbone.Model.extend({
    modelName: 'PaymentLine',
    defaults: {
      'amount': OB.DEC.Zero,
      'origAmount': OB.DEC.Zero,
      'referenceno': null,
      'accountno':null,
      'owneracc':null,
      'phoneno':null,
      'bank':null,
      'paid': OB.DEC.Zero,
      // amount - change...
      'date': null
    },
    printAmount: function () {
      if (this.get('rate')) {
        return OB.I18N.formatCurrency(OB.DEC.mul(this.get('amount'), this.get('rate')));
      } else {
        return OB.I18N.formatCurrency(this.get('amount'));
      }
    },
    printForeignAmount: function () {
      return '(' + OB.I18N.formatCurrency(this.get('amount')) + ' ' + this.get('isocode') + ')';
    }
  });

	
OB.Model.Order.prototype.adjustPayment = function() {

    var i, max, p;
    var payments = this.get('payments');
    var total = OB.DEC.abs(this.getTotal());

    var nocash = OB.DEC.Zero;
    var cash = OB.DEC.Zero;
    var origCash = OB.DEC.Zero;
    var auxCash = OB.DEC.Zero;
    var prevCash = OB.DEC.Zero;
    var paidCash = OB.DEC.Zero;
    var pcash;

    for (i = 0, max = payments.length; i < max; i++) {
      p = payments.at(i);
      
      if(p.get('paymentData')){
    	  //alert(p.get('paymentData').CardNumberMasked);
      if(p.get('paymentData').CardNumberMasked){
    	  p.set('referenceno', p.get('paymentData').CardNumberMasked);}
      if(p.get('paymentData').NoAccount){  p.set('accountno', p.get('paymentData').NoAccount);}
      if(p.get('paymentData').POwner){  p.set('owneracc', p.get('paymentData').POwner);}
      if(p.get('paymentData').NoPhone){  p.set('phoneno', p.get('paymentData').NoPhone);}
      if(p.get('paymentData').Bank){  p.set('bank', p.get('paymentData').Bank);}
      if(p.get('paymentData').ExpDate){  p.set('expirationdate', p.get('paymentData').ExpDate);}
    	  //alert('Tomando el valor'+p.get('referenceno'));
      }
      
      if (p.get('rate') && p.get('rate') !== '1') {
        p.set('origAmount', OB.DEC.mul(p.get('amount'), p.get('rate')));
      } else {
        p.set('origAmount', p.get('amount'));
      }
      p.set('paid', p.get('origAmount'));
      if (p.get('kind') === OB.POS.modelterminal.get('paymentcash')) {
        // The default cash method
        cash = OB.DEC.add(cash, p.get('origAmount'));
        pcash = p;
        paidCash = OB.DEC.add(paidCash, p.get('origAmount'));
      } else if (OB.POS.modelterminal.hasPayment(p.get('kind')) && OB.POS.modelterminal.hasPayment(p.get('kind')).paymentMethod.iscash) {
        // Another cash method
        origCash = OB.DEC.add(origCash, p.get('origAmount'));
        pcash = p;
        paidCash = OB.DEC.add(paidCash, p.get('origAmount'));
      } else {
        nocash = OB.DEC.add(nocash, p.get('origAmount'));
      }
    }

    // Calculation of the change....
    //FIXME
    if (pcash) {
      if (pcash.get('kind') !== OB.POS.modelterminal.get('paymentcash')) {
        auxCash = origCash;
        prevCash = cash;
      } else {
        auxCash = cash;
        prevCash = origCash;
      }
      if (OB.DEC.compare(nocash - total) > 0) {
        pcash.set('paid', OB.DEC.Zero);
        this.set('payment', nocash);
        this.set('change', OB.DEC.add(cash, origCash));
      } else if (OB.DEC.compare(OB.DEC.sub(OB.DEC.add(OB.DEC.add(nocash, cash), origCash), total)) > 0) {
        pcash.set('paid', OB.DEC.sub(total, OB.DEC.add(nocash, OB.DEC.sub(paidCash, pcash.get('origAmount')))));
        this.set('payment', total);
        //The change value will be computed through a rounded total value, to ensure that the total plus change
        //add up to the paid amount without any kind of precission loss
        this.set('change', OB.DEC.sub(OB.DEC.add(OB.DEC.add(nocash, cash), origCash), OB.Utilities.Number.roundJSNumber(total, 2)));
      } else {
        pcash.set('paid', auxCash);
        this.set('payment', OB.DEC.add(OB.DEC.add(nocash, cash), origCash));
        this.set('change', OB.DEC.Zero);
      }
    } else {
      if (payments.length > 0) {
        if (this.get('payment') === 0 || nocash > 0) {
          this.set('payment', nocash);
        }
      } else {
        this.set('payment', OB.DEC.Zero);
      }
      this.set('change', OB.DEC.Zero);
    }
  };
