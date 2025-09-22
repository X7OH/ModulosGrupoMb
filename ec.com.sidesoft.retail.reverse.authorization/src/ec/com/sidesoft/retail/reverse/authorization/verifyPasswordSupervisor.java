package ec.com.sidesoft.retail.reverse.authorization;

import java.text.DecimalFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.process.MobileService.MobileServiceQualifier;
import org.openbravo.model.ad.access.User;
import org.openbravo.retail.posterminal.JSONProcessSimple;

@MobileServiceQualifier(serviceName = "ec.com.sidesoft.retail.reverse.authorization.verifyPasswordSupervisor")
public class verifyPasswordSupervisor extends JSONProcessSimple {

  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {

    JSONObject result = new JSONObject();

    String supervisorid = jsonsent.getString("supervisorid");
    Date fechaActual = new Date();

    // RECUPERAR OBJETO DE SUPERVISOR
    User objSupervisor = null;
    objSupervisor = OBDal.getInstance().get(User.class, supervisorid);
    Date ultimaFechaUpdatePassword = objSupervisor.getLastPasswordUpdate();
    Boolean claveCaducada = objSupervisor.isPasswordExpired();

    DecimalFormat crunchifyFormatter = new DecimalFormat("###,###");

    long diff = fechaActual.getTime() - ultimaFechaUpdatePassword.getTime();

    int diffmin = (int) (diff / (60 * 1000));

    // tipo de error
    // 0 todo ok
    // 1 tiempo expirado
    // 2 contraseña caducada

    // VERIFICO SI EL TIEMPO EXPIRO
    if (diffmin > 6) {

      objSupervisor.setPasswordExpired(true);
      result.put("message", "Tiempo Caducado");
      result.put("tipo", 1);

    } else {

      // VERIFICO SI LA CLAVE YA HA SIDO UTILIZADA
      Boolean compare = claveCaducada.equals(false);
      if (compare) {

        result.put("message", "Continuar");
        result.put("tipo", 0);

      } else {

        objSupervisor.setPasswordExpired(true);
        result.put("message", "Contraseña Caducada, solicitar nuevo cambio de clave");
        result.put("tipo", 2);
      }

    }

    OBDal.getInstance().save(objSupervisor);
    OBDal.getInstance().flush();

    return result;

  }

}