package ec.com.sidesoft.retail.giftcard.master;

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

public class GiftCardRetail extends ProcessHQLQuery {
  public static final String adlistPropertyExtension = "OBPOS_adlistExtension";

  @Inject
  @Any
  @Qualifier(adlistPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    List<String> adlistList = new ArrayList<String>();
    HQLPropertyList adlistHQLProperties = ModelExtensionUtils.getPropertyExtensions(extensions);
    adlistList.add(
        "select giftcard.id as id,giftcard.searchKey as searchKey, giftcard.searchKey as _identifier, giftcard.alertStatus as alertStatus, giftcard.amount as amount, "
        + "giftcard.currentamount as currentamount, giftcard.iscancelled as iscancelled, giftcard.obgcneExpirationdate as obgcneExpirationdate "
        + "from  GCNV_GiftCardInst giftcard " 
        + "where giftcard.amount > 0 "
        + "and giftcard.iscancelled = 'N' ");

    return adlistList;
  }

}