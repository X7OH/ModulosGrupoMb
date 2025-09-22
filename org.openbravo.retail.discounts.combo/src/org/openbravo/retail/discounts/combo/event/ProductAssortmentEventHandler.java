/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.discounts.combo.event;

import javax.enterprise.event.Observes;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.retail.config.OBRETCOProlProduct;
import org.openbravo.retail.discounts.combo.ComboProduct;

public class ProductAssortmentEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      OBRETCOProlProduct.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final OBRETCOProlProduct assortmentProduct = (OBRETCOProlProduct) event.getTargetInstance();

    OBCriteria<ComboProduct> critComboProd = OBDal.getInstance().createCriteria(ComboProduct.class);
    critComboProd
        .add(Restrictions.eq(ComboProduct.PROPERTY_PRODUCT, assortmentProduct.getProduct()));
    critComboProd.setMaxResults(1);

    ScrollableResults scrollableResults = critComboProd.scroll(ScrollMode.FORWARD_ONLY);
    if (scrollableResults.next()) {
      throw new OBException(OBMessageUtils.messageBD("OBCOMBO_DELETEPRODUCT_ASSORTMENT"));
    }
  }
}