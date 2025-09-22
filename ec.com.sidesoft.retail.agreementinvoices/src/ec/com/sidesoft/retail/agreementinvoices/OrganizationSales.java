package ec.com.sidesoft.retail.agreementinvoices;

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

public class OrganizationSales extends ProcessHQLQuery {
  public static final String adlistPropertyExtension = "Spai_adlistExtension";

  @Inject
  @Any
  @Qualifier(adlistPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    List<String> adlistList = new ArrayList<String>();
    HQLPropertyList adlistHQLProperties = ModelExtensionUtils.getPropertyExtensions(extensions);
    adlistList.add("select      o.id as id   , o.id as idtpv, "
        + " cbp.id as idcustomer   , cbp.name as namecustomer   ,cbpl.id as idlocation "
        + " , cbpl.name as location       " + " from Organization o     "
        + " inner join o.obretcoCBpartner as cbp     "
        + " inner join o.obretcoCBpLocation as cbpl");

    return adlistList;
  }

}
