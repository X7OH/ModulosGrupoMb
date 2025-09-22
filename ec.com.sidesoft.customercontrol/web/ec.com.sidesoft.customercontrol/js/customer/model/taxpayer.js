
var TaxPayer = OB.Data.ExtensibleModel.extend({
    modelName: 'TaxPayer',
    tableName: 'ad_taxlist',
    entityName: 'TaxPayer',
    source: 'ec.com.sidesoft.customercontrol.TaxPayer',
  });

  // Add the required properties for the model Serve Option
  TaxPayer.addProperties([{
    name: 'id',
    column: 'id',
    primaryKey: true,
    type: 'TEXT'
  }, {
	name: 'searchKey',
	column: 'searchKey',
	type: 'TEXT'
  }, {
    name: 'name',
    column: 'name',
    type: 'TEXT'
  }, {
    name: '_identifier',
    column: '_identifier',
    type: 'TEXT'
  }]);

  // Register the model in the application
  OB.Data.Registry.registerModel(TaxPayer);
 
  // Add the model to the main Web POS window.
  // This loads the data when the main Web POS window is loaded.
  OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push(TaxPayer);
