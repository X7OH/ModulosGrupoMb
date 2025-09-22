package ec.com.sidesoft.customercontrol;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.model.common.geography.Country;
import org.openbravo.retail.posterminal.ProcessHQLQuery;


public class Region extends ProcessHQLQuery {
	public static final String regionPropertyExtension = "OBPOS_regionExtension";

	@Inject
	@Any
	@Qualifier(regionPropertyExtension)
	private Instance<ModelExtension> extensions;

	@Override
	protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
		List<String> regionList = new ArrayList<String>();
		HQLPropertyList adlistHQLProperties = ModelExtensionUtils.getPropertyExtensions(extensions);
		Country country=OBContext.getOBContext().getCurrentOrganization().
				getOrganizationInformationList().get(0).getLocationAddress().getCountry();		
		regionList.add("select "
						+ adlistHQLProperties.getHqlSelect()
						+ "from Region reg "
						+ "where country.id= '"+country.getId()+"'");

		return regionList;
	}
}
