/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.returns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

/**
 * @author mdejuana
 * 
 */
@ApplicationScoped
@ComponentProvider.Qualifier(ReturnsComponentProvider.QUALIFIER)
public class ReturnsComponentProvider extends BaseComponentProvider {

  public static final String QUALIFIER = "OBRETUR";

  private List<ComponentResource> globalResources = null;

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public synchronized List<ComponentResource> getGlobalComponentResources() {
    if (globalResources != null) {
      return globalResources;
    }
    globalResources = new ArrayList<ComponentResource>();

    final String prefix = "web/org.openbravo.retail.returns/";

    final String[] resourceDependency = { "returnReceiptButton", "modalReturnLines",
        "hookReturnPaidOrder", "hookReturnChangeQty", "hookRenderOrderLine",
        "modalproductattributeverifiedreturns" };

    for (final String resource : resourceDependency) {
      globalResources.add(createComponentResource(ComponentResourceType.Static, prefix + "js/"
          + resource + ".js", "WebPOS"));
    }

    globalResources.add(createComponentResource(ComponentResourceType.Stylesheet, prefix
        + "assets/css/" + "ob-returns.css", "WebPOS"));

    return globalResources;
  }
}
