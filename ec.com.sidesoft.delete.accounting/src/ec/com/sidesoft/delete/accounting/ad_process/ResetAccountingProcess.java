/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package ec.com.sidesoft.delete.accounting.ad_process;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

/**
 * 
 * @deprecated use {@link ec.com.sidesoft.delete.accounting.actionhandler}
 * 
 */
@Deprecated
public class ResetAccountingProcess extends DalBaseProcess {

  public void doExecute(ProcessBundle bundle) throws Exception {
    try {
      String adClientId = (String) bundle.getParams().get("adClientId");
      String adOrgId = (String) bundle.getParams().get("adOrgId");
      String deletePosting = (String) bundle.getParams().get("deleteposting");
      String adTableId = (String) bundle.getParams().get("adTableId");
      String recordId = (String) bundle.getParams().get("recordId");
      String datefrom = (String) bundle.getParams().get("datefrom");
      String dateto = (String) bundle.getParams().get("dateto");
      HashMap<String, Integer> results = new HashMap<String, Integer>();

      /**
       * VALIDAR PERIODOS
       */

      SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
      final Date startDate = formatter.parse(datefrom);
      final Date endDate = formatter.parse(dateto);
      String strPeriodErrors = "";

      OBCriteria<Period> obc = OBDal.getInstance().createCriteria(Period.class);
      obc.add(Restrictions.or(Restrictions.between("startingDate", startDate, endDate),
          Restrictions.between("endingDate", startDate, endDate)));
      obc.add(Restrictions.eq(Period.PROPERTY_OPENCLOSE, "O"));

      if (obc.list().size() > 0) {

        for (Period periodList : obc.list()) {
          String period = periodList.getName();
          strPeriodErrors = strPeriodErrors + ", " + period;

        }

      }
      OBError myError = new OBError();

      if (!strPeriodErrors.equals("")) {
        // throw new OBException("Los siguientes periodos se encentran cerrados\n" +
        // strPeriodErrors);
        myError.setType("Error");
        myError.setTitle("@Error@");
        myError.setMessage(Utility.parseTranslation(bundle.getConnection(),
            bundle.getContext().toVars(), bundle.getContext().toVars().getLanguage(),
            "@SDACCT_PeriodError@" + "\n" + strPeriodErrors));
        bundle.setResult(myError);
      } else {

        if ("Y".equals(deletePosting)) {
          results = ResetAccounting.delete(adClientId, adOrgId, adTableId, recordId, datefrom,
              dateto);
        } else {
          List<String> tableIds = StringUtils.isEmpty(adTableId) ? null : Arrays.asList(adTableId);
          results = ResetAccounting.restore(adClientId, adOrgId, tableIds, datefrom, dateto);
        }
        int counter = results.get("updated");
        int counterDeleted = results.get("deleted");
        myError.setType("Success");
        myError.setTitle("@Success@");
        myError.setMessage(Utility.parseTranslation(bundle.getConnection(),
            bundle.getContext().toVars(), bundle.getContext().toVars().getLanguage(),
            "@UnpostedDocuments@ = " + counter + ", @DeletedEntries@ = " + counterDeleted));
      }
      bundle.setResult(myError);
    } catch (OBException e) {
      throw e;
    } catch (Exception e) {
      // catch any possible exception and throw it as a Quartz
      // JobExecutionException
      throw new JobExecutionException(e.getMessage(), e);
    }
  }

  protected String formatDate(java.util.Date date) {
    return new SimpleDateFormat((String) OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .get(KernelConstants.DATE_FORMAT_PROPERTY)).format(date);
  }
}