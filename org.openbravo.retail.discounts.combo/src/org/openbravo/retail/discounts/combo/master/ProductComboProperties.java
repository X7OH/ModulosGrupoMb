/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.discounts.combo.master;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;

/**
 * @author migueldejuana
 * 
 */
@Qualifier(Product.productComboPropertyExtension)
public class ProductComboProperties extends ModelExtension {

  public static final Logger log = Logger.getLogger(ProductComboProperties.class);

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
      private static final long serialVersionUID = 1L;
      {
        add(new HQLProperty("cp.id", "id"));
        add(new HQLProperty("cp.obcomboFamily.id", "obcomboFamily"));
        add(new HQLProperty("cp.product.id", "product"));
        add(new HQLProperty(
            "(case when cp.active = 'Y' and cp.obcomboFamily.active = 'Y' and  cp.obcomboFamily.priceAdjustment.active = 'Y' then true else false end)",
            "active"));
        add(new HQLProperty(
            "concat(cp.obcomboFamily.priceAdjustment.name, ' - ', cp.obcomboFamily.name, ' - ', cp.product.name)",
            "_identifier"));
      }
    };
    return list;
  }
}
