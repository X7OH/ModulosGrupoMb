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

import org.apache.log4j.Logger;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftcardReason;
import org.openbravo.service.db.DalConnectionProvider;

public class GCNVGiftcardReasonEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      GiftcardReason.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes
  EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    GiftcardReason reason = (GiftcardReason) event.getTargetInstance();
    validateGiftCardReason(reason);
  }

  public void onSave(@Observes
  EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    GiftcardReason reason = (GiftcardReason) event.getTargetInstance();
    validateGiftCardReason(reason);
  }

  private void validateGiftCardReason(GiftcardReason reason) {
    if (!reason.isOnlyOrg() && !"0".equals(reason.getOrganization().getId())
        && !reason.getOrganization().getOrganizationType().isLegalEntity()) {
      Organization legalEntity = FIN_Utility.getLegalEntityOrg(reason.getOrganization());
      if (legalEntity == null) {
        throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
            "GCNV_MustBeOnlyThisOrg", OBContext.getOBContext().getLanguage().getLanguage()));
      }
    }
  }
}