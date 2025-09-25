/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore.process;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.retail.config.CashManagementEvents;
import org.openbravo.retail.config.OBRETCOProductList;
import org.openbravo.retail.copystore.process.CopyStoreProcess.LogLevel;
import org.openbravo.retail.copystore.process.CopyStoreProcess.ProcessType;
import org.openbravo.retail.copystore.process.CopyStoreProcess;
import org.openbravo.retail.posterminal.OBPOSAppPayment;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.retail.posterminal.TerminalType;
import org.openbravo.retail.posterminal.TerminalTypePaymentMethod;

/**
 * Defines all default handlers for properties that shouldn't be managed as default when cloning
 * objects {@link CopyStoreProcess#cloneObject(BaseOBObject, BaseOBObject, boolean)}
 */
public class DefaultPropertiesHandler {
  /**
   * Gets default value from Defaults table in case the original one is not in the tree. If it is in
   * the tree, original value is returned
   * 
   */
  public static BaseOBObject getDefault(BaseOBObject original, String defaultPropName,
      CopyStoreProcess process) {
    if (original == null || process.isInOrgTree((OrganizationEnabled) original)) {
      return original;
    } else {
      return (BaseOBObject) process.defaults.get(defaultPropName);
    }
  }

  /**
   * Gets currency from parameter. If it is not set and getFromStore is true, it is taken from
   * original store.
   * 
   */
  static Currency getCurrency(CopyStoreProcess process, boolean getFromStore) {
    String currencyId = process.getParam("currency");
    Currency currency = null;
    if (currencyId != null) {
      currency = OBDal.getInstance().get(org.openbravo.model.common.currency.Currency.class,
          currencyId);
    } else if (getFromStore) {
      currency = process.originalStore.getCurrency();
    }
    return currency;
  }

  /**
   * Holder for all classes implementing Handlers for Organiation
   * 
   * @author alostale
   * 
   */
  public static class OrganizationProperties {
    @Qualifier("Organization.searchKey")
    public static class OrganizationSearchKey extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newStore,
          CopyStoreProcess process) {
        process.newStore.setSearchKey(process.getParam("searchKey"));
      }
    }

    @Qualifier("Organization.name")
    public static class OrganizationName extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newStore,
          CopyStoreProcess process) {
        process.newStore.setName(process.getParam("name"));
      }
    }

    @Qualifier("Organization.ready")
    public static class OrganizationReady extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newStore,
          CopyStoreProcess process) {
        process.newStore.setReady(Boolean.FALSE);
      }
    }

    @Qualifier("Organization.currency")
    public static class OrganizationCurrencyProperty extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newStore,
          CopyStoreProcess process) {
        ((Organization) newStore).setCurrency(getCurrency(process, true));
      }
    }

    @Qualifier("Organization.obretcoCBpartner")
    public static class BP extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        process.newStore.setObretcoCBpartner(process.anonymousBP);
      }
    }

    @Qualifier("Organization.obretcoCBpLocation")
    public static class BPLocation extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        process.newStore.setObretcoCBpLocation((Location) process.newObjects.get("bpLocation"));
      }
    }

    @Qualifier("Organization.obretcoPricelist")
    public static class OrganizationPriceList extends PropertyHandler {
      /**
       * Reuse price list if it is in tree, if not, create a new one
       */
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        process.newStore.setObretcoPricelist(process.getPriceList());
      }
    }

    @Qualifier("Organization.obretcoProductlist")
    public static class Assortment extends PropertyHandler {
      /**
       * Reuse assortment if it is in tree, if not, create a new one
       */
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        String assortmentId = process.getParam("assortment");
        OBRETCOProductList assortment;
        if (assortmentId != null) {
          assortment = OBDal.getInstance().get(OBRETCOProductList.class, assortmentId);
        } else {
          assortment = OBProvider.getInstance().get(OBRETCOProductList.class);

          process.cloneObject(process.originalStore.getObretcoProductlist(), assortment, true);

          process.newObjects.put("assortment", assortment);
          process.addLog(LogLevel.WARN, OBMessageUtils.messageBD("OBPOSCS_EmptyAssortment"));
        }
        process.newStore.setObretcoProductlist(assortment);
      }
    }

    @Qualifier("Organization.obretcoDbpBpcatid")
    public static class DefaultBPCategory extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("obretcoDbpBpcatid", process.anonymousBP.getBusinessPartnerCategory());
      }
    }

    @Qualifier("Organization.obretcoDbpCountryid")
    public static class DefaultBPCountry extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("obretcoDbpCountryid", process.getCountry());
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Price List
   * 
   * @author alostale
   * 
   */

  public static class PriceListProperties {
    @Qualifier("PricingPriceList.name")
    public static class PriceListName extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("name", process.getParam("searchKey") + " - Price List");
      }
    }

    @Qualifier("PricingPriceList.currency")
    public static class PriceListCurrencyProperty extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("currency", getCurrency(process, true));
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Assortment
   * 
   * @author alostale
   * 
   */

  public static class AssortmentProperties {
    @Qualifier("OBRETCO_ProductList.name")
    public static class AssortmentName extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("name", process.getParam("searchKey") + " - Assortment");
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Financial Account
   * 
   * @author alostale
   * 
   */

  public static class FinancialAccount {
    @Qualifier("FIN_Financial_Account.initialBalance")
    public static class InitialBalance extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("initialBalance", BigDecimal.ZERO);
      }
    }

    @Qualifier("FIN_Financial_Account.currency")
    public static class Currency extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        org.openbravo.model.common.currency.Currency currency = getCurrency(process, false);
        if (currency == null) {
          currency = ((FIN_FinancialAccount) originalObject).getCurrency();
        }
        newObject.set("currency", currency);
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Payment Method in Financial Account
   * 
   * @author alostale
   * 
   */

  public static class FinancialAccountPaymentMethod {
    @Qualifier("FinancialMgmtFinAccPaymentMethod.account")
    public static class Account extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        String originalAcctId = ((FinAccPaymentMethod) originalObject).getAccount().getId();
        newObject.set("account",
            process.newObjects.get(FIN_FinancialAccount.ENTITY_NAME + "-" + originalAcctId));
      }
    }

    @Qualifier("FinancialMgmtFinAccPaymentMethod.paymentMethod")
    public static class PymtMethod extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        FIN_PaymentMethod originalPaymentMethod = ((FinAccPaymentMethod) originalObject)
            .getPaymentMethod();
        newObject.set("paymentMethod",
            process.getObjectInTree(originalPaymentMethod, new MutableBoolean(false)));
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Cash Management Events
   * 
   * @author alostale
   * 
   */

  public static class CashMgtEvents {
    @Qualifier("OBRETCO_CashManagementEvents.name")
    public static class CashMgtEventsName extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        CashManagementEvents originalEvent = (CashManagementEvents) originalObject;
        String name;
        if (originalEvent.getName().contains(originalEvent.getOrganization().getSearchKey())) {
          name = originalEvent.getName().replace(originalEvent.getOrganization().getSearchKey(),
              process.getParam("searchKey"));
        } else {
          name = originalEvent.getName() + " - " + process.getParam("searchKey");
        }
        newObject.set("name", name);
      }
    }

    @Qualifier("OBRETCO_CashManagementEvents.currency")
    public static class CashMgtEventsCurrency extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        org.openbravo.model.common.currency.Currency currency = getCurrency(process, false);
        if (currency == null) {
          currency = ((CashManagementEvents) originalObject).getCurrency();
        }
        newObject.set("currency", currency);
      }
    }

    @Qualifier("OBRETCO_CashManagementEvents.paymentMethod")
    public static class CashMgtEventsPaymentMethod extends PropertyHandler {

      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        FIN_PaymentMethod originalPaymentMethod = ((CashManagementEvents) originalObject)
            .getPaymentMethod();
        newObject.set("paymentMethod",
            process.getObjectInTree(originalPaymentMethod, new MutableBoolean(false)));
      }
    }

    @Qualifier("OBRETCO_CashManagementEvents.financialAccount")
    public static class CashMgtEventsFinancialAccount extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        FIN_FinancialAccount originalFinAcct = ((CashManagementEvents) originalObject)
            .getFinancialAccount();
        newObject.set("financialAccount",
            process.getObjectInTree(originalFinAcct, new MutableBoolean(false)));
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Terminal
   * 
   * @author alostale
   * 
   */

  public static class Terminal {
    @Qualifier("OBPOS_Applications.obposTerminaltype")
    public static class TerminalPOSTerminalType extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        TerminalType originalType = ((OBPOSApplications) originalObject).getObposTerminaltype();
        MutableBoolean isNew = new MutableBoolean(false);
        TerminalType selectedType = (TerminalType) process.getObjectInTree(originalType, isNew);
        newObject.set("obposTerminaltype", selectedType);
        if (isNew.booleanValue()) {
          for (TerminalTypePaymentMethod pymt : originalType.getOBPOSAppPaymentTypeList()) {
            TerminalTypePaymentMethod newPymt = OBProvider.getInstance().get(
                TerminalTypePaymentMethod.class);
            process.cloneObject(pymt, newPymt, false);
            newPymt.setObposTerminaltype(selectedType);
            OBDal.getInstance().save(newPymt);
            process.newObjects.put(TerminalTypePaymentMethod.ENTITY_NAME + "-" + pymt.getId(),
                newPymt);
          }

          // once the payment methods are copied, reuse it in header
          if (originalType.getPaymentMethod() != null) {
            selectedType.setPaymentMethod((TerminalTypePaymentMethod) process.getObjectInTree(
                originalType.getPaymentMethod(), new MutableBoolean(false)));
          }
        }
      }
    }

    @Qualifier("OBPOS_Applications.searchKey")
    public static class TerminalSearchKey extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        String searchKey;
        if (process.type == ProcessType.copyStore) {
          String originalKey = (String) originalObject.get("searchKey");
          String suffix;

          // Looking for digits, assuming them as suffix (starting from first one)
          int i = 0;
          while (i < originalKey.length() && !Character.isDigit(originalKey.charAt(i))) {
            i++;
          }
          if (i < originalKey.length()) {
            // there are digits in the original key
            suffix = originalKey.substring(i);
            if (!process.verifySuffix(suffix)) {
              suffix = suffix + process.getNewTerminalIdx();
            }
          } else {
            suffix = process.getNewTerminalIdx();
          }

          searchKey = process.getParam("terminalSearchKey") + suffix;
        } else {
          searchKey = process.getParam("terminalSearchKey");
        }
        newObject.set("searchKey", searchKey);
      }
    }

    @Qualifier("OBPOS_Applications.name")
    public static class TerminalName extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        if (process.type == ProcessType.copyStore) {
          // assuming searchKey is set before than name
          newObject.set("name", newObject.get("searchKey"));
        } else {
          // assuming searchKey is set before than name
          newObject.set("name", process.getParam("terminalName"));
        }
      }
    }

    @Qualifier("OBPOS_Applications.orderdocnoPrefix")
    public static class TerminalDocPrefix extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        // assuming searchKey is set before than name
        newObject.set("orderdocnoPrefix", newObject.get("searchKey"));
      }
    }

    @Qualifier("OBPOS_Applications.quotationdocnoPrefix")
    public static class TerminalDocQuotationPrefix extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        // assuming searchKey is set before than name
        newObject.set("quotationdocnoPrefix", newObject.get("searchKey") + "QT");
      }
    }

    @Qualifier("OBPOS_Applications.hardwareurl")
    public static class TerminalHwURL extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        String url = null;
        if (process.type == ProcessType.copyTerminal) {
          url = process.getParam("hwUrl");
        }
        newObject.set("hardwareurl", url);
      }
    }

    @Qualifier("OBPOS_Applications.scaleurl")
    public static class TerminalScaleURL extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        String url = null;
        if (process.type == ProcessType.copyTerminal) {
          url = process.getParam("scaleUrl");
        }
        newObject.set("scaleurl", url);
      }
    }

    /**
     * Note this should be included within some module depending on Sessions Management
     * 
     */
    @Qualifier("OBPOS_Applications.possSession")
    public static class TerminalOpenTill extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("possSession", false);
      }
    }

    @Qualifier("OBPOS_Applications.terminalKey")
    public static class TerminalTerminalKeyIdentifier extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        // assuming searchKey is set before than name
        if (ModelProvider.getInstance().getEntity(OBPOSApplications.class)
            .hasProperty("terminalKey")) {
          newObject.set("terminalKey", null);
        }
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Payment Types in Terminal
   * 
   * @author alostale
   * 
   */

  public static class TerminalPaymentTypeProperties {
    @Qualifier("OBPOS_App_Payment_Type.documentType")
    public static class TerminalPaymentTypeDocType extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        DocumentType newDoc = null;
//        DocumentType newDoc = process.getDocumentType(((TerminalTypePaymentMethod) originalObject)
//            .getDocumentType());
////            .getObposTerminaltype().getDocumentType());
        newObject.set("documentType", newDoc);
      }
    }

    @Qualifier("OBPOS_App_Payment_Type.glitemWriteoff")
    public static class TerminalPaymentTypeWriteoff extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set(
            "glitemWriteoff",
            getDefault((BaseOBObject) originalObject.get("glitemWriteoff"), "gLItemForWriteoff",
                process));
      }
    }

    @Qualifier("OBPOS_App_Payment_Type.cashDifferences")
    public static class TerminalPaymentTypeCashDifferences extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set(
            "cashDifferences",
            getDefault((BaseOBObject) originalObject.get("cashDifferences"), "cashDifferences",
                process));
      }
    }

    @Qualifier("OBPOS_App_Payment_Type.paymentMethod")
    public static class TerminalPaymentTypePaymentMethod extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        FIN_PaymentMethod originalPaymentMethod = ((TerminalTypePaymentMethod) originalObject)
            .getPaymentMethod();
        BaseOBObject newPaymentMethod = process.getObjectInTree(originalPaymentMethod,
            new MutableBoolean(false));

        newObject.set("paymentMethod", newPaymentMethod);
      }
    }

    @Qualifier("OBPOS_App_Payment_Type.gLItemForDrops")
    public static class TerminalPaymentTypeGLItemForDrops extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        GLItem originalGLItem = (GLItem) originalObject.get("gLItemForDrops");
        if (originalGLItem == null) {
          return;
        }

        newObject.set(
            "gLItemForDrops",
            getDefault((BaseOBObject) originalObject.get("gLItemForDrops"), "gLItemForWithdrawals",
                process));
      }
    }

    @Qualifier("OBPOS_App_Payment_Type.gLItemForDeposits")
    public static class TerminalPaymentTypeGLItemForDeposits extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        GLItem originalGLItem = (GLItem) originalObject.get("gLItemForDeposits");
        if (originalGLItem == null) {
          return;
        }

        newObject.set(
            "gLItemForDeposits",
            getDefault((BaseOBObject) originalObject.get("gLItemForDeposits"), "gLItemForDeposits",
                process));
      }
    }

    @Qualifier("OBPOS_App_Payment_Type.glitemDropdep")
    public static class TerminalPaymentTypeGLItemDropdep extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        GLItem originalGLItem = (GLItem) originalObject.get("glitemDropdep");
        if (originalGLItem == null) {
          return;
        }

        newObject.set(
            "glitemDropdep",
            getDefault((BaseOBObject) originalObject.get("glitemDropdep"),
                "gLItemForCashDropDeposit", process));
      }
    }

    @Qualifier("OBPOS_App_Payment_Type.currency")
    public static class TerminalPaymentTypeCurrencyProperty extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        Currency currency = getCurrency(process, false);
        if (currency == null) {
          currency = (Currency) originalObject.get("currency");
        }
        newObject.set("currency", currency);
      }
    }

  }

  public static class TerminalPaymentProperties {
    @Qualifier("OBPOS_App_Payment.financialAccount")
    public static class PaymentFinancialAccount extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        FIN_FinancialAccount origAcct = ((OBPOSAppPayment) originalObject).getFinancialAccount();
        FIN_FinancialAccount newAcct;

        MutableBoolean createdNewAcct = new MutableBoolean(false);
        if (process.type == ProcessType.copyStore) {
          // when copying store reuse the one created in the tree
          newAcct = (FIN_FinancialAccount) process.getObjectInTree(origAcct, createdNewAcct);
        } else {
          // when copying terminal force creation
          createdNewAcct.setValue(true);
          newAcct = OBProvider.getInstance().get(FIN_FinancialAccount.class);
          if (origAcct != null) {
            process.cloneObject(origAcct, newAcct, true);
            process.newObjects.put(FIN_FinancialAccount.ENTITY_NAME + "-" + origAcct.getId(),
                newAcct);
          } else {
            createdNewAcct.setValue(false);
          }
        }
        newObject.set("financialAccount", newAcct);

        if (createdNewAcct.booleanValue()) {
          process.addPaymentMethods(origAcct, newAcct);
        }
      }
    }

    @Qualifier("OBPOS_App_Payment.paymentMethod")
    public static class PaymentMethod extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        TerminalTypePaymentMethod originalPaymentMethod = ((OBPOSAppPayment) originalObject)
            .getPaymentMethod();
        BaseOBObject newPaymentMethod = process.getObjectInTree(originalPaymentMethod,
            new MutableBoolean(false));
        newObject.set("paymentMethod", newPaymentMethod);
      }
    }

    @Qualifier("OBPOS_App_Payment.obretcoCmevents")
    public static class CashUpEvent extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {

        CashManagementEvents originalCashUpEvent = ((OBPOSAppPayment) originalObject)
            .getObretcoCmevents();
        CashManagementEvents newCashUpEvent;
        if (process.type == ProcessType.copyStore) {
          if (originalCashUpEvent != null) {
            newCashUpEvent = (CashManagementEvents) process.newObjects.get("CashMgtEvent-"
                + originalCashUpEvent.getId());
          } else {
            newCashUpEvent = null;
          }
        } else {
          newCashUpEvent = (CashManagementEvents) process.getObjectInTree(originalCashUpEvent,
              new MutableBoolean(false));
        }
        newObject.set("obretcoCmevents", newCashUpEvent);
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Terminal Type
   * 
   * @author alostale
   * 
   */

  public static class TerminalTypeProperties {

    @Qualifier("OBPOS_TerminalType.documentType")
    public static class DocType extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {

        DocumentType newDoc = process.getDocumentType(((TerminalType) originalObject)
            .getDocumentType());
        newObject.set("documentType", newDoc);
      }
    }

    @Qualifier("OBPOS_TerminalType.documentTypeForReturns")
    public static class DocTypeRet extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        DocumentType newDoc = process.getDocumentType(((TerminalType) originalObject)
            .getDocumentTypeForReturns());
        newObject.set("documentTypeForReturns", newDoc);
      }
    }

    @Qualifier("OBPOS_TerminalType.documentTypeForQuotations")
    public static class DocTypeQuotation extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        DocumentType newDoc = process.getDocumentType(((TerminalType) originalObject)
            .getDocumentTypeForQuotations());
        newObject.set("documentTypeForQuotations", newDoc);
      }
    }

    @Qualifier("OBPOS_TerminalType.documentTypeForReconciliations")
    public static class DocTypeRecon extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        DocumentType newDoc = process.getDocumentType(((TerminalType) originalObject)
            .getDocumentTypeForReconciliations());
        newObject.set("documentTypeForReconciliations", newDoc);
      }
    }
  }

  public static class AnonymousBP {
    @Qualifier("BusinessPartner.searchKey")
    public static class AnonymousBPSearchKey extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        BusinessPartner originalEvent = (BusinessPartner) originalObject;
        String searchKey;
        if (originalEvent.getSearchKey().contains(originalEvent.getOrganization().getSearchKey())) {
          searchKey = originalEvent.getSearchKey().replace(
              originalEvent.getOrganization().getSearchKey(), process.getParam("searchKey"));
        } else {
          searchKey = process.getParam("searchKey") + " - " + originalEvent.getSearchKey();
        }
        newObject.set("searchKey", searchKey);
      }
    }

    @Qualifier("BusinessPartner.name")
    public static class AnonymousBPName extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        BusinessPartner originalEvent = (BusinessPartner) originalObject;
        String name;
        if (originalEvent.getName().contains(originalEvent.getOrganization().getSearchKey())) {
          name = originalEvent.getName().replace(originalEvent.getOrganization().getSearchKey(),
              process.getParam("searchKey"));
        } else {
          name = process.getParam("searchKey") + " - " + originalEvent.getName();
        }
        newObject.set("name", name);
      }
    }

    @Qualifier("BusinessPartner.businessPartnerCategory")
    public static class BPCategory extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("businessPartnerCategory",
            OBDal.getInstance().getProxy(Category.ENTITY_NAME, process.getParam("bpCategory")));
      }
    }

    @Qualifier("BusinessPartner.priceList")
    public static class AnonymousBPPriceList extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("priceList", process.getPriceList());
      }
    }
  }

  /**
   * Holder for all classes implementing Handlers for Role
   * 
   * @author alostale
   * 
   */
  public static class RoleProperties {
    @Qualifier("ADRole.name")
    public static class RoleName extends PropertyHandler {
      @Override
      public void handleProperty(BaseOBObject originalObject, BaseOBObject newObject,
          CopyStoreProcess process) {
        newObject.set("name", originalObject.get("name") + "-" + process.getParam("searchKey"));
      }
    }
  }

  /**
   * Defines all properties that are null by default
   * 
   * @author alostale
   * 
   */
  public static class DefaulBlankProperties implements BlankProperties {

    @Override
    public void addBlankProperties(List<String> blankProperties) {
      // Financial Account
      blankProperties.add("FIN_Financial_Account.description");
      blankProperties.add("FIN_Financial_Account.businessPartner");
      blankProperties.add("FIN_Financial_Account.location");

      // Financial Account bank info
      blankProperties.add("FIN_Financial_Account.bankCode");
      blankProperties.add("FIN_Financial_Account.branchCode");
      blankProperties.add("FIN_Financial_Account.bankDigitcontrol");
      blankProperties.add("FIN_Financial_Account.accountDigitcontrol");
      blankProperties.add("FIN_Financial_Account.partialAccountNo");

      blankProperties.add("Organization.pOSSBusinessDate");
    }
  }

}
