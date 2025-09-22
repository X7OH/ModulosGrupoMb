/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package ec.com.sidesoft.localization.ecuador.resupply.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.plm.Product;

public class DisplayedValue extends SimpleCallout {
  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strProductId = info.getStringParameter("inpmProductId", null);
    String strUOMProduct = info.getStringParameter("inpmProductUomId", IsIDFilter.instance);

    Product productobj = OBDal.getInstance().get(Product.class, strProductId);

    info.addResult("inpvalue", productobj.getSearchKey());
    info.addResult("inpcUomId", productobj.getUOM().getId());

    // Alternative UOM
    // Set AUM based on default
    if (UOMUtil.isUomManagementEnabled() && StringUtils.isEmpty(strUOMProduct)) {
      String finalAUM = UOMUtil.getDefaultAUMForLogistic(strProductId);
      if (StringUtils.isNotEmpty(finalAUM)) {
        info.addResult("inpcAum", finalAUM);
      }
    }
  }
}
