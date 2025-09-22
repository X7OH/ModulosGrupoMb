package ec.com.sidesoft.localization.ecuador.resupply.businessevent;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.localization.ecuador.resupply.ssrsresupply;

public class Ssrs_ValideLineResupply extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ssrsresupply.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final ssrsresupply be_ressuply = (ssrsresupply) event.getTargetInstance();

    int countLines = 0;

    countLines = be_ressuply.getSsrsResupplylineList().size();

    if (countLines > 0 && be_ressuply.getDocumentStatus().equals("DR")) {
      ConnectionProvider conn = new DalConnectionProvider(false);
      String language = OBContext.getOBContext().getLanguage().getLanguage();

      throw new OBException(Utility.messageBD(conn, "Ssrs_ErrorDeleteLines", language));

    }

  }

}
