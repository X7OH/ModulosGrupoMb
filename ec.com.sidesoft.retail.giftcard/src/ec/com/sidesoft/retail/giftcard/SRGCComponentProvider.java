package ec.com.sidesoft.retail.giftcard;

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
@ComponentProvider.Qualifier(SRGCComponentProvider.QUALIFIER)
public class SRGCComponentProvider extends BaseComponentProvider {

  public static final String QUALIFIER = "SRGC_Main";
  public static final String MODULE_JAVA_PACKAGE = "ec.com.sidesoft.retail.giftcard";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final GlobalResourcesHelper grhelper = new GlobalResourcesHelper();
    
    grhelper.add("model/giftcard.js");
    grhelper.add("model/GiftCardUtilsRetail.js");
    grhelper.add("model/giftCardFilterRetail.js");

    //MODAL PRINCIPAL PARA EL METODO DE PAGO SRGC_.UI.GiftCardConnector 
    //ESTE MODAL LLAMA AL MODAL DEL ARCHIVO GiftCardSearch.js
    grhelper.add("view/GiftCard.js");
    //MODAL DONDE SE REALIZA LA BUSQUEDA DE LAS GIFTCARD
    grhelper.add("view/GiftCardSearch.js");
    //MODAL DONDE SE MUESTRA EL DETALLE DE LA GIFTCARD SELECIONADA
    grhelper.add("view/GiftCardDetails.js");
    grhelper.add("view/GiftCardMessage.js");
    grhelper.add("view/GiftCardCancel.js");
    
    grhelper.add("hooks/preremovepaymenthook.js");
    
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