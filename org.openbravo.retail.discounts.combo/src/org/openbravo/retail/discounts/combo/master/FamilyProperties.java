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
@Qualifier(Family.familyPropertyExtension)
public class FamilyProperties extends ModelExtension {

  public static final Logger log = Logger.getLogger(FamilyProperties.class);

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
      private static final long serialVersionUID = 1L;
      {
        add(new HQLProperty("fp.id", "id"));
        add(new HQLProperty("fp.priceAdjustment.id", "priceAdjustment"));
        add(new HQLProperty("fp.name", "name"));
        add(new HQLProperty("fp.discountType", "discountType"));
        add(new HQLProperty("fp.fixedPrice", "fixedPrice"));
        add(new HQLProperty("fp.percentage", "percentage"));
        add(new HQLProperty("fp.quantity", "quantity"));
        add(new HQLProperty("fp.fixedDiscount", "fixedDiscount"));
        add(new HQLProperty(
            "(case when fp.active = 'Y' and fp.priceAdjustment.active = 'Y' then true else false end)",
            "active"));
        add(new HQLProperty("concat(fp.priceAdjustment.name, ' - ', fp.name)", "_identifier"));
      }
    };
    return list;
  }
}
