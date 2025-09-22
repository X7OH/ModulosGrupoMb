/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, enyo, _ */


OB.Data.Registry.registerModel(Backbone.Model.extend({
  modelName: 'FreeProduct',
  generatedStructure: true,
  entityName: 'DISCT_FREEPRODUCT',
  source: 'org.openbravo.retail.discounts.bytotal.master.FreeProduct'
}));

OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push({
  modelName: 'FreeProduct',
  generatedModel: true
});