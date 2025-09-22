/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;

@ApplicationScoped
@ComponentProvider.Qualifier(CopyStoreComponentProvider.QUALIFIER)
public class CopyStoreComponentProvider extends BaseComponentProvider {
  protected static final String QUALIFIER = "OBPOSCS";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources.add(createStaticResource(
        "web/org.openbravo.retail.copystore/js/obposcs-copy-store-result.js", false));

    globalResources
        .add(createStyleSheetResource(
            "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
                + KernelConstants.SKIN_PARAMETER
                + "/org.openbravo.retail.copystore/obposcs-styles.css", false));
    return globalResources;
  }

}
