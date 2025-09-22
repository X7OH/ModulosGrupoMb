/**
 * 
 */

  OB.Model.BusinessPartner.addProperties([{
    name: 'sswhTaxidtype',
    column: 'sswhTaxidtype',
    type: 'TEXT'
  },  {
	name: 'taxidtype_name',
    column: 'taxidtype_name',
    type: 'TEXT'
  }]);
  
  var PropertyOptionDefinition = {
    kind: 'OB.UI.CustomerComboProperty',
    name: 'taxidtypecombo',
    modelProperty: 'sswhTaxidtype',
    modelPropertyText: 'taxidtype_name',
    i18nLabel: 'SSPOS_TaxIDType',
    // Model Collection to use. This definition has been created when registering the
    // ServeOption model
    collection: new OB.Collection.TaxPayerList(),
    retrievedPropertyForValue: 'searchKey',
    retrievedPropertyForText: '_identifier',
    // This function is called to determine whether the property is displayed or not
    // This implementation shows the property only if the product has a Serve Option Category
    // This function is called to get the serve options for the selected product.
    // In this case we filter by the serve options category of the selected product.
    defaultValue: function () {
        return OB.MobileApp.model.get('terminal').defaultbp_taxidtype;
      },
    fetchDataFunction: function (args) {
      // Call to client DAL functionality to query for client data for
      // OB.Model.ServeOption filtering by field serveOptionCategory
      var me = this,
      criteria;
      criteria = {
    		  _orderByClause: '_identifier asc'
      };
      OB.Dal.find(OB.Model.TaxPayer, criteria, function (data, args) {
               // In case of success assign the results to the property component
        me.dataReadyFunction(data, args);
      }, function (error) {
        // In case of error, show an error message to the user and
        // clean the property component
        OB.UTIL.showError(OB.I18N.getLabel('OBCOMTR_ErrorGettingServeOptions'));
        me.dataReadyFunction(null, args);
      }, args);
    }
  };

  OB.OBPOSPointOfSale.UI.customers.edit_createcustomers_impl.prototype.newAttributes.push(PropertyOptionDefinition);
