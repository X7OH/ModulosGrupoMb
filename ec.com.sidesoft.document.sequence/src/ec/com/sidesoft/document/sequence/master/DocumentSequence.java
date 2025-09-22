package ec.com.sidesoft.document.sequence.master;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationTree;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.retail.posterminal.ProcessHQLQuery;
import org.openbravo.retail.posterminal.master.Brand;

import ec.com.sidesoft.document.sequence.PoinOfSaleSequenceLine;
import ec.com.sidesoft.document.sequence.PointOfSaleSeq;

public class DocumentSequence extends ProcessHQLQuery {
  public static final String documentSequencePropertyExtension = "ECSDS_document_sequence";
  public static final Logger log = Logger.getLogger(Brand.class);

  @Inject
  @Any
  @Qualifier(documentSequencePropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {

    String posTerminalId = jsonsent.getString("pos");
    List<String> hqlQueries = new ArrayList<String>();

    String strUpdateSequence = "";
    try {
      strUpdateSequence = getHQL(posTerminalId);
    } catch (Exception e) {

    }

    OBPOSApplications posTerminal = OBDal.getInstance().get(OBPOSApplications.class, posTerminalId);

    String RUC = getRUCFromLegalWithAccountingOrg(posTerminal.getOrganization());

    hqlQueries.add("select "
        + "seqSum.id as id, seqSum.invoiceSeq as invoiceSeq, seqSum.searchKey as searchKey, comproType.identifier as identifier, "
        + "organization.ecsdsIsdevelopment as inDevelopment, min(seqLine.sequence) as currentSeq, seqSum.store as store, '"
        + RUC + "' as RUC "
        + "from ECSDS_PointOfSaleSeq seqSum join seqSum.ecsdsComproType comproType "
        + "join seqSum.organization as organization, ECSDS_PoinOfSaleSequenceLine as seqLine "
        + "where seqLine.ecsdsPsaleSeq.id = seqSum.id " + "and seqLine.used = false "
        + "and seqSum.pOSTerminal.id ='" + posTerminalId + "' " //
        + "and $naturalOrgCriteria and seqSum.$incrementalUpdateCriteria "
        + "group by seqSum.id, seqSum.searchKey, comproType.identifier,organization.ecsdsIsdevelopment");

    return hqlQueries;
  }

  private static String getRUCFromLegalWithAccountingOrg(Organization orgRegion) {

    if (orgRegion.getOrganizationType().isLegalEntityWithAccounting()) {
      return orgRegion.getOrganizationInformationList().get(0).getTaxID();
    } else {
      return getRUCFromLegalWithAccountingOrg(getParentOfOrg(orgRegion));
    }
  }

  private static Organization getParentOfOrg(Organization orgRegion) {

    OrganizationTree orgTree;
    try {
      OBContext.setAdminMode(false);
      OBCriteria<OrganizationTree> orgTreeCriteria = OBDal.getInstance()
          .createCriteria(OrganizationTree.class);
      orgTreeCriteria.add(Restrictions.eq(OrganizationTree.PROPERTY_ORGANIZATION, orgRegion));
      orgTreeCriteria.add(Restrictions.gt(OrganizationTree.PROPERTY_LEVELNO, 1L));
      orgTreeCriteria.addOrder(Order.asc(OrganizationTree.PROPERTY_LEVELNO));
      orgTreeCriteria.setMaxResults(1);
      orgTree = (OrganizationTree) orgTreeCriteria.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
    return orgTree.getParentOrganization();
  }

  private static String getHQL(String strTerminalID) {

    OBPOSApplications terminal = OBDal.getInstance().get(OBPOSApplications.class, strTerminalID);
    if (terminal != null) {
      int countSeqInvoice = 0;
      int countSeqInvoiceND = 0;

      OBCriteria<PointOfSaleSeq> seq = OBDal.getInstance().createCriteria(PointOfSaleSeq.class);
      seq.add(Restrictions.eq(PointOfSaleSeq.PROPERTY_ACTIVE, true));
      seq.add(Restrictions.eq(PointOfSaleSeq.PROPERTY_POSTERMINAL, terminal));
      seq.add(Restrictions.eq(PointOfSaleSeq.PROPERTY_INVOICESEQ, false));
      seq.setMaxResults(1);
      List<PointOfSaleSeq> seqList = seq.list();
      if (seqList.size() == 1) {

        PointOfSaleSeq seqSearch = OBDal.getInstance().get(PointOfSaleSeq.class,
            seqList.get(0).getId());
        OBCriteria<PoinOfSaleSequenceLine> seqLine = OBDal.getInstance()
            .createCriteria(PoinOfSaleSequenceLine.class);
        seqLine.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_ACTIVE, true));
        seqLine.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_USED, true));
        seqLine.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_ECSDSPSALESEQ, seqSearch));
        seqLine.setProjection(Projections.max("sequence"));

        List<PoinOfSaleSequenceLine> seqLineList = seqLine.list();
        countSeqInvoice = seqLineList.size();

        if (countSeqInvoice > 0) {

          Long fromNumber = seqSearch.getSequenceFrom();
          Long toNumber = (Long) seqLine.uniqueResult();

          OBCriteria<PoinOfSaleSequenceLine> seqLineupd = OBDal.getInstance()
              .createCriteria(PoinOfSaleSequenceLine.class);
          seqLineupd.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_ECSDSPSALESEQ, seqSearch));
          seqLineupd.add(
              Restrictions.between(PoinOfSaleSequenceLine.PROPERTY_SEQUENCE, fromNumber, toNumber));

          if (seqLineupd.list().size() > 0) {
            List<PoinOfSaleSequenceLine> seqLineList3 = seqLineupd.list();

            for (PoinOfSaleSequenceLine updlines : seqLineList3) {
              PoinOfSaleSequenceLine line = updlines;
              line.setUsed(true);
              OBDal.getInstance().save(line);
            }
            OBDal.getInstance().flush();

          }

        }
      }

      OBCriteria<PointOfSaleSeq> seq2 = OBDal.getInstance().createCriteria(PointOfSaleSeq.class);
      seq2.add(Restrictions.eq(PointOfSaleSeq.PROPERTY_ACTIVE, true));
      seq2.add(Restrictions.eq(PointOfSaleSeq.PROPERTY_POSTERMINAL, terminal));
      seq2.add(Restrictions.eq(PointOfSaleSeq.PROPERTY_INVOICESEQ, true));
      seq2.setMaxResults(1);
      List<PointOfSaleSeq> seqList2 = seq2.list();
      if (seqList2.size() == 1) {

        PointOfSaleSeq seqSearch = OBDal.getInstance().get(PointOfSaleSeq.class,
            seqList2.get(0).getId());
        OBCriteria<PoinOfSaleSequenceLine> seqLine = OBDal.getInstance()
            .createCriteria(PoinOfSaleSequenceLine.class);
        seqLine.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_ACTIVE, true));
        seqLine.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_USED, true));
        seqLine.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_ECSDSPSALESEQ, seqSearch));
        seqLine.setProjection(Projections.max("sequence"));

        List<PoinOfSaleSequenceLine> seqLineList = seqLine.list();
        countSeqInvoice = seqLineList.size();
        if (countSeqInvoice > 0) {

          Long fromNumber = seqSearch.getSequenceFrom();
          Long toNumber = (Long) seqLine.uniqueResult();

          OBCriteria<PoinOfSaleSequenceLine> seqLineupd = OBDal.getInstance()
              .createCriteria(PoinOfSaleSequenceLine.class);
          seqLineupd.add(Restrictions.eq(PoinOfSaleSequenceLine.PROPERTY_ECSDSPSALESEQ, seqSearch));
          seqLineupd.add(
              Restrictions.between(PoinOfSaleSequenceLine.PROPERTY_SEQUENCE, fromNumber, toNumber));

          if (seqLineupd.list().size() > 0) {
            List<PoinOfSaleSequenceLine> seqLineList3 = seqLineupd.list();

            for (PoinOfSaleSequenceLine updlines : seqLineList3) {
              PoinOfSaleSequenceLine line = updlines;
              line.setUsed(true);
              OBDal.getInstance().save(line);
            }
            OBDal.getInstance().flush();

          }

        }
      }

    }

    return "OK";

  }
}