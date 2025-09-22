/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.discounts.bytotal.master;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.mobile.core.CoreAppCacheResourceProvider;

public class DiscountsByTotalAppCacheProvider implements CoreAppCacheResourceProvider {

  @Override
  public List<String> getResources() {

    return new ArrayList<String>() {
      private static final long serialVersionUID = 1L;
      {
        add("../../org.openbravo.mobile.core/OBMOBC_Main/ClientModel?entity=DISCT_FREEPRODUCT&modelName=FreeProduct&source=org.openbravo.retail.discounts.bytotal.master.FreeProduct");
      }
    };
  }

}
