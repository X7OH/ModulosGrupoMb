package ec.com.sidesoft.document.sequence.master;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.retail.posterminal.master.BusinessPartner;

@Qualifier(BusinessPartner.businessPartnerPropertyExtension)
public class ExtendBPMaster extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {

    try {
      org.openbravo.model.common.businesspartner.BusinessPartner.class
          .getMethod("isSscmbIsagreement", null);

      ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
        private static final long serialVersionUID = 1L;
        {
          add(new HQLProperty("bp.sscmbIsagreement", "sscmbIsagreement"));
        }
      };
      return list;

    } catch (NoSuchMethodException | SecurityException e) {
      ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
        private static final long serialVersionUID = 1L;
        {
          add(new HQLProperty("COALESCE(false, false)", "sscmbIsagreement"));
        }
      };
      return list;
    }

  }

}