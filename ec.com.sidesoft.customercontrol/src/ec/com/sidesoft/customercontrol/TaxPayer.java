package ec.com.sidesoft.customercontrol;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.retail.posterminal.ProcessHQLQuery;

public class TaxPayer extends ProcessHQLQuery {
	public static final String adlistPropertyExtension = "OBPOS_adlistExtension";

	@Inject
	@Any
	@Qualifier(adlistPropertyExtension)
	private Instance<ModelExtension> extensions;

	@Override
	protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
		List<String> adlistList = new ArrayList<String>();
		HQLPropertyList adlistHQLProperties = ModelExtensionUtils.getPropertyExtensions(extensions);
		adlistList.add("select "
						+ adlistHQLProperties.getHqlSelect()
						+ "from ADList adlist "
						+ "where reference.id='DCF62925DDB84921955D3390BA35E72A' AND "
						+ "adlist.$readableClientCriteria AND "
						+ "adlist.$naturalOrgCriteria AND "
						+ "(adlist.$incrementalUpdateCriteria) AND (adlist.$incrementalUpdateCriteria)) order by adlist.name");

		return adlistList;
	}

}
