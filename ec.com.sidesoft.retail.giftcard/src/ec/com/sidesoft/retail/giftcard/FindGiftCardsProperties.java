/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.retail.giftcard;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;

@Qualifier(FindGiftCards.findGiftCardsPropertyExtension)
public class FindGiftCardsProperties extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
      private static final long serialVersionUID = 1L;
      {
        add(new HQLProperty("gci.id", "id"));
        add(new HQLProperty("gci.searchKey", "searchKey"));
        add(new HQLProperty("gci.alertStatus", "alertStatus"));
        add(new HQLProperty("gci.amount", "amount"));
        add(new HQLProperty("gci.businessPartner.id", "businessPartner"));
        add(new HQLProperty("gci.currentamount", "currentamount"));
        add(new HQLProperty("gci.category.id", "category"));
        add(new HQLProperty("gci.gLItem.id", "gLItem"));
        add(new HQLProperty("gci.product.id", "product"));
        add(new HQLProperty("gci.type", "type"));
      }
    };
    return list;
  }
}