package com.sidesoft.hrm.payroll.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.sidesoft.hrm.payroll.sspr_settlement;
import com.sidesoft.hrm.payroll.sspr_settlementdata;

public class SsprSettlementDataEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(sspr_settlementdata.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final sspr_settlementdata line = (sspr_settlementdata) event.getTargetInstance();
    sspr_settlement head = line.getSsprSettlement();
    if (head != null && head.getPosted().equals("Y")) {
      throw new OBException(OBMessageUtils.messageBD("@DocumentPosted@"));
    }
  }
}
