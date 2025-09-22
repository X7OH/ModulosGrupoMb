package ec.com.sidesoft.quickbilling.advanced.ad_callouts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

public class UpdatePartner extends SimpleCallout {
  
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strTaxIdType = info.getStringParameter("inpsswhTaxidtype");
    String strChanged = info.getStringParameter("inpcifNif");

    String strMessage = null;
    org.openbravo.database.ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    UpdatePartnerData[] data = UpdatePartnerData.select(this, strChanged);    

    if (data == null || data.length == 0) {
        // VALIDAR TAXID
        strMessage = validateTaxId(strTaxIdType, strChanged, conn);
        if (strMessage != null) {
          strMessage = strMessage.replace("@", "");
          log4j.debug(Utility.messageBD(conn, strMessage, language));
          info.addResult("ERROR", Utility.messageBD(conn, strMessage, language));
        }
    	
      info.addResult("inpnamePartner", null);
      info.addResult("inpcBpartnerId", null);
      info.addResult("inpemail", null);
      info.addResult("inpnewAddress", true);
      info.addResult("inpaddress1AliasRef", "");
      
      info.addResult("inpphone", null);
      info.addResult("inpaddress1", null);
      info.addResult("inpcBpartnerLocationId", "");
      info.addResult("inpaddress1Alias", null);
      info.addResult("inpaddressComplete", null);
      info.addResult("inpcSalesregionId", "");
      
      info.addResult("inpnewAddressFac", true);
      info.addResult("inpphoneFac", null);
     // info.addResult("inpaddress1Fac", null);
      info.addResult("inpcBpartnerLocationFacId", "");
      info.addResult("inpaddress1AliasFac", null);
      info.addResult("inpaddressCompleteFac", null);
      info.addResult("inpaddress1AliasFac", "");
      
      info.addResult("inpdeliverycontactName",null);
    } else {

      info.addResult("inpnamePartner", data[0].name);
      info.addResult("inpcBpartnerId", data[0].cBpartnerId);
      info.addResult("inpemail", data[0].emEeiEmail);
      info.addResult("inpsswhTaxidtype", data[0].emSswhTaxidtype);

      if (data[0].cBpartnerLocationId == null || data[0].cBpartnerLocationId.equals("") || strChanged.equals("9999999999") ) {
        info.addResult("inpnewAddress", true);
        info.addResult("inpnewAddressFac", true);
      } else {
        info.addResult("inpnewAddress", false);
        info.addResult("inpnewAddressFac", false);
      }
      if(!strChanged.equals("9999999999")){ 
     	info.addResult("inpaddress1AliasRef", (data[0].cBpartnerLocationId.equals("")?null:data[0].cBpartnerLocationId));
     	info.addResult("inpaddress1AliasRefFac", (data[0].cBpartnerLocationId.equals("")?null:data[0].cBpartnerLocationId));
      }
      
      info.addResult("inpdeliverycontactName", data[0].name);
    }

  }

  public String validateTaxId(String strTaxIdType, String strTaxId,
      org.openbravo.database.ConnectionProvider conn) {
    // throw new OBException("EXCEPCION TEST");
    String strResult = null;

    // CallableStatement cllTaxidvalidate = null;
    PreparedStatement cllTaxidvalidate = null;
    ResultSet result = null;
    try {

      cllTaxidvalidate = conn.getConnection().prepareCall(
          "select sswh_taxidvalidate(?,?) from dual");

      cllTaxidvalidate.setString(1, strTaxIdType);
      cllTaxidvalidate.setString(2, strTaxId);
      result = cllTaxidvalidate.executeQuery();
      while (result.next()) {
        strResult = result.getString(1);
      }
    } catch (Exception e) {
      strResult = e.getMessage();
    } finally {
      try {
        cllTaxidvalidate.close();
        conn.destroy();
      } catch (Exception ex) {

      }
    }

    return strResult;

  }
}
