/*
 ************************************************************************************
 * Copyright (C) 2016-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.retail.giftcard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.retail.posterminal.ProcessHQLQuery;

public class FindGiftCards extends ProcessHQLQuery {

  public static final String findGiftCardsPropertyExtension = "SRGC_FindGiftCardsExtension";

  @Inject
  @Any
  @Qualifier(findGiftCardsPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<HQLPropertyList> getHqlProperties(JSONObject jsonsent) {
    // Get Product Properties
    List<HQLPropertyList> propertiesList = new ArrayList<HQLPropertyList>();
    Map<String, Object> args = new HashMap<String, Object>();
    HQLPropertyList characteristicsHQLProperties = ModelExtensionUtils.getPropertyExtensions(
        extensions, args);
    propertiesList.add(characteristicsHQLProperties);

    return propertiesList;
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    List<String> hqlQueries = new ArrayList<String>();

    HQLPropertyList gcHQLProperties = ModelExtensionUtils.getPropertyExtensions(extensions);

    final StringBuilder hql = new StringBuilder();

    //***************************************************************//
    //               CAPTURO EL VALOR DEL IDENTIFICADOR              // 
    //***************************************************************//
    String search = "";  
    JSONArray remoteFiltersSearch = jsonsent.getJSONArray("remoteFilters");
    for (int i = 0; i < remoteFiltersSearch.length(); i++) {
      if (jsonsent.getJSONArray("remoteFilters").getJSONObject(i).getJSONArray("columns")
          .getString(0).equals("searchKey")) {
          search =  remoteFiltersSearch.getJSONObject(i).getString("value");
        break;
      }
    }
    //***************************************************************//
    //***************************************************************//    

    //***************************************************************//
    //               CAPTURO EL VALOR DEL TERCERO              // 
    //***************************************************************//
    /* String searchbusinessPartner = "";  
    JSONArray remoteFiltersSearchbusinessPartner = jsonsent.getJSONArray("remoteFilters");
    for (int i = 0; i < remoteFiltersSearchbusinessPartner.length(); i++) {
      if (jsonsent.getJSONArray("remoteFilters").getJSONObject(i).getJSONArray("columns")
          .getString(0).equals("businessPartner")) {
          searchbusinessPartner =  remoteFiltersSearchbusinessPartner.getJSONObject(i).getString("value");
        break;
      }
    } */
    //***************************************************************//
    //***************************************************************//      

    hql.append("SELECT " + gcHQLProperties.getHqlSelect() + " FROM GCNV_GiftCardInst AS gci ");
    //hql.append("WHERE $filtersCriteria ");
    //hql.append("WHERE ( (gci.businessPartner.id = :valueEqId0 )  AND  (upper(gci.searchKey) like upper(:valueCon1 ))  AND  (upper(gci.type)=upper(:valueEqUp2 )) ) ");
    //hql.append("WHERE ( (gci.businessPartner.id = '"+ searchbusinessPartner +"' ) AND (upper(gci.searchKey) like upper('%"+ search +"%' )) ) ");
    hql.append("WHERE ( (upper(gci.searchKey) like upper('%"+ search +"%' )) ) ");
    hql.append("AND gci.currentamount > 0 ");
    hql.append("AND gci.iscancelled = 'N' ");

    //***************************************************************//
    //               CAPTURO EL VALOR DEL ESTADO                     // 
    //***************************************************************//    
    boolean isFilterdByStatus = false;
    String alertStatusClause = "";
    JSONArray remoteFilters = jsonsent.getJSONArray("remoteFilters");
    for (int i = 0; i < remoteFilters.length(); i++) {
      if (jsonsent.getJSONArray("remoteFilters").getJSONObject(i).getJSONArray("columns")
          .getString(0).equals("alertStatus")) {
        alertStatusClause =  remoteFilters.getJSONObject(i).getString("value");
        isFilterdByStatus = true;
        break;
      }
    }
    
    if (!isFilterdByStatus) {
      hql.append("AND gci.alertStatus <> 'C' ");
    }else {
      hql.append("AND gci.alertStatus = '"+ alertStatusClause +"' ");
    }
    //***************************************************************//
    //***************************************************************//  

    //***************************************************************//
    //               CAPTURO EL VALOR DEL AMOUNT                     // 
    //***************************************************************//    
    boolean isFilterdByAmount = false;
    String amountClause = "";
    float amountClause1 = Float.parseFloat("0");
    String operadorAmount = "";
    String operadorAmountSymbol = "=";
    JSONArray remoteFiltersAmount = jsonsent.getJSONArray("remoteFilters");
    for (int i = 0; i < remoteFiltersAmount.length(); i++) {
      if (jsonsent.getJSONArray("remoteFilters").getJSONObject(i).getJSONArray("columns")
          .getString(0).equals("amount")) {
        
        amountClause =  remoteFiltersAmount.getJSONObject(i).getString("value");
        amountClause1 = Float.parseFloat(amountClause); 
        operadorAmount =  remoteFiltersAmount.getJSONObject(i).getString("operator");
        
        if(operadorAmount.equals("greaterThan") ) {
          operadorAmountSymbol = ">";
        }else if(operadorAmount.equals("lessThan")) {
          operadorAmountSymbol = "<";
        }else if(operadorAmount.equals("equals")) {
          operadorAmountSymbol = "=";
        }else if(operadorAmount.equals("notEquals")) {
          operadorAmountSymbol = "<>";
        }
        
        isFilterdByAmount = true;
        break;
      }
    }
    
    if (isFilterdByAmount) {
      hql.append("AND gci.amount "+ operadorAmountSymbol +" "+ amountClause1 +" ");
    }
    //***************************************************************//
    //***************************************************************//     

    //***************************************************************//
    //           CAPTURO EL VALOR DEL CURRENTAMOUNT                  // 
    //***************************************************************//    
    boolean isFilterdByCurrentAmount = false;
    String currentAmountClause = "";
    float currentAmountClause1 = Float.parseFloat("0");
    String operadorCurrentAmount = "";
    String operadorCurrentAmountSymbol = "=";
    JSONArray remoteFiltersCurrentAmount = jsonsent.getJSONArray("remoteFilters");
    for (int i = 0; i < remoteFiltersCurrentAmount.length(); i++) {
      if (jsonsent.getJSONArray("remoteFilters").getJSONObject(i).getJSONArray("columns")
          .getString(0).equals("currentamount")) {
        
        currentAmountClause =  remoteFiltersCurrentAmount.getJSONObject(i).getString("value");
        currentAmountClause1 = Float.parseFloat(currentAmountClause); 
        operadorCurrentAmount =  remoteFiltersCurrentAmount.getJSONObject(i).getString("operator");
        
        if(operadorCurrentAmount.equals("greaterThan") ) {
          operadorCurrentAmountSymbol = ">";
        }else if(operadorCurrentAmount.equals("lessThan")) {
          operadorCurrentAmountSymbol = "<";
        }else if(operadorCurrentAmount.equals("equals")) {
          operadorCurrentAmountSymbol = "=";
        }else if(operadorCurrentAmount.equals("notEquals")) {
          operadorCurrentAmountSymbol = "<>";
        }
        
        isFilterdByCurrentAmount = true;
        break;
      }
    }
    
    if (isFilterdByCurrentAmount) {
      hql.append("AND gci.currentamount "+ operadorCurrentAmountSymbol +" "+ currentAmountClause1 +" ");
    }
    //***************************************************************//
    //***************************************************************//        
    
    //filtra por cliente y por organizacion
    hql.append("AND gci.$readableSimpleClientCriteria AND gci.$naturalOrgCriteria ");
    
    if (jsonsent.has("orderByClause") && jsonsent.get("orderByClause") != JSONObject.NULL) {
      hql.append("$orderByCriteria");
    }

    hqlQueries.add(hql.toString());
    
    return hqlQueries;
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

}
