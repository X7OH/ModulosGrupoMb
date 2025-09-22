/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.giftcards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.retail.posterminal.POSUtils;

/**
 * @author adrianromero
 * 
 */
@ApplicationScoped
@ComponentProvider.Qualifier(GCNVComponentProvider.QUALIFIER)
public class GCNVComponentProvider extends BaseComponentProvider {

  public static final String QUALIFIER = "GCNV_Main";
  public static final String MODULE_JAVA_PACKAGE = "org.openbravo.retail.giftcards";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final GlobalResourcesHelper grhelper = new GlobalResourcesHelper();

    grhelper.add("model/giftcardproducts.js");
    grhelper.add("model/giftCardFilter.js");
    grhelper.add("model/GiftCardUtils.js");
    grhelper.add("model/GiftCard.js");
    grhelper.add("model/CashManagement.js");
    grhelper.add("model/giftcardproductproperty.js");
    grhelper.add("model/product-properties.js");
    grhelper.add("model/businesspartner-properties.js");
    grhelper.add("components/lineproperties.js");
    grhelper.add("components/GiftCardCertificate.js");
    grhelper.add("components/GiftCardDialog.js");
    grhelper.add("components/GiftCardDetails.js");
    grhelper.add("components/GiftCardPaymentAmount.js");
    grhelper.add("components/GiftCardPaymentVoucher.js");
    grhelper.add("components/GiftCardCancel.js");
    grhelper.add("components/GiftCardMessage.js");
    grhelper.add("components/GiftCardSearchDialog.js");
    grhelper.add("components/GiftCardMenu.js");
    grhelper.add("components/GiftCardReturnsInjection.js");
    grhelper.add("components/GCNewEntitiesExpirationDate.js");
    grhelper.add("components/GCNewEntitiesGCOwner.js");
    grhelper.add("hooks/preaddpaymenthook.js");
    grhelper.add("hooks/preremovepaymenthook.js");
    grhelper.add("hooks/AddButtonToCashManagementHook.js");
    grhelper.add("hooks/printGiftCardPrePrintHook.js");
    grhelper.add("hooks/PreReversePaymentHook.js");
    grhelper.add("hooks/precustomersavehook.js");
    grhelper.add("hooks/preSaveCashManagementsHook.js");
    grhelper.add("hooks/predeletelinehook.js");
    grhelper.add("hooks/checkGiftCardMandatoryFields.js");

    grhelper.addStyle("gcnv.css");

    return grhelper.getGlobalResources();
  }

  private class GlobalResourcesHelper {
    private final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    private final String prefix = "web/" + MODULE_JAVA_PACKAGE + "/js/";
    private final String cssPrefix = "web/" + MODULE_JAVA_PACKAGE + "/css/";

    public void add(String file) {
      globalResources.add(createComponentResource(ComponentResourceType.Static, prefix + file,
          POSUtils.APP_NAME));
    }

    public void addStyle(String file) {
      globalResources.add(createComponentResource(ComponentResourceType.Stylesheet, cssPrefix
          + file, POSUtils.APP_NAME));
    }

    public List<ComponentResource> getGlobalResources() {
      return globalResources;
    }
  }
}
