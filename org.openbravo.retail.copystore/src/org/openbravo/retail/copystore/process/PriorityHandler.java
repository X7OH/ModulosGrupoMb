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

/**
 * Defines the priority of the handler, so in case there are more than one with the same qualifier,
 * the one with lowest priority is selected.
 * {@link CopyStoreProcess#cloneObject(BaseOBObject, BaseOBObject, boolean)}
 * 
 * @author alostale
 * 
 */
public abstract class PriorityHandler {
  public int getPriority() {
    return 100;
  }
}
