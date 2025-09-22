OB.Constants = {
    FIELDSEPARATOR: '$',
    IDENTIFIER: '_identifier'
  };

OB.OBCOMBO = {
		  comboRuleId: '7899A7A4204749AD92881133C4EE7A57',
		  discountType: {
		    fixPrice: 'FIXPRICE',
		    percentage: 'PERCENTAGE',
		    fixDiscount: 'FIXDISC'
		  }
		};
//OB.OBCOMBO = {};
//OB.OBCOMBO.comboRuleId = '7B2C3E7E5C314D22963CF5CA06AECE85';
OB.OBCOMBO.comboFixPrice = {
		  comboRuleId: '71895FA82C9645949CB752564FB1389D',
		  discountType: {
		    comboFixPrice: 'COMBOFIXPRICE'
		  }
		};
//OB.OBCOMBO.comboFixPrice = {};
//OB.OBCOMBO.comboFixPrice.comboRuleId = '7B2C3E7E5C314D22963CF5CA06AECE85';

//var BigDecimal = Java.type('java.math.BigDecimal');
var BigDecimal = Packages.java.math.BigDecimal;

(function () {

	  OB.DEC = {};

	  var scale = 2;
	  var roundingmode = 4;

	  var toBigDecimal = function (a) {
	      return new BigDecimal(a.toString());
	      };

	  var toNumber = function (big, arg_scale) {
	      var localscale = arg_scale || scale;
	      if (big.scale) {
	        return parseFloat(big.setScale(localscale, roundingmode).toString(), 10);
	      } else {
	        if (_.isNumber(big)) {
	          return big;
	        } else {
	          OB.error("toNumber: Argument cannot be converted toNumber", big);
	        }
	      }
	      };

	  OB.DEC.Zero = toNumber(0);
	  OB.DEC.One = toNumber(1);

	  OB.DEC.getScale = function () {
	    return scale;
	  };

	  OB.DEC.getRoundingMode = function () {
	    return roundingmode;
	  };

	  OB.DEC.isNumber = function (a) {
	    return typeof (a) === 'number' && !isNaN(a);
	  };

	  OB.DEC.add = function (a, b, arg_scale) {
	    return toNumber(toBigDecimal(a).add(toBigDecimal(b)), arg_scale);
	  };

	  OB.DEC.sub = function (a, b, arg_scale) {
	    return toNumber(toBigDecimal(a).subtract(toBigDecimal(b)), arg_scale);
	  };

	  OB.DEC.mul = function (a, b, arg_scale) {
	    return toNumber(toBigDecimal(a).multiply(toBigDecimal(b)), arg_scale);
	  };

	  OB.DEC.div = function (a, b, arg_scale) {
	    return toNumber(toBigDecimal(a).divide(toBigDecimal(b), arg_scale || scale, roundingmode), arg_scale);
	  };

	  OB.DEC.compare = function (a) {
	    return toBigDecimal(a).compareTo(0);
	  };

	  OB.DEC.number = function (jsnumber) {
	    return jsnumber; // toNumber(toBigDecimal(jsnumber));
	  };

	  OB.DEC.setContext = function (s, r) {
	    scale = s;
	    roundingmode = r;
	  };

	  OB.DEC.toBigDecimal = function (a) {
	    return toBigDecimal(a);
	  };

	  OB.DEC.toNumber = function (a, arg_scale) {
	    return toNumber(a, arg_scale);
	  };

	  OB.DEC.abs = function (a, arg_scale) {
	    return toNumber(toBigDecimal(a).abs(), arg_scale);
	  };

	}());