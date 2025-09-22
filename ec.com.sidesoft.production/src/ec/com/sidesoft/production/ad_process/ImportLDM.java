/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.production.ad_process;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.common.uom.UOMTrl;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ProductionPlan;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;
import org.openbravo.module.idljava.proc.IdlServiceJava;

/**
 * 
 * @author Dieguito
 */
public class ImportLDM extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Numero de documento", Parameter.STRING),
        new Parameter("Linea", Parameter.STRING), new Parameter("Tipo", Parameter.STRING),
        new Parameter("Cod. Prod. Elaborado", Parameter.STRING),
        new Parameter("Nombre Prod Elaborado", Parameter.STRING),
        new Parameter("Ubicación Entrega", Parameter.STRING),
        new Parameter("Cod. Ingrediente", Parameter.STRING),
        new Parameter("Nombre Ingrediente", Parameter.STRING),
        new Parameter("Cantidad", Parameter.STRING), new Parameter("Unidad", Parameter.STRING),
        new Parameter("Ubicación Consumo", Parameter.STRING) };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 32);
    validator.checkString(values[1], 32);
    validator.checkString(values[2], 32);
    validator.checkString(values[3], 32);
    validator.checkString(values[4], 500);
    validator.checkString(values[5], 32);
    validator.checkString(values[6], 32);
    validator.checkString(values[7], 500);
    validator.checkString(values[8], 32);
    validator.checkString(values[9], 32);
    validator.checkString(values[10], 100);
    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createLDM((String) values[0], (String) values[1], (String) values[2], (String) values[3],
        (String) values[4], (String) values[5], (String) values[6], (String) values[7],
        (String) values[8], (String) values[9], (String) values[10]);
  }

  public BaseOBObject createLDM(final String documentno, final String linea, final String typeprod,
      final String product, final String productname, final String locator, final String producting,
      final String productnameing, final String amount, final String unit, final String locatoring)
      throws Exception {

    String type = typeprod.trim();
    ProductionTransaction productionid = findDALInstance(false, ProductionTransaction.class,
        new Value(ProductionTransaction.PROPERTY_DOCUMENTNO, documentno));
    if ((productionid == null || productionid.equals("")) && type.equals("PT")) {
      throw new OBException("Numero de documento  " + documentno + " no existe");
    }

    Product productid = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, product));
    if ((productid == null || productid.equals("")) && type.equals("PT")) {
      throw new OBException("Producto: " + product + " no existe");
    }
    Locator locatorid = findDALInstance(false, Locator.class,
        new Value(Locator.PROPERTY_SEARCHKEY, locator));
    if ((locatorid == null || locatorid.equals("")) && type.equals("PT")) {
      throw new OBException("Ubicacion de entrega: " + locator + " no existe");
    }

    ProductionPlan productionplan = OBProvider.getInstance().get(ProductionPlan.class);

    try {
      if (type.equals("PT")) {
        productionplan.setProduction(productionid);
        productionplan.setLineNo(new Long(linea));
        productionplan.setProduct(productid);
        productionplan.setProductionQuantity(Parameter.BIGDECIMAL.parse(amount));
        productionplan.setStorageBin(locatorid);
        productionplan.setDescription(productname);
        productionplan.setOrganization(productionid.getOrganization());

        OBDal.getInstance().save(productionplan);
        OBDal.getInstance().flush();

        productionid.setRecordsCreated(true);
        OBDal.getInstance().save(productionid);
        OBDal.getInstance().flush();

      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    ProductionTransaction productionidline = findDALInstance(false, ProductionTransaction.class,
        new Value(ProductionTransaction.PROPERTY_DOCUMENTNO, documentno));
    if ((productionidline == null || productionidline.equals("")) && type.equals("MP")) {
      throw new OBException("Numero de documento  " + documentno + " no existe");
    }
    Product productidline = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, product));
    if ((productidline == null || productidline.equals("")) && type.equals("MP")) {
      throw new OBException("Producto: " + product + " no existe");
    }

    ProductionPlan productionplanid = findDALInstance(false, ProductionPlan.class,
        new Value(ProductionPlan.PROPERTY_PRODUCT, productidline),
        new Value(ProductionPlan.PROPERTY_PRODUCTION, productionidline));
    if ((productionplanid == null || productionplanid.equals("")) && type.equals("MP")) {
      throw new OBException("LDM Incorrecta  " + documentno + " - " + product + " no existe");
    }

    // validacion lista de materiales
    Product productingid = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, producting));
    if ((productingid == null || productingid.equals("")) && type.equals("MP")) {
      throw new OBException("Producto: " + producting + " no existe");
    }
    // Validacion unidad de medida traduccion
    UOMTrl uomtrlid = findDALInstance(false, UOMTrl.class, new Value(UOMTrl.PROPERTY_NAME, unit));
    if (uomtrlid == null || uomtrlid.equals("")) {
      throw new OBException("Unidad de medida: " + unit + " no existe");
    }
    // new Value(UOMTrl.PROPERTY_LANGUAGE, La));
    UOM uomid = findDALInstance(false, UOM.class,
        new Value(UOM.PROPERTY_EDICODE, uomtrlid.getUOM().getEDICode()));
    if ((uomid == null || uomid.equals("")) && type.equals("MP")) {
      throw new OBException("Unidad : " + unit + " no existe");
    }
    Locator locatoringid = findDALInstance(false, Locator.class,
        new Value(Locator.PROPERTY_SEARCHKEY, locatoring));
    if ((locatoringid == null || locatoringid.equals("")) && type.equals("MP")) {
      throw new OBException("Ubicacion de consumo: " + locatoring + " no existe");
    }

    ProductionLine productionline = OBProvider.getInstance().get(ProductionLine.class);
    try {
      if (type.equals("MP")) {
        productionline.setProductionPlan(productionplanid);
        productionline.setLineNo(new Long(linea));
        productionline.setProduct(productingid);
        productionline.setMovementQuantity(Parameter.BIGDECIMAL.parse(amount));
        productionline.setUOM(uomid);
        productionline.setStorageBin(locatoringid);
        productionline.setDescription(productnameing);
        productionline.setOrganization(productionid.getOrganization());

        OBDal.getInstance().save(productionline);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    OBDal.getInstance().commitAndClose();
    return productionplan;
  }
}
