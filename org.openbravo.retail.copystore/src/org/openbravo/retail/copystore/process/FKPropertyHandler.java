/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore.process;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.base.structure.BaseOBObject;

/**
 * Overwrites standard behavior when cloning FK references
 * {@link CopyStoreProcess#cloneObject(BaseOBObject, BaseOBObject, boolean)}
 * 
 * @author alostale
 * 
 */
@ApplicationScoped
public abstract class FKPropertyHandler extends PriorityHandler {
  public abstract void handleProperty(String propertyName, BaseOBObject originalObject,
      BaseOBObject newObject, CopyStoreProcess process);
}
