/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.retail.giftcard;

import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.retail.posterminal.CustomInitialValidation;
import org.openbravo.retail.posterminal.OBPOSAppPayment;
import org.openbravo.retail.posterminal.OBPOSApplications;

public class AutomaticDepositInitialValidations extends CustomInitialValidation {

  @Override
  public void validation(OBPOSApplications posTerminal) throws JSONException {
    for (final OBPOSAppPayment oBPOSAppPayment : posTerminal.getOBPOSAppPaymentList()) {
      if (oBPOSAppPayment.getSearchKey().equals("SRGC_GiftCard.Tarjetas")
          || oBPOSAppPayment.getSearchKey().equals("GCNV_payment.creditnote")) {
        final String finPaymentMethodId = oBPOSAppPayment.getPaymentMethod().getPaymentMethod()
            .getId();
        final List<FinAccPaymentMethod> finAccPaymentMethodList = oBPOSAppPayment
            .getFinancialAccount().getFinancialMgmtFinAccPaymentMethodList();
        for (final FinAccPaymentMethod finAccPaymentMethod : finAccPaymentMethodList) {
          if (finAccPaymentMethod.getPaymentMethod().getId().equals(finPaymentMethodId)
              && !finAccPaymentMethod.isAutomaticDeposit()) {
            throw new JSONException("GCNV_NotAutomaticDeposit");
          }
        }
      }
    }
  }
}
