/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore.process;

import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;

/**
 * Defines all default instances of FKPropertyHandler
 * 
 * @author alostale
 * 
 */
public class DefaultFKPropertiesHandler {

  /**
   * All references from Organization to Print Templates are set as null
   * 
   * @author alostale
   * 
   */
  public static class OrganizationFKs {
    @Qualifier("Organization.OBPOS_Print_Template")
    public static class OrganizationFKsSearchKey extends FKPropertyHandler {
      @Override
      public void handleProperty(String propertyName, BaseOBObject originalObject,
          BaseOBObject newObject, CopyStoreProcess process) {
        // Don't copy templates
      }
    }
  }
}
