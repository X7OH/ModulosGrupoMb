/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, $ */

(function () {

  var GiftCard = Backbone.Model.extend({
    dataLimit: 300
  });
  var GiftCardList = Backbone.Collection.extend({
    model: GiftCard
  });

  window.GCNV = window.GCNV || {};
  window.GCNV.Model = window.GCNV.Model || {};
  window.GCNV.Collection = window.GCNV.Collection || {};

  window.GCNV.Model.GiftCard = GiftCard;
  window.GCNV.Collection.GiftCardList = GiftCardList;
}());