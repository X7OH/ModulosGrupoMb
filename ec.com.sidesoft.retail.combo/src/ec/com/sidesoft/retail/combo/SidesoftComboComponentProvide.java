package ec.com.sidesoft.retail.combo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(SidesoftComboComponentProvide.QUALIFIER)
public class SidesoftComboComponentProvide extends BaseComponentProvider {
  public static final String QUALIFIER = "SSRCM";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/ec.com.sidesoft.retail.combo";

    final String[] resourceDependency = { "combo-utils", "combo-rule-impl", "combo-popup",
        "combo-delete-combo-button", "combo-edit-combo-button", "combo-hooks",
        "combo-modif-renderorderline", "combo-change-printreceipt", "combo-stock-popup",
        "combo-product-model-extension", "combo-product-extension", "combo-hide-combo-products",
        "combo-change-pcat-pcattree", "combo-product-category-extension" };

    final String[] erpResourceDependency = { "combo-tab-lines", "combo-manual-window" };

    final String[] cssDependency = { "css/combo-popup" };

    for (String resource : resourceDependency) {
      globalResources.add(createComponentResource(ComponentResourceType.Static,
          prefix + "/source/" + resource + ".js", "WebPOS"));
    }

    for (String resource : cssDependency) {
      globalResources.add(createComponentResource(ComponentResourceType.Stylesheet,
          prefix + "/assets/" + resource + ".css", "WebPOS"));
    }

    for (String resource : erpResourceDependency) {
      globalResources.add(createStaticResource(prefix + "/source/" + resource + ".js", true));
    }

    for (String resource : cssDependency) {
      globalResources
          .add(createStyleSheetResource(prefix + "/assets/" + resource + ".css", false, true));
    }

    return globalResources;
  }

}
