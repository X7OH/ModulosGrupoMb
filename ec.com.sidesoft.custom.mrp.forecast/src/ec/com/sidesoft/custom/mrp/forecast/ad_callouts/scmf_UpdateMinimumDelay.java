package ec.com.sidesoft.custom.mrp.forecast.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.plm.ProductOrg;

public class scmf_UpdateMinimumDelay extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub

  //  BigDecimal scmfPercentage = info.getBigDecimalParameter("inpscmfPercentage");
  //  BigDecimal minQuantity = info.getBigDecimalParameter("inpminQuantity");
	  BigDecimal scmfPercentage = info.getBigDecimalParameter("inpemScmfPercentage");
	  BigDecimal minQuantity = info.getBigDecimalParameter("inpqtymin");
    //BigDecimal minimumLeadTime = new BigDecimal("0");
    //BigDecimal cien = new BigDecimal("100");
	  Double minimumLeadTime = new Double("0");
	  Double cien = new Double("100");
    /*
    if(Integer.valueOf(scmfPercentage.intValue()) > 0 &&  Integer.valueOf(minQuantity.intValue()) > 0){
    	
    	minimumLeadTime = minQuantity.multiply(scmfPercentage).divide(cien);
    	info.addResult("inpstockmin", String.valueOf(minimumLeadTime));
   	
    }else{
    	
    	info.addResult("inpstockmin", String.valueOf(0));
   }*/
if(Double.valueOf(scmfPercentage.toString()) > 0 &&  Double.valueOf(minQuantity.toString()) > 0){
    	
    	minimumLeadTime =  (Double.valueOf(minQuantity.toString())*  Double.valueOf(scmfPercentage.toString()))/(cien);
    	
    	info.addResult("inpstockmin", minimumLeadTime.toString());
   	
    }else{
    	
    	info.addResult("inpstockmin", String.valueOf(0));
   }	
    
  }

}
