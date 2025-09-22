package ec.com.sidesoft.delivery.sales.channel.ad_process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.delivery.sales.channel.SdschOrgChanelSql;
import ec.com.sidesoft.delivery.sales.channel.SdschOrgDeliverySql;
import ec.com.sidesoft.delivery.sales.channel.SdschRptcSql;

public class Sdsch_ConfigChannelsAndDelivery extends DalBaseProcess {
	private final Logger logger = Logger.getLogger(Sdsch_ConfigChannelsAndDelivery.class);

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		OBError msg = new OBError();
		OBContext.setAdminMode(true);
		final String recordId = (String) bundle.getParams().get("Sdsch_Rptc_Sql_ID");

		List<String[]> query1Results = new ArrayList<>();
		List<String[]> query2Results = new ArrayList<>();

		ConnectionProvider con = new DalConnectionProvider(false);
//		VariablesSecureApp varsAux = bundle.getContext().toVars();
//		String StrUser = varsAux.getUser();
//		String StrClient = varsAux.getClient();
//		String StrOrg = varsAux.getOrg();

		try {

//			UpdateSdschOrgChanelSql(con);
//			UpdateSdschOrgDeliverySql(con);

			SdschRptcSql sdschRptcSql = OBDal.getInstance().get(SdschRptcSql.class, recordId);
			String query1 = sdschRptcSql.getSqlscriptchannels();
			String query2 = sdschRptcSql.getSqlscriptdelivery();

			try (PreparedStatement preparedStatement1 = con.getPreparedStatement(query1)) {
				if (query1.trim().toUpperCase().startsWith("SELECT")) {
					try (ResultSet rs1 = preparedStatement1.executeQuery()) {
						while (rs1.next()) {
							query1Results.add(new String[] { rs1.getString("value"), rs1.getString("name"),
									rs1.getString("ad_table_id") });
						}
					}
				} else {
					preparedStatement1.executeUpdate();
				}
			}

			try (PreparedStatement preparedStatement2 = con.getPreparedStatement(query2)) {
				if (query2.trim().toUpperCase().startsWith("SELECT")) {
					try (ResultSet rs2 = preparedStatement2.executeQuery()) {
						while (rs2.next()) {
							query2Results.add(new String[] { rs2.getString("value"), rs2.getString("name"),
									rs2.getString("ad_table_id") });
						}
					}
				} else {
					preparedStatement2.executeUpdate();
				}
			}

			// Guardar resultados de query1 en SdschOrgChanelSql con manejo de errores
			// individual
			for (String[] result : query1Results) {
				boolean exist = false;
				exist = getDelivery(result[0]);
				if(!exist) {
					try {

						OBCriteria<SdschOrgChanelSql> querychanelRecord = OBDal.getInstance()
								.createCriteria(SdschOrgChanelSql.class);
						querychanelRecord.add(Restrictions.eq(SdschOrgChanelSql.PROPERTY_SEARCHKEY, result[0]));
						List<SdschOrgChanelSql> existingRecords = querychanelRecord.list();
						if (existingRecords.isEmpty()) {
							SdschOrgChanelSql chanelRecord = OBProvider.getInstance().get(SdschOrgChanelSql.class);
							chanelRecord.setSearchKey(result[0]);
							chanelRecord.setCommercialName(result[1]);
							chanelRecord.setTable(result[2]);

							OBDal.getInstance().save(chanelRecord);
							logger.info("Registro insertado con searchKey: " + result[0]);
						} else {

							SdschOrgChanelSql existingRecord = existingRecords.get(0);
							existingRecord.setActive(true); // Cambiar a un estado deseado, como "UPDATED"
							OBDal.getInstance().save(existingRecord);
							logger.info(
									"Registro existente con searchKey " + result[0] + " actualizado a estado 'UPDATED'.");

						}

						OBDal.getInstance().flush();

					} catch (Exception e) {
						logger.error("Error al guardar SdschOrgChanelSql para el valor: " + result[0], e);
					}
				}
				
			}

			// Guardar resultados de query2 en SdschOrgDeliverySql con manejo de errores
			// individual
			for (String[] result : query2Results) {
				boolean exist = false;
				exist = getDelivery(result[0]);
				if(!exist) {
					try {

						OBCriteria<SdschOrgDeliverySql> querychanelRecord = OBDal.getInstance()
								.createCriteria(SdschOrgDeliverySql.class);
						querychanelRecord.add(Restrictions.eq(SdschOrgDeliverySql.PROPERTY_SEARCHKEY, result[0]));
						List<SdschOrgDeliverySql> existingRecords = querychanelRecord.list();

						if (existingRecords.isEmpty()) {
							SdschOrgDeliverySql deliveryRecord = OBProvider.getInstance().get(SdschOrgDeliverySql.class);
							deliveryRecord.setSearchKey(result[0]);
							deliveryRecord.setCommercialName(result[1]);
							deliveryRecord.setTable(result[2]);

							OBDal.getInstance().save(deliveryRecord);
							logger.info("Registro insertado con searchKey: " + result[0]);
						} else {

							SdschOrgDeliverySql existingRecord = existingRecords.get(0);
							existingRecord.setActive(true); // Cambiar a un estado deseado, como "UPDATED"
							OBDal.getInstance().save(existingRecord);
							logger.info(
									"Registro existente con searchKey " + result[0] + " actualizado a estado 'UPDATED'.");

						}

						OBDal.getInstance().flush();

					} catch (Exception e) {
						logger.error("Error al guardar SdschOrgDeliverySql para el valor: " + result[0], e);
					}
				}
			}

			msg.setType("Success");
			msg.setMessage("Consultas ejecutadas exitosamente y resultados almacenados.");
		} catch (final Exception e) {
			OBDal.getInstance().rollbackAndClose();
			logger.error("Excepción en Sdsch_ConfigChannelsAndDelivery: ", e);
			Throwable throwable = DbUtility.getUnderlyingSQLException(e);
			msg.setTitle(OBMessageUtils.messageBD("Error"));
			msg.setType("Error");
			msg.setMessage(OBMessageUtils.translateError(throwable.getMessage()).getMessage());
		} finally {
			OBContext.setAdminMode(false);
			bundle.setResult(msg);
		}
	}

	public static String UpdateSdschOrgChanelSql(ConnectionProvider connectionProvider) throws Exception {
		String strSql = "UPDATE sdsch_orgchanel_sql SET Isactive = 'N'";
		String strReturn = null;
		try (PreparedStatement st = connectionProvider.getPreparedStatement(strSql);
				ResultSet result = st.executeQuery()) {
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return strReturn;
	}

	public static String UpdateSdschOrgDeliverySql(ConnectionProvider connectionProvider) throws Exception {
		String strSql = "UPDATE sdsch_orgdelivery_sql SET Isactive = 'N'";
		String strReturn = null;
		try (PreparedStatement st = connectionProvider.getPreparedStatement(strSql);
				ResultSet result = st.executeQuery()) {
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return strReturn;
	}
	
	public boolean getDelivery(String deliveryId) {

		// Configuración de conexión a la base de datos
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
		boolean pkrLogId = false;

		try {
			String sql = " select count(value) as num from sdsch_orgdelivery_sql where value = UPPER(?);";

			st = conn.getPreparedStatement(sql);
			st.setString(1, deliveryId);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				if(rs.getInt("num")!=0) {
					pkrLogId = true;
				}
			}
			rs.close();
			st.close();
		} catch (Exception e) {
			String message = e.getMessage();
		} finally {
			try {
				conn.releasePreparedStatement(st);
				conn.destroy();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}

		return pkrLogId;
	}
	
	public boolean getChannel(String channelId) {

		// Configuración de conexión a la base de datos
		ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
		boolean pkrLogId = false;

		try {
			String sql = " select count(value) as num from sdsch_orgdelivery_sql where value = UPPER(?);";

			st = conn.getPreparedStatement(sql);
			st.setString(1, channelId);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				if(rs.getInt("num")!=0) {
					pkrLogId = true;
				}
			}
			rs.close();
			st.close();
		} catch (Exception e) {
			String message = e.getMessage();
		} finally {
			try {
				conn.releasePreparedStatement(st);
				conn.destroy();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}

		return pkrLogId;
	}

}
