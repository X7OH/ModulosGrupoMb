(function () {
	
  // Creamos el modelo de la tabla Sqllite
  var GiftCardRetail = OB.Data.ExtensibleModel.extend({  
    modelName: 'GiftCardRetail',
    tableName: 'giftcardretail',
    entityName: 'GiftCardRetail',
    source: 'ec.com.sidesoft.retail.giftcard.master.GiftCardRetail',
    remote: 'OBPOS_remote.product',
    dataLimit: 300
  });

  //se definen las propiedades del query que estan en el archivo
  //ec.com.sidesoft.retail.giftcard.master.GiftCardRetail.java
  GiftCardRetail.addProperties([{    
    
    name: 'id',
    column: 'giftcard_id',
    primaryKey: true,
    type: 'TEXT'
  },{
    name: 'searchKey',
    column: 'searchKey',
    primaryKey: false,
    type: 'TEXT'
  }, {
    name: '_identifier',
    column: '_identifier',
    primaryKey: false,
    filter: true,
    type: 'TEXT'
  }, {
    name: 'alertStatus',
    column: 'alertStatus',
    primaryKey: false,
    type: 'TEXT'
  }, {
    name: 'amount',
    column: 'amount',
    primaryKey: false,
    type: 'NUMERIC'
  },  {
    name: 'currentamount',
    column: 'currentamount',
    primaryKey: false,
    type: 'NUMERIC'
  }, {
    name: 'iscancelled',
    column: 'iscancelled',
    primaryKey: false,
    type: 'TEXT'
  },{
    name: 'obgcneExpirationdate',
    column: 'obgcneExpirationdate',
    primaryKey: false,
    type: 'TEXT'
  }]);

  var GiftCardListRetail = Backbone.Collection.extend({
    model: GiftCardRetail
  });

  window.OB = window.OB || {};
  window.OB.Model = window.OB.Model || {};
  window.OB.Collection = window.OB.Collection || {};

  window.OB.Model.GiftCardRetail = GiftCardRetail;
  window.OB.Collection.GiftCardListRetail = GiftCardListRetail;

  // add the model to the window.
  OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push(OB.Model.GiftCardRetail);
}());