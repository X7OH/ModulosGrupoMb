/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.giftcards.master;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;

@Qualifier(GiftCard.giftCardPropertyExtension)
public class GiftCardProperties extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
      private static final long serialVersionUID = 1L;
      {
        add(new HQLProperty("p.id", "id"));
        add(new HQLProperty("p.name", "_identifier"));
        add(new HQLProperty("p.gcnvGiftcardtype", "giftCardType"));
        add(new HQLProperty("p.gcnvAllowpartialreturn", "allowPartialReturn"));
        add(new HQLProperty("p.gcnvAmount", "amount"));
        add(new HQLProperty("p.active", "active"));
      }
    };
    return list;
  }

}
