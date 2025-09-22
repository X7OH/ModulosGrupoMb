// Creamos el modelo de la tabla Sqllite
var ReturnOrders = OB.Data.ExtensibleModel.extend({
    modelName: 'ReturnOrders',
    tableName: 'returnorders',
    entityName: 'ReturnOrders',
    source: '',
    local: true
});

// Añadimos las propiedades para el modelo Serve Option
ReturnOrders.addProperties([{
    name: 'id',
    column: 'id',
    primaryKey: true,
    type: 'TEXT'
},{
    name: 'isprocess',
    column: 'isprocess',
    type: 'TEXT'
},{
    name: 'payments',
    column: 'payments',
    type: 'TEXT'
},{
    name: 'giftcardPaymentId',
    column: 'giftcardPaymentId',
    type: 'TEXT'
}]);
  
// Registro del modelo en el POS (Sqllite)
OB.Data.Registry.registerModel(ReturnOrders);

//Añade el modedlo al ventana principal del Web POS
OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push(ReturnOrders); 

