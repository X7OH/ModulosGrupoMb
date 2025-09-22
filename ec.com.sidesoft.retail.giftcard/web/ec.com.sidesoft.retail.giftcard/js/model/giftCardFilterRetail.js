(function () {

  OB.Model.GiftCardFilterRetail = OB.Data.ExtensibleModel.extend({
    source: 'ec.com.sidesoft.retail.giftcard.FindGiftCards',
    //remoteDataLimit: OB.Dal.REMOTE_DATALIMIT
    remoteDataLimit: '300'
  });

  OB.Model.GiftCardFilterRetail.addProperties([{
    name: 'id',
    column: 'gci.id',
    primaryKey: true,
    filter: false,
    type: 'TEXT'
  }, {
    name: 'searchKey',
    column: 'gci.searchKey',
    filter: true,
    type: 'TEXT',
    caption: 'GCNV_SearchKey',
    operator: OB.Dal.CONTAINS,
    isFixed: true
  }, {
    name: 'alertStatus',
    column: 'gci.alertStatus',
    filter: true,
    type: 'TEXT',
    caption: 'GCNV_LblStatus',
    operator: OB.Dal.EQ,
    isList: true,
    idList: '03A85444FDE7486CB8BCD6EB88D48C47',
    propertyId: 'id',
    propertyName: 'name'
  }, {
    name: 'businessPartner',
    column: 'gci.businessPartner.id',
    filter: true,
    type: 'TEXT',
    caption: 'GCNV_LblBusinessPartner',
    isSelector: true,
    selectorPopup: 'modalcustomer',
    operator: OB.Dal.EQ,
    preset: {
      id: '',
      name: ''
    }
  }, {
    name: 'amount',
    column: 'gci.amount',
    filter: true,
    type: 'NUMERIC',
    caption: 'GCNV_LblAmount',
    isAmount: true
  }, {
    name: 'currentamount',
    column: 'gci.currentamount',
    filter: true,
    type: 'NUMERIC',
    caption: 'GCNV_LblCurrentAmount',
    isAmount: true
  }, {
    name: 'category',
    column: 'gci.category.id',
    filter: false,
    type: 'TEXT'
  }, {
    name: 'gLItem',
    column: 'gci.gLItem.id',
    filter: false,
    type: 'TEXT'
  }, {
    name: 'product',
    column: 'gci.product.id',
    filter: false,
    type: 'TEXT'
  }, {
    name: 'type',
    column: 'type',
    filter: false,
    type: 'TEXT'
  }]);

}());