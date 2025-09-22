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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package ec.com.sidesoft.process.print.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.impl.cookie.DateUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;

public abstract class DataSource {

  private static final Logger log4j = Logger.getLogger(DataSource.class);

  private List<String> attributes;
  private List<String> initilize;
  private List<String> finalParams;
  private List<SSPRITemplate> template;
  private List<SSPRITempLib> templateLib;
  private String params;
  private String docType;

  public DataSource() {
    attributes = new ArrayList<String>();
    initilize = new ArrayList<String>();
    template = new ArrayList<SSPRITemplate>();
    templateLib = new ArrayList<SSPRITempLib>();
  }

  public void doGet() {

  }

  public List<String> getAttributes() {
    return attributes;
  }

  public List<String> getInitialization() {
    return initilize;
  }

  public List<String> getFinalParams() {
    return finalParams;
  }

  public String getParams() {
    return params;
  }

  public String getDocType() {
    return docType;
  }

  public String fromDateToString(Date date) {
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateTimeFormat.java");
    return DateUtils.formatDate(date, dateFormat);
  }

  public List<SSPRITemplate> getTemplate() {
    return this.template;
  }

  public List<SSPRITempLib> getTemplateLib() {
    return this.templateLib;
  }

  public void sendDocument(String document) throws IOException {

  }

  public void getTemplatesFromDb(String processId, DocumentType docType, Organization org) {
    org.openbravo.model.ad.ui.Process process = OBDal.getInstance()
        .get(org.openbravo.model.ad.ui.Process.class, processId);

    if (process != null) {
      OBCriteria<SSPRIProcTemp> templateProcCri = OBDal.getInstance()
          .createCriteria(SSPRIProcTemp.class);
      if (docType != null) {
        templateProcCri.add(Restrictions.eq(SSPRIProcTemp.PROPERTY_DOCUMENTTYPE, docType));
      } else {
        templateProcCri.add(Restrictions.eq(SSPRIProcTemp.PROPERTY_ORGANIZATION, org));
      }
      templateProcCri.add(Restrictions.eq(SSPRIProcTemp.PROPERTY_PROCESS, process));
      if (templateProcCri.count() > 0) {
        List<SSPRIProcTemp> templateProcList = templateProcCri.list();
        for (SSPRIProcTemp tempProc : templateProcList) {
          template.add(tempProc.getSspriTemplate());
          templateLib.addAll(tempProc.getSspriTemplate().getSSPRITempLibList());
        }
      }
    } else {
      org.openbravo.client.application.Process obuiappProcess = OBDal.getInstance()
          .get(org.openbravo.client.application.Process.class, processId);
      OBCriteria<SSPRIProcTemp> templateProcCri = OBDal.getInstance()
          .createCriteria(SSPRIProcTemp.class);
      if (docType != null) {
        templateProcCri.add(Restrictions.eq(SSPRIProcTemp.PROPERTY_DOCUMENTTYPE, docType));
      } else {
        templateProcCri.add(Restrictions.eq(SSPRIProcTemp.PROPERTY_ORGANIZATION, org));
      }
      templateProcCri
          .add(Restrictions.eq(SSPRIProcTemp.PROPERTY_PROCESSFORADDINGRECORDS, obuiappProcess));
      if (templateProcCri.count() > 0) {
        List<SSPRIProcTemp> templateProcList = templateProcCri.list();
        for (SSPRIProcTemp tempProc : templateProcList) {
          template.add(tempProc.getSspriTemplate());
          templateLib.addAll(tempProc.getSspriTemplate().getSSPRITempLibList());
        }
      }
    }
  }

}
