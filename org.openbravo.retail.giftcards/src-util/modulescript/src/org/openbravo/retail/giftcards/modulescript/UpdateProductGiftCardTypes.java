/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards.modulescript;

import java.sql.PreparedStatement;

import org.apache.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

public class UpdateProductGiftCardTypes extends ModuleScript {

  private static final Logger log4j = Logger.getLogger(UpdateProductGiftCardTypes.class);
  private static final String RETAIL_PACK_MODULE_ID = "03FAB282A7BF47D3B1B242AC67F7845B";

  public void execute() {
    try {
      log4j.info("Update Based on Produts Gift Card Instances type ...");
      final StringBuilder sql = new StringBuilder();
      sql.append("UPDATE gcnv_giftcard_inst gci ");
      sql.append("SET type = ( ");
      sql.append("CASE WHEN (SELECT p.em_gcnv_giftcardtype FROM m_product p WHERE p.m_product_id = gci.m_product_id) = 'G' THEN 'BasedOnProductGiftCard' ");
      sql.append("ELSE 'BasedOnVoucher' ");
      sql.append("END ");
      sql.append(") ");
      sql.append("WHERE gci.m_product_id IS NOT NULL");

      ConnectionProvider cp = getConnectionProvider();
      PreparedStatement ps = cp.getPreparedStatement(sql.toString());
      ps.executeUpdate();
    } catch (Exception e) {
      handleError(e);
    }
  }

  @Override
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return new ModuleScriptExecutionLimits(RETAIL_PACK_MODULE_ID, null,
        new OpenbravoVersion(1, 8, 2700));
  }

}