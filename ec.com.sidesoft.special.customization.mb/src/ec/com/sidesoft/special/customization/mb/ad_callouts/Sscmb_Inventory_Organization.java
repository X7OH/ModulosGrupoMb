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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package ec.com.sidesoft.special.customization.mb.ad_callouts;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;

public class Sscmb_Inventory_Organization extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    String strMWarehouseId = info.vars.getStringParameter("inpmWarehouseId");
    boolean updateWarehouse = true;
    Organization obdalOrganization = OBDal.getInstance().get(Organization.class,
    		info.vars.getStringParameter("inpadOrgId"));
    
    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    String dateString = format.format(new Date());
    
    info.addResult("inpname", dateString);    
    
    if (obdalOrganization!=null) {
  	  String strOrgTypeTPV = obdalOrganization.getOBRETCORetailOrgType()==null? "ND": obdalOrganization.getOBRETCORetailOrgType();
  	  
  	  if(strOrgTypeTPV.equals("S")) {
  		  
  		  
  		  String strCostCenter = "ND";
  		  try {
  				  strCostCenter = obdalOrganization.getScmbaCostcenter().getId().toString()==null?"ND": obdalOrganization.getScmbaCostcenter().getId().toString();
  		  }catch(Exception e) {
  			  
  		  }
  		  
  		  if (strCostCenter.equals("ND")) {
	  			
	  			info.addResult("ERROR", "La organización seleccionada es de tipo tienda y no tiene configurado el centro de costo");
	  	  }
  		 
  		  if (!strCostCenter.equals("ND")) {
  			  info.addResult("inpcCostcenterId", strCostCenter);
  		  }
  	  }
    }
  }
}
