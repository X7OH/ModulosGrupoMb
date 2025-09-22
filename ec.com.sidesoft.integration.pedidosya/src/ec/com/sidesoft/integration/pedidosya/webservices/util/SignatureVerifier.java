package ec.com.sidesoft.integration.pedidosya.webservices.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;

public class SignatureVerifier {
    private static final String SECRET_KEY = "12345";

    public boolean checkSignature(String body, String passedSignature) {
        try {
            // Inicializa el algoritmo HmacSHA1
            Mac sha1HMAC = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            sha1HMAC.init(secretKeySpec);

            // Calcula el HMAC del cuerpo del mensaje
            byte[] hmacBytes = sha1HMAC.doFinal(body.getBytes(StandardCharsets.UTF_8));

            // Codifica en Base64 usando Apache Commons Codec
            String calculatedSignature = Base64.encodeBase64String(hmacBytes);

            // Compara la firma calculada con la firma recibida
            return passedSignature.equals(calculatedSignature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
