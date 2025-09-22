/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone */

(function () {

  var GiftCard = OB.Data.ExtensibleModel.extend({
    modelName: 'GiftCard',
    tableName: 'giftcard',
    entityName: 'GiftCard',
    source: 'org.openbravo.retail.giftcards.master.GiftCard',
    remote: 'OBPOS_remote.product',
    dataLimit: 300
  });

  GiftCard.addProperties([{
    name: 'id',
    column: 'giftcard_id',
    primaryKey: true,
    type: 'TEXT'
  }, {
    name: '_identifier',
    column: '_identifier',
    primaryKey: false,
    type: 'TEXT'
  }, {
    name: 'giftCardType',
    column: 'giftCardType',
    primaryKey: false,
    type: 'TEXT'
  }, {
    name: 'allowPartialReturn',
    column: 'allowPartialReturn',
    primaryKey: false,
    type: 'TEXT'
  }, {
    name: 'amount',
    column: 'amount',
    primaryKey: false,
    type: 'NUMERIC'
  }]);

  var GiftCardList = Backbone.Collection.extend({
    model: GiftCard
  });

  window.OB = window.OB || {};
  window.OB.Model = window.OB.Model || {};
  window.OB.Collection = window.OB.Collection || {};

  window.OB.Model.GiftCard = GiftCard;
  window.OB.Collection.GiftCardList = GiftCardList;

  // add the model to the window.
  OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push(OB.Model.GiftCard);
}());