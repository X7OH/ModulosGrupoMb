package ec.com.sidesoft.payroll.events.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.ConceptAmount;

import ec.com.sidesoft.payroll.events.SPEVDetailNews;

public class DeleteUpdateEventValidate extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ConceptAmount.ENTITY_NAME) };
  private static final Logger logger = LogManager.getLogger(ConceptAmount.ENTITY_NAME);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    final ConceptAmount conceptamount = (ConceptAmount) event.getTargetInstance();
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    OBCriteria<SPEVDetailNews> obcSPEVdetail = OBDal.getInstance()
        .createCriteria(SPEVDetailNews.class);
    obcSPEVdetail.add(Restrictions.eq(SPEVDetailNews.PROPERTY_PERIOD, conceptamount.getPeriod()));
    obcSPEVdetail
        .add(Restrictions.eq(SPEVDetailNews.PROPERTY_SSPRCONCEPT, conceptamount.getSsprConcept()));
    obcSPEVdetail
        .add(Restrictions.eq(SPEVDetailNews.PROPERTY_BPARTNER, conceptamount.getBusinessPartner()));
    obcSPEVdetail.add(Restrictions.eq(SPEVDetailNews.PROPERTY_TYPE, "PR"));
    if (obcSPEVdetail.count() > 0) {
      throw new OBException(Utility.messageBD(conn,
          "Existe un evento procesado relacionado a este concepto", language));
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    final ConceptAmount conceptamount = (ConceptAmount) event.getTargetInstance();
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    OBCriteria<SPEVDetailNews> obcSPEVdetail = OBDal.getInstance()
        .createCriteria(SPEVDetailNews.class);
    obcSPEVdetail.add(Restrictions.eq(SPEVDetailNews.PROPERTY_PERIOD, conceptamount.getPeriod()));
    obcSPEVdetail
        .add(Restrictions.eq(SPEVDetailNews.PROPERTY_SSPRCONCEPT, conceptamount.getSsprConcept()));
    obcSPEVdetail
        .add(Restrictions.eq(SPEVDetailNews.PROPERTY_BPARTNER, conceptamount.getBusinessPartner()));
    obcSPEVdetail.add(Restrictions.eq(SPEVDetailNews.PROPERTY_TYPE, "PR"));
    if (obcSPEVdetail.count() > 0) {
      throw new OBException(Utility.messageBD(conn,
          "Existe un evento procesado relacionado a este concepto", language));
    }
  }

}
