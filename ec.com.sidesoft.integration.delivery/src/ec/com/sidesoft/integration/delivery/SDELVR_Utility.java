package ec.com.sidesoft.integration.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.integration.delivery.SDELVRConfig;
import ec.com.sidesoft.integration.delivery.SDELVRLogs;

public class SDELVR_Utility {

	public void saveLogs(String deliveryType, String json, Order order, SDELVRConfig config, Boolean type,
			String description, String Reject, String Accept, String Prepared, String PickedUpURL, String JSOrderID) {

		UUID uuid = UUID.randomUUID();
		String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

		SDELVRLogs log = OBProvider.getInstance().get(SDELVRLogs.class);
		log.setNewOBObject(true);
		log.setId(randomUUIDString);
		log.setClient(config.getClient());
		log.setOrganization(config.getOrganization());
		log.setDeliveryType(deliveryType);
		log.setSpeyaUrlreject(Reject);
		log.setSpeyaUrlprepared(Prepared);
		log.setSpeyaUrlaccept(Accept);
		log.setSpeyaUrlpickedup(PickedUpURL);
		log.setSpeyaListstates("accepted");
		log.setSpeyaPedidosya(JSOrderID);
		log.setJson(json);
		log.setOrder(order);
		if (type) {
			log.setStatus(true);
		} else {
			log.setStatus(false);
		}
		log.setDescription(description);
		log.setCreatedBy(config.getUserContact());
		log.setUpdatedBy(config.getUserContact());
		OBDal.getInstance().save(log);
		OBDal.getInstance().flush();

	}

	public void saveLogs(String deliveryType, String json, Order order, SDELVRConfig config, Boolean type,
			String description) {

		UUID uuid = UUID.randomUUID();
		String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

		SDELVRLogs log = OBProvider.getInstance().get(SDELVRLogs.class);
		log.setNewOBObject(true);
		log.setId(randomUUIDString);
		log.setClient(config.getClient());
		log.setOrganization(config.getOrganization());
		log.setDeliveryType(deliveryType);
		log.setJson(json);
		log.setOrder(order);
		if (type) {
			log.setStatus(true);
		} else {
			log.setStatus(false);
		}
		log.setDescription(description);
		log.setCreatedBy(config.getUserContact());
		log.setUpdatedBy(config.getUserContact());
		OBDal.getInstance().save(log);
		OBDal.getInstance().flush();

	}

	public BigDecimal convertCentsToDollars(String amount, int stdPrecision) {

		BigDecimal operation;
		BigDecimal centsAmount = new BigDecimal(amount);
		BigDecimal factor = new BigDecimal(100);
		operation = centsAmount.divide(factor).setScale(stdPrecision, RoundingMode.HALF_UP);

		return operation;

	}

	public boolean validateIdentification(String taxid) {

		Boolean validate = false;
		Integer sizeTaxID = taxid.length();
		String typeTaxID = "";
		String strResult = "";

		if (taxid.matches("[0-9]+")) {
			if (sizeTaxID == 13) {
				typeTaxID = "R";
				strResult = validate(taxid, typeTaxID);
			} else if (sizeTaxID == 10) {
				typeTaxID = "D";
				strResult = validate(taxid, typeTaxID);
			}
		}

		if (strResult.equals("1")) {
			validate = true;
		}

		return validate;

	}

	private String validate(String taxid, String typeTaxID) {
		ConnectionProvider conn = new DalConnectionProvider(false);
		String strResult = null;

		try {

			String strSql = "SELECT sdelvr_validate_taxid('" + taxid + "','" + typeTaxID + "') as valid FROM DUAL;";
			PreparedStatement st = null;

			st = conn.getPreparedStatement(strSql);
			ResultSet rsConsulta = st.executeQuery();

			while (rsConsulta.next()) {
				strResult = rsConsulta.getString("valid");
			}

			return strResult;

		} catch (Exception e) {
			throw new OBException("Error al consultar CI/RUC valido " + e.getMessage());
		}

	}

}
