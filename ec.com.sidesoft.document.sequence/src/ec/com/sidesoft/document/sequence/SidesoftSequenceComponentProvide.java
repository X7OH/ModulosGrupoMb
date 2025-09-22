package ec.com.sidesoft.document.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(SidesoftSequenceComponentProvide.QUALIFIER)
public class SidesoftSequenceComponentProvide extends BaseComponentProvider {
  public static final String QUALIFIER = "ECSDS";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/ec.com.sidesoft.document.sequence";

    final String[] resourceDependency = { "document-sequence-model", "document-sequence-hooks",
        "document-sequence-error-modal", "document-sequence-extend-bp" };

    final String[] erpResourceDependency = {};

    final String[] cssDependency = {};

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
