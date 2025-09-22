/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.discounts.bytotal;

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
@ComponentProvider.Qualifier(DiscTComponentProvider.QUALIFIER)
public class DiscTComponentProvider extends BaseComponentProvider {

  public static final String QUALIFIER = "DiscT_Main";
  public static final String MODULE_JAVA_PACKAGE = "org.openbravo.retail.discounts.bytotal";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final GlobalResourcesHelper grhelper = new GlobalResourcesHelper();

    grhelper.add("promotion-discounttotal.js");
    grhelper.add("promotion-percentagediscounttotal.js");
    grhelper.add("promotion-freeitemstotal.js");
    grhelper.add("freeitems-model.js");
    grhelper.add("promotion-by-total-utils.js");
    grhelper.add("promotionManualByTotal.js");
    grhelper.add("ticketdiscountExtension.js");
    grhelper.add("deleteDiscountExtension.js");
    grhelper.add("preApplyDiscountsHook.js");
    grhelper.add("preApplyAutomaticDiscountHook.js");

    return grhelper.getGlobalResources();
  }

  private class GlobalResourcesHelper {
    private final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    private final String prefix = "web/" + MODULE_JAVA_PACKAGE + "/js/";

    public void add(String file) {
      globalResources.add(createComponentResource(ComponentResourceType.Static, prefix + file,
          POSUtils.APP_NAME));
    }

    public List<ComponentResource> getGlobalResources() {
      return globalResources;
    }
  }
}
