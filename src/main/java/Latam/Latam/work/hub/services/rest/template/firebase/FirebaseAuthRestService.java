package Latam.Latam.work.hub.services.rest.template.firebase;

import Latam.Latam.work.hub.exceptions.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FirebaseAuthRestService {
    private final String firebaseApiKey= "AIzaSyB9b7mzFtoRDNB0YroNRe6tF9uQFGfvzXQ";
    private final RestTemplate restTemplate;
    public Map<String, Object> signInWithEmailAndPassword(String email, String password) {
        String firebaseAuthUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("returnSecureToken", true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    firebaseAuthUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            return responseEntity.getBody();
        } catch (HttpClientErrorException e) {
            throw new AuthException("Credenciales inválidas: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new AuthException("Error al comunicarse con Firebase: " + e.getMessage());
        }
    }

    public Map<String, Object> sendPasswordResetEmail(String email) {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + firebaseApiKey;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("requestType", "PASSWORD_RESET");
        requestBody.put("email", email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new AuthException("Error al enviar correo de recuperación: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new AuthException("Error al comunicarse con Firebase: " + e.getMessage());
        }
    }

}
