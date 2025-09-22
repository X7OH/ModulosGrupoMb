/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.retail.copystore.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.servlet.ServletException;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.InitialSetupUtility;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleInheritance;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentTemplate;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.Location;
import org.openbravo.model.common.geography.Region;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.retail.config.CashManagementEvents;
import org.openbravo.retail.copystore.OBPOSCS_Defaults;
import org.openbravo.retail.posterminal.OBPOSAppPayment;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies an store based on an other one
 * 
 * @author alostale
 * 
 */
public class CopyStoreProcess {
  private static final Logger log = LoggerFactory.getLogger(CopyStoreProcess.class);

  public enum LogLevel {
    ERROR, WARN, INFO, SUCCESS
  }

  public enum ProcessType {
    copyStore, copyTerminal
  }

  private List<LogEntry> processLog = new ArrayList<LogEntry>();

  private Instance<PropertyHandler> propertyHandlers;
  private Instance<FKPropertyHandler> fkPropertyHandlers;

  public boolean hasErrors = false;

  private JSONObject params;

  public Organization originalStore;

  public Map<String, BaseOBObject> newObjects = new HashMap<String, BaseOBObject>();

  public Organization newStore;
  public BusinessPartner anonymousBP;

  private List<String> orgTree;

  private List<String> blankProperties;

  private int terminalIdx = 0;

  public OBPOSCS_Defaults defaults;

  private Organization parentOrg;

  private Country country;
  public ProcessType type;

  private boolean validated;

  private Instance<PostProcessHandler> postProcessHandlers;

  private List<String> terminalIdList = new ArrayList<String>();

  public CopyStoreProcess(JSONObject params, Instance<PropertyHandler> propertyHandlers,
      Instance<FKPropertyHandler> fkPropertyHandlers,
      Instance<BlankProperties> blankPropertiesHandlers,
      Instance<PostProcessHandler> postProcessHandlers) {
    this.params = params;
    // cannot directly inject here because constructor is used
    this.propertyHandlers = propertyHandlers;
    this.fkPropertyHandlers = fkPropertyHandlers;
    this.postProcessHandlers = postProcessHandlers;

    if (getParam("defaults") != null) {
      defaults = OBDal.getInstance().get(OBPOSCS_Defaults.class, getParam("defaults"));
    }

    blankProperties = new ArrayList<String>();

    for (BlankProperties bph : blankPropertiesHandlers) {
      bph.addBlankProperties(blankProperties);
    }
  }

  /**
   * Performs some basic validations on the parameters before starting the Copy Store Process
   * 
   */
  public boolean validateCopyStore() {
    addLog(LogLevel.INFO, OBMessageUtils.messageBD("OBPOSCS_Prevalidations"));

    if (getParam("anonymousCustomer") == null && getParam("bpCategory") == null) {
      addLog(LogLevel.ERROR, OBMessageUtils.messageBD("OBPOSCS_MissingBPorCat"));
    }

    OBCriteria<Organization> qOrgKey = OBDal.getInstance().createCriteria(Organization.class);
    qOrgKey.add(Restrictions.eq(Organization.PROPERTY_SEARCHKEY, getParam("searchKey")));
    if (qOrgKey.count() > 0) {
      addLog(LogLevel.ERROR, OBMessageUtils.messageBD("OBPOSCS_ExistingValue"));
    }

    OBCriteria<Organization> qOrgName = OBDal.getInstance().createCriteria(Organization.class);
    qOrgName.add(Restrictions.eq(Organization.PROPERTY_NAME, getParam("name")));
    if (qOrgName.count() > 0) {
      addLog(LogLevel.ERROR, OBMessageUtils.messageBD("OBPOSCS_ExistingName"));
    }

    validated = !hasErrors;

    if (!validated) {
      addLog(LogLevel.WARN, OBMessageUtils.messageBD("OBPOSCS_PrevalidationsFailed"));
    }
    return validated;
  }

  /**
   * Performs some basic validations on the parameters before starting the Copy Terminal Process
   * 
   */
  public boolean validateCopyTerminal() {
    addLog(LogLevel.INFO, OBMessageUtils.messageBD("OBPOSCS_Prevalidations"));
    OBCriteria<OBPOSApplications> qTerminalSearchKey = OBDal.getInstance().createCriteria(
        OBPOSApplications.class);
    qTerminalSearchKey.add(Restrictions.eq(OBPOSApplications.PROPERTY_SEARCHKEY,
        getParam("terminalSearchKey")));
    if (qTerminalSearchKey.count() > 0) {
      addLog(LogLevel.ERROR, OBMessageUtils.messageBD("OBPOSCS_ExistingTerminalValue"));
    }

    OBCriteria<OBPOSApplications> qTerminalName = OBDal.getInstance().createCriteria(
        OBPOSApplications.class);
    qTerminalName.add(Restrictions.eq(OBPOSApplications.PROPERTY_SEARCHKEY,
        getParam("terminalName")));
    if (qTerminalName.count() > 0) {
      addLog(LogLevel.ERROR, OBMessageUtils.messageBD("OBPOSCS_ExistingTerminalName"));
    }

    validated = !hasErrors;

    if (!validated) {
      addLog(LogLevel.WARN, OBMessageUtils.messageBD("OBPOSCS_PrevalidationsFailed"));
    }
    return validated;
  }

  public boolean isValidated() {
    return validated;
  }

  /**
   * Execute the whole process
   */
  public void execute() {
    OBContext.setAdminMode();
    OBInterceptor.setDisableCheckReferencedOrganizations(true);

    try {
      type = ProcessType.copyStore;
      OrganizationStructureProvider orgStructure = OBContext.getOBContext()
          .getOrganizationStructureProvider();
      orgTree = orgStructure.getParentList(params.getString("parentOrg"), true);
      orgTree.add("0");

      originalStore = OBDal.getInstance().get(Organization.class, params.getString("organization"));
      newStore = OBProvider.getInstance().get(Organization.class);

      processAnonymousCustomer();
      copyFinancialAccounts();
      copyCashMgtEvents();
      copyTerminals();

      cloneObject(originalStore, newStore, true);

      OBDal.getInstance().flush();
      orgInfo();
      createWarehouse();

      // resetting new organization to new objects, this cannot be done before because it requieres
      // to be saved in advance
      for (BaseOBObject newObj : newObjects.values()) {
        if (newObj instanceof OrganizationEnabled) {
          ((OrganizationEnabled) newObj).setOrganization(newStore);
        }
      }
      OBDal.getInstance().flush();

      newStore.setObretcoDbpOrgid(newStore);

      postProcess();

      setOrgInTree();
      fixPermissions();
      setOrgReady();
    } catch (Exception e) {
      handleExecption(e);
    } catch (Throwable e) {
      hasErrors = true;
      log.error("Error in process", e);

      addLog(LogLevel.ERROR, e.getMessage());

    } finally {
      if (hasErrors) {
        OBDal.getInstance().rollbackAndClose();
        addLog(LogLevel.INFO, OBMessageUtils.messageBD("OBPOSCS_RollBack"));
      }
      OBContext.restorePreviousMode();
    }
  }

  public void executeCopyTerminal(String terminalId) {
    OBContext.setAdminMode();

    try {
      type = ProcessType.copyTerminal;
      OBPOSApplications originalTerminal = OBDal.getInstance().get(OBPOSApplications.class,
          terminalId);

      OrganizationStructureProvider orgStructure = OBContext.getOBContext()
          .getOrganizationStructureProvider();
      orgTree = orgStructure.getParentList(originalTerminal.getOrganization().getId(), true);
      orgTree.add("0");

      // hack to directly set the correct org to new object
      parentOrg = originalTerminal.getOrganization();

      copySingelTerminal(originalTerminal);
      postProcess();

      OBDal.getInstance().flush();

    } catch (Exception e) {
      handleExecption(e);
    } catch (Throwable e) {
      hasErrors = true;
      log.error("Error in process", e);

      addLog(LogLevel.ERROR, e.getMessage());

    } finally {
      if (hasErrors) {
        OBDal.getInstance().rollbackAndClose();
        addLog(LogLevel.INFO, OBMessageUtils.messageBD("OBPOSCS_RollBack"));
      }
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generic process to clone objects.
   * 
   * It iterates over all properties and executes this logic:
   * <ul>
   * <li>Primitive properties are directly copied from original to new object
   * <li>FK properties are copied just in case the referenced object is in the new organization
   * tree, if it is not the porperty is set as {@code null}
   * </ul>
   * 
   * This default logic can be overwritten for concrete properties:
   * <ul>
   * <li>Having this property as blank in {@link BlankProperties}
   * <li>Implementing a {@link PropertyHandler} with its qualifier being [EntityName].[PropertyName]
   * <li>In case of FK defining a {@link FKPropertyHandler} with its qualifier being
   * [EntityName].[ReferencedEntityName]. In this case all FK properties linking to that entity will
   * be processed by this handler
   * </ul>
   * 
   * @param originalObject
   *          original object to copy from
   * @param newObject
   *          destination object to copy to
   * @param save
   *          should OBDal.save be performed on new object
   */
  public void cloneObject(BaseOBObject originalObject, BaseOBObject newObject, boolean save) {
    String entityName = originalObject.getEntityName();

    if (newObject instanceof OrganizationEnabled) {
      // for now setting parent org as new object's org, later it will be changed to actual org
      // this is to prevent cycles with non yet created org
      ((OrganizationEnabled) newObject).setOrganization(getParentOrg());
    }

    for (Property prop : originalObject.getEntity().getProperties()) {
      if (prop.isComputedColumn() || prop.isAuditInfo() || prop.isId() || prop.isOneToMany()) {
        continue;
      }

      String propName = prop.getName();
      if (Entity.COMPUTED_COLUMNS_PROXY_PROPERTY.equals(propName)) {
        continue;
      }

      if (isBlank(entityName + "." + propName)) {
        newObject.set(propName, null);
        continue;
      }

      if (BusinessPartner.PROPERTY_ORGANIZATION.equals(propName)) {
        // organization is already set
        continue;
      }

      PropertyHandler handler = getHandler(propertyHandlers, entityName + "." + propName);

      if (handler != null) {
        handler.handleProperty(originalObject, newObject, this);
      } else {
        if (prop.isPrimitive()) {
          newObject.set(propName, originalObject.get(propName));
        } else {
          if (prop.getTargetEntity() != null) {
            FKPropertyHandler fkHandler = getHandler(fkPropertyHandlers, entityName + "."
                + prop.getTargetEntity().getName());

            if (fkHandler != null) {
              fkHandler.handleProperty(prop.getName(), originalObject, newObject, this);
              continue;
            }
          }

          BaseOBObject value = (BaseOBObject) originalObject.get(propName);
          if (value == null) {
            continue;
          }

          if (!(value instanceof OrganizationEnabled)) {
            continue;
          }

          if (isInOrgTree((OrganizationEnabled) value)) {
            try {
              newObject.set(propName, value);
            } catch (Exception e) {
              log.error("Error setting property {} with value {}", propName, value);
            }
          }
        }
      }
    }

    String logEntityName = getEntityNameForLog(newObject);
    addLog(
        LogLevel.SUCCESS,
        OBMessageUtils.getI18NMessage("OBPOSCS_Copied", new String[] { logEntityName,
            originalObject.getIdentifier(), newObject.getIdentifier() }), newObject);

    if (save) {
      OBDal.getInstance().save(newObject);
    }
  }

  /**
   * Obtains the proper handler based on its priority. Returns null if there's no handler for the
   * given qualifier
   */
  private <T extends PriorityHandler> T getHandler(Instance<T> handlers, String qualifier) {
    T handler = null;
    for (T h : handlers.select(new ComponentProvider.Selector(qualifier))) {
      if (handler == null) {
        handler = h;
      } else if (h.getPriority() < handler.getPriority()) {
        handler = h;
      } else if (h.getPriority() == handler.getPriority()) {
        log.warn(
            "Trying to get istance of {} for qualifier {}, has more than one instance with same priority",
            h.getClass().getName(), qualifier);
      }
    }
    return handler;
  }

  /**
   * Obtains the anonymous customer for the new store, it can be set as parameter or created in the
   * given category
   */
  private void processAnonymousCustomer() {
    String anonBPId = getParam("anonymousCustomer");
    if (anonBPId != null) {
      anonymousBP = (BusinessPartner) OBDal.getInstance().getProxy(BusinessPartner.ENTITY_NAME,
          anonBPId);
    } else {
      BusinessPartner newAnonymousBP = OBProvider.getInstance().get(BusinessPartner.class);

      cloneObject(originalStore.getObretcoCBpartner(), newAnonymousBP, true);

      anonymousBP = newAnonymousBP;
      newObjects.put("AnonymousBP", newAnonymousBP);
      OBDal.getInstance().save(newAnonymousBP);
      addLog(
          LogLevel.SUCCESS,
          OBMessageUtils.getI18NMessage("OBPOSCS_AnonBP",
              new String[] { anonymousBP.getIdentifier() }), newAnonymousBP);
      addLog(LogLevel.WARN, OBMessageUtils.messageBD("OBPOSCS_ReviewBP"));
    }

    org.openbravo.model.common.businesspartner.Location newBPLocation = OBProvider.getInstance()
        .get(org.openbravo.model.common.businesspartner.Location.class);

    newBPLocation.setClient(anonymousBP.getClient());
    // newBPLocation.setOrganization(anonymousBP.getOrganization());
    newBPLocation.setBusinessPartner(anonymousBP);
    newBPLocation.setLocationAddress(createLocation());
    newObjects.put("bpLocation", newBPLocation);
    OBDal.getInstance().save(newBPLocation);
  }

  /**
   * Copies all Financial Accounts existing in original store to new one
   */
  private void copyFinancialAccounts() {
    OBCriteria<FIN_FinancialAccount> qAcct = OBDal.getInstance().createCriteria(
        FIN_FinancialAccount.class);
    qAcct.add(Restrictions.eq(FIN_FinancialAccount.PROPERTY_ORGANIZATION, originalStore));
    for (FIN_FinancialAccount acct : qAcct.list()) {
      FIN_FinancialAccount newAcct = OBProvider.getInstance().get(FIN_FinancialAccount.class);
      cloneObject(acct, newAcct, true);
      newObjects.put(FIN_FinancialAccount.ENTITY_NAME + "-" + acct.getId(), newAcct);
      addPaymentMethods(acct, newAcct);
    }
  }

  /**
   * Adds to newAcct all payment methods present in origAcct
   */
  public void addPaymentMethods(FIN_FinancialAccount origAcct, FIN_FinancialAccount newAcct) {
    for (FinAccPaymentMethod acctPayMethod : origAcct.getFinancialMgmtFinAccPaymentMethodList()) {
      FinAccPaymentMethod newAcctPayMethod = OBProvider.getInstance()
          .get(FinAccPaymentMethod.class);

      cloneObject(acctPayMethod, newAcctPayMethod, true);
      newObjects.put("FinAcctMethod-" + acctPayMethod.getId(), newAcctPayMethod);
    }
  }

  /**
   * Copies all Cash Management Events existing in original store to new one
   */
  private void copyCashMgtEvents() {
    for (CashManagementEvents originalEvent : originalStore.getOBRETCOCashManagementEventsList()) {
      CashManagementEvents newEvent = OBProvider.getInstance().get(CashManagementEvents.class);
      cloneObject(originalEvent, newEvent, true);
      newObjects.put("CashMgtEvent-" + originalEvent.getId(), newEvent);
    }
  }

  /**
   * Copies all Terminals existing in original store to new one
   */
  private void copyTerminals() {
    OBCriteria<OBPOSApplications> qTerminals = OBDal.getInstance().createCriteria(
        OBPOSApplications.class);
    qTerminals.add(Restrictions.eq(OBPOSApplications.PROPERTY_ORGANIZATION, originalStore));
    qTerminals.setFilterOnActive(false);
    for (OBPOSApplications origTerminal : qTerminals.list()) {
      copySingelTerminal(origTerminal);
    }
  }

  private void copySingelTerminal(OBPOSApplications origTerminal) {
    OBPOSApplications newTerminal = OBProvider.getInstance().get(OBPOSApplications.class);
    cloneObject(origTerminal, newTerminal, true);

    newObjects.put("Terminal-" + origTerminal.getId(), newTerminal);
    newTerminal.setLastassignednum((long) 0);

    for (OBPOSAppPayment origPayment : origTerminal.getOBPOSAppPaymentList()) {
      OBPOSAppPayment newPayment = OBProvider.getInstance().get(OBPOSAppPayment.class);
      cloneObject(origPayment, newPayment, false);
      newPayment.setObposApplications(newTerminal);
      OBDal.getInstance().save(newPayment);
      if (isNewObject(newPayment.getFinancialAccount())) {
        // financial account was created during the process, add to its search key terminal's one
        String finName = origPayment.getFinancialAccount().getName();
        if (finName.contains(origTerminal.getSearchKey())) {
          finName = finName.replace(origTerminal.getSearchKey(), newTerminal.getSearchKey());
        } else {
          finName += "-" + newTerminal.getSearchKey();
        }
        newPayment.getFinancialAccount().setName(finName);
      }

      newObjects.put("TerminalPayment-" + origPayment.getId(), newPayment);
    }
  }

  private boolean isNewObject(BaseOBObject obj) {
    for (BaseOBObject newObj : newObjects.values()) {
      if (newObj == obj) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds new location to new store
   */
  private void orgInfo() {
    OBDal.getInstance().refresh(newStore);
    List<OrganizationInformation> infos = newStore.getOrganizationInformationList();
    if (!infos.isEmpty()) {
      OrganizationInformation info = infos.get(0);
      info.setLocationAddress(createLocation());
    } else {
      addLog(LogLevel.WARN, OBMessageUtils.messageBD("OBPOSCS_CouldNoCreateLocation"));
      log.warn("New store has no org info!");
    }
  }

  /**
   * Creates a new Warehouse for the new Store
   */
  private void createWarehouse() {
    Warehouse warehouse = OBProvider.getInstance().get(Warehouse.class);
    String searchKey = getParam("searchKey") + " - Warehouse";
    warehouse.setSearchKey(searchKey);
    warehouse.setName(searchKey);
    warehouse.setLocationAddress(createLocation());
    OBDal.getInstance().save(warehouse);

    Locator bin = OBProvider.getInstance().get(Locator.class);
    bin.setWarehouse(warehouse);
    bin.setSearchKey(getParam("searchKey") + " 0-0-0");
    bin.setRowX("0");
    bin.setStackY("0");
    bin.setLevelZ("0");
    OBDal.getInstance().save(bin);

    OrgWarehouse orgWarehouse = OBProvider.getInstance().get(OrgWarehouse.class);
    orgWarehouse.setWarehouse(warehouse);
    OBDal.getInstance().save(orgWarehouse);

    newObjects.put("warehouse", warehouse);
    newObjects.put("bin", bin);
    newObjects.put("orgWarehouse", orgWarehouse);

    addLog(
        LogLevel.SUCCESS,
        OBMessageUtils.getI18NMessage("OBPOSCS_NewWarehouse",
            new String[] { warehouse.getIdentifier() }), warehouse);
  }

  /**
   * Executes all {@link PostProcessHandler} based on their priority
   * 
   */
  private void postProcess() {
    // get the list of implementators and sort it
    List<PostProcessHandler> sortedHandlers = new ArrayList<PostProcessHandler>();
    for (PostProcessHandler handler : postProcessHandlers) {
      sortedHandlers.add(handler);
    }
    Collections.sort(sortedHandlers, new Comparator<PostProcessHandler>() {
      @Override
      public int compare(PostProcessHandler o1, PostProcessHandler o2) {
        int sort = o1.getPriority() - o2.getPriority();
        if (sort == 0) {
          // same priority sort by name
          sort = o1.getClass().getName().compareTo(o2.getClass().getName());
        }
        return sort;
      }
    });

    // execute post processes
    for (PostProcessHandler handler : sortedHandlers) {
      handler.execute(this);
    }
  }

  /**
   * Adds the new store as a child of its parent organization
   */
  private void setOrgInTree() throws Exception {
    newStore.setReady(false);

    Client client = OBContext.getOBContext().getCurrentClient();
    Tree tree = InitialSetupUtility.getOrgTree(OBContext.getOBContext().getCurrentClient());
    TreeNode orgNode = InitialSetupUtility.getTreeNode(newStore, tree, client);

    InitialSetupUtility.updateOrgTree(tree, orgNode, getParentOrg());

    OBDal.getInstance().flush();
  }

  /**
   * Refreshes permissions to take into account recently created organization
   */
  private void fixPermissions() throws ServletException {
    // Make new stuff visible
    OBContext.getOBContext().addWritableOrganization(newStore.getId());
    OBContext.getOBContext().getOrganizationStructureProvider().reInitialize();

    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    DalConnectionProvider cp = new DalConnectionProvider(false);
    OBContext ctx = OBContext.getOBContext();

    LoginUtils.fillSessionArguments(cp, vars, ctx.getUser().getId(), ctx.getLanguage()
        .getLanguage(), ctx.getLanguage().isRTLLanguage() ? "Y" : "N", ctx.getRole().getId(), ctx
        .getCurrentClient().getId(), ctx.getCurrentOrganization().getId(), ctx.getWarehouse()
        .getId());
    createRoles();
  }

  /**
   * Executes AD_Org_Ready process which does some validations and finally set the new store as
   * ready
   * 
   * @throws ServletException
   */
  private void setOrgReady() throws ServletException {
    ProcessInstance pInstance = CallProcess.getInstance().call("AD_Org_Ready", newStore.getId(),
        null);
    if (pInstance.getResult() == 0L) {
      hasErrors = true;
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      String msg = pInstance.getErrorMsg();

      if (msg.startsWith("@ERROR=")) {
        msg = msg.substring(7);
      }
      addLog(
          LogLevel.ERROR,
          OBMessageUtils.translateError(new DalConnectionProvider(false), vars, vars.getLanguage(),
              msg).getMessage());
    } else {
      addLog(LogLevel.SUCCESS, OBMessageUtils.messageBD("OBPOSCS_OrganizationReady"));
    }
  }

  /**
   * Copies those roles that only have access to original organization. Note these roles are not
   * assigned to any user
   */
  @SuppressWarnings("unchecked")
  private void createRoles() {
    String query = "as r where r.id in ("//
        + "   select ro.role.id" //
        + "     from ADRoleOrganization ro" //
        + "    where exists (select 1 " //
        + "                    from ADRoleOrganization ro2 " //
        + "                   where ro2.role = ro.role " //
        + "                     and ro2.organization.id = :org)" //
        + "    group by ro.role.id" //
        + "   having count(*) = 1)";

    OBQuery<Role> qRole = OBDal.getInstance().createQuery(Role.class, query);
    qRole.setNamedParameter("org", originalStore.getId());

    for (Role origRole : qRole.list()) {
      Role newRole = OBProvider.getInstance().get(Role.class);
      cloneObject(origRole, newRole, false);
      newRole.setOrganization((Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME,
          "0"));
      OBDal.getInstance().save(newRole);
      addLog(LogLevel.INFO, OBMessageUtils.messageBD("OBPOSCS_RoleWithoutUser"));

      RoleOrganization orgAccess = OBProvider.getInstance().get(RoleOrganization.class);
      orgAccess.setOrganization(newStore);
      orgAccess.setRole(newRole);
      OBDal.getInstance().save(orgAccess);

      for (Property prop : origRole.getEntity().getProperties()) {
        // Continue if:
        // 1. It is not one to many
        // 2. It is not InheritedAccessEnabled
        if (!prop.isOneToMany() || !(prop.getTargetEntity().isInheritedAccessEnabled())) {
          continue;
        }
        for (BaseOBObject origAccess : (List<BaseOBObject>) origRole.get(prop.getName())) {
          // 3. It is InheritedAccessEnabled and it is an inherited field
          // 4. It is RoleOrganization
          if (((InheritedAccessEnabled) origAccess).getInheritedFrom() != null
              || origAccess instanceof RoleOrganization) {
            continue;
          }
          BaseOBObject newAccess = (BaseOBObject) OBProvider.getInstance().get(
              origAccess.getEntity().getMappingClass());
          cloneObject(origAccess, newAccess, false);
          if (newAccess instanceof Preference) {
            newAccess.set("visibleAtRole", newRole);
          } else {
            newAccess.set("role", newRole);
          }
          newAccess.set("organization",
              (Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME, "0"));
          OBDal.getInstance().save(newAccess);
        }
      }
      OBDal.getInstance().flush();
      // Copying Role Inheritance, some Access will be inherited according to the Role Inheritance
      // tab information
      for (RoleInheritance origRoleInheritance : origRole.getADRoleInheritanceList()) {
        RoleInheritance newRoleInheritance = OBProvider.getInstance().get(RoleInheritance.class);
        cloneObject(origRoleInheritance, newRoleInheritance, false);
        newRoleInheritance.setRole(newRole);
        newRoleInheritance.setOrganization((Organization) OBDal.getInstance().getProxy(
            Organization.ENTITY_NAME, "0"));
        OBDal.getInstance().save(newRoleInheritance);
      }
    }
    OBDal.getInstance().flush();
  }

  /**
   * Adds logs to be displayed to user after process execution
   * 
   * @param level
   *          log level
   * @param message
   *          message to display. Note the String message will be displayed as is, so translations
   *          should be managed befor invoking this method
   */
  public void addLog(LogLevel level, String message) {
    processLog.add(new LogEntry(level, message));
    if (level == LogLevel.ERROR) {
      log.error("Error in copy store:" + message);
      hasErrors = true;
    }
  }

  /**
   * Same as {@link CopyStoreProcess#addLog(LogLevel, String)} but including a link to a
   * BaseOBObject. Note this link will be only shown in case the whole process has no errors.
   * 
   * @param level
   * @param message
   * @param link
   */
  public void addLog(LogLevel level, String message, BaseOBObject link) {
    LogEntry entry = new LogEntry(level, message);
    entry.link = link;
    processLog.add(entry);
    if (level == LogLevel.ERROR) {
      hasErrors = true;
    }
  }

  /**
   * Returns a human readable entity name based on its associated window to be properly displayed in
   * log
   * 
   */
  public String getEntityNameForLog(BaseOBObject obj) {
    String tableId = obj.getEntity().getTableId();
    Table logTable = OBDal.getInstance().get(Table.class, tableId);
    if (logTable == null || logTable.getWindow() == null) {
      return obj.getEntityName();
    }

    for (Tab tab : logTable.getWindow().getADTabList()) {
      if (tableId.equals(tab.getTable().getId())) {
        String windowName = (String) tab.getWindow().get(Window.PROPERTY_NAME,
            OBContext.getOBContext().getLanguage());
        String tabName = (String) tab
            .get(Tab.PROPERTY_NAME, OBContext.getOBContext().getLanguage());
        String linkName = windowName;
        if (!windowName.equals(tabName)) {
          linkName += " - " + tabName;
        }
        return "<i>" + linkName + "</i>";
      }
    }
    return obj.getEntityName();
  }

  /**
   * Checks if an object is in the new organization tree
   */
  public boolean isInOrgTree(OrganizationEnabled value) {
    return orgTree.contains(((OrganizationEnabled) value).getOrganization().getId());
  }

  /**
   * Returns parent organization set as parameter
   * 
   */
  public Organization getParentOrg() {
    if (parentOrg == null) {
      parentOrg = OBDal.getInstance().get(Organization.class, getParam("parentOrg"));
    }
    return parentOrg;
  }

  /**
   * Returns country set as parameter
   */
  public Country getCountry() {
    if (country == null) {
      try {
        country = (Country) OBDal.getInstance().getProxy(Country.ENTITY_NAME,
            params.getString("country"));
      } catch (JSONException ignore) {
        log.error("error getting country", ignore);
      }
    }
    return country;
  }

  /**
   * Creates new location based on set parameters, note locations are not reused but a new one is
   * create each time it is referenced
   */
  public Location createLocation() {
    Location newLocation = OBProvider.getInstance().get(Location.class);

    Country theCountry = getCountry();

    newLocation.setOrganization(getParentOrg());
    newLocation.setCountry(theCountry);
    newLocation.setAddressLine1(getParam("line1"));
    newLocation.setAddressLine2(getParam("line2"));
    newLocation.setPostalCode(getParam("postalCode"));

    String regionId = getParam("region");
    Region region = null;
    if (regionId != null) {
      region = (Region) OBDal.getInstance().getProxy(Region.ENTITY_NAME, regionId);
      newLocation.setRegion(region);
    }

    String cityName = getParam("city");
    if (cityName != null) {
      City city = OBProvider.getInstance().get(City.class);

      city.setOrganization(getParentOrg());
      city.setName(cityName);
      city.setPostalCode(getParam("postalCode"));
      city.setCountry(theCountry);
      city.setRegion(region);

      OBDal.getInstance().save(city);
      newLocation.setCity(city);
      newLocation.setCityName(cityName);
      newObjects.put("city", city);
    }
    newObjects.put("newLocation-" + SequenceIdData.getUUID(), newLocation);
    OBDal.getInstance().save(newLocation);
    return newLocation;
  }

  /**
   * Returns the value of a given parameter set in UI
   */
  public String getParam(String paramName) {
    if (!params.has(paramName) || params.isNull(paramName)) {
      return null;
    }

    try {
      return params.getString(paramName);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Returns all the generated log as a {@link JSONArray} to be shown in the UI
   * 
   */
  public JSONArray getLog() {
    JSONArray result = new JSONArray();
    for (LogEntry entry : processLog) {
      JSONObject jsonEntry = new JSONObject();
      try {
        jsonEntry.put("level", entry.level);
        jsonEntry.put("msg", entry.msg);
        if (!this.hasErrors && entry.link != null) {
          Table table = OBDal.getInstance().get(Table.class, entry.link.getEntity().getTableId());
          if (table != null && table.getWindow() != null) {
            JSONObject link = new JSONObject();
            link.put("recordId", entry.link.getId());
            link.put("targetEntity", entry.link.getEntityName());
            link.put("keyColumn", entry.link.getEntity().getIdProperties().get(0).getColumnName());
            jsonEntry.put("link", link);
          } else {
            log.warn("Couldn't create link for object {}", entry.link);
          }
        }
        result.put(jsonEntry);
      } catch (JSONException e) {
        log.error("Error generating log");
      }
    }
    return result;
  }

  /**
   * Should the property ([EntityName].[PropertyName]) be set as null
   * 
   * @see BlankProperties
   */
  public boolean isBlank(String propName) {
    return blankProperties.contains(propName);
  }

  /**
   * Looks if originalObject is in new orgTree, if so it can be reused so it is returned.
   * 
   * If it is not, looks if there is already a new object based on the original one.
   * 
   * Finally if none of the cases above is satisfied a new object is created
   * 
   * @param originalObject
   * @param requiredCreation
   * @return
   */
  public BaseOBObject getObjectInTree(BaseOBObject originalObject, MutableBoolean requiredCreation) {
    BaseOBObject selectedObject;
    if (originalObject == null || isInOrgTree((OrganizationEnabled) originalObject)) {
      selectedObject = originalObject;
      requiredCreation.setValue(false);
    } else {
      String newObjectId = originalObject.getEntityName() + "-" + originalObject.getId();
      if (newObjects.containsKey(newObjectId)) {
        selectedObject = (BaseOBObject) newObjects.get(newObjectId);
        requiredCreation.setValue(false);
      } else {
        Class<?> clz = originalObject.getEntity().getMappingClass();
        selectedObject = (BaseOBObject) OBProvider.getInstance().get(clz);
        OBProvider.getInstance().get(clz).getClass();
        cloneObject(originalObject, selectedObject, true);
        newObjects.put(newObjectId, selectedObject);
        requiredCreation.setValue(true);
      }
    }
    return selectedObject;
  }

  /**
   * Calculates and returns a new prefix for terminals
   */
  public String getNewTerminalIdx() {
    terminalIdx += 1;
    return String.format("%02d", terminalIdx);
  }

  /**
   * Verify new suffix for terminals
   */
  public boolean verifySuffix(String suffix) {
    if (!terminalIdList.contains(suffix)) {
      terminalIdList.add(suffix);
      return true;
    }
    return false;
  }

  private void handleExecption(Exception e) {
    OBDal.getInstance().rollbackAndClose();
    hasErrors = true;
    log.error("Error in process", e);

    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    log.error(e.getMessage(), e);

    try {
      Throwable ex = DbUtility.getUnderlyingSQLException(e);
      String message = OBMessageUtils.translateError(new DalConnectionProvider(true), vars,
          vars.getLanguage(), ex.getMessage()).getMessage();
      addLog(LogLevel.ERROR, message);

    } catch (Exception e2) {
      log.error("Error parsing message", e2);
      addLog(LogLevel.ERROR, e.getMessage());
    }
  }

  /**
   * Helper class to keep log
   * 
   * @author alostale
   * 
   */
  private class LogEntry {
    public BaseOBObject link;
    String msg;
    LogLevel level;

    public LogEntry(LogLevel level, String msg) {
      this.msg = msg;
      this.level = level;
    }
  }

  /**
   * If priceList param is set, it returns that pricelist.
   * 
   * If not, it checks if it has already been created and returns or it creates a new one.
   * 
   * @return PriceList to be used as default
   */
  public org.openbravo.model.pricing.pricelist.PriceList getPriceList() {
    String priceListId = getParam("priceList");
    org.openbravo.model.pricing.pricelist.PriceList priceList;
    if (priceListId != null) {
      priceList = OBDal.getInstance().get(org.openbravo.model.pricing.pricelist.PriceList.class,
          priceListId);
    } else if (newObjects.containsKey("priceList")) {
      priceList = (PriceList) newObjects.get("priceList");
    } else {
      priceList = OBProvider.getInstance().get(
          org.openbravo.model.pricing.pricelist.PriceList.class);

      cloneObject(originalStore.getObretcoPricelist(), priceList, true);

      newObjects.put("priceList", priceList);
      addLog(LogLevel.WARN, OBMessageUtils.messageBD("OBPOSCS_EmptyPriceList"));
    }
    return priceList;
  }

  public DocumentType getDocumentType(DocumentType documentType) {
    MutableBoolean created = new MutableBoolean(false);
    DocumentType newDoc = (DocumentType) getObjectInTree(documentType, created);
    if (created.booleanValue()) {
      String name;
      if (newDoc.getName().contains(documentType.getOrganization().getSearchKey())) {
        name = newDoc.getName().replace(documentType.getOrganization().getSearchKey(),
            getParam("searchKey"));
      } else {
        name = newDoc.getName() + " - " + getParam("searchKey");
      }
      newDoc.setName(name);
      completeSODocumentType(newDoc, documentType);
      if (documentType.getDocumentSequence() != null) {
        Sequence sequence = (Sequence) getObjectInTree(documentType.getDocumentSequence(), created);
        if (created.booleanValue()) {
          String seqName;
          if (sequence.getName().contains(documentType.getOrganization().getSearchKey())) {
            seqName = sequence.getName().replace(documentType.getOrganization().getSearchKey(),
                getParam("searchKey"));
          } else {
            seqName = sequence.getName() + " - " + getParam("searchKey");
          }
          sequence.setName(seqName);
          sequence.setNextAssignedNumber(sequence.getStartingNo());
        }
        newDoc.setDocumentSequence(sequence);
      }
    }
    return newDoc;
  }

  private void completeSODocumentType(DocumentType soDocumentType, DocumentType originalDocType) {
    if (originalDocType.getDocumentTypeForShipment() != null) {
      DocumentType shipmentDoc = getDocumentType(originalDocType.getDocumentTypeForShipment());
      soDocumentType.setDocumentTypeForShipment(shipmentDoc);
    }
    if (originalDocType.getDocumentTypeForInvoice() != null) {
      DocumentType invoiceDoc = getDocumentType(originalDocType.getDocumentTypeForInvoice());
      soDocumentType.setDocumentTypeForInvoice(invoiceDoc);
    }
    if (originalDocType.getDocumentCancelled() != null) {
      DocumentType cancelled = getDocumentType(originalDocType.getDocumentCancelled());
      soDocumentType.setDocumentCancelled(cancelled);
    }
    if (originalDocType.getDocumentTemplateList() != null) {
      for (DocumentTemplate documentTemplate : originalDocType.getDocumentTemplateList()) {
        DocumentTemplate newDocTemp = OBProvider.getInstance().get(DocumentTemplate.class);
        cloneObject(documentTemplate, newDocTemp, false);
        newDocTemp.setDocumentType(soDocumentType);
        OBDal.getInstance().save(newDocTemp);
      }
    }
  }
}
