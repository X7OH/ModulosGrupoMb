/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.retail.giftcard;

import java.util.ArrayList;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.retail.giftcards.org.openbravo.retail.giftcards.GiftCardInst;
import org.openbravo.retail.posterminal.JSONProcessSimple;

public class CheckDuplicityOfID extends JSONProcessSimple {
  @Override
  public JSONObject exec(JSONObject jsonData) throws JSONException, ServletException {
    String strGCId = jsonData.getString("gcid");
    JSONArray gcids = null;
    if ("null".equals(strGCId)) {
      gcids = jsonData.getJSONArray("gcids");
    }
    JSONObject result = checkCardId(strGCId, gcids);
    int status = 0;

    JSONObject finalResult = new JSONObject();
    finalResult.put("data", result);
    finalResult.put("status", status);
    return finalResult;
  }

  public static JSONObject checkCardId(String strGCId, JSONArray gcids) {
    JSONObject result = new JSONObject();
    ArrayList<Object> objIds = new ArrayList<Object>();

    OBContext.setAdminMode(true);
    try {
      OrganizationStructureProvider orgStructure = new OrganizationStructureProvider();

      OBCriteria<GiftCardInst> obc = OBDal.getInstance().createCriteria(GiftCardInst.class);
      if (!"null".equals(strGCId)) {
        obc.add(Restrictions.eq(GiftCardInst.PROPERTY_SEARCHKEY, strGCId));
      } else {
        for (int i = 0; i < gcids.length(); i++) {
          objIds.add(gcids.getJSONObject(i).getString("gcid"));
        }
        obc.add(Restrictions.in(GiftCardInst.PROPERTY_SEARCHKEY, objIds));
      }
      obc.add(Restrictions.in(GiftCardInst.PROPERTY_ORGANIZATION + ".id", orgStructure
          .getParentList(OBContext.getOBContext().getCurrentOrganization().getId(), true)));
      obc.setMaxResults(1);
      if (obc.uniqueResult() == null) {
        result.put("used", false);
      } else {
        GiftCardInst gcins = (GiftCardInst) obc.uniqueResult();
        result.put("used", true);
        result.put("id", gcins.getSearchKey());
        result.put("cancellation", true);
      }

    } catch (Exception e) {
      throw new OBException();
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}