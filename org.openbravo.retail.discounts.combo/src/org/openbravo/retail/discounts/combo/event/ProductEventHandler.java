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

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.retail.discounts.combo.ComboProduct;
import org.openbravo.retail.discounts.combo.ComboProductFamily;

public class ProductEventHandler extends EntityPersistenceEventObserver {
  final private static String strComboDiscountType = "7899A7A4204749AD92881133C4EE7A57";
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ComboProduct.ENTITY_NAME) };

  // private static final Logger log = LoggerFactory.getLogger(ProductEventHandler.class);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ComboProduct product = (ComboProduct) event.getTargetInstance();
    if (!isComboDiscount(product)) {
      return;
    }
    checkDuplicatedProduct(product);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ComboProduct product = (ComboProduct) event.getTargetInstance();
    if (!isComboDiscount(product)) {
      return;
    }
    checkDuplicatedProduct(product);
  }

  private boolean isComboDiscount(ComboProduct product) {
    return strComboDiscountType.equals(product.getObcomboFamily().getPriceAdjustment()
        .getDiscountType().getId());
  }

  // Checks that a product is only defined once in all families of the combo
  private void checkDuplicatedProduct(ComboProduct product) {
    OBCriteria<ComboProduct> critComboProd = OBDal.getInstance().createCriteria(ComboProduct.class);
    critComboProd.createAlias(ComboProduct.PROPERTY_OBCOMBOFAMILY, "fam");
    critComboProd.add(Restrictions.eq("fam." + ComboProductFamily.PROPERTY_PRICEADJUSTMENT, product
        .getObcomboFamily().getPriceAdjustment()));
    critComboProd.add(Restrictions.eq(ComboProduct.PROPERTY_PRODUCT, product.getProduct()));
    critComboProd.add(Restrictions.ne("id", product.getId()));
    // critComboProd.setMaxResults(1);
    if (critComboProd.count() > 0) {
      throw new OBException(OBMessageUtils.messageBD("OBCOMBO_DUPLICATEDPRODUCT"));
    }
  }
}
