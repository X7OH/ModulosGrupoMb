package ec.com.sidesoft.process.print.model;

import java.util.HashMap;
import java.util.Map;

public class BusinessPartner {
  private static String JS_INIT = "{";
  private static String JS_CLOSE = "}";
  private static String JS_TEXT = "'";
  private static String JS_SEP = ",";
  private static String NEW_LINE = "\n";
  private String id;
  private Map<String, String> arguments;

  public BusinessPartner() {
    arguments = new HashMap<String, String>();
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setArgument(String name, String value) {
    arguments.put(name, value);
  }

  public String getArgumentsJS() {
    StringBuffer js = new StringBuffer();
    js.append(JS_INIT);
    for (Map.Entry<String, String> entry : arguments.entrySet()) {
      if (entry.getValue() != null) {
        if (entry.getValue().startsWith("[") || entry.getValue().startsWith("{")) {
          js.append(entry.getKey()).append(":").append(entry.getValue()).append(JS_SEP);
        } else {
          js.append(entry.getKey()).append(":").append(JS_TEXT).append(entry.getValue())
              .append(JS_TEXT).append(JS_SEP);
        }
      } else {
        js.append(entry.getKey()).append(":").append(JS_TEXT).append(entry.getValue())
            .append(JS_TEXT).append(JS_SEP);
      }
    }
    js.delete(js.length() - 1, js.length()).append(JS_CLOSE);
    return js.toString();
  }

}
