package ec.com.sidesoft.retail.agreementinvoices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.retail.posterminal.POSUtils;


@ApplicationScoped
@ComponentProvider.Qualifier(SpaiComponentProvider.QUALIFIER)
public class SpaiComponentProvider extends BaseComponentProvider {

  public static final String QUALIFIER = "SPCO_Main";
  public static final String MODULE_JAVA_PACKAGE = "ec.com.sidesoft.retail.agreementinvoices";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/" + MODULE_JAVA_PACKAGE + "/js/";

    globalResources.add(createComponentResource(ComponentResourceType.Static, prefix
            + "organizationSales.js", POSUtils.APP_NAME));
    
    globalResources.add(createComponentResource(ComponentResourceType.Static, prefix
        + "paymentAgreements.js", POSUtils.APP_NAME));

    return globalResources;
  }
}
