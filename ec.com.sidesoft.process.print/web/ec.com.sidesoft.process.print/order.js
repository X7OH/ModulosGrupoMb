
var priceScale = function () {
    return OB.Format.formats.priceEdition.length - OB.Format.formats.priceEdition.indexOf('.') - 1;
  };
  
  var amountScale = function () {
    return OB.Format.formats.amountInform.length - OB.Format.formats.amountInform.indexOf('.') - 1;
  };
  


  OB.I18N.formatCurrency = function (number) {
    var maskNumeric = OB.Format.formats.amountInform,
        decSeparator = OB.Format.defaultDecimalSymbol,
        groupSeparator = OB.Format.defaultGroupingSymbol,
        groupInterval = OB.Format.defaultGroupingSize;

    maskNumeric = maskNumeric.replace(',', 'dummy').replace('.', decSeparator).replace('dummy', groupSeparator);

    return OB.Utilities.Number.JSToOBMasked(number, maskNumeric, decSeparator, groupSeparator, groupInterval);
  };
  
  
  
var BusinessPartner = Backbone.Model.extend({
    includeDocNoSeperator: true,
    modelName: 'BusinessPartner',
    tableName: 'c_bpartner',
    entityName: 'BusinessPartner',
    source: '',
    properties: ['id', 'json'],
    propertyMap: {
      'id': 'c_order_id',
      'json': 'json'
    },
    initialize: function (attributes) {
    	if (attributes && attributes.id && attributes.json) {
            // The attributes of the order are stored in attributes.json
            // Makes sure that the id is copied
            attributes = JSON.parse(attributes.json);
        }
    	if (attributes) {
	    	this.set('id', attributes.id);
	        this.set('_identifier', attributes.identifier);
	        this.set('taxID', attributes.taxID);
    	}
      }
});

var ProductCallCenter = Backbone.Model.extend({
	modelName: 'ProductCallCenter',
    tableName: 'm_product',
    entityName: 'ProductCallCenter',
    initialize: function (attributesP) {
    	
        if (attributesP) {
          //this.set('id', attributes.identifier);        
          this.set('_identifier', attributesP.prodidentifier);
          this.set('identifier', attributesP.prodidentifier);
          this.set('listPrice', attributesP.prodlistPrice);
        }
      }
});

var Promotion = Backbone.Model.extend({
    includeDocNoSeperator: true,
    modelName: 'OrderLineOffer',
    tableName: 'C_OrderLine_Offer',
    entityName: 'OrderLineOffer',
    source: '',
    properties: ['id', 'json'],
    propertyMap: {
      'id': 'c_order_id',
      'json': 'json'
    },
    initialize: function (attributesPromo) {
        
        if (attributesPromo) {
          this.set('id', attributesPromo.id);  
          this.set('discountType', attributesPromo.discountType);  
          this.set('amt', attributesPromo.amt); 
          this.set('name', attributesPromo.name);
        }
      }
});

var PromotionList = Backbone.Collection.extend({
	  model: Promotion,
	  
	});

var OrderLine = Backbone.Model.extend({
	  modelName: 'OrderLineCallCenter',
	  /*defaults: {
	    product: null,
	    productidentifier: null,
	    uOM: null,
	    description: '',
	    attributeValue: ''
	  },*/

	  initialize: function (attributesOL) {
	    if (attributesOL && attributesOL.product) {
	      this.set('auxp', attributesOL.product);
	      //this.set('product', new ProductCallCenter(attributesOL.product));
	      //this.set('product', {id: attributes.product.id, _identifier:attributes.product.identifier, listPrice: attributes.product.listPrice});
	      //this.set('productsec',  new ProductCallCenter().reset(attributesOL.product));
	      //this.set('product', attributesOL.product);
	      //this.set('product', new ProductCallCenter(attributesOL.product));
	      //this.set('product', attributesOL.product);
	      this.set('product', new ProductCallCenter(attributesOL.product));
	      this.set('productidentifier', attributesOL.productidentifier);
	      this.set('qty', attributesOL.qty);
	      this.set('price', attributesOL.price);
	      this.set('gross', attributesOL.gross);
	      this.set('net', attributesOL.net);
	      this.set('promotions', attributesOL.promotions);
	      //this.set('promotions', new PromotionList().reset(attributesOL.promotions));
	      this.set('priceIncludesTax', attributesOL.priceIncludesTax);
	      this.set('description', attributesOL.description);
	      this.set('linerate', attributesOL.linerate);
	      this.set('chunks', attributesOL.chunks);
	      this.set('id', attributesOL.id);
	      this.set('comboId', attributesOL.id);
	    }

	  },

	  getQty: function () {
	    return this.get('qty');
	  },

	  printQty: function () {
		  //return OB.DEC.toNumber(OB.DEC.toBigDecimal(this.get('qty')), OB.I18N.qtyScale());
		 var qty = this.get('qty'); 
		 if (qty && !_.isUndefined(qty)) {
		  qty = OB.DEC.toBigDecimal(this.get('qty')); 
		  qty.setScale(OB.I18N.qtyScale()); 
	  	 } else {
	  		qty = OB.DEC.toBigDecimal(OB.DEC.Zero);
			qty.setScale(OB.I18N.qtyScale());
	  	 }
		  return qty.toString();
	  },

	  printPrice: function () {
		  var price = this.get('_price') || this.get('nondiscountedprice') || this.get('price');
		  if (price && !_.isUndefined(price)) {
			  price = OB.DEC.toBigDecimal(this.get('_price') || this.get('nondiscountedprice') || this.get('price'));
			  price.setScale(priceScale());
		  } else {
			price = OB.DEC.toBigDecimal(OB.DEC.Zero);
			price.setScale(priceScale());
		  }
		  return  OB.I18N.formatCurrency(price);
	    //return OB.I18N.formatCurrency(this.get('_price') || this.get('nondiscountedprice') || this.get('price'));
	  },

	  printGross: function () {
		  var gross = this.get('_gross') || this.getGross();
		  if (gross && !_.isUndefined(gross)) {
			  gross = OB.DEC.toBigDecimal(this.get('_gross') || this.getGross());
			  gross.setScale(amountScale());
		  } else {
			  gross = OB.DEC.toBigDecimal(OB.DEC.Zero);
			  gross.setScale(amountScale());
		  }
	      return gross;
	  },

	  printNet: function () {
		  var net = this.get('nondiscountednet') || this.getNet();
		  if (net && !_.isUndefined(net)) {
		  	net = OB.DEC.toBigDecimal(this.get('nondiscountednet') || this.getNet());
		  	net.setScale(amountScale());
	      } else {
	    	net = OB.DEC.toBigDecimal(OB.DEC.Zero);
	    	net.setScale(amountScale()); 
	      }
	      return net;
	  },

	  getGross: function () {
	    return this.get('gross');
	  },

	  getNet: function () {
	    return this.get('net');
	  }
	});

	//Sales.OrderLineCol Model.
	var OrderLineCallCenterList = Backbone.Collection.extend({
	  model: OrderLine/*,
	  isProductPresent: function (product) {
	    var result = null;
	    if (this.length > 0) {
	      result = _.find(this.models, function (line) {
	        if (line.get('product').get('id') === product.get('id')) {
	          return true;
	        }
	      }, this);
	      if (_.isUndefined(result) || _.isNull(result)) {
	        return false;
	      } else {
	        return true;
	      }
	    } else {
	      return false;
	    }
	  }*/
	});

	var Payment = Backbone.Model.extend({
	    includeDocNoSeperator: true,
	    modelName: 'Payment',
	    tableName: 'FIN_Payment_Sched_Ord_V',
	    entityName: 'FIN_Payment_Sched_Ord_V',
	    source: '',
	    properties: ['id', 'json'],
	    propertyMap: {
	      'id': 'c_order_id',
	      'json': 'json'
	    },
	    initialize: function (attributes) {
	        var orderId;
	        if (attributes && attributes.id && attributes.json) {
	          // The attributes of the order are stored in attributes.json
	          // Makes sure that the id is copied
	          attributes = JSON.parse(attributes.json);
	        }
	        var bpModel;
	        if (attributes) {
	          this.set('id', attributes.id);  
	          this.set('name', attributes.name);
	          this.set('rate', attributes.rate);    
	          this.set('amount', attributes.amount);    
	          this.set('origAmount', attributes.origAmount);    
	          this.set('isocode', attributes.isocode);  
	        }
	      }
	});

	var PaymentLineList = Backbone.Collection.extend({
	  model: Payment
	});


var Order = Backbone.Model.extend({
    includeDocNoSeperator: true,
    modelName: 'Order',
    tableName: 'c_order',
    entityName: 'Order',
    source: '',
    properties: ['id', 'json', 'session', 'hasbeenpaid', 'isbeingprocessed'],
    propertyMap: {
      'id': 'c_order_id',
      'json': 'json',
      'session': 'ad_session_id',
      'hasbeenpaid': 'hasbeenpaid',
      'isbeingprocessed': 'isbeingprocessed'
    },
    initialize: function (attributes) {
        var orderId;
        if (attributes && attributes.id && attributes.json) {
          // The attributes of the order are stored in attributes.json
          // Makes sure that the id is copied
          orderId = attributes.id;
          attributes = JSON.parse(attributes.json);
          attributes.id = orderId;
        }
        var bpModel;
        if (attributes && attributes.documentNo) {
          this.set('id', attributes.id);  
          this.set('organization', attributes.organization);
          this.set('priceIncludesTax', attributes.priceIncludesTax);
          this.set('currency', attributes.currency);
          this.set('currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, attributes.currencyidentifier);
          this.set('orderDate', new Date(Number(attributes.orderDate)));
          //this.set('orderDate', new Date(Number(attributes.orderDate)));
          this.set('documentNo', attributes.documentNo);
          this.set('bp', new BusinessPartner(attributes.bp));
          this.set('lines', new OrderLineCallCenterList().reset(attributes.lines));
          
          this.set('payments', new PaymentLineList().reset(attributes.payments));
          this.set('qty', attributes.qty);
          this.set('gross', attributes.gross);
          this.set('net', attributes.net);
          this.set('taxes', attributes.taxes);
          this.set('description', attributes.description);
          this.set('invoiceDocumentNo', attributes.invoiceDocumentNo);
          this.set('validationCode', attributes.validationCode);
          this.set('posTerminal'+OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, attributes.posTerminal);
        }
      },

      printQty: function () {
        return OB.DEC.toNumber(OB.DEC.toBigDecimal(this.get('qty')), OB.I18N.qtyScale()).toString();
      },

      printPrice: function () {
        return OB.I18N.formatCurrency(this.get('_price') || this.get('nondiscountedprice') || this.get('price'));
      },

      printGross: function () {
		  var gross = OB.DEC.toBigDecimal(this.get('_gross') || this.getGross());
		  gross.setScale(amountScale());
	    return gross;
	  },

	  printNet: function () {
		  var net = OB.DEC.toBigDecimal(this.get('nondiscountednet') || this.getNet());
		  net.setScale(amountScale());
	    return net;
	  },

	  getGross: function () {
	    return this.get('gross');
	  },

	  getNet: function () {
	    return this.get('net');
	  }
});

var Currency = Backbone.Model.extend({
    includeDocNoSeperator: true,
    modelName: 'Currency',
    tableName: 'C_Currency',
    entityName: 'Currency',
    source: '',
    properties: ['id', 'json'],
    propertyMap: {
      'id': 'c_order_id',
      'json': 'json'
    },
    initialize: function (attributes) {
        if (attributes && attributes.id && attributes.json) {
          // The attributes of the order are stored in attributes.json
          // Makes sure that the id is copied
          attributes = JSON.parse(attributes.json);
        }
        if (attributes) {
          this.set('id', attributes.id);    
          this.set('_identifier', attributes.identifier); 
          this.set('currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, attributes.identifier);
        }
      }
});

var Tax = Backbone.Model.extend({
    includeDocNoSeperator: true,
    modelName: 'OrderTax',
    tableName: 'C_Order_Tax',
    entityName: 'OrderTax',
    source: '',
    properties: ['id', 'json'],
    propertyMap: {
      'id': 'c_order_id',
      'json': 'json'
    },
    initialize: function (attributes) {
        if (attributes && attributes.id && attributes.json) {
          // The attributes of the order are stored in attributes.json
          // Makes sure that the id is copied
          attributes = JSON.parse(attributes.json);
        }
        if (attributes) {
          this.set('id', attributes.id);    
          this.set('net', Number(attributes.net)); 
          this.set('amount', Number(attributes.amount)); 
        }
      }
});

var User = Backbone.Model.extend({
    includeDocNoSeperator: true,
    modelName: 'User',
    tableName: 'AD_User',
    entityName: 'User',
    initialize: function (attributeUser) {
        if (attributeUser) {
          this.set('id', attributeUser.id);  
          this.set('user', attributeUser.user);
          this.set('org', attributeUser.org);   
        }
      }
});
