/*
 ************************************************************************************
 * Copyright (C) 2013 - 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.discounts.combo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(ComboComponentProvide.QUALIFIER)
public class ComboComponentProvide extends BaseComponentProvider {
  public static final String QUALIFIER = "OBCOMBO";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/org.openbravo.retail.discounts.combo";

    final String[] resourceDependency = { "combo-model", "comboFixPrice-model", "combo-rule-impl",
        "combo-popup" };

    final String[] cssDependency = { "css/combo-popup" };

    for (String resource : resourceDependency) {
      globalResources.add(createComponentResource(ComponentResourceType.Static, prefix + "/source/"
          + resource + ".js", "WebPOS"));
    }

    for (String resource : cssDependency) {
      globalResources.add(createComponentResource(ComponentResourceType.Stylesheet, prefix
          + "/assets/" + resource + ".css", "WebPOS"));
    }

    return globalResources;
  }

}
