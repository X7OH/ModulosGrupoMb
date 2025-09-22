package ec.com.sidesoft.retail.delivery.options;

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
@ComponentProvider.Qualifier(SRDPOComponentProvider.QUALIFIER)
public class SRDPOComponentProvider extends BaseComponentProvider {
public static final String QUALIFIER = "SRDPO_Main";
public static final String MODULE_JAVA_PACKAGE = "ec.com.sidesoft.retail.delivery.options"; 

@Override
public Component getComponent(String componentId, Map<String, Object> parameters) {
  throw new IllegalArgumentException("Component id " + componentId + " not supported.");
}

@Override
public List<ComponentResource> getGlobalComponentResources() {
  final GlobalResourcesHelper grhelper = new GlobalResourcesHelper();
  // Add all the javascript source files needed in our module
  grhelper.add("model/orderProcessed.js");
  grhelper.add("model/countordersdomicilie.js");
  //grhelper.add("view/extbpartnerposf.js");

  return grhelper.getGlobalResources();
}

private class GlobalResourcesHelper {
  private final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
  private final String prefix = "web/" + MODULE_JAVA_PACKAGE + "/js/";

  public void add(String file) {
    globalResources.add(
        createComponentResource(ComponentResourceType.Static, prefix + file, POSUtils.APP_NAME));
  }

  public List<ComponentResource> getGlobalResources() {
    return globalResources;
  }
}
}