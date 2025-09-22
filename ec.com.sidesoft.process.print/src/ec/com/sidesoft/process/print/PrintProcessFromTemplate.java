package ec.com.sidesoft.process.print;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Message;
import org.openbravo.model.ad.ui.MessageTrl;
import org.openbravo.utils.FormatUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ec.com.sidesoft.process.print.data.DataSource;
import ec.com.sidesoft.process.print.data.DataSourceOrder;
import ec.com.sidesoft.process.print.data.SSPRILibrary;
import ec.com.sidesoft.process.print.data.SSPRITempLib;
import ec.com.sidesoft.process.print.data.SSPRITemplate;

public class PrintProcessFromTemplate {

  private static Logger log = Logger.getLogger(PrintProcessFromTemplate.class);
  private static String MESSAGE = "M";
  private static String INFO = "I";
  private static final String TEMPORARY_DECIMAL_REPLACE = ";;;";

  private Map<String, FormatDefinition> formatDefinitions = null;
  List<SSPRITemplate> template = new ArrayList<SSPRITemplate>();
  List<SSPRITempLib> templateLib = new ArrayList<SSPRITempLib>();
  List<SSPRILibrary> libraries = new ArrayList<SSPRILibrary>();

  public void printProcess(String processId, String id, HashMap<String, String> printParams) {

    // Set data from Format.xml
    DataSource dataSource = new DataSourceOrder(processId, id, printParams);
    setInitializeComputeFormatDefinitions();

    // order = OBDal.getInstance().get(Order.class, id);
    template = dataSource.getTemplate();
    templateLib = dataSource.getTemplateLib();
    // Load common libraries

    try {

      OBContext.setAdminMode(true);

      // create a script engine manager
      ScriptEngineManager factory = new ScriptEngineManager();
      // create JavaScript engine
      ScriptEngine engine = factory.getEngineByName("JavaScript");

      loadLibraries(engine);
      engine.eval(loadMessages());
      loadLibrariesFromTemplate(engine);

      for (int i = 0; i < template.size(); i++) {

        dataSource.doGet();
        // Read template, load data and call HM
        String path = OBConfigFileProvider.getInstance().getServletContext().getRealPath("/") + "/"
            + template.get(i).getRootPath();
        Document doc = loadDocumentFromString(path);

        for (String attribute : dataSource.getAttributes()) {
          engine.eval(attribute);
        }

        for (String initialize : dataSource.getInitialization()) {
          engine.eval(initialize);
        }

        for (String finalParam : dataSource.getFinalParams()) {
          engine.eval(finalParam);
        }

        String callout = "function(args){if (args && args.exception && args.exception.message) { return args.exception.message;}}";
        String params = dataSource.getParams();

        String finalDocument = getFinalDocument(doc, path, params, callout);

        String documentToSend = (String) engine.eval(finalDocument);
        log.debug(documentToSend);
        dataSource.sendDocument(documentToSend);

      }

    } catch (FileNotFoundException e) {
      log.error(e);
    } catch (ScriptException ex) {
      System.out.print(ex);
      log.error(ex);
    } catch (IOException exio) {
      log.error(exio);
    } catch (ParserConfigurationException exp) {
      log.error(exp);
    } catch (SAXException exsax) {
      log.error(exsax);
    } catch (TransformerException extr) {
      log.error(extr);
    } catch (JSONException exjson) {
      log.error(exjson);
    } /*
       * catch (NoSuchMethodException exns) { log.error(exns); }
       */ finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getFinalDocument(Document doc, String path, String params, String callout)
      throws TransformerException {
    return "OB.DS.HWServer.prototype._print(new OB.DS.HWResource('" + path + "', '"
        + FormatUtilities.replaceJS(StringEscapeUtils.unescapeXml(getStringFromDocument(doc))
            .replace("%1", "<%").replace("2%", "%>").replace("&lt; ", "< ").replace(" &gt;", " >")
            .replace(" &lt;=", "<=").replace(" &gt;=", " >=").replace("&amp;", "&")
            .replace("&apos;", "'"))
        + "')," + params + " , " + callout + ");";
  }

  public String fromDateToString(Date date) {
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    return DateUtils.formatDate(date, dateFormat);
  }

  public void loadLibraries(ScriptEngine engine)
      throws FileNotFoundException, ScriptException, JSONException {
    OBCriteria<SSPRILibrary> libraryCri = OBDal.getInstance().createCriteria(SSPRILibrary.class);
    libraryCri.addOrderBy(SSPRILibrary.PROPERTY_LINENO, true);
    List<SSPRILibrary> libraryList = libraryCri.list();
    engine.eval("var window = {};");
    // engine.eval("var console = {};");
    for (SSPRILibrary library : libraryList) {
      if (library.getTemplatePath().contains("datasource")) {
        engine.eval("OB.DS = {};");
        engine.eval("OB.DS.HWResource = {};");
      } else if (library.getTemplatePath().contains("i18")) {
        String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
            .getProperty("dateFormat.js");

        engine.eval("OB.Format = {};");
        engine.eval("OB.Format.formats = {};");
        engine.eval("OB.Format.formats.qtyEdition = '"
            + formatDefinitions.get("qtyEdition").getFormat() + "';");
        engine.eval("OB.Format.formats.priceEdition = '"
            + formatDefinitions.get("priceEdition").getFormat() + "';");
        engine.eval("OB.Format.formats.priceInform = '"
            + formatDefinitions.get("priceInform").getFormat() + "';");
        engine.eval("OB.Format.formats.amountInform = '"
            + formatDefinitions.get("amountInform").getFormat() + "';");
        engine.eval("OB.Format.defaultDecimalSymbol = '"
            + formatDefinitions.get("qtyEdition").getDecimalSymbol() + "';");
        engine.eval("OB.Format.defaultGroupingSymbol = '"
            + formatDefinitions.get("qtyEdition").getGroupingSymbol() + "';");
        engine.eval("OB.Format.defaultGroupingSize = 3;");
        engine.eval("OB.Format.formats.euroEdition = '"
            + formatDefinitions.get("euroEdition").getFormat() + "';");
        engine.eval("OB.Format.date = '" + dateFormat + "';");

      }
      engine.eval(new java.io.FileReader(
          OBConfigFileProvider.getInstance().getServletContext().getRealPath("/") + "/"
              + library.getTemplatePath()));
    }
  }

  private synchronized Map<String, FormatDefinition> setInitializeComputeFormatDefinitions() {

    final Map<String, FormatDefinition> localFormatDefinitions = new HashMap<String, FormatDefinition>();
    final org.dom4j.Document doc = OBPropertiesProvider.getInstance().getFormatXMLDocument();
    final org.dom4j.Element root = doc.getRootElement();
    for (Object object : root.elements()) {
      final org.dom4j.Element element = (org.dom4j.Element) object;
      final FormatDefinition formatDefinition = new FormatDefinition();

      formatDefinition.setDecimalSymbol(element.attributeValue("decimal"));
      formatDefinition.setFormat(correctMaskForGrouping(element.attributeValue("formatOutput"),
          element.attributeValue("decimal"), element.attributeValue("grouping")));
      formatDefinition.setGroupingSymbol(element.attributeValue("grouping"));
      localFormatDefinitions.put(element.attributeValue("name"), formatDefinition);
    }
    formatDefinitions = localFormatDefinitions;
    return formatDefinitions;
  }

  public void loadLibrariesFromTemplate(ScriptEngine engine)
      throws FileNotFoundException, ScriptException {

    for (SSPRITempLib library : templateLib) {
      engine.eval(new java.io.FileReader(
          OBConfigFileProvider.getInstance().getServletContext().getRealPath("/") + "/"
              + library.getTemplatePath()));
    }
  }

  public String loadMessages() throws ScriptException {
    StringBuffer messages = new StringBuffer();

    messages.append("OB.I18N.labels={};");

    OBCriteria<Message> messagesCri = OBDal.getInstance().createCriteria(Message.class);
    messagesCri.add(Restrictions.eq(Message.PROPERTY_MESSAGETYPE, INFO));

    List<Message> messagesList = messagesCri.list();
    for (Message message : messagesList) {
      OBCriteria<MessageTrl> messagesTrlCri = OBDal.getInstance().createCriteria(MessageTrl.class);
      messagesTrlCri.add(Restrictions.eq(MessageTrl.PROPERTY_MESSAGE, message));
      messagesTrlCri.add(
          Restrictions.eq(MessageTrl.PROPERTY_LANGUAGE, OBContext.getOBContext().getLanguage()));
      if (messagesTrlCri.count() > 0) {
        MessageTrl messageTrl = messagesTrlCri.list().get(0);
        messages.append("OB.I18N.labels['" + message.getSearchKey() + "']='"
            + FormatUtilities.replaceJS(messageTrl.getMessageText()) + "';");
      } else {
        messages.append("OB.I18N.labels['" + message.getSearchKey() + "']='"
            + FormatUtilities.replaceJS(message.getMessageText()) + "';");
      }
    }

    return messages.toString();

  }

  public Document loadDocument(String path)
      throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    DocumentBuilder b = f.newDocumentBuilder();
    Document doc = b.parse(new File(path));
    return doc;
  }

  public Document loadDocumentFromString(String path)
      throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    DocumentBuilder b = f.newDocumentBuilder();

    byte[] encoded = Files.readAllBytes(Paths.get(path));
    String file = new String(encoded, "UTF8");

    Document doc = b
        .parse(new ByteArrayInputStream(file.replace("'product'", "'auxp'").replace("&", "&amp;")
            .replace("'", "&apos;").replace("<%", "%1").replace("%>", "2%").replace("< ", "&lt; ")
            .replace("<=", " &lt;=").replace(" >=", " &gt;=").replace(" >", " &gt;").getBytes()));
    return doc;
  }

  public void transformDocument(Document doc, ScriptEngine engine) throws ScriptException {
    // Get the root element
    doc.getDocumentElement().normalize();

    NodeList lineList = doc.getElementsByTagName("line");

    for (int i = 0; i < lineList.getLength(); i++) {
      Node line = lineList.item(i);

      if (line.getNodeType() == Node.ELEMENT_NODE) {
        Element eElement = (Element) line;

        Node text = eElement.getElementsByTagName("text").item(0);
        if (text != null) {
          String value = text.getTextContent();
          if ((value != null && !"".equals(value)) && value.contains("%=")) {
            value = value.replace("%=", "").replace("%", "");
            String newValue = (String) engine.eval(value);
            text.setTextContent(newValue);
          }
        }
      }

    }
  }

  public String getStringFromDocument(Document doc) throws TransformerException {
    Transformer tf = TransformerFactory.newInstance().newTransformer();
    tf.setOutputProperty(OutputKeys.INDENT, "yes");
    tf.setOutputProperty(OutputKeys.METHOD, "xml");
    // tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    DOMSource domSource = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult sr = new StreamResult(writer);
    tf.transform(domSource, sr);
    return writer.toString();
  }

  public void sendDocument(Document doc) throws TransformerException {
    Transformer tf = TransformerFactory.newInstance().newTransformer();
    tf.setOutputProperty(OutputKeys.INDENT, "yes");
    tf.setOutputProperty(OutputKeys.METHOD, "xml");
    // tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    DOMSource domSource = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult sr = new StreamResult(writer);
    tf.transform(domSource, sr);
    System.out.println(writer.toString());
  }

  private String correctMaskForGrouping(String mask, String decimalSymbol, String groupingSymbol) {
    String localMask = mask.replace(".", TEMPORARY_DECIMAL_REPLACE);
    localMask = localMask.replace(",", groupingSymbol);
    return localMask.replaceAll(TEMPORARY_DECIMAL_REPLACE, decimalSymbol);
  }

  public static class FormatDefinition {
    private String decimalSymbol;
    private String groupingSymbol;
    private String format;

    public String getDecimalSymbol() {
      return decimalSymbol;
    }

    public void setDecimalSymbol(String decimalSymbol) {
      this.decimalSymbol = decimalSymbol;
    }

    public String getGroupingSymbol() {
      return groupingSymbol;
    }

    public void setGroupingSymbol(String groupingSymbol) {
      this.groupingSymbol = groupingSymbol;
    }

    public String getFormat() {
      return format;
    }

    public void setFormat(String format) {
      this.format = format;
    }

  }

}
