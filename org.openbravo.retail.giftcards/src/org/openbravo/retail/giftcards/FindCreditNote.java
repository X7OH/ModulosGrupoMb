/*
 ************************************************************************************
 * Copyright (C) 2017-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.giftcards;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.posterminal.JSONProcessSimple;

public class FindCreditNote extends JSONProcessSimple {
  @Override
  public JSONObject exec(JSONObject jsonData) throws JSONException, ServletException {
    try {
      OBContext.setAdminMode(false);
      BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class,
          jsonData.getJSONObject("bp").getString("id"));
      JSONObject creditNote = new JSONObject();
      GiftCardInst result = creditNoteLast(bp);
      int status = 0;

      creditNote.put("searchKey", result != null ? result.getSearchKey() : null);
      creditNote.put("businessPartnerId", result != null ? result.getBusinessPartner() : null);
      creditNote.put("businessPartnerName", result != null ? result.getBusinessPartner()
          .getIdentifier() : null);
      creditNote.put("amount", result != null ? result.getAmount() : null);
      creditNote.put("currentAmount", result != null ? result.getCurrentamount() : null);

      JSONObject finalResult = new JSONObject();
      finalResult.put("data", creditNote);
      finalResult.put("status", status);
      return finalResult;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static GiftCardInst creditNoteLast(BusinessPartner bp) {
    OBContext.setAdminMode(true);
    try {
      final OBCriteria<GiftCardInst> giftCardInstCriteria = OBDal.getInstance().createCriteria(
          GiftCardInst.class);
      giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_BUSINESSPARTNER, bp));
      giftCardInstCriteria.add(Restrictions.ne(GiftCardInst.PROPERTY_ALERTSTATUS, "C"));
      giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_ISCANCELLED, false));
      giftCardInstCriteria.add(Restrictions.eq(GiftCardInst.PROPERTY_TYPE, "BasedOnCreditNote"));
      giftCardInstCriteria.setMaxResults(1);
      return (GiftCardInst) giftCardInstCriteria.uniqueResult();
    } catch (Exception e) {
      throw new OBException();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}