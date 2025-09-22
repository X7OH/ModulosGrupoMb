package ec.com.sidesoft.integration.plugthem.background;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationTree;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.integration.plugthem.SsplugPostSaleSettings;
import ec.com.sidesoft.integration.plugthem.ad_process.PostSaleAPI;

public class PostSaleBackground extends DalBaseProcess implements
		KillableProcess {

	private static final Logger log4j = Logger
			.getLogger(PostSaleBackground.class);
	private ProcessLogger logger;
	private boolean killProcess = false;

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {

		OBError result = new OBError();
		ConnectionProvider conn = new DalConnectionProvider(false);
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		String strSessionUserId = OBContext.getOBContext().getUser().getId();
		logger = bundle.getLogger();
		try {

			OBContext.setAdminMode(true);

			// MAYFLOWER
			OBCriteria<SsplugPostSaleSettings> ObjSettings = OBDal
					.getInstance().createCriteria(SsplugPostSaleSettings.class);
			ObjSettings.add(Restrictions.eq(
					SsplugPostSaleSettings.PROPERTY_ACTIVE, true));
			ObjSettings.add(Restrictions.eq(
					SsplugPostSaleSettings.PROPERTY_BRANDID, "1"));
			ObjSettings.setMaxResults(1);

			if (ObjSettings.list().size() == 0) {
				logger.logln("No existe parametrización activa con marca 'Mayflower'.");
			} else {
				logger.logln("Inicia envío marca 'Mayflower'.");
				SsplugPostSaleSettings objSetting = (SsplugPostSaleSettings) ObjSettings
						.uniqueResult();
				send(logger, objSetting);
			}

			// BUFFALOS
			OBCriteria<SsplugPostSaleSettings> ObjSettings1 = OBDal
					.getInstance().createCriteria(SsplugPostSaleSettings.class);
			ObjSettings1.add(Restrictions.eq(
					SsplugPostSaleSettings.PROPERTY_ACTIVE, true));
			ObjSettings1.add(Restrictions.eq(
					SsplugPostSaleSettings.PROPERTY_BRANDID, "2"));
			ObjSettings1.setMaxResults(1);

			if (ObjSettings1.list().size() == 0) {
				logger.logln("No existe parametrización activa con marca 'Buffalos'.");
			} else {
				logger.logln("Inicia envío marca 'Buffalos'.");
				SsplugPostSaleSettings objSetting1 = (SsplugPostSaleSettings) ObjSettings1
						.uniqueResult();
				send(logger, objSetting1);
			}

		} catch (Exception e) {
			result.setTitle(Utility.messageBD(conn, "Error", language));
			result.setType("Error");
			result.setMessage(e.getMessage());
			bundle.setResult(result);

			log4j.error(result.getMessage(), e);
			logger.logln(e.getMessage());
			e.printStackTrace();
			return;
		} finally {
			OBDal.getInstance().flush();
			OBContext.restorePreviousMode();
			try {
				conn.destroy();
			} catch (Exception e) {

			}
		}

	}

	public JSONObject send(ProcessLogger logger,
			SsplugPostSaleSettings objSetting) throws JSONException {

		JSONObject resultLogger = new JSONObject();
		int intIteration = 0, intSended = 0, intToSend = 0;

		OBCriteria<OrganizationTree> ObjOrgTree = OBDal.getInstance()
				.createCriteria(OrganizationTree.class);
		ObjOrgTree.add(Restrictions.eq(
				OrganizationTree.PROPERTY_PARENTORGANIZATION,
				objSetting.getOrganization()));

		List<Organization> lstOrganization = new ArrayList<Organization>();
		for (OrganizationTree objOrganizationTree : ObjOrgTree.list()) {
			lstOrganization.add(objOrganizationTree.getOrganization());
		}

		if (lstOrganization.size() != 0) {

			log4j.debug("Ejecuntando consulta de facturas.");

			OBCriteria<Invoice> objInvoices = OBDal.getInstance()
					.createCriteria(Invoice.class, "invoice");
			objInvoices.createAlias("invoice.documentType", "documentType");
			objInvoices.createAlias("invoice.organization", "organization");
			objInvoices.createAlias("invoice.businessPartner",
					"businessPartner");
			objInvoices.createAlias("invoice.salesOrder", "salesOrder");
			objInvoices.add(Restrictions.in(Invoice.PROPERTY_ORGANIZATION,
					lstOrganization));
			objInvoices.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION,
					true));
			objInvoices.add(Restrictions.eq("documentType.return", false));
			objInvoices.add(Restrictions.eq("documentType.reversal", false));
			objInvoices.add(Restrictions.eq(
					Invoice.PROPERTY_SSPLUGSURVEYSENDED, false));
			objInvoices.add(Restrictions.isNotNull("businessPartner.eEIEmail"));
			objInvoices.add(Restrictions.ne("businessPartner.taxID",
					"9999999999"));
			objInvoices.addOrder(org.hibernate.criterion.Order
					.asc("invoiceDate"));
			objInvoices.addOrder(org.hibernate.criterion.Order
					.asc("organization.searchKey"));
			objInvoices.addOrder(org.hibernate.criterion.Order
					.asc("documentNo"));

			intToSend = objInvoices.list().size();
			log4j.debug("Transacciones a enviar: " + intToSend);

			for (Invoice objInvoice : objInvoices.list()) {
				if (killProcess) {
					throw new OBException("Process killed");
				}
				log4j.debug("*****************************************");
				try {
					intIteration++;
					log4j.debug("Procesando transacción: " + intIteration + "/"
							+ intToSend);
					PostSaleAPI.consumeServiceWS(objInvoice, objSetting);
					intSended++;
				} catch (Exception e) {
					
					log4j.debug("Error documento: "
							+ objInvoice.getDocumentNo() + " - "
							+ e.getMessage());
					logger.logln("Error documento: "
							+ objInvoice.getDocumentNo() + " - "
							+ e.getMessage());			
				}
				log4j.debug("*****************************************");
			}
			log4j.debug("Proceso ejecutado exitosamente. Encuestas enviadas: "
					+ intSended + "/" + intToSend);
			logger.logln("Proceso ejecutado exitosamente. Encuestas enviadas: "
					+ intSended + "/" + intToSend);
		} else {
			log4j.debug("No existen organizaciones hijas de la configurada. ("
					+ (objSetting.getOrganization() == null ? "" : objSetting
							.getOrganization()) + ")");
			logger.logln("No existen organizaciones hijas de la configurada. ("
					+ (objSetting.getOrganization() == null ? "" : objSetting
							.getOrganization()) + ")");
		}

		return resultLogger;

	}

	@Override
	public void kill(ProcessBundle processBundle) throws Exception {
		OBDal.getInstance().flush();
		this.killProcess = true;
	}

}
