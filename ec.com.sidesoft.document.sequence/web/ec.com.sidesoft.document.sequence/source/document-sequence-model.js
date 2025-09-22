(function () {

  var InvoiceDocumentSequence = OB.Data.ExtensibleModel.extend({
    modelName: 'InvoiceDocumentSequence',
    tableName: 'ecsds_invoice_doc_seq',
    entityName: 'InvoiceDocumentSequence',
    source: 'ec.com.sidesoft.document.sequence.master.DocumentSequence',
    local: false
  });

  InvoiceDocumentSequence.addProperties([{
    name: 'id',
    column: 'ecsds_invoice_doc_seq_id',
    primaryKey: true,
    type: 'TEXT'
  }, {
    name: 'currentSeq',
    column: 'currentSeq',
    primaryKey: false,
    type: 'NUMERIC'
  }, {
    name: 'identifier',
    column: 'identifier',
    primaryKey: false,
    type: 'TEXT'
  }, {
    name: 'inDevelopment',
    column: 'inDevelopment',
    primaryKey: false,
    type: 'TEXT'
  }, {
    name: 'invoiceSeq',
    column: 'invoiceSeq',
    primaryKey: false,
    type: 'TEXT'
  }, {
    name: 'searchKey',
    column: 'searchKey',
    primaryKey: false,
    type: 'TEXT'
  },{
    name: 'store',
    column: 'store',
    primaryKey: false,
    type: 'TEXT'
  },{
    name: 'RUC',
    column: 'RUC',
    primaryKey: false,
    type: 'TEXT'
  }]);

  OB.Data.Registry.registerModel(InvoiceDocumentSequence);

  OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push(OB.Model.InvoiceDocumentSequence);
}());