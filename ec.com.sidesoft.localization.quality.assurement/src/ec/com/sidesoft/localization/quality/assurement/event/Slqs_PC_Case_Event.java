package ec.com.sidesoft.localization.quality.assurement.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.manufacturing.quality.Case;

public class Slqs_PC_Case_Event extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Case.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Case objCase = (Case) event.getTargetInstance();

    if (objCase.getSlqsDocumentType() != null) {

      String seqnumber = objCase.getSlqsDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity entityCase = ModelProvider.getInstance().getEntity(Case.ENTITY_NAME);
        final Property valueProperty = entityCase.getProperty(Case.PROPERTY_SLQSDOCUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }

      Sequence sequence = objCase.getSlqsDocumentType().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
    }

  }
}