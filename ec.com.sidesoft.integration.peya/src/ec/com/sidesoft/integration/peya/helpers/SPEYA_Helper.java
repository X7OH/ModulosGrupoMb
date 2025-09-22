package ec.com.sidesoft.integration.peya.helpers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

import ec.com.sidesoft.integration.delivery.SDELVRConfig;

abstract public class SPEYA_Helper {

	static public SDELVRConfig getConfig(String peyaStore) {
		SDELVRConfig config = null;

		OBCriteria<SDELVRConfig> cfgCrt = OBDal.getInstance().createCriteria(SDELVRConfig.class);
		cfgCrt.add(Restrictions.eq(SDELVRConfig.PROPERTY_SPEYASTORE, peyaStore));
		config = (SDELVRConfig) cfgCrt.uniqueResult();

		return config;
	}

	public static SDELVRConfig getConfigPEYA() {
		SDELVRConfig config = null;

		OBCriteria<SDELVRConfig> obCriteria = OBDal.getInstance().createCriteria(SDELVRConfig.class);
		obCriteria.add(Restrictions.isNotNull("speyaClientSecret"));
		obCriteria.add(Restrictions.isNotNull("speyaClientid"));

		if (obCriteria.list().size() > 0) {
			config = obCriteria.list().get(0);
		}

		return config;
	}

	static public String getGrandTotal(String order_id) {
		ConnectionProvider conn = new DalConnectionProvider(false);
		String strResult = null;
		try {

			String strSql = "SELECT grandtotal FROM c_order WHERE c_order_id = '" + order_id + "'";
			PreparedStatement st = null;

			st = conn.getPreparedStatement(strSql);
			ResultSet rsConsulta = st.executeQuery();

			while (rsConsulta.next()) {
				strResult = rsConsulta.getString("grandtotal");
			}

			return strResult;

		} catch (Exception e) {
			throw new OBException("Error al consultar el grandtotal del pedido. " + e.getMessage());
		}

	}

	static public BusinessPartner getBpartner(String taxID) {
		BusinessPartner bp = null;

		OBCriteria<BusinessPartner> cfgCrt = OBDal.getInstance().createCriteria(BusinessPartner.class);
		cfgCrt.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, taxID));
		bp = (BusinessPartner) cfgCrt.uniqueResult();

		return bp;
	}

	static public BusinessPartner createBpartner(String taxID, String direccion, String nombre, String telefono,
			String referencia, String correo, FIN_PaymentMethod payMet, SDELVRConfig utilConfig) {

		BusinessPartner bp = null;
		String typeTaxID;
		ConnectionProvider connectionProvider = new DalConnectionProvider(false);

		UUID uuid = UUID.randomUUID();
		String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

		Integer sizeTaxID = taxID.length();
		// SI LA CEDULA QUE ENVIAN ES SOLO NUMEROS
		if (taxID.matches("[0-9]+")) {

			if (sizeTaxID == 13) {
				typeTaxID = "R";
			} else if (sizeTaxID == 10) {
				typeTaxID = "D";
			} else {
				typeTaxID = "P";
			}

		} else {
			typeTaxID = "P";
		}

		String strSqlBPartner = null;

		strSqlBPartner = "INSERT INTO c_bpartner(\n"
				+ "            c_bpartner_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, VALUE, NAME, NAME2, TAXID,  \n"
				+ "            EM_SSWH_TAXPAYER_ID, EM_SSWH_TAXIDTYPE, C_BP_GROUP_ID, AD_LANGUAGE,"
				+ "            m_pricelist_id,BP_Currency_ID, EM_EEI_Eeioice, EM_Gcnv_Uniquecreditnote,"
				+ "            EM_EEI_Email,EM_Eei_Portal_Pass, C_PaymentTerm_ID, FIN_Paymentmethod_ID)\n"
				+ "    VALUES (?, ?, ?, 'Y', \n" + "             NOW(), ?, NOW(), ?, ?, ?, ?, ?, \n"
				+ "             ?, ?, ?, ?, ?, ?, 'Y', 'Y', ?, ?, ?, ?)";

		int updateCount = 0;
		PreparedStatement st = null;

		try {
			st = connectionProvider.getPreparedStatement(strSqlBPartner);

			st.setString(1, randomUUIDString);
			st.setString(2, utilConfig.getClient().getId());
			st.setString(3, "0");
			st.setString(4, utilConfig.getUserContact().getId());
			st.setString(5, utilConfig.getUserContact().getId());
			st.setString(6, taxID);
			st.setString(7, nombre.toUpperCase());
			st.setString(8, nombre.toUpperCase());
			st.setString(9, taxID);
			st.setString(10, utilConfig.getSswhTaxpayer().getId());
			st.setString(11, typeTaxID);
			st.setString(12, utilConfig.getBusinessPartnerCategory().getId());
			st.setString(13, utilConfig.getLanguage().getLanguage());
			st.setString(14, utilConfig.getPriceList().getId());
			st.setString(15, utilConfig.getCurrency().getId());
			st.setString(16, correo);
			st.setString(17, taxID);
			st.setString(18, utilConfig.getOrganization().getObretcoDbpPtermid().getId());
			st.setString(19, utilConfig.getOrganization().getObretcoDbpPmethodid().getId());

			updateCount = st.executeUpdate();
			if (updateCount > 0) {
				createLocGeo(randomUUIDString, direccion, telefono, referencia, nombre.toUpperCase(), correo,
						utilConfig);
				OBCriteria<BusinessPartner> cfgCrt = OBDal.getInstance().createCriteria(BusinessPartner.class);
				cfgCrt.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, taxID));
				bp = (BusinessPartner) cfgCrt.uniqueResult();
			} else {
				System.out.println("TERCERO NO INSERTADO.");
			}
			st.close();
		} catch (Exception ex) {
			String errorMsg = null;
			System.out.println(ex.getMessage());
			OBDal.getInstance().rollbackAndClose();
			Throwable e = DbUtility.getUnderlyingSQLException(ex);
			if (ex.getMessage() != null) {
				errorMsg = "Error al insertar cabecera del pedido, " + ex.getMessage();
			} else if (e.getMessage() != null) {
				errorMsg = "Error al insertar cabecera del pedido, " + e.getMessage();
			} else {
				errorMsg = "Error al insertar cabecera del pedido, Error no tipificado por el sistema, revise la data enviada.";
			}
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				System.out.println(ignore.getMessage());
				ignore.printStackTrace();
			}
		}

		return bp;
	}

	static public void createLocGeo(String c_bpartner_id, String direccion, String telefono, String referencia,
			String nombre, String correo, SDELVRConfig utilConfig) {

		UUID uuidLocation = UUID.randomUUID();
		String randomUUIDStringLocation = uuidLocation.toString().replaceAll("-", "").toUpperCase();
		ConnectionProvider connectionProvider = new DalConnectionProvider(false);

		String strSqlLocGeo = null;

		strSqlLocGeo = "INSERT INTO c_location(\n" + "            c_location_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, address1, address2,c_country_id )  \n"
				+ "             VALUES ( ?, ?, ?, 'Y', NOW(), ?, NOW(), ?, ?, ?, ? )";

		int updateCount = 0;
		PreparedStatement st = null;

		try {

			// SE VALIDA LONGUITUD DE LA DIRECCION
			Integer maxSize = 59;
			if (direccion.length() > maxSize) {
				direccion = direccion.substring(0, maxSize);
			}

			// SE VALIDA LONGUITUD DE LA DIRECCION
			if (referencia.length() > maxSize) {
				referencia = referencia.substring(0, maxSize);
			}

			st = connectionProvider.getPreparedStatement(strSqlLocGeo);

			st.setString(1, randomUUIDStringLocation);
			st.setString(2, utilConfig.getClient().getId());
			st.setString(3, "0");
			st.setString(4, utilConfig.getUserContact().getId());
			st.setString(5, utilConfig.getUserContact().getId());
			st.setString(6, direccion);
			st.setString(7, referencia);
			st.setString(8, OBDal.getInstance().get(Country.class, "171").getId());

			updateCount = st.executeUpdate();
			if (updateCount > 0) {
				createLocationBPartner(c_bpartner_id, randomUUIDStringLocation, telefono, nombre, correo, utilConfig);
			} else {
				System.out.println("LOCATION GEO NO INSERTADA.");
			}

			st.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				System.out.println(ignore.getMessage());
				ignore.printStackTrace();
			}
		}

	}

	static public void createLocationBPartner(String c_bpartner_id, String c_location_id, String telefono,
			String nombre, String correo, SDELVRConfig utilConfig) {

		UUID uuidLoc = UUID.randomUUID();
		String randomUUIDStringLoc = uuidLoc.toString().replaceAll("-", "").toUpperCase();
		ConnectionProvider connectionProvider = new DalConnectionProvider(false);

		String strSqlLocation = null;

		strSqlLocation = "INSERT INTO c_bpartner_location(\n"
				+ "            c_bpartner_location_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, name, em_saqb_alias, phone , c_bpartner_id, c_location_id, isbillto, isshipto, ispayfrom, isremitto)\n"
				+ "             VALUES ( ? , ? , ? , 'Y' , NOW() , ? , NOW() , ? , ? , ? , ? , ? , ?, 'Y', 'Y', 'Y','Y')";

		int updateCount = 0;
		PreparedStatement st = null;

		try {

			st = connectionProvider.getPreparedStatement(strSqlLocation);

			st.setString(1, randomUUIDStringLoc);
			st.setString(2, utilConfig.getClient().getId());
			st.setString(3, "0");
			st.setString(4, utilConfig.getUserContact().getId());
			st.setString(5, utilConfig.getUserContact().getId());
			st.setString(6, "CONTACTO GLOVO");
			st.setString(7, "CONTACTO GLOVO");
			st.setString(8, telefono);
			st.setString(9, c_bpartner_id);
			st.setString(10, c_location_id);

			updateCount = st.executeUpdate();
			if (updateCount > 0) {
				createContactPersonBPartner(c_bpartner_id, telefono, nombre, correo, utilConfig);
			}

			st.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				System.out.println(ignore.getMessage());
				ignore.printStackTrace();
			}
		}

	}

	static public void createContactPersonBPartner(String c_bpartner_id, String telefono, String nombre, String correo,
			SDELVRConfig utilConfig) {

		UUID uuidLoc = UUID.randomUUID();
		String randomUUIDStringContact = uuidLoc.toString().replaceAll("-", "").toUpperCase();
		ConnectionProvider connectionProvider = new DalConnectionProvider(false);

		String strSqlLocation = null;

		strSqlLocation = "INSERT INTO ad_user(\n" + "            ad_user_id, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, name, email, phone , c_bpartner_id, EM_Opcrm_Donotcall, username)\n"
				+ "             VALUES ( ? , ? , ? , 'Y' , NOW() , ? , NOW() , ? , ? , ?, ?, ?, 'N', ?)";

		int updateCount = 0;
		PreparedStatement st = null;

		try {

			st = connectionProvider.getPreparedStatement(strSqlLocation);

			st.setString(1, randomUUIDStringContact);
			st.setString(2, utilConfig.getClient().getId());
			st.setString(3, "0");
			st.setString(4, utilConfig.getUserContact().getId());
			st.setString(5, utilConfig.getUserContact().getId());
			st.setString(6, nombre);
			st.setString(7, correo);
			st.setString(8, telefono);
			st.setString(9, c_bpartner_id);
			st.setString(10, nombre);

			updateCount = st.executeUpdate();
			if (updateCount > 0) {
				System.out.println("CONTACT PERSON BPARTNER INSERTADA");
			}

			st.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				System.out.println(ignore.getMessage());
				ignore.printStackTrace();
			}
		}

	}

	static public Location getBpAddress(List<Location> addressBp) {
		Location billaddress = null;

		for (Location location : addressBp) {
			if (location.isInvoiceToAddress()) {
				billaddress = location;
			}
		}

		if (billaddress == null && addressBp.size() > 0) {
			billaddress = (Location) addressBp.get(0);
		}

		return billaddress;
	}

}
