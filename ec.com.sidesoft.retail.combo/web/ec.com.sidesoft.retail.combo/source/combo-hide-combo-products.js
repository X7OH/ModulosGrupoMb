//Ocultarlo de la carga de categorías
OB.UI.ListProducts.prototype.loadCategory = function (category) {

  if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
    return;
  }
  var criteria, me = this;

  function errorCallback(tx, error) {
    OB.UTIL.showError("OBDAL error: " + error);
  }

  function successCallbackProducts(dataProducts) {
    var realCategory = category.get('realCategory');
    var filteredDataProducts;
    if (me.destroyed) {
      return;
    }
    if (dataProducts && dataProducts.length > 0) {
      filteredDataProducts = new OB.Collection.ProductList(dataProducts.filter(function (model) {
        if (realCategory === 'Y' || realCategory === undefined) {
          return ((model.get('productType') !== 'S' || !model.get('isLinkedToProduct')) && model.get('sSRCMHideFromWebPOS') === false);
        } else {
          return model.get('productType') !== 'S' || !model.get('isLinkedToProduct');
        }
      }));
      me.products.reset(filteredDataProducts.models);
    } else {
      me.products.reset();
    }
    //      TODO
    me.$.productTable.getHeader().setHeader(category.get('_identifier'));
  }

  if (category) {
    this.currentCategory = category;
    var where = "where ";
    if (category.get('id') === 'OBPOS_bestsellercategory') {
      criteria = {
        'bestseller': 'true'
      };
      where += "p.bestseller = ?";
      if (this.useCharacteristics) {
        criteria.generic_product_id = null;
      }
    } else {
      if (category.get('realCategory') === 'N') {
        criteria = {
          'productCategory': category.get('id')
        };
      } else {
        criteria = {
          'productComboCategory': category.get('id')
        };
      }

      where += "(p.m_product_category_id = ? or p.productComboCategory = ?)";
      if (this.useCharacteristics) {
        criteria.generic_product_id = null;
      }
    }
    criteria._orderByClause = 'upper(_identifier) asc';
    if (OB.MobileApp.model.hasPermission('OBPOS_productLimit', true)) {
      criteria._limit = OB.DEC.abs(OB.MobileApp.model.hasPermission('OBPOS_productLimit', true));
    }
    if (OB.MobileApp.model.hasPermission('EnableMultiPriceList', true) && this.currentPriceList && this.currentPriceList !== OB.MobileApp.model.get('terminal').priceList) {
      var select = "select p.*, pp.pricestd as currentStandardPrice " //
        +
        "from m_product p inner join m_productprice pp on p.m_product_id = pp.m_product_id and pp.m_pricelist_id = ? " //
        +
        where;
      var limit = null;
      if (OB.MobileApp.model.hasPermission('OBPOS_productLimit', true)) {
        limit = OB.DEC.abs(OB.MobileApp.model.hasPermission('OBPOS_productLimit', true));
      }
      OB.Dal.query(OB.Model.Product, select, [this.currentPriceList, category.get('id') === 'OBPOS_bestsellercategory' ? 'true' : category.get('id'), category.get('id')], successCallbackProducts, errorCallback, null, null, limit);
    } else {
      OB.Dal.find(OB.Model.Product, criteria, successCallbackProducts, errorCallback);
    }
  } else {
    this.products.reset();
    this.$.productTable.getHeader().setHeader(OB.I18N.getLabel('OBMOBC_LblNoCategory'));
  }
}

//Ocultarlo de la búsqueda sencilla
OB.UI.SearchProduct.prototype.searchAction = function (inSender, inEvent) {
  var me = this,
    whereClause = 'where ',
    params = [];

  function errorCallback(tx, error) {
    OB.UTIL.showError("OBDAL error: " + error);
  }

  // Initializing combo of categories without filtering

  function successCallbackProducts(dataProducts) {
    var filteredDataProducts;
    if (me.destroyed) {
      return;
    }
    if (dataProducts && dataProducts.length > 0) {

      filteredDataProducts = dataProducts.filter(function (model) {
        return model.get('sSRCMHideFromWebPOS') === false;
      })

      me.products.reset(filteredDataProducts.models);
      me.products.trigger('reset');
    } else {
      OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_NoProductsFound'));
      me.products.reset();
    }
  }

  if (inEvent.productName) {
    inEvent.productName = '%' + inEvent.productName + '%';
    whereClause += '_filter like ?';
    params.push(inEvent.productName);
  } else {
    whereClause += '1 = 1';
  }

  if (inEvent.productCat && inEvent.productCat.indexOf('__all__') === -1) {
    whereClause = whereClause + ' and m_product_category_id ';
    if (inEvent.categoryTree) {
      whereClause = whereClause + ' in (' + inEvent.productCat + ')';
    } else {
      whereClause = whereClause + ' = ?';
      params.push(inEvent.productCat);
    }
  }
  OB.Dal.query(OB.Model.Product, 'select * from m_product ' + whereClause, params, successCallbackProducts, errorCallback);
}

//Ocultarlo de la búsqueda con characteristics
OB.UI.SearchProductCharacteristic.prototype.searchAction = function (inSender, inEvent) {
  this.$.products.hide();
  this.$.renderLoading.show();
  this.params = [];
  this.whereClause = '';

  var criteria = {},
    me = this,
    filterWhereClause = '',
    valuesString = '',
    brandString = '',
    i, j, forceRemote, doLocalIfRemoteFails = true;

  // Disable the filters button
  me.disableFilters(true);

  function errorCallback(tx, error) {
    OB.UTIL.showError("OBDAL error: " + error);
  }

  // Initializing combo of categories without filtering

  function postProccessFilters(index, dataProducts, queryWasExecutedOnline) {
    var filteredDataProducts;
    if (index < OB.UI.SearchProductCharacteristic.postProcessCustomFilters.length) {
      OB.UI.SearchProductCharacteristic.postProcessCustomFilters[index].postProcess(dataProducts, function (queryWasExecutedOnline) {
        postProccessFilters(++index, dataProducts, queryWasExecutedOnline);
      }, queryWasExecutedOnline);
    } else {
      if (_.pluck(me.customFilters, 'filterName').indexOf('ServicesFilter') === -1) {
        filteredDataProducts = new OB.Collection.ProductList(dataProducts.filter(function (model) {
          return (model.get('productType') !== 'S' || !model.get('isLinkedToProduct')) && model.get('sSRCMHideFromWebPOS') === false;
        }));
        me.showProducts(filteredDataProducts);
      } else {
        me.showProducts(dataProducts);
      }
    }
  }
  var synchId = null;

  function successCallbackProductCh(dataProductCh) {

    var filterWhereClause = '',
      characteristicId = '';
    if (dataProductCh && dataProductCh.length > 0) {
      for (i = 0; i < dataProductCh.length; i++) {
        for (j = 0; j < me.model.get('filter').length; j++) {
          characteristicId = dataProductCh.models[i].get('id');
          if (characteristicId === me.model.get('filter')[j].characteristic_id) {
            dataProductCh.models[i].set('filtering', true);
          }
        }
      }
      me.productsCh.reset(dataProductCh.models);
      me.$.renderLoadingCh.hide();
      me.$.productsCh.show();
    } else if (me.model.get('filter').length > 0) {
      if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && !forceRemote) {
        filterWhereClause = ' and prod_ch.m_characteristic_id in (' + "'" + me.model.get('filter')[0].characteristic_id + "'" + ')';
        for (i = 1; i < me.model.get('filter').length; i++) {
          filterWhereClause = filterWhereClause + ' or prod_ch.m_characteristic_id in (' + "'" + me.model.get('filter')[i].characteristic_id + "'" + ')';
        }
        OB.Dal.query(OB.Model.ProductCharacteristicValue, 'select distinct(m_characteristic_id), _identifier from m_product_ch_value as prod_ch where obposFilteronwebpos = "true" and 1=1' + filterWhereClause + ' order by UPPER(_identifier) asc', [], function (dataProdCh) {
          if (dataProdCh && dataProdCh.length > 0) {
            for (i = 0; i < dataProdCh.length; i++) {
              dataProdCh.models[i].set('filtering', true);
            }
            me.productsCh.reset(dataProdCh.models);
            me.$.renderLoadingCh.hide();
            me.$.productsCh.show();
          }
        }, function (error) {
          OB.UTIL.showError("OBDAL error: " + error);
        });
      } else {
        var productFilterText = inSender.$.productFilterText,
          productcategory = inSender.$.productcategory;

        var remoteCriteria = [],
          characteristicParams = "",
          characteristic = [],
          characteristicValue = [],
          brandparams = [];
        var criteria = {},
          characteristicfilter = {},
          brandfilter = {},
          chFilter = {};
        var productText;
        if (productFilterText !== undefined && productcategory !== undefined) {
          if (me.model.get('filter').length > 0) {
            for (i = 0; i < me.model.get('filter').length; i++) {
              if (!characteristic.includes(me.model.get('filter')[i].characteristic_id)) {
                characteristic.push(me.model.get('filter')[i].characteristic_id);
              }
            }
            for (i = 0; i < characteristic.length; i++) {
              for (j = 0; j < me.model.get('filter').length; j++) {
                if (characteristic[i] === me.model.get('filter')[j].characteristic_id) {
                  characteristicValue.push(me.model.get('filter')[j].id);
                }
              }
              if (i > 0) {
                characteristicParams += ";";
              }
              characteristicParams += characteristicValue;
              characteristicValue = [];
            }
          }
          characteristicfilter.columns = [];
          characteristicfilter.operator = OB.Dal.FILTER;
          characteristicfilter.value = me.productCharacteristicFilterQualifier;
          if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
            productText = (OB.MobileApp.model.hasPermission('OBPOS_remote.product' + OB.Dal.USESCONTAINS, true) ? '%' : '') + productFilterText.getValue() + '%';
          } else {
            productText = '%' + productFilterText.getValue() + '%';
          }
          characteristicfilter.params = [productText, productcategory.value, characteristicParams];
          remoteCriteria.push(characteristicfilter);
        }
        if (me.model.get('brandFilter').length > 0) {
          for (i = 0; i < me.model.get('brandFilter').length; i++) {
            brandparams.push(me.model.get('brandFilter')[i].id);
          }
          if (brandparams.length > 0) {
            brandfilter = {
              columns: [],
              operator: OB.Dal.FILTER,
              value: 'BChar_Filter',
              params: [brandparams]
            };
            remoteCriteria.push(brandfilter);
          }
        }
        criteria.hqlCriteria = [];
        me.customFilters.forEach(function (hqlFilter) {
          if (!_.isUndefined(hqlFilter.hqlCriteriaCharacteristics)) {
            var hqlCriteriaFilter = hqlFilter.hqlCriteriaCharacteristics();
            if (!_.isUndefined(hqlCriteriaFilter)) {
              hqlCriteriaFilter.forEach(function (filter) {
                if (filter) {
                  remoteCriteria.push(filter);
                }
              });
            }
          }
        });
        criteria.remoteFilters = remoteCriteria;
        criteria.forceRemote = forceRemote;
        OB.Dal.find(OB.Model.Characteristic, criteria, function (dataProdCh) {
          if (dataProdCh && dataProdCh.length > 0) {
            for (i = 0; i < dataProdCh.length; i++) {
              dataProdCh.models[i].set('filtering', true);
            }
            me.productsCh.reset(dataProdCh.models);
            me.$.renderLoadingCh.hide();
            me.$.productsCh.show();
          }
        }, function (error) {
          OB.UTIL.showError("OBDAL error: " + error);
        });
      }
    } else {
      me.productsCh.reset();
      me.$.renderLoadingCh.hide();
      me.$.productsCh.show();
    }
  }

  function filterProductCharacterisctics() {
    synchId = OB.UTIL.SynchronizationHelper.busyUntilFinishes('filterProductCharacterisctics');
    var productFilterText, brandparams = [],
      remoteCriteria = [],
      characteristic = [],
      characteristicValue = [],
      characteristicParams = "",
      criteria = {},
      characteristicfilter = {},
      brandfilter = {},
      chFilter = {},
      productCategory, productText;

    if (inSender.$.searchProductCharacteristicHeader !== undefined) {
      productFilterText = inSender.$.searchProductCharacteristicHeader.$.productFilterText;
    } else {
      productFilterText = inSender.$.productFilterText;
    }
    productCategory = me.getProductCategoryFilter(forceRemote);

    if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && !forceRemote) {
      var BChar_Filter = "",
        productCharacteristicFilter = "",
        params = [],
        num, brandStr;
      // brand filter
      if (me.model.get('brandFilter').length > 0) {
        num = 0;
        brandStr = "";
        for (i = 0; i < me.model.get('brandFilter').length; i++) {
          if (me.model.get('brandFilter')[i]) {
            num++;
            if (num > 1) {
              brandStr += ',';
            }
            brandStr += "'" + me.model.get('brandFilter')[i].id + "'";
          }
        }
        BChar_Filter = " and ( exists (select 1 from m_product_ch_value pc join m_product mp where pc.m_product_id = mp.m_product_id and ch.id = pc.m_characteristic_id and ( mp.brand in ( " + brandStr + " ) )) ) ";

      }
      // product name and category
      if (productFilterText !== undefined && productCategory !== undefined) {
        if (productFilterText !== "" || productCategory !== "__all__" || productCategory !== "'__all__'") {
          if (productFilterText !== "") {
            params.push("%" + productFilterText.getValue() + "%");
          }
          productCharacteristicFilter = "AND (EXISTS (SELECT 1 FROM m_product_ch_value pchvf, m_product pf WHERE  pchvf.m_product_id = pf.m_product_id AND ch.id = pchvf.m_characteristic_id AND ";
          if ((productCategory === "__all__") || (productCategory === "'__all__'") || (productCategory === "") || (productCategory === "''")) {
            productCharacteristicFilter += " (Upper(pf._filter) LIKE Upper(?)) ";
          } else if (productCategory === "OBPOS_bestsellercategory") {
            productCharacteristicFilter += "pf.bestseller = 'true' AND ( Upper(pf._filter) LIKE Upper(?) ) ";
          } else {
            productCharacteristicFilter += " (Upper(pf._filter) LIKE Upper(?)) AND(pf.m_product_category_id IN ( " + productCategory + " )) ";
          }
        }
      }
      // characteristic filter
      if (me.model.get('filter').length > 0) {
        for (i = 0; i < me.model.get('filter').length; i++) {
          if (!characteristic.includes(me.model.get('filter')[i].characteristic_id)) {
            characteristic.push(me.model.get('filter')[i].characteristic_id);
          }
        }
        for (i = 0; i < characteristic.length; i++) {
          var characteristicsValuesStr = "";
          num = 0;
          for (j = 0; j < me.model.get('filter').length; j++) {
            if (characteristic[i] === me.model.get('filter')[j].characteristic_id) {
              if (num > 0) {
                characteristicsValuesStr += ',';
              }
              characteristicsValuesStr += "'" + me.model.get('filter')[j].id + "'";
              num++;
            }

          }
          productCharacteristicFilter += "AND ( EXISTS (SELECT 1 FROM   m_product_ch_value ppchv WHERE  ppchv.m_product_id = pchvf.m_product_id AND ( ppchv.m_ch_value_id IN (" + characteristicsValuesStr + ") ))  ) ";
        }
      }
      if (productFilterText !== undefined && productCategory !== undefined) {
        productCharacteristicFilter += "))";
      }
      // external modules filter
      var sqlCriteriaFilter = "";
      me.customFilters.forEach(function (sqlFilter) {
        if (!_.isUndefined(sqlFilter.sqlFilterQueryCharacteristics)) {
          var criteriaFilter = sqlFilter.sqlFilterQueryCharacteristics();
          if (criteriaFilter.query !== null) {
            params = params.concat(criteriaFilter.filters);
            sqlCriteriaFilter += criteriaFilter.query;
          }
        }
      });
      OB.UTIL.SynchronizationHelper.finished(synchId, 'filterProductCharacterisctics');
      OB.Dal.query(OB.Model.Characteristic, "select distinct(ch.id), ch._identifier from m_characteristic as ch where 1=1  " + BChar_Filter + productCharacteristicFilter + sqlCriteriaFilter + " order by UPPER(ch._identifier) asc", params, successCallbackProductCh, errorCallback, this);
    } else {
      //brand filter
      if (me.model.get('brandFilter').length > 0) {
        for (i = 0; i < me.model.get('brandFilter').length; i++) {
          if (me.model.get('brandFilter')[i]) {
            brandparams.push(me.model.get('brandFilter')[i].id);
          }
        }
        if (brandparams.length > 0) {
          brandfilter = {
            columns: [],
            operator: OB.Dal.FILTER,
            value: 'BChar_Filter',
            params: [brandparams]
          };
          remoteCriteria.push(brandfilter);
        }
      }
      // product name and category 
      if (productFilterText !== undefined && productCategory) {
        // characteristic filter
        if (me.model.get('filter').length > 0) {
          for (i = 0; i < me.model.get('filter').length; i++) {
            if (!characteristic.includes(me.model.get('filter')[i].characteristic_id)) {
              characteristic.push(me.model.get('filter')[i].characteristic_id);
            }
          }
          for (i = 0; i < characteristic.length; i++) {
            for (j = 0; j < me.model.get('filter').length; j++) {
              if (characteristic[i] === me.model.get('filter')[j].characteristic_id) {
                characteristicValue.push(me.model.get('filter')[j].id);
              }
            }
            if (i > 0) {
              characteristicParams += ";";
            }
            characteristicParams += characteristicValue;
            characteristicValue = [];
          }
        }
        if (!_.isEmpty(productFilterText.getValue()) || (productCategory.value)) {
          var category = inEvent.productCat.indexOf('OBPOS_bestsellercategory') >= 0 ? 'OBPOS_bestsellercategory' : (inEvent.productCat.indexOf('__all__') >= 0 ? '__all__' : [productCategory.value]);
          characteristicfilter.columns = [];
          characteristicfilter.operator = OB.Dal.FILTER;
          characteristicfilter.value = me.productCharacteristicFilterQualifier;
          if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
            productText = (OB.MobileApp.model.hasPermission('OBPOS_remote.product' + OB.Dal.USESCONTAINS, true) ? '%' : '') + productFilterText.getValue() + '%';
          } else {
            productText = '%' + productFilterText.getValue() + '%';
          }
          characteristicfilter.params = [productText, productCategory.filter ? productCategory.params[0] : category, characteristicParams];
          remoteCriteria.push(characteristicfilter);
        }
      }

      //external modules filter
      criteria.hqlCriteria = [];
      me.customFilters.forEach(function (hqlFilter) {
        if (!_.isUndefined(hqlFilter.hqlCriteriaCharacteristics)) {
          var hqlCriteriaFilter = hqlFilter.hqlCriteriaCharacteristics();
          if (!_.isUndefined(hqlCriteriaFilter)) {
            hqlCriteriaFilter.forEach(function (filter) {
              if (filter) {
                remoteCriteria.push(filter);
              }
            });
          }
        }
      });
      criteria.remoteFilters = remoteCriteria;
      criteria.forceRemote = forceRemote;
      OB.UTIL.SynchronizationHelper.finished(synchId, 'filterProductCharacterisctics');
      OB.Dal.find(OB.Model.Characteristic, criteria, successCallbackProductCh, errorCallback);
    }
  }
  var synchIdSearchAction = null;

  function successCallbackProducts(dataProducts, online) {
    postProccessFilters(0, dataProducts, online);
    if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) || forceRemote) {
      OB.UTIL.SynchronizationHelper.finished(synchIdSearchAction, 'searchAction');
      if (!inEvent.skipProductCharacteristic) {
        filterProductCharacterisctics();
      }
    }
    me.disableFilters(false);
  }

  function errorCallbackProducts(tx, error) {
    var message = error ? error.message : undefined;
    OB.UTIL.HookManager.executeHooks('OBPOS_PostFailureRemoteProductSearch', {
      tx: tx,
      error: error,
      message: message,
      doLocalIfRemoteFails: doLocalIfRemoteFails
    }, function (args) {
      var message;
      OB.UTIL.SynchronizationHelper.finished(synchIdSearchAction, 'searchAction');
      me.disableFilters(false);
      message = args.message && args.message.className ? 'OBPOS_ErrorServerGeneric' : args.message;
      if (!error && !OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && doLocalIfRemoteFails) {
        OB.UTIL.showError(OB.I18N.getLabel(message ? message : 'OBMOBC_RemoteConnectionFail'));
        me.doSearchAction({
          categoryTree: me.$.searchProductCharacteristicHeader.categoryTree,
          productCat: me.$.searchProductCharacteristicHeader.getSelectedCategories(),
          productName: me.$.searchProductCharacteristicHeader.$.productFilterText.getValue(),
          filter: me.model.get('filter'),
          skipProduct: false,
          skipProductCharacteristic: false,
          forceOffline: true
        });
      } else {
        if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
          me.doClearAction();
        }
        OB.UTIL.showWarning(OB.I18N.getLabel(message ? message : 'OBMOBC_ConnectionFail'));
        me.$.renderLoading.hide();
        me.$.products.collection.reset();
        me.$.products.show();
      }
    });
  }

  me.whereClause = ' where 1 = 1 ';
  OB.UTIL.HookManager.executeHooks('OBPOS_PreSearchProducts', {
    context: me,
    inEvent: inEvent
  }, function (args) {
    if (args && args.cancelOperation && args.cancelOperation === true) {
      me.disableFilters(false);
      return;
    }
    if (!inEvent.skipProduct && OB.UI.SearchProductCharacteristic.prototype.forceRemote) {
      forceRemote = inEvent.forceOffline ? false : true;
      if (forceRemote) {
        me.customFilters.forEach(function (hqlFilter) {
          if (!_.isUndefined(hqlFilter.hqlCriteria) && !_.isUndefined(hqlFilter.forceRemote)) {
            hqlFilter.hqlCriteria().forEach(function (filter) {
              if (filter !== "" && hqlFilter.forceRemote) {
                doLocalIfRemoteFails = false;
              }
            });
          }
        });
      }
    } else if (!inEvent.skipProduct) {
      forceRemote = false;
      me.customFilters.forEach(function (hqlFilter) {
        if (!_.isUndefined(hqlFilter.hqlCriteria) && !_.isUndefined(hqlFilter.forceRemote)) {
          hqlFilter.hqlCriteria().forEach(function (filter) {
            if (filter !== "" && !forceRemote && hqlFilter.forceRemote) {
              forceRemote = true;
              doLocalIfRemoteFails = false;
            }
          });
        }
      });
    }

    if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && !forceRemote) {
      me.whereClause = me.whereClause + " and isGeneric = 'false'";

      me.addWhereFilter(inEvent);

      if (me.genericParent) {
        me.whereClause = me.whereClause + ' and generic_product_id = ?';
        me.params.push(me.genericParent.get('id'));
      }
      if (me.model.get('filter').length > 0) {
        for (i = 0; i < me.model.get('filter').length; i++) {
          if (i !== 0 && (me.model.get('filter')[i].characteristic_id !== me.model.get('filter')[i - 1].characteristic_id)) {
            filterWhereClause = filterWhereClause + ' and exists (select * from m_product_ch_value as char where m_ch_value_id in (' + valuesString + ') and char.m_product_id = product.m_product_id)';
            valuesString = '';
          }
          if (valuesString !== '') {
            valuesString = valuesString + ', ' + "'" + me.model.get('filter')[i].id + "'";
          } else {
            valuesString = "'" + me.model.get('filter')[i].id + "'";
          }
          if (i === me.model.get('filter').length - 1) { //last iteration
            filterWhereClause = filterWhereClause + ' and exists (select * from m_product_ch_value as char where m_ch_value_id in (' + valuesString + ') and char.m_product_id = product.m_product_id)';
            valuesString = '';
          }
        }
      }
      if (me.model.get('brandFilter').length > 0) {
        for (i = 0; i < me.model.get('brandFilter').length; i++) {
          brandString = brandString + "'" + me.model.get('brandFilter')[i].id + "'";
          if (i !== me.model.get('brandFilter').length - 1) {
            brandString = brandString + ', ';
          }
        }
        filterWhereClause = filterWhereClause + ' and product.brand in (' + brandString + ')';
      }
      // Add custom parameters
      var customParams = [];
      me.params.forEach(function (param) {
        customParams.push(param);
      });
      if (!inEvent.skipProduct) {
        // Add custom filters
        me.customFilters.forEach(function (filter) {
          var sqlFilter = filter.sqlFilter();
          if (sqlFilter && sqlFilter.where) {
            filterWhereClause = filterWhereClause + sqlFilter.where;
            if (sqlFilter.filters && sqlFilter.filters.length > 0) {
              sqlFilter.filters.forEach(function (item) {
                customParams.push(item);
              });
            }
          }
        });
        var limit = null;
        if (OB.MobileApp.model.hasPermission('OBPOS_productLimit', true)) {
          limit = OB.DEC.abs(OB.MobileApp.model.hasPermission('OBPOS_productLimit', true));
        }
        if (OB.MobileApp.model.hasPermission('EnableMultiPriceList', true) && me.currentPriceList && me.currentPriceList !== OB.MobileApp.model.get('terminal').priceList) {
          var select = "select product. * , pp.pricestd as currentStandardPrice " //
            +
            " from m_product product join m_productprice pp on product.m_product_id = pp.m_product_id and pp.m_pricelist_id = '" + me.currentPriceList + "'" //
            +
            me.whereClause + filterWhereClause;
          OB.Dal.query(OB.Model.Product, select, customParams, function (dataProducts) {
            successCallbackProducts(dataProducts, false);
          }, errorCallbackProducts, me, null, limit);
        } else {
          OB.Dal.query(OB.Model.Product, 'select * from m_product as product' + me.whereClause + filterWhereClause, customParams, function (dataProducts) {
            successCallbackProducts(dataProducts, false);
          }, errorCallbackProducts, me, null, limit);
        }
      } else {
        me.$.renderLoading.hide();
        me.$.products.show();
        me.disableFilters(false);
      }
      if (!inEvent.skipProductCharacteristic) {
        me.$.renderLoadingCh.show();
        me.$.productsCh.hide();
        filterProductCharacterisctics();
      }
    } else {
      var characteristicparams = [],
        brandparams = [],
        selectedcharacteristic = [];

      if (me.model.get('filter').length > 0) {
        for (i = 0; i < me.model.get('filter').length; i++) {
          if (me.model.get('filter')[i].characteristic_id) {
            selectedcharacteristic.push(me.model.get('filter')[i].characteristic_id);
          }
        }
      }
      if (!inEvent.skipProduct) {
        if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) || forceRemote) {
          synchIdSearchAction = OB.UTIL.SynchronizationHelper.busyUntilFinishes('searchAction');
        }
        var remoteCriteria = [],
          characteristic = [],
          characteristicValue = [];
        var brandfilter = {},
          productCategory = me.getProductCategoryFilter(forceRemote),
          characteristicfilter = {
            columns: [],
            operator: OB.Dal.FILTER,
            value: 'Characteristic_Filter',
            params: [characteristicparams.toString()]
          },
          productName = {
            columns: ['_filter'],
            operator: OB.Dal.STARTSWITH,
            value: inEvent.productName
          },
          ispack = {
            columns: ['ispack'],
            operator: 'equals',
            value: 'false',
            fieldType: 'forceString'
          };

        if (inEvent.productCat !== "'__all__'") {
          if (inEvent.productCat.indexOf('OBPOS_bestsellercategory') !== -1) {
            var bestsellers = {
              columns: ['bestseller'],
              operator: 'equals',
              value: true,
              boolean: true
            };
            productCategory = null;
            remoteCriteria.push(bestsellers);
          } else if (inEvent.productCat.indexOf("'") === 0) {
            //TODO: improve the way packs and combos are handled
            if (inEvent.productCat.indexOf('7899A7A4204749AD92881133C4EE7A57') === -1 && inEvent.productCat.indexOf('BE5D42E554644B6AA262CCB097753951') === -1) {
              remoteCriteria.push(ispack);
            }
          }
        }
        if (me.model.get('brandFilter').length > 0) {
          for (i = 0; i < me.model.get('brandFilter').length; i++) {
            brandparams.push(me.model.get('brandFilter')[i].id);
          }
          if (brandparams.length > 0) {
            brandfilter = {
              columns: [],
              operator: OB.Dal.FILTER,
              value: 'Brand_Filter',
              params: [brandparams]
            };
            remoteCriteria.push(brandfilter);
          }
        }
        if (me.model.get('filter').length > 0) {
          for (i = 0; i < me.model.get('filter').length; i++) {
            if (!characteristic.includes(me.model.get('filter')[i].characteristic_id)) {
              characteristic.push(me.model.get('filter')[i].characteristic_id);
            }
          }
          for (i = 0; i < characteristic.length; i++) {
            for (j = 0; j < me.model.get('filter').length; j++) {
              if (characteristic[i] === me.model.get('filter')[j].characteristic_id) {
                characteristicValue.push(me.model.get('filter')[j].id);
              }
            }
            if (characteristicValue.length > 0) {
              characteristicfilter = {
                columns: [],
                operator: OB.Dal.FILTER,
                value: 'Characteristic_Filter',
                filter: characteristic[i],
                params: [characteristicValue]
              };
              remoteCriteria.push(characteristicfilter);
              characteristicValue = [];
            }
          }
        }
        if (me.model.get('filter').length > 0) {
          remoteCriteria.push(ispack);
        }
        if (me.model.get('brandFilter').length > 0) {
          remoteCriteria.push(ispack);
        }
        criteria.hqlCriteria = [];
        me.customFilters.forEach(function (hqlFilter) {
          if (!_.isUndefined(hqlFilter.hqlCriteria)) {
            var hqlCriteriaFilter = hqlFilter.hqlCriteria();
            if (hqlCriteriaFilter) {
              hqlCriteriaFilter.forEach(function (filter) {
                if (filter) {
                  remoteCriteria.push(filter);
                }
              });
            }
          }
        });
        if (productCategory) {
          remoteCriteria.push(productCategory);
        }
        remoteCriteria.push(productName);
        criteria.remoteFilters = remoteCriteria;
        criteria.forceRemote = forceRemote;
        if (OB.MobileApp.model.hasPermission('OBPOS_productLimit', true)) {
          criteria._limit = OB.DEC.abs(OB.MobileApp.model.hasPermission('OBPOS_productLimit', true));
        }
        if (OB.MobileApp.model.hasPermission('EnableMultiPriceList', true) && me.currentPriceList && me.currentPriceList !== OB.MobileApp.model.get('terminal').priceList) {
          var currentPriceList = {
            'currentPriceList': me.currentPriceList
          };
          criteria.remoteParams = currentPriceList;
          OB.Dal.find(OB.Model.Product, criteria, function (dataProducts) {
            successCallbackProducts(dataProducts, true);
          }, errorCallbackProducts, me);

        } else {
          OB.Dal.find(OB.Model.Product, criteria, function (dataProducts) {
            successCallbackProducts(dataProducts, true);
          }, errorCallbackProducts, me);
        }
      } else {
        me.$.renderLoading.hide();
        me.$.products.show();
        me.disableFilters(false);
      }
      // when (!inEvent.skipProduct) filterProductCharacterisctics  funtion is called in successCallbackProducts
      if (!inEvent.skipProductCharacteristic && inEvent.skipProduct) {
        me.$.products.collection.reset();
        filterProductCharacterisctics();
      }
    }
  });
}

//ocultarlo de la búsqueda de código de barras
OB.UTIL.HookManager.registerHook('OBPOS_BarcodeSearch', function (args, c) {
  args.dataProducts = args.dataProducts.filter(function (model) {
    return model.get('sSRCMHideFromWebPOS') === false;
  })

  OB.UTIL.HookManager.callbackExecutor(args, c);
});