package ec.com.sidesoft.delivery.sales.channel.ad_helpers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;

public class DeliveryHelpers {

	public String getDelivery(String OrgID, String OrgChannel) {
		
		// Configuración de conexión a la base de datos
        ConnectionProvider conn = new DalConnectionProvider(false);
		PreparedStatement st = null;
		String DeliveryId = null;

		try {
			String sql = " SELECT \n"
					+ "    COALESCE(\n"
					+ "        (SELECT sdsch_orgdelivery_sql_id \n"
					+ "         FROM sdsch_orgchaneldelivery_sql \n"
					+ "         WHERE ad_org_id = ? \n"
					+ "           AND sdsch_orgchanel_sql_id = ?),\n"
					+ "        'URB'\n"
					+ "    ) AS resultado;";

			st = conn.getPreparedStatement(sql);
			st.setString(1, OrgID);
			st.setString(2, OrgChannel);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				DeliveryId = rs.getString("resultado");
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

        return DeliveryId;
    }
	
}

