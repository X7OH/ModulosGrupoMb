// Creamos el modelo de la tabla Sqllite
var OrganizationSales = OB.Data.ExtensibleModel.extend({
    modelName: 'OrganizationSales',
    tableName: 'OrganizationSales',
    entityName: 'OrganizationSales',
    source: 'ec.com.sidesoft.retail.agreementinvoices.OrganizationSales'
  });

// Añadimos las propiedades para el modelo Serve Option
OrganizationSales.addProperties([{
    name: 'id',
    column: 'id',
    primaryKey: true,
    type: 'TEXT'
  }, {
	    name: 'idtpv',
	    column: 'idtpv',
	    type: 'TEXT'
  }, {
	    name: 'idcustomer',
	    column: 'idcustomer',
	    type: 'TEXT'
  }, {
	    name: 'namecustomer',
	    column: 'namecustomer',
	    type: 'TEXT'
  }, {
	    name: 'idlocation',
	    column: 'idlocation',
	    type: 'TEXT'
  }, {
	    name: 'location',
	    column: 'location',
	    type: 'TEXT'
  } 
  
  ]);

  // Registro del modelo en el POS (Sqllite)
  OB.Data.Registry.registerModel(OrganizationSales);
 

  //Añade el modedlo al ventana principal del Web POS
  // Esto carga la información cuando inicia la ventana principal del WebPos
  OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push(OrganizationSales);
