/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.DalConnectionProvider;

public class GCNVProductEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Product.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Product product = (Product) event.getTargetInstance();
    validateGiftCardProduct(product);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Product product = (Product) event.getTargetInstance();
    validateGiftCardProduct(product);
  }

  private void validateGiftCardProduct(Product product) {
    if (StringUtils.equals(product.getGcnvGiftcardtype(), "G")) {
      // Validate exits one taxRate marked as tax exempt
      final OBCriteria<TaxRate> taxRateQuery = OBDal.getInstance().createCriteria(TaxRate.class);
      taxRateQuery.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, product.getTaxCategory()));
      taxRateQuery.add(Restrictions.or(Restrictions.eq(TaxRate.PROPERTY_SALESPURCHASETYPE, "S"),
          Restrictions.eq(TaxRate.PROPERTY_SALESPURCHASETYPE, "B")));
      taxRateQuery.add(Restrictions.eq(TaxRate.PROPERTY_TAXEXEMPT, true));

      if (product.isStocked()) {
        throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
            "GCNV_GiftCardTaxExemptNotStock", OBContext.getOBContext().getLanguage().getLanguage()));
      } else if (taxRateQuery.list().size() == 0) {
        throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
            "GCNV_GiftCardTaxExempt", OBContext.getOBContext().getLanguage().getLanguage()));
      } else if (product.isObposGroupedproduct()) {
        throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
            "GCNV_GiftCardGroupedProduct", OBContext.getOBContext().getLanguage().getLanguage()));
      }
    }
  }
}