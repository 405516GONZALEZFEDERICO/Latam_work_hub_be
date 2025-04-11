package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.dtos.TokenDto;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.TokenValidationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class TokenValidationServiceImpl implements TokenValidationService {
    private final FirebaseRoleService firebaseRoleService;

    private String firebaseApiKey = "AIzaSyB9b7mzFtoRDNB0YroNRe6tF9uQFGfvzXQ";

    @Override
    public TokenDto refrescarToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new AuthException("Refresh token no proporcionado");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("grant_type", "refresh_token");
        requestBody.put("refresh_token", refreshToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                    "https://securetoken.googleapis.com/v1/token?key=" + firebaseApiKey,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = responseEntity.getBody();

            String idToken = (String) responseBody.get("id_token");
            String newRefreshToken = (String) responseBody.get("refresh_token");
            String expiresIn = String.valueOf(responseBody.get("expires_in"));

            FirebaseUserInfoDto userInfo = null;
            try {
                userInfo = firebaseRoleService.verificarRol(idToken);
            } catch (FirebaseAuthException e) {
                log.warn("No se pudo verificar el rol del usuario al refrescar token: {}", e.getMessage());
            }

            TokenDto tokenDto = TokenDto.builder()
                    .token(idToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(expiresIn)
                    .build();

            if (userInfo != null) {
                tokenDto.setRole(userInfo.getRole());
            }

            return tokenDto;
        } catch (HttpClientErrorException e) {
            log.error("Error al refrescar token: {}", e.getResponseBodyAsString());
            throw new AuthException("Error al refrescar token: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error interno al refrescar token: {}", e.getMessage());
            throw new AuthException("Error interno al refrescar token", e);
        }
    }

    @Override
    public boolean esTokenValido(String idToken) {
        try {
            FirebaseAuth.getInstance().verifyIdToken(idToken);
            return true;
        } catch (FirebaseAuthException e) {
            log.debug("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public TokenDto exchangeCustomTokenForIdToken(String customToken) {
        RestTemplate restTemplate = new RestTemplate();
        String firebaseAuthUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=" + firebaseApiKey;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("token", customToken);
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

            Map<String, Object> responseBody = responseEntity.getBody();
            String idToken = (String) responseBody.get("idToken");

            TokenDto tokenDto = TokenDto.builder()
                    .token(idToken)
                    .refreshToken((String) responseBody.get("refreshToken"))
                    .expiresIn((String) responseBody.get("expiresIn"))
                    .build();

            try {
                FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRol(idToken);
                tokenDto.setRole(userInfo.getRole());
            } catch (FirebaseAuthException e) {
                log.warn("No se pudo verificar el rol del usuario al intercambiar token: {}", e.getMessage());
            }

            return tokenDto;
        } catch (Exception e) {
            log.error("Error al intercambiar token: {}", e.getMessage());
            throw new AuthException("Error al procesar token", e);
        }
    }
}
