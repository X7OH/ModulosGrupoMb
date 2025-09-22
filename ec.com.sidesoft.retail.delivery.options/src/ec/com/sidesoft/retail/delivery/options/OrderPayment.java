package ec.com.sidesoft.retail.delivery.options;

import java.util.ArrayList;
import java.util.Arrays;
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

public class OrderPayment extends ProcessHQLQuery {
  public static final String countryPropertyExtension = "OBPOS_CountryExtension";

  @Inject
  @Any
  @Qualifier(countryPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<HQLPropertyList> getHqlProperties(JSONObject jsonsent) {
    List<HQLPropertyList> propertiesList = new ArrayList<HQLPropertyList>();
    HQLPropertyList regularCountryHQLProperties = ModelExtensionUtils
        .getPropertyExtensions(extensions);

    propertiesList.add(regularCountryHQLProperties);

    return propertiesList;
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {

    String documentno = jsonsent.getString("documentNo");
    String posId = jsonsent.getString("pos");
    
     String hql = " select pm.amount as monto, "
                  + "(SELECT searchKey FROM OBPOS_App_Payment "
                  + "WHERE obposApplications.id = '" + posId + "' "
                  + " AND paymentMethod.paymentMethod.id = pm.paymentMethod.id) as searchKey "                  
                  + "from saqbp_payment_methods pm " 
                  + "join pm.saqbOrder AS order  " 
                  + " where order.documentNo = '" + documentno + "' ";  

     return Arrays.asList(new String[] { hql });
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

  @Override
  protected boolean isAdminMode() {
    return true;
  }

}
