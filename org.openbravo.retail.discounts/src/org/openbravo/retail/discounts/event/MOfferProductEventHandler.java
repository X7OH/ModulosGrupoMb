/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.discounts.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.pricing.priceadjustment.Product;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Checks in M_Offer_Product table
 *
 */
public class MOfferProductEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Product.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Product discProduct = (Product) event.getTargetInstance();
    if (discProduct.getPriceAdjustment().getDiscountType().getId()
        .equals("BE5D42E554644B6AA262CCB097753951")
        && discProduct.getProduct().isLinkedToProduct()) {
      throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
          "OBDISC_DiscServicesLinkedProduct", OBContext.getOBContext().getLanguage().getLanguage()));
    }
    if (discProduct.getPriceAdjustment().getDiscountType().getId()
        .equals("BE5D42E554644B6AA262CCB097753951")
        && (discProduct.getObdiscQty() == null || discProduct.getObdiscQty() <= 0)) {
      throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
          "OBDISC_NoQtyDefined", OBContext.getOBContext().getLanguage().getLanguage()));
    }
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Product discProduct = (Product) event.getTargetInstance();
    if (discProduct.getPriceAdjustment().getDiscountType().getId()
        .equals("BE5D42E554644B6AA262CCB097753951")
        && discProduct.getProduct().isLinkedToProduct()) {
      throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
          "OBDISC_DiscServicesLinkedProduct", OBContext.getOBContext().getLanguage().getLanguage()));
    }
    if (discProduct.getPriceAdjustment().getDiscountType().getId()
        .equals("BE5D42E554644B6AA262CCB097753951")
        && (discProduct.getObdiscQty() == null || discProduct.getObdiscQty() <= 0)) {
      throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
          "OBDISC_NoQtyDefined", OBContext.getOBContext().getLanguage().getLanguage()));
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }
}
