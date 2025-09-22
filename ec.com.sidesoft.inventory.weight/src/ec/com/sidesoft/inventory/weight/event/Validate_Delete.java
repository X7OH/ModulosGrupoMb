package ec.com.sidesoft.inventory.weight.event;

import java.util.List;

import javax.enterprise.event.Observes;
import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import ec.com.sidesoft.inventory.weight.SinwWeights;

public class Validate_Delete extends EntityPersistenceEventObserver {

	private static Entity[] entities = { ModelProvider.getInstance().getEntity(
			ShipmentInOutLine.ENTITY_NAME) };

	@Override
	protected Entity[] getObservedEntities() {
		return entities;
	}

	public void onDelete(@Observes EntityDeleteEvent event)
			throws ServletException {
		if (!isValidEvent(event)) {
			return;
		}

		final ShipmentInOutLine objInOutLine = (ShipmentInOutLine) event.getTargetInstance();
		SearchLines(objInOutLine);
	}

	private void SearchLines(ShipmentInOutLine objInOutLine) {
		try {
			ShipmentInOut objInoutLine = OBDal.getInstance().get(ShipmentInOut.class,objInOutLine.getShipmentReceipt().getId());
			
			if(!objInoutLine.isSalesTransaction()){

				List<SinwWeights> lstWeights = objInOutLine.getSinwWeightList();
				
				if (lstWeights.size() >0){
					throw new OBException(
							"Existen regitros en la solapa Pesos de la línea "+ objInOutLine.getLineNo());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new OBException(
					"Error al consultar solapa de Pesos. "
							+ e.getMessage());
		}

	}


}
