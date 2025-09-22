package ec.com.sidesoft.inventory.weight.event;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.servlet.ServletException;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.plm.ProductAUM;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.inventory.weight.SinwWeights;

public class Total_Weight extends EntityPersistenceEventObserver {

	private static Entity[] entities = { ModelProvider.getInstance().getEntity(
			SinwWeights.ENTITY_NAME) };

	@Override
	protected Entity[] getObservedEntities() {
		return entities;
	}

	public void onSave(@Observes EntityNewEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
		final SinwWeights weights = (SinwWeights) event.getTargetInstance();
		ShipmentInOut objInoutLine = OBDal.getInstance().get(
				ShipmentInOut.class,
				weights.getGoodsShipmentLine().getShipmentReceipt().getId());
		if (!objInoutLine.isSalesTransaction()) {
			SinwWeights newWeights = CalculateWeights(weights, event, null, "I");
			CalculateWeightsAll(weights, newWeights, "I");
		}
	}

	public void onUpdate(@Observes EntityUpdateEvent event)
			throws ServletException {
		if (!isValidEvent(event)) {
			return;
		}
		final SinwWeights weights = (SinwWeights) event.getTargetInstance();
		ShipmentInOut objInoutLine = OBDal.getInstance().get(
				ShipmentInOut.class,
				weights.getGoodsShipmentLine().getShipmentReceipt().getId());
		if (!objInoutLine.isSalesTransaction()) {
			SinwWeights newWeights = CalculateWeights(weights, null, event, "U");
			CalculateWeightsAll(weights, newWeights, "U");
		}

	}

	public void onDelete(@Observes EntityDeleteEvent event)
			throws ServletException {
		if (!isValidEvent(event)) {
			return;
		}

		final SinwWeights weights = (SinwWeights) event.getTargetInstance();
		ShipmentInOut objInoutLine = OBDal.getInstance().get(
				ShipmentInOut.class,
				weights.getGoodsShipmentLine().getShipmentReceipt().getId());
		if (!objInoutLine.isSalesTransaction()) {
			CalculateWeightsAll(weights, weights, "D");
		}
	}

	private SinwWeights CalculateWeights(SinwWeights weights,
			EntityNewEvent event1, EntityUpdateEvent event2, String strClave) {
		try {

			if (weights.getGoodsShipmentLine().getProduct().getSinwTares()== null) {
				throw new OBException(
						"Tara no configurada en el producto.");
			}

			if (weights.getWeightWithTare().compareTo(BigDecimal.ZERO) < 0) {
				throw new OBException(
						"Solamente se permiten valores positivos.");
			}

			if (weights.getGoodsShipmentLine().getProduct().getSinwTares().getWeight().compareTo(weights.getWeightWithTare()) >= 0) {
				throw new OBException(
						"El peso con tara debe ser mayor al peso de la tara.");
			}
			BigDecimal bd_peso_tara = weights.getGoodsShipmentLine()
					.getProduct().getSinwTares().getWeight();
			BigDecimal bd_peso_neto = weights.getWeightWithTare().subtract(
					bd_peso_tara);

			BigDecimal bd_peso_total_previo = SelectPreviousLine(weights);

			BigDecimal bd_peso_total_nuevo = bd_peso_total_previo
					.add(bd_peso_neto);

			weights.setWeightTare(bd_peso_tara);
			weights.setNetWeight(bd_peso_neto);
			weights.setTotalWeight(bd_peso_total_nuevo);

			final Entity WeightEntity = ModelProvider.getInstance().getEntity(
					SinwWeights.ENTITY_NAME);
			final Property prWeightTare = WeightEntity
					.getProperty(SinwWeights.PROPERTY_WEIGHTTARE);
			final Property prNetWeight = WeightEntity
					.getProperty(SinwWeights.PROPERTY_NETWEIGHT);
			final Property prTotalWeight = WeightEntity
					.getProperty(SinwWeights.PROPERTY_TOTALWEIGHT);
			if (event1 != null && strClave.equals("I")) {
				event1.setCurrentState(prWeightTare, bd_peso_tara);
				event1.setCurrentState(prNetWeight, bd_peso_neto);
				event1.setCurrentState(prTotalWeight, bd_peso_total_nuevo);
			} else {
				event2.setCurrentState(prWeightTare, bd_peso_tara);
				event2.setCurrentState(prNetWeight, bd_peso_neto);
				event2.setCurrentState(prTotalWeight, bd_peso_total_nuevo);
			}

			return weights;
		} catch (Exception e) {
			e.printStackTrace();
			throw new OBException(
					"Error al actualizar los valores del registro. "
							+ e.getMessage());
		}

	}

	private void CalculateWeightsAll(SinwWeights weights,
			SinwWeights newWeights, String strClave) {
		try {
			OBCriteria<SinwWeights> ObjWeights = OBDal.getInstance()
					.createCriteria(SinwWeights.class);
			ObjWeights.add(Restrictions.eq("goodsShipmentLine",
					weights.getGoodsShipmentLine()));

			ObjWeights.addOrder(Order.asc("lineNo"));

			List<SinwWeights> lstWeights = ObjWeights.list();
			if (strClave.equals("I")) {
				lstWeights.add(newWeights);

			} else if (strClave.equals("U")) {
				int intContador = 0;
				for (SinwWeights detalleObj1 : lstWeights) {
					if (detalleObj1.getId() == newWeights.getId()) {
						lstWeights.set(intContador, detalleObj1);
						break;
					}
					intContador++;
				}
			} else if (strClave.equals("D")) {
				int intContador1 = 0;
				for (SinwWeights detalleObj2 : lstWeights) {
					if (detalleObj2.getId() == newWeights.getId()) {
						lstWeights.remove(intContador1);
						break;
					}
					intContador1++;
				}
			}
			// ORDENAR LISTA
			Collections.sort(lstWeights, new Comparator<SinwWeights>() {
				@Override
				public int compare(SinwWeights o1, SinwWeights o2) {
					return o1.getLineNo().compareTo(o2.getLineNo());
				}
			});

			BigDecimal bg_total_weight = new BigDecimal(0);
			if (strClave.equals("D") || strClave.equals("U")) {
				for (SinwWeights detalleObj : lstWeights) {

					bg_total_weight = bg_total_weight.add(detalleObj
							.getNetWeight());
					Updatestatus(bg_total_weight, detalleObj.getId());

				}
			} else {
				bg_total_weight = newWeights.getTotalWeight();
			}
			
			OBCriteria<ProductAUM> objAlternateAUM = OBDal.getInstance()
					.createCriteria(ProductAUM.class);
			objAlternateAUM.add(Restrictions.eq("product",
					weights.getGoodsShipmentLine().getProduct()));

			ProductAUM  objAUM= null; 
			objAUM = (ProductAUM) objAlternateAUM.uniqueResult();

			BigDecimal bg_operativeQuantity = new BigDecimal(0);
			if(objAUM == null){
				bg_operativeQuantity = bg_total_weight;
			}else{
				bg_operativeQuantity = bg_total_weight.divide(objAUM.getConversionRate());
			}
			
			
			ShipmentInOutLine objInoutLine = OBDal.getInstance().get(
					ShipmentInOutLine.class,
					weights.getGoodsShipmentLine().getId());
			objInoutLine.setMovementQuantity(bg_total_weight);
			objInoutLine.setOperativeQuantity(bg_operativeQuantity);
			
			

		} catch (Exception e) {
			e.printStackTrace();
			throw new OBException("Error al actualizar los valores totales. "
					+ e.getMessage());
		}

	}

	public static BigDecimal SelectPreviousLine(SinwWeights weights) {
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			String strSql = "SELECT coalesce(total_weight,0) as totallinea FROM  sinw_weight WHERE m_inoutline_id = ? AND line < ? ORDER BY line desc";
			PreparedStatement st = null;
			BigDecimal bdParametro = new BigDecimal(0);
			st = conn.getPreparedStatement(strSql);
			st.setString(1, weights.getGoodsShipmentLine().getId());
			st.setDouble(2, weights.getLineNo());
			ResultSet rsConsulta = st.executeQuery();
			int contador = 0;
			while (rsConsulta.next()) {
				contador = contador + 1;
				if (contador == 1) {
					bdParametro = rsConsulta.getBigDecimal("totallinea");
					break;
				}
			}
			return bdParametro;
		} catch (Exception e) {

			throw new OBException(
					"Error al consultar la tabla c_invoiceline (Referencia Albarán) "
							+ e);
		} finally {
			try {
				conn.destroy();
			} catch (Exception e) {

			}
		}

	}

	public static BigDecimal Updatestatus(BigDecimal bdTotal, String strID) {
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			String strSql = "UPDATE sinw_weight set total_weight = ? where sinw_weight_id = ?";
			PreparedStatement st = null;
			BigDecimal bdParametro = new BigDecimal(0);
			st = conn.getPreparedStatement(strSql);
			st.setBigDecimal(1, bdTotal);
			st.setString(2, strID);
			st.executeUpdate();

			return bdParametro;
		} catch (Exception e) {

			throw new OBException(
					"Error al ejecutar actualización en la tabla sinw_weight. "
							+ e);
		} finally {
			try {
				conn.destroy();
			} catch (Exception e) {

			}
		}

	}

}
