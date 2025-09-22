package ec.com.sidesoft.retail.duplicatepartner.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.retail.posterminal.ErrorComparator;
import org.openbravo.retail.posterminal.OBPOSErrors;
import org.openbravo.retail.posterminal.POSUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import ec.com.sidesoft.retail.duplicatepartner.process.SRDUPEErrors;
import ec.com.sidesoft.retail.duplicatepartner.process.SRDUPEConfig;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.ad.access.User;
import org.openbravo.retail.posterminal.OBPOSAppCashup;
import java.util.Date;

public class ProcessErrorsBusinessPartner extends DalBaseProcess {
  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    try {
      
      // ********************************************************************
      // ERRORES DE TERCEROS EN PEDIDOS
      // ********************************************************************      
      OBCriteria<OBPOSErrors> queryListErrors = OBDal.getInstance().createCriteria(
          OBPOSErrors.class);
      queryListErrors.add(Restrictions.eq(OBPOSErrors.PROPERTY_ORDERSTATUS, "N"));
      queryListErrors.add(Restrictions.eq(OBPOSErrors.PROPERTY_TYPEOFDATA, "Order"));
      queryListErrors.add(Restrictions.ilike(OBPOSErrors.PROPERTY_ERROR, "%Error in OrderLoader: No row with the given identifier exists: [BusinessPartner%"));
      queryListErrors.addOrderBy(OBPOSErrors.PROPERTY_CREATIONDATE, true);

      List<OBPOSErrors> listErrors = queryListErrors.list();
      Collections.sort(listErrors, new ErrorComparator());

      // INICIO ITERAR SOBRE ERRORES DE TERCEROS EN PEDIDOS
      for (OBPOSErrors error : listErrors) {
        
        String errorId = error.getId();
        JSONObject json = new JSONObject(error.getJsoninfo());
        String jsonReplace = error.getJsoninfo();
        String jsonOriginal = error.getJsoninfo();
        String idemp = "";
        
        // INICIO SE VERIFICA QUE EXISTA LA PROPIEDAD BP EN EL JSON
        if (json.has("bp")) {
          
          JSONObject tercero = new JSONObject(json.getString("bp"));
          String taxID = tercero.getString("taxID");
          String oldID = tercero.getString("id");
          String oldLocID = tercero.getString("locId");
          String oldContactID = tercero.getString("contactId");
          
          // INICIO SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
          OBCriteria<SRDUPEErrors> processed = OBDal.getInstance().createCriteria(SRDUPEErrors.class);
          processed.add(Restrictions.eq(SRDUPEErrors.PROPERTY_OBPOSERRORS, error));
          processed.add(Restrictions.eq(SRDUPEErrors.PROPERTY_ERRORUPDATE, false));
          processed.addOrderBy(SRDUPEErrors.PROPERTY_CREATIONDATE, false);
          processed.setMaxResults(1);         
          if (processed.list().size() == 0 || processed.list().size() > 0) {
            
            if(processed.list().size() == 0) {
              idemp = saveTempData(error);          
            }else {
              idemp = processed.list().get(0).getId();
            }
            
            BusinessPartner bp = null;
            bp = getBpartner(taxID);

            // INICIO SE VERIFICA QUE EL TERCEO EXISTE EN LA BASE DE DATOS
            if (bp != null) {
              
              
              OBCriteria<SRDUPEConfig> config = OBDal.getInstance()
                  .createCriteria(SRDUPEConfig.class);
              config.add(Restrictions.eq(SRDUPEConfig.PROPERTY_ACTIVE, true));
              config.addOrderBy(SRDUPEConfig.PROPERTY_CREATIONDATE, false);
              config.setMaxResults(1);

              if (config.list() != null && config.list().size() > 0) {
                
                // SE VERIFICA QUE EL TERCERO TENGO CONDICIONES DE PAGO
                if(bp.getPaymentTerms() == null) {
                    BusinessPartner bpUpdatePaymentTerm = OBDal.getInstance().get(BusinessPartner.class, bp.getId());
                    bpUpdatePaymentTerm.setPaymentTerms(config.list().get(0).getPaymentterm());
                    OBDal.getInstance().flush();
                }                   
                
                // SE VERIFICA QUE EL TERCERO TENGO METODOS DE PAGO
                if(bp.getPaymentMethod() == null) {
                    BusinessPartner bpUpdateFinPaymentMethod = OBDal.getInstance().get(BusinessPartner.class, bp.getId());
                    bpUpdateFinPaymentMethod.setPaymentMethod(config.list().get(0).getFINPaymentmethod());
                    OBDal.getInstance().flush();
                }
                
                // SE VERIFICA QUE EL TERCERO TENGA UNA TARIFA
                if(bp.getPriceList() == null) {
                    BusinessPartner bpUpdatePriceList = OBDal.getInstance().get(BusinessPartner.class, bp.getId());
                    bpUpdatePriceList.setPriceList(config.list().get(0).getPricelist());
                    OBDal.getInstance().flush();
                }                
              }
              
              
              // INICIO SE VERIFICA QUE EL TERCEO TENGA CREADO UNA PERSONA DE CONTACTO
              if(bp.getADUserList().size() == 0) {
                
                   // SE CREA LA PERSONA DE CONTACTO
                   createContactPerson(bp);
                   logger.logln("Se crea la Persona de contancto para " +  bp.getName());
                   
              }else {
                
                String newID = bp.getId();
                String newLocID = bp.getBusinessPartnerLocationList().get(0).getId();
                String newContactID = bp.getADUserList().get(0).getId();
                
                // SE REEMPLAZA EL ID DEL TERCERO
                String replace1 = jsonReplace.replaceAll(oldID, newID);
                
                // SE REEMPLAZA EL LOCID Y SHIPLOCID DEL TERCERO              
                String replace2 = replace1.replaceAll(oldLocID, newLocID);
                
                // SE REEMPLAZA EL CONTACTID DEL TERCERO
                String replace3 = replace2.replaceAll(oldContactID, newContactID);
                
                try {
                  OBContext.setAdminMode(true);
                  
                  // SE ACTUALIZA EL JSON EN LA TABLA DE ERRORES
                  error = OBDal.getInstance().get(OBPOSErrors.class, errorId);
                  error.setJsoninfo(replace3);

                  // SE ACTUALIZA EL ERROR_UPDATE EN LA TABLA TEMPORAL
                  SRDUPEErrors errorTemp = OBDal.getInstance().get(SRDUPEErrors.class, idemp);
                  errorTemp.setErrorUpdate(true);
                  
                  OBDal.getInstance().flush();
                  OBDal.getInstance().commitAndClose();
                } finally {
                  OBContext.restorePreviousMode();
                }
                
                // INICIO VERIFICO QUE EL JSON GUARDADO NO ESTE EN NULL
                OBPOSErrors errorVerify = OBDal.getInstance().get(OBPOSErrors.class, errorId);
                if(errorVerify.getJsoninfo() == null || errorVerify.getJsoninfo().equals("")) {
                  try {
                    OBContext.setAdminMode(true);

                    // EL JSON ESTA VACIO SE ACTUALIZA CON EL ORIGINAL
                    OBPOSErrors errorVerifyUpdate = OBDal.getInstance().get(OBPOSErrors.class, errorId);
                    errorVerifyUpdate.setJsoninfo(jsonOriginal); 

                    // SE CAMBIA EL ESTADO DE ACTUALIZADO EN LA TABLA TEMPORAL
                    SRDUPEErrors errorTempVerify = OBDal.getInstance().get(SRDUPEErrors.class, idemp);
                    errorTempVerify.setErrorUpdate(false);
                    
                    OBDal.getInstance().flush();
                    OBDal.getInstance().commitAndClose();
                  } finally {
                    OBContext.restorePreviousMode();
                  }                  
                }
                // FIN VERIFICO QUE EL JSON GUARDADO NO ESTE EN NULL
              }
              // FIN SE VERIFICA QUE EL TERCEO TENGA CREADO UNA PERSONA DE CONTACTO
            } 
            //  FIN SE VERIFICA QUE EL TERCEO EXISTE EN LA BASE DE DATOS
          }   
          // FIN SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
        }
        // FIN SE VERIFICA QUE EXISTA LA PROPIEDAD BP EN EL JSON
      }
      // FIN ITERAR SOBRE ERRORES DE TERCEROS EN PEDIDOS
      logger.logln(OBMessageUtils.messageBD("Success"));      
      
      // ********************************************************************
      // ERRORES DE TERCEROS DUPLICADOS
      // ********************************************************************
      OBCriteria<OBPOSErrors> queryListErrorsDuplicate = OBDal.getInstance().createCriteria(
          OBPOSErrors.class);
      queryListErrorsDuplicate.add(Restrictions.eq(OBPOSErrors.PROPERTY_ORDERSTATUS, "N"));
      queryListErrorsDuplicate.add(Restrictions.eq(OBPOSErrors.PROPERTY_TYPEOFDATA, "BusinessPartner"));
      queryListErrorsDuplicate.add(Restrictions.ilike(OBPOSErrors.PROPERTY_ERROR, "%ERROR: duplicate key value violates unique constraint \"em_sswh_unique_taxid\"%"));
      queryListErrorsDuplicate.addOrderBy(OBPOSErrors.PROPERTY_CREATIONDATE, true);
      
      List<OBPOSErrors> listErrorsDuplicate = queryListErrorsDuplicate.list();
      Collections.sort(listErrorsDuplicate, new ErrorComparator());  
      
      // INICIO ITERAR SOBRE ERRORES DE TERCEROS DUPLICADOS      
      for (OBPOSErrors errorDuplicate : listErrorsDuplicate) {
        
        String errorIdDuplicate = errorDuplicate.getId();
        String idDuplicateTemp = "";
        
        // INICIO SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
        OBCriteria<SRDUPEErrors> processedNullPointer = OBDal.getInstance().createCriteria(SRDUPEErrors.class);
        processedNullPointer.add(Restrictions.eq(SRDUPEErrors.PROPERTY_OBPOSERRORS, errorDuplicate));
        processedNullPointer.add(Restrictions.eq(SRDUPEErrors.PROPERTY_ERRORUPDATE, false));
        processedNullPointer.addOrderBy(SRDUPEErrors.PROPERTY_CREATIONDATE, false);
        processedNullPointer.setMaxResults(1);          
        if (processedNullPointer.list().size() == 0 || processedNullPointer.list().size() > 0) {
          
          //GUARDO EN LA TABLA TEMPORAL
          if(processedNullPointer.list().size() == 0) {
            idDuplicateTemp = saveTempData(errorDuplicate);          
          }else {
            idDuplicateTemp = processedNullPointer.list().get(0).getId();
          }
          
          try {
            OBContext.setAdminMode(true);
            
            //SE ACTUALIZA EL JSON EN LA TABLA DE ERRORES
            errorDuplicate = OBDal.getInstance().get(OBPOSErrors.class, errorIdDuplicate);
            errorDuplicate.setOrderstatus("Y");
            
            //SE ACTUALIZA EL ERROR_UPDATE EN LA TABLA TEMPORAL
            SRDUPEErrors errorTempDuplicate = OBDal.getInstance().get(SRDUPEErrors.class, idDuplicateTemp);
            errorTempDuplicate.setErrorUpdate(true);          

            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          } finally {
            OBContext.restorePreviousMode();
          }             
        }        
        // FIN SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
      }   
      // FIN ITERAR SOBRE ERRORES DE TERCEROS DUPLICADOS      
      logger.logln("Proceso completado satisfactoriamente. Duplicate key value violates unique constraint");
      
      // ********************************************************************      
      // ERRORES DE NULLPOINTEREXCEPTION
      // ********************************************************************      
      OBCriteria<OBPOSErrors> queryListErrorsNullPoiter = OBDal.getInstance().createCriteria(
          OBPOSErrors.class);
      queryListErrorsNullPoiter.add(Restrictions.eq(OBPOSErrors.PROPERTY_ORDERSTATUS, "N"));
      queryListErrorsNullPoiter.add(Restrictions.eq(OBPOSErrors.PROPERTY_TYPEOFDATA, "BusinessPartner"));
      queryListErrorsNullPoiter.add(Restrictions.ilike(OBPOSErrors.PROPERTY_ERROR, "%java.lang.NullPointerException%"));
      queryListErrorsNullPoiter.addOrderBy(OBPOSErrors.PROPERTY_CREATIONDATE, true);   
      
      List<OBPOSErrors> listErrorsNullPointer = queryListErrorsNullPoiter.list();
      Collections.sort(listErrorsNullPointer, new ErrorComparator());  
      
      // INICIO ITERAR SOBRE ERRORES DE TERCEROS NULL POINTER EXCEPTION       
      for (OBPOSErrors errorNullPointer : listErrorsNullPointer) {
        
        String errorIdNullPointer = errorNullPointer.getId();
        JSONObject jsonNullPinter = new JSONObject(errorNullPointer.getJsoninfo());
        String jsonOriginalNullPointer = errorNullPointer.getJsoninfo();
        String taxIDNullPointer = jsonNullPinter.getString("taxID");
        String idNullPointerTemp = "";
        
        // INICIO SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
        OBCriteria<SRDUPEErrors> processedNullPointer = OBDal.getInstance().createCriteria(SRDUPEErrors.class);
        processedNullPointer.add(Restrictions.eq(SRDUPEErrors.PROPERTY_OBPOSERRORS, errorNullPointer));
        processedNullPointer.add(Restrictions.eq(SRDUPEErrors.PROPERTY_ERRORUPDATE, false));
        processedNullPointer.addOrderBy(SRDUPEErrors.PROPERTY_CREATIONDATE, false);
        processedNullPointer.setMaxResults(1);         
        if (processedNullPointer.list().size() == 0 || processedNullPointer.list().size() > 0) {

          //GUARDO EN LA TABLA TEMPORAL
          if(processedNullPointer.list().size() == 0) {
            idNullPointerTemp = saveTempData(errorNullPointer);          
          }else {
            idNullPointerTemp = processedNullPointer.list().get(0).getId(); 
          }
          
          if (!jsonNullPinter.has("organization")) {
            jsonNullPinter.put("organization","0");
          }
          
          if (!jsonNullPinter.has("searchKey")) {
            jsonNullPinter.put("searchKey",taxIDNullPointer);
          }   
          
          if (!jsonNullPinter.has("countryId")) {
            jsonNullPinter.put("countryId","171");
          }           
          
          try {
            OBContext.setAdminMode(true);
            
            //SE ACTUALIZA EL JSON EN LA TABLA DE ERRORES
            errorNullPointer = OBDal.getInstance().get(OBPOSErrors.class, errorIdNullPointer);
            errorNullPointer.setJsoninfo(jsonNullPinter.toString());
            
            //SE ACTUALIZA EL ERROR_UPDATE EN LA TABLA TEMPORAL
            SRDUPEErrors errorTempNullPointer = OBDal.getInstance().get(SRDUPEErrors.class, idNullPointerTemp);
            errorTempNullPointer.setErrorUpdate(true);            

            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          } finally {
            OBContext.restorePreviousMode();
          }  
          
          // INICIO VERIFIO QUE EL JSON GUARDADO NO ESTE EN NULL O VACIO
          OBPOSErrors errorVerifyNullPointer = OBDal.getInstance().get(OBPOSErrors.class, errorIdNullPointer);
          if(errorVerifyNullPointer.getJsoninfo() == null || errorVerifyNullPointer.getJsoninfo().equals("")) {
            try {
              OBContext.setAdminMode(true);
              
              // EL JSON ESTA VACIO SE ACTUALIZA CON EL ORIGINAL
              OBPOSErrors errorVerifyUpdateNullPointer = OBDal.getInstance().get(OBPOSErrors.class, errorIdNullPointer);
              errorVerifyUpdateNullPointer.setJsoninfo(jsonOriginalNullPointer); 
              
              // SE CAMBIA EL ESTADO DE ACTUALIZADO EN LA TABLA TEMPORAL              
              SRDUPEErrors errorTempVerifyNullPointer = OBDal.getInstance().get(SRDUPEErrors.class, idNullPointerTemp);
              errorTempVerifyNullPointer.setErrorUpdate(false);
              
              OBDal.getInstance().flush();
              OBDal.getInstance().commitAndClose();
            } finally {
              OBContext.restorePreviousMode();
            }                  
          }
          // FIN VERIFIO QUE EL JSON GUARDADO NO ESTE EN NULL O VACIO          
        }
        // FIN SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
      }   
      // FIN ITERAR SOBRE ERRORES DE TERCEROS NULL POINTER EXCEPTION
      logger.logln("Proceso completado satisfactoriamente. NullPointerException");
      
      // ********************************************************************
      // ERRORES DE PEDIDOS YA PROCEADOS LOADED
      // ********************************************************************      
      OBCriteria<OBPOSErrors> queryListErrorsLoaded = OBDal.getInstance().createCriteria(
          OBPOSErrors.class);
      queryListErrorsLoaded.add(Restrictions.eq(OBPOSErrors.PROPERTY_ORDERSTATUS, "N"));
      queryListErrorsLoaded.add(Restrictions.eq(OBPOSErrors.PROPERTY_TYPEOFDATA, "Order"));
      queryListErrorsLoaded.add(Restrictions.ilike(OBPOSErrors.PROPERTY_ERROR, "%org.codehaus.jettison.json.JSONException: JSONObject[\"loaded\"] not found.%"));
      queryListErrorsLoaded.addOrderBy(OBPOSErrors.PROPERTY_CREATIONDATE, true);

      List<OBPOSErrors> listErrorsLoaded = queryListErrorsLoaded.list();
      Collections.sort(listErrorsLoaded, new ErrorComparator());

      // INICIO ITERAR SOBRE ERRORES DE PEDIDOS LOADED
      for (OBPOSErrors errorLoaded : listErrorsLoaded) {
        
        String errorIdLoaded = errorLoaded.getId();
        JSONObject jsonLoaded = new JSONObject(errorLoaded.getJsoninfo());
        String orderID = jsonLoaded.getString("id");
        String idLoadedTemp = "";

        // INICIO SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
        OBCriteria<SRDUPEErrors> processedLoaded = OBDal.getInstance().createCriteria(SRDUPEErrors.class);
        processedLoaded.add(Restrictions.eq(SRDUPEErrors.PROPERTY_OBPOSERRORS, errorLoaded));
        processedLoaded.add(Restrictions.eq(SRDUPEErrors.PROPERTY_ERRORUPDATE, false));
        processedLoaded.addOrderBy(SRDUPEErrors.PROPERTY_CREATIONDATE, false);
        processedLoaded.setMaxResults(1);         
        if (processedLoaded.list().size() == 0 || processedLoaded.list().size() > 0) {

          //GUARDO EN LA TABLA TEMPORAL
          if(processedLoaded.list().size() == 0) {
            idLoadedTemp = saveTempData(errorLoaded);          
          }else {
            idLoadedTemp = processedLoaded.list().get(0).getId(); 
          }

          Order orderModify = null;
          orderModify = getOrder(orderID);          
          // INICIO SE VERIFICA QUE LA ORDEN EXISTA EN LA BASE DE DATOS
          if (orderModify != null) {
            // INICIO SE VERIFICA QUE LA ORDEN ESTE PROCESADA
            if(orderModify.getObposAppCashup() != null) {
              try {
                OBContext.setAdminMode(true);
                
                //SE ACTUALIZA EL JSON EN LA TABLA DE ERRORES
                errorLoaded = OBDal.getInstance().get(OBPOSErrors.class, errorIdLoaded);
                errorLoaded.setOrderstatus("Y");
                
                //SE ACTUALIZA EL ERROR_UPDATE EN LA TABLA TEMPORAL
                SRDUPEErrors errorTempLoaded = OBDal.getInstance().get(SRDUPEErrors.class, idLoadedTemp);
                errorTempLoaded.setErrorUpdate(true);                    
                
                OBDal.getInstance().flush();
                OBDal.getInstance().commitAndClose();
              } finally {
                OBContext.restorePreviousMode();
              } 
            }
            // FIN SE VERIFICA QUE LA ORDEN ESTE PROCESADA
          }
          // FIN SE VERIFICA QUE LA ORDEN EXISTA EN LA BASE DE DATOS           
        }
        // FIN SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO       
      }   
      // FIN ITERAR SOBRE ERRORES DE PEDIDOS LOADED      
      logger.logln("Proceso completado satisfactoriamente. Loaded");
      
      // ********************************************************************
      // PROCESO DE INVENTARIOS NEGATIVOS
      // ********************************************************************
      inventoryErrors();
      logger.logln("Proceso completado satisfactoriamente. Inventarios Negativos");
      
      // ********************************************************************
      // PROCESO DE UPDATE COSTEO
      // ********************************************************************
      costingErrors();
      logger.logln("Proceso completado satisfactoriamente. Update Costeo");
      
      // ********************************************************************
      // PROCESO DE LOTES EN 0
      // ********************************************************************
      attributesetinstanceZero();
      logger.logln("Proceso completado satisfactoriamente. Lotes en 0");   

      // ********************************************************************      
      // ERRORES DE CIERRES DE CAJA PROCESADOS
      // ********************************************************************
      OBCriteria<OBPOSErrors> queryListErrorsCashClose = OBDal.getInstance().createCriteria(
          OBPOSErrors.class);
      queryListErrorsCashClose.add(Restrictions.eq(OBPOSErrors.PROPERTY_ORDERSTATUS, "N"));
      queryListErrorsCashClose.add(Restrictions.eq(OBPOSErrors.PROPERTY_TYPEOFDATA, "OBPOS_App_Cashup"));
      queryListErrorsCashClose.add(Restrictions.ilike(OBPOSErrors.PROPERTY_ERROR, "%org.openbravo.base.exception.OBException: Cash up is processed and cannot be set as processed again. OBPOS_APP_CASHUP_ID:%"));
      queryListErrorsCashClose.addOrderBy(OBPOSErrors.PROPERTY_CREATIONDATE, true);  

      List<OBPOSErrors> listErrorsCashClose = queryListErrorsCashClose.list();
      Collections.sort(listErrorsCashClose, new ErrorComparator()); 

      // INICIO ITERAR SOBRE ERRORES DE CIERRE DE CAJA YA PROCESADOS
      for (OBPOSErrors errorCashClose : listErrorsCashClose) {
        
        String errorIdCashClose = errorCashClose.getId();
        JSONObject jsonCashClose = new JSONObject(errorCashClose.getJsoninfo());
        String cashCloseID = jsonCashClose.getString("id");
        String idCashCloseTemp = "";

        // INICIO SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
        OBCriteria<SRDUPEErrors> processedCashClose = OBDal.getInstance().createCriteria(SRDUPEErrors.class);
        processedCashClose.add(Restrictions.eq(SRDUPEErrors.PROPERTY_OBPOSERRORS, errorCashClose));
        processedCashClose.add(Restrictions.eq(SRDUPEErrors.PROPERTY_ERRORUPDATE, false));
        processedCashClose.addOrderBy(SRDUPEErrors.PROPERTY_CREATIONDATE, false);
        processedCashClose.setMaxResults(1);         
        if (processedCashClose.list().size() == 0 || processedCashClose.list().size() > 0) {

          //GUARDO EN LA TABLA TEMPORAL
          if(processedCashClose.list().size() == 0) {
            idCashCloseTemp = saveTempData(errorCashClose);          
          }else {
            idCashCloseTemp = processedCashClose.list().get(0).getId(); 
          }

          OBPOSAppCashup cashupModify = null;
          cashupModify = getCashClose(cashCloseID);
          // INICIO SE VERIFICA QUE EL CIERRE DE CAJA EXISTA EN LA BASE DE DATOS
          if (cashupModify != null) {
            // INICIO SE VERIFICA QUE CIERRE DE CAJA ESTE PROCESADO PROCESADA
            if(cashupModify.isProcessed() && cashupModify.isProcessedbo()) {
              try {
                OBContext.setAdminMode(true);
                
                //SE ACTUALIZA EL JSON EN LA TABLA DE ERRORES
                errorCashClose = OBDal.getInstance().get(OBPOSErrors.class, errorIdCashClose);
                errorCashClose.setOrderstatus("Y");
                
                //SE ACTUALIZA EL ERROR_UPDATE EN LA TABLA TEMPORAL
                SRDUPEErrors errorTempCashClose = OBDal.getInstance().get(SRDUPEErrors.class, idCashCloseTemp);
                errorTempCashClose.setErrorUpdate(true);                    
                
                OBDal.getInstance().flush();
                OBDal.getInstance().commitAndClose();
              } finally {
                OBContext.restorePreviousMode();
              } 
            }
            // FIN SE VERIFICA QUE CIERRE DE CAJA ESTE PROCESADO PROCESADA
          }
          // FIN SE VERIFICA QUE EL CIERRE DE CAJA EXISTA EN LA BASE DE DATOS           
        }
        // FIN SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO       
      }   
      // FIN ITERAR SOBRE ERRORES DE CIERRE DE CAJA YA PROCESADOS
      logger.logln("Proceso completado satisfactoriamente. Cierres de Caja ya procesados");         

      // ********************************************************************
      // ERRORES DE RESERVA FUERA DE FECHA
      // ********************************************************************      
      OBCriteria<OBPOSErrors> queryListErrorsOutDatedData = OBDal.getInstance().createCriteria(
          OBPOSErrors.class);
      queryListErrorsOutDatedData.add(Restrictions.eq(OBPOSErrors.PROPERTY_ORDERSTATUS, "N"));
      queryListErrorsOutDatedData.add(Restrictions.eq(OBPOSErrors.PROPERTY_TYPEOFDATA, "Order"));
      queryListErrorsOutDatedData.add(Restrictions.ilike(OBPOSErrors.PROPERTY_ERROR, "%org.openbravo.mobile.core.process.OutDatedDataChangeException: Datos fuera de fecha, las  reservas no pueden ser actualizadas. Verifique el error en la ventana Errores al importar pedidos desde el TPV.%"));
      queryListErrorsOutDatedData.addOrderBy(OBPOSErrors.PROPERTY_CREATIONDATE, true);

      List<OBPOSErrors> listErrorsOutDatedData = queryListErrorsOutDatedData.list();
      Collections.sort(listErrorsOutDatedData, new ErrorComparator());

      // INICIO ITERAR SOBRE ERRORES DE RESERVA FUERA DE FECHA
      for (OBPOSErrors errorOutDatedData : listErrorsOutDatedData) {
        
        JSONObject jsonOutDatedData = new JSONObject(errorOutDatedData.getJsoninfo());
        String idLoadedTempOutDatedData = "";

        // INICIO SE VERIFICA QUE EXISTA LA PROPIEDAD LOADED EN EL JSON
        if (jsonOutDatedData.has("loaded")) {

          String orderIDOutDatedData = jsonOutDatedData.getString("id");

          // INICIO SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
          OBCriteria<SRDUPEErrors> processedOutDatedData = OBDal.getInstance().createCriteria(SRDUPEErrors.class);
          processedOutDatedData.add(Restrictions.eq(SRDUPEErrors.PROPERTY_OBPOSERRORS, errorOutDatedData));
          processedOutDatedData.add(Restrictions.eq(SRDUPEErrors.PROPERTY_ERRORUPDATE, false));
          processedOutDatedData.addOrderBy(SRDUPEErrors.PROPERTY_CREATIONDATE, false);
          processedOutDatedData.setMaxResults(1);         
          if (processedOutDatedData.list().size() == 0 || processedOutDatedData.list().size() > 0) {

            //GUARDO EN LA TABLA TEMPORAL
            if(processedOutDatedData.list().size() == 0) {
              idLoadedTempOutDatedData = saveTempData(errorOutDatedData);          
            }else {
              idLoadedTempOutDatedData = processedOutDatedData.list().get(0).getId(); 
            }

            Order orderModifyOutDatedData = null;
            orderModifyOutDatedData = getOrder(orderIDOutDatedData);          
            // INICIO SE VERIFICA QUE LA ORDEN EXISTA EN LA BASE DE DATOS
            if (orderModifyOutDatedData != null) {

              Date  loaded = POSUtils.dateFormatUTC.parse(jsonOutDatedData.getString("loaded")),
                    updated = OBMOBCUtils.convertToUTC(orderModifyOutDatedData.getUpdated());

              long diffdates = Math.abs(updated.getTime() - loaded.getTime()); 
              long diffInHours = TimeUnit.MILLISECONDS.toHours(diffdates);
              diffInHours = diffInHours + 2;
              updateHour(diffInHours,orderIDOutDatedData);

                try {
                  OBContext.setAdminMode(true);
                  
                  //SE ACTUALIZA EL ERROR_UPDATE EN LA TABLA TEMPORAL
                  SRDUPEErrors errorTempOutDatedData = OBDal.getInstance().get(SRDUPEErrors.class, idLoadedTempOutDatedData);
                  errorTempOutDatedData.setErrorUpdate(true);                     
                  
                  OBDal.getInstance().flush();
                  OBDal.getInstance().commitAndClose();
                } finally {
                  OBContext.restorePreviousMode();
                } 

            }
            // FIN SE VERIFICA QUE LA ORDEN EXISTA EN LA BASE DE DATOS           
          }
          // FIN SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO           
        }       
        // FIN SE VERIFICA QUE EXISTA LA PROPIEDAD LOADED EN EL JSON
      }   
      // FIN ITERAR SOBRE ERRORES DE RESERVA FUERA DE FECHA      
      logger.logln("Proceso completado satisfactoriamente. Reservas Fuera de fecha"); 
      
      // ********************************************************************
      // PROCESO DE UPDATE Attempts
      // ********************************************************************
      updateAttempts();
      logger.logln("Proceso completado satisfactoriamente. Update Attempts");       
      
      // ********************************************************************      
      // ERRORES DE TIMEZONEOFFSET
      // ********************************************************************      
     OBCriteria<OBPOSErrors> queryListErrorsTimeZoneOffset = OBDal.getInstance().createCriteria(
         OBPOSErrors.class);
     queryListErrorsTimeZoneOffset.add(Restrictions.eq(OBPOSErrors.PROPERTY_ORDERSTATUS, "N"));
     queryListErrorsTimeZoneOffset.add(Restrictions.eq(OBPOSErrors.PROPERTY_TYPEOFDATA, "BusinessPartner"));
     queryListErrorsTimeZoneOffset.add(Restrictions.ilike(OBPOSErrors.PROPERTY_ERROR, "%org.codehaus.jettison.json.JSONException: JSONObject[\"timezoneOffset\"] not found.%"));
     queryListErrorsTimeZoneOffset.addOrderBy(OBPOSErrors.PROPERTY_CREATIONDATE, true);   
     
     List<OBPOSErrors> listErrorsTimeZoneOffset = queryListErrorsTimeZoneOffset.list();
     Collections.sort(listErrorsTimeZoneOffset, new ErrorComparator());  
     
     // INICIO ITERAR SOBRE ERRORES DE TERCEROS TIMEZONE
     for (OBPOSErrors error : listErrorsTimeZoneOffset) {
       
       String errorIdTimeZoneOffset = error.getId();
       JSONObject jsonTimeZoneOffset = new JSONObject(error.getJsoninfo());
       String jsonOriginalTimeZoneOffset = error.getJsoninfo();
       String idTimeZoneTemp = "";
       
       // INICIO SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
       OBCriteria<SRDUPEErrors> processedTimeZoneOffset = OBDal.getInstance().createCriteria(SRDUPEErrors.class);
       processedTimeZoneOffset.add(Restrictions.eq(SRDUPEErrors.PROPERTY_OBPOSERRORS, error));
       processedTimeZoneOffset.add(Restrictions.eq(SRDUPEErrors.PROPERTY_ERRORUPDATE, false));
       processedTimeZoneOffset.addOrderBy(SRDUPEErrors.PROPERTY_CREATIONDATE, false);
       processedTimeZoneOffset.setMaxResults(1);         
       if (processedTimeZoneOffset.list().size() == 0 || processedTimeZoneOffset.list().size() > 0) {

         //GUARDO EN LA TABLA TEMPORAL
         if(processedTimeZoneOffset.list().size() == 0) {
           idTimeZoneTemp = saveTempData(error);          
         }else {
           idTimeZoneTemp = processedTimeZoneOffset.list().get(0).getId(); 
         }
         
         if (!jsonTimeZoneOffset.has("timezoneOffset")) {
           jsonTimeZoneOffset.put("timezoneOffset",300);
         }
         
         try {
           OBContext.setAdminMode(true);
           
           //SE ACTUALIZA EL JSON EN LA TABLA DE ERRORES
           error = OBDal.getInstance().get(OBPOSErrors.class, errorIdTimeZoneOffset);
           error.setJsoninfo(jsonTimeZoneOffset.toString());
           
           //SE ACTUALIZA EL ERROR_UPDATE EN LA TABLA TEMPORAL
           SRDUPEErrors errorTempTimeZoneOffset = OBDal.getInstance().get(SRDUPEErrors.class, idTimeZoneTemp);
           errorTempTimeZoneOffset.setErrorUpdate(true);            

           OBDal.getInstance().flush();
           OBDal.getInstance().commitAndClose();
         } finally {
           OBContext.restorePreviousMode();
         }  
         
         // INICIO VERIFIO QUE EL JSON GUARDADO NO ESTE EN NULL O VACIO
         OBPOSErrors errorVerifyTimeZoneOffset = OBDal.getInstance().get(OBPOSErrors.class, errorIdTimeZoneOffset);
         if(errorVerifyTimeZoneOffset.getJsoninfo() == null || errorVerifyTimeZoneOffset.getJsoninfo().equals("")) {
           try {
             OBContext.setAdminMode(true);
             
             // EL JSON ESTA VACIO SE ACTUALIZA CON EL ORIGINAL
             OBPOSErrors errorVerifyUpdateTimeZoneOffset = OBDal.getInstance().get(OBPOSErrors.class, errorIdTimeZoneOffset);
             errorVerifyUpdateTimeZoneOffset.setJsoninfo(jsonOriginalTimeZoneOffset); 
             
             // SE CAMBIA EL ESTADO DE ACTUALIZADO EN LA TABLA TEMPORAL              
             SRDUPEErrors errorTempVerifyTimeZOneOffset = OBDal.getInstance().get(SRDUPEErrors.class, idTimeZoneTemp);
             errorTempVerifyTimeZOneOffset.setErrorUpdate(false);
             
             OBDal.getInstance().flush();
             OBDal.getInstance().commitAndClose();
           } finally {
             OBContext.restorePreviousMode();
           }                  
         }
         // FIN VERIFIO QUE EL JSON GUARDADO NO ESTE EN NULL O VACIO          
       }
       // FIN SE VERIFICA QUE EL ERROR NO HAYA SIDO ACTUALIZADO
     }   
     // FIN ITERAR SOBRE ERRORES DE TERCEROS TIMEZONE
     logger.logln("Proceso completado satisfactoriamente. timezoneOffset");     
 
    } catch (Exception e) {// won't' happen
      logger.logln(e.getMessage());
      //logger.logln(OBMessageUtils.messageBD("Error"));
    }
  }
  
  public void updateAttempts(){
    
    ConnectionProvider conn = new DalConnectionProvider(false);
    // SE ACTUALIZAN SOLO CUANDO EL NUMERO DE ATTEMPS SEA MAYOR O IGUAL A 3
    String strSql = "UPDATE obpos_errors SET attempts = 0 WHERE obpos_errors_id IN(select obpos_errors_id from obpos_errors where attempts >= 3);";      

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = conn.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();
      logger.logln("Se ejecuto la funcion de update Attempts.");
    } catch (Exception e) {
      logger.logln("Hubo Errores en la ejecucion de la funcion de update Attempts " + e.getMessage());
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {
      }
    }
  }
    
  public void updateHour(long hour, String idOrder){
    
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = "UPDATE c_order SET updated = (updated - interval '"+hour+" hour') WHERE c_order_id = '"+idOrder+"';";;    

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = conn.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();
      logger.logln("Se ejecuto la funcion de reservas fuera de tiempo.");
    } catch (Exception e) {
      logger.logln("Hubo Errores en la ejecucion de la funcion de reservas fuera de tiempo." + e.getMessage());
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {
      }
    }
  }
  
  private void inventoryErrors() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;
    try {
      strSql = "SELECT srdupe_error_inventory() FROM DUAL;";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      logger.logln("Se ejecuto la funcion de inventarios negativos.");
    } catch (Exception e) {
      logger.logln("Hubo Errores en la ejecucion de la funcion de inventarios negativos." + e.getMessage());
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {
      }
    }
  }
  
  private void costingErrors() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;
    try {
      strSql = "SELECT srdupe_costing() FROM DUAL;";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      logger.logln("Se ejecuto la funcion de actualizar fecha costeo.");
    } catch (Exception e) {
      logger.logln("Hubo Errores en la ejecucion de la funcion de actualizar fecha costeo." + e.getMessage());
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {
      }
    }
  } 
  
  private void attributesetinstanceZero() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;
    try {
      strSql = "SELECT srdupe_attributesetinstance() FROM DUAL;";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      logger.logln("Se ejecuto la funcion de lotes en cero.");
    } catch (Exception e) {
      logger.logln("Hubo Errores en la ejecucion de la funcion de lotes en cero." + e.getMessage());
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {
      }
    }
  }    
  
  private BusinessPartner getBpartner(String taxID) {
    BusinessPartner bp = null;

    OBCriteria<BusinessPartner> cfgCrt = OBDal.getInstance().createCriteria(BusinessPartner.class);
    cfgCrt.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, taxID));
    bp = (BusinessPartner) cfgCrt.uniqueResult();

    return bp;
  }
  
  private Order getOrder(String orderId) {
    Order order = null;

    OBCriteria<Order> cfgCrt = OBDal.getInstance().createCriteria(Order.class);
    cfgCrt.add(Restrictions.eq(BusinessPartner.PROPERTY_ID, orderId));
    order = (Order) cfgCrt.uniqueResult();

    return order;
  }
  
  private OBPOSAppCashup getCashClose(String cashCloseId) {
    OBPOSAppCashup cashclose = null;

    OBCriteria<OBPOSAppCashup> cfgCrt = OBDal.getInstance().createCriteria(OBPOSAppCashup.class);
    cfgCrt.add(Restrictions.eq(OBPOSAppCashup.PROPERTY_ID, cashCloseId));
    cashclose = (OBPOSAppCashup) cfgCrt.uniqueResult();

    return cashclose;
  }  
  
  private static String saveTempData(OBPOSErrors error) {
    try {
      OBContext.setAdminMode(true);
      
      UUID uuid = UUID.randomUUID();
      String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();
      
      SRDUPEErrors temp = OBProvider.getInstance().get(SRDUPEErrors.class);
      temp.setNewOBObject(true);
      temp.setId(randomUUIDString);
      temp.setObposErrors(error);
      temp.setClient(error.getClient());
      temp.setOrganization(error.getOrganization());
      temp.setCreatedBy(error.getCreatedBy());
      temp.setUpdatedBy(error.getCreatedBy());
      temp.setJSONInfo(error.getJsoninfo());
      temp.setError(error.getError());
      temp.setSaveagain(error.isSaveagain());
      temp.setDeleteerror(error.isDeleteerror());
      temp.setOrderstatus(error.getOrderstatus());
      temp.setTypeOfData(error.getTypeofdata());
      temp.setObposApplications(error.getObposApplications());
      temp.setProcessNow(error.isProcessNow());
      OBDal.getInstance().save(temp);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      
      return temp.getId();
      
    } finally {
      OBContext.restorePreviousMode();
    }    
  }

  private static String createContactPerson(BusinessPartner bp) {
    try {
      OBContext.setAdminMode(true);
      
      UUID uuid = UUID.randomUUID();
      String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();
      
      User temp = OBProvider.getInstance().get(User.class);
      temp.setNewOBObject(true);
      temp.setId(randomUUIDString);
      temp.setClient(bp.getClient());
      temp.setOrganization(bp.getOrganization());
      temp.setCreatedBy(bp.getCreatedBy());
      temp.setUpdatedBy(bp.getCreatedBy());
      temp.setName(bp.getName());
      temp.setFirstName(bp.getName());
      temp.setLastName(bp.getName());
      temp.setUsername(bp.getName());
      temp.setEmail(bp.getEEIEmail());
      temp.setBusinessPartner(bp);
      temp.setOpcrmDonotcall(false);
      OBDal.getInstance().save(temp);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      
      return temp.getId();
      
    } finally {
      OBContext.restorePreviousMode();
    }    
  }

}
