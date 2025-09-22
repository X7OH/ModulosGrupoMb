/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.giftcards.term;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.retail.posterminal.term.Terminal;
import org.openbravo.retail.posterminal.term.TerminalProperties;

@Qualifier(Terminal.terminalPropertyExtension)
public class TemplateTerminalProperties extends TerminalProperties {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {

    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>();

    addTemplateProperty(Organization.PROPERTY_OBPGCCREDITTEMPLATE, "printCreditNoteTemplate", list);
    return list;
  }
}