/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore.process;

/**
 * After executing CopyStore process all classes implementing PostProcessHandler will be invoked.
 * They are invoked based on their priority starting from lower ones.
 * 
 * @author alostale
 * 
 */
public abstract class PostProcessHandler extends PriorityHandler {

  /**
   * Method invoked at the end of the process execution
   */
  public abstract void execute(CopyStoreProcess process);
}
