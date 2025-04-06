package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.dtos.TokenDto;
import Latam.Latam.work.hub.services.TokenValidationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TokenValidationServiceImpl implements TokenValidationService {
    @Value("${firebase.api.key}")
    private String firebaseApiKey;
    @Override
    public TokenDto refrescarToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = request.getHeader("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new RuntimeException("Refresh token no proporcionado");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                    "https://securetoken.googleapis.com/v1/token?key=" + firebaseApiKey,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = responseEntity.getBody();

            String idToken = (String) responseBody.get("id_token");
            String newRefreshToken = (String) responseBody.get("refresh_token");

            return TokenDto.builder()
                    .token(idToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error al refrescar token con Firebase", e);
        }
    }
    @Override
    public boolean esTokenValido(String idToken) {
        try {
            FirebaseAuth.getInstance().verifyIdToken(idToken);
            return true;
        } catch (FirebaseAuthException e) {
            return false;
        }
    }
}
