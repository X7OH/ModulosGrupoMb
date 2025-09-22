/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.retail.giftcard.process;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class ExpirationDateProcess extends DalBaseProcess implements KillableProcess {

  private static final Logger log4j = Logger.getLogger(ExpirationDateProcess.class);
  private boolean killProcess = false;
  private ProcessLogger logger;
  private int counter = 0;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    long start = System.currentTimeMillis();
    String clientId = bundle.getContext().getClient();
    try {
      closeGiftCards(clientId);
    } catch (Exception e) {
      logger.logln(OBMessageUtils.getI18NMessage("GCNV_TimeElapsed",
          new String[] { String.valueOf(System.currentTimeMillis() - start) }));
      throw new OBException(OBMessageUtils.getI18NMessage("GCNV_ExceptionClosingGiftcard", null), e);
    }
    logger.logln(OBMessageUtils.getI18NMessage("GCNV_TimeElapsed",
        new String[] { String.valueOf(System.currentTimeMillis() - start) }));
    logger.log(OBMessageUtils.getI18NMessage("GCNV_GiftCardsClosed",
        new String[] { String.valueOf(counter) }));
  }

  private void closeGiftCards(String clientId) throws Exception {
    int closedCounter = 0;
    List<String> openGiftCards = getOpenGiftCards(clientId);
    if (openGiftCards.size() > 0 && !killProcess) {
      if (killProcess) {
        return;
      }
      long init = System.currentTimeMillis();
      for (String giftcard : openGiftCards) {
        GiftCardGLItemUtils.close((String) giftcard, "E");
        closedCounter++;
        if (closedCounter == openGiftCards.size() || closedCounter % 50 == 0) {
          int closedGiftCards = closedCounter % 50 == 0 ? 50 : closedCounter % 50;
          counter += closedGiftCards;
          logger.logln(OBMessageUtils.getI18NMessage("GCNV_GiftCardsClosed",
              new String[] { String.valueOf(closedGiftCards) }));
          OBDal.getInstance().getConnection(true).commit();
          OBDal.getInstance().getSession().clear();

          logger
              .logln(OBMessageUtils.getI18NMessage(
                  "GCNV_TimeConsumedGiftCards",
                  new String[] { String.valueOf(closedGiftCards),
                      String.valueOf(System.currentTimeMillis() - init) }));
          log4j.debug(String.format("*************************************** "
              + "Time consumed in closing last %s Gift Cards: %s", closedGiftCards,
              ((System.currentTimeMillis() - init) + " milis")));
          init = System.currentTimeMillis();
          if (killProcess) {
            logger.logln(OBMessageUtils.getI18NMessage("GCNV_ExpirationDateProcessKilled", null));
            return;
          }
        }
      }
      closeGiftCards(clientId);
    }
  }

  @SuppressWarnings("unchecked")
  protected List<String> getOpenGiftCards(String clientId) throws Exception {
    OBContext.setAdminMode(false);
    try {
      final StringBuilder hqlString = new StringBuilder();
      hqlString.append("select e.id");
      hqlString.append(" from GCNV_GiftCardInst as e");
      hqlString.append(" where e.obgcneExpirationdate < trunc(now())");
      hqlString.append(" and e.alertStatus <> 'C'");
      hqlString.append(" and e.currentamount > 0");
      hqlString.append(" and e.type = 'BasedOnGLItem'");
      hqlString.append(" and e.client.id = :client");
      final Session session = OBDal.getInstance().getSession();
      final Query query = session.createQuery(hqlString.toString());
      query.setParameter("client", clientId);
      query.setMaxResults(1000);
      // TODO: Review Organization filter
      return query.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void kill(ProcessBundle processBundle) throws Exception {
    processBundle.getLogger().log("process killed");
    // When kill is called set variable 'killProcess' to true so the process will be interrupted in
    // the next iteration
    killProcess = true;

  }
}