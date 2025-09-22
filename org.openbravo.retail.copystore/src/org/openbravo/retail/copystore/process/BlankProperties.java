/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore.process;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

/**
 * Utility interface that allows to define properties to be set as null.
 * 
 * Implement this interface in modules to overwrite default behavior for given properties
 * 
 * @author alostale
 * 
 */
@ApplicationScoped
public interface BlankProperties {
  /**
   * Receives a list of properties that should be blank, add here (or manipulate) null properties.
   * Properties are defined as [EntityName].[PropertyName]
   * 
   */
  public void addBlankProperties(List<String> blankProperties);
}
