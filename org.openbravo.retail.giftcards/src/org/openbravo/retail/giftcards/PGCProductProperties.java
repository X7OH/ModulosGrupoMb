/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards;

import java.util.Arrays;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.retail.posterminal.master.Product;

/**
 * @author guillermogil
 * 
 */
@Qualifier(Product.productPropertyExtension)
public class PGCProductProperties extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    return Arrays
        .asList(
            new HQLProperty(
                "(select template.templatePath from OBPOS_Print_Template template where product.obpgcPrinttemplate.id=template.id)",
                "printTemplate"),
            new HQLProperty("product.obpgcPrintcard", "printCard"),
            new HQLProperty(
                "(select template.ispdf from OBPOS_Print_Template template where product.obpgcPrinttemplate.id=template.id)",
                "templateIsPdf"),
            new HQLProperty(
                "(select template.printer from OBPOS_Print_Template template where product.obpgcPrinttemplate.id=template.id)",
                "templatePrinter"), new HQLProperty("product.obgcneExpirationdays",
                "expirationDays"));

  }
}