/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

(function () {

  // Add printCard & printTemplate fields to local database
  OB.Model.Product.addProperties([{
    name: 'printCard',
    column: 'em_obpgc_printcard',
    primaryKey: false,
    filter: false,
    type: 'BOOL'
  }, {
    name: 'printTemplate',
    column: 'em_obpgc_printtemplate_id',
    primaryKey: false,
    filter: false,
    type: 'TEXT'
  }, {
    name: 'templateIsPdf',
    column: 'templateIsPdf',
    primaryKey: false,
    filter: false,
    type: 'BOOL'
  }, {
    name: 'templatePrinter',
    column: 'templatePrinter',
    primaryKey: false,
    filter: false,
    type: 'TEXT'
  }, {
    name: 'expirationDays',
    column: 'em_obgcne_expirationDays',
    primaryKey: false,
    filter: false,
    type: 'NUMERIC'
  }]);

}());