package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.GoogleAuthService;
import Latam.Latam.work.hub.services.TokenValidationService;
import Latam.Latam.work.hub.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FirebaseRoleService firebaseRoleService;
    private final TokenValidationService tokenValidationService;
    private String firebaseApiKey = "AIzaSyB9b7mzFtoRDNB0YroNRe6tF9uQFGfvzXQ";

    @Transactional
    public AuthResponseGoogleDto loginWithGoogle(String idToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String photoUrl = decodedToken.getPicture();

            UserEntity user = userService.createOrUpdateLocalUser(email, uid, name, photoUrl);

            FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRol(idToken);

            return AuthResponseGoogleDto.builder()
                    .idToken(idToken) 
                    .email(email)
                    .localId(uid)
                    .role(userInfo.getRole())
                    .name(name)
                    .build();

        } catch (FirebaseAuthException e) {
            throw new AuthException("Token de Google inválido o expirado", e);
        }
    }


    @Override
    public String registerWithGoogle(String idToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();

            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                return "El usuario ya está registrado";
            }

            userService.createOrUpdateLocalUser(email, uid,picture,name);

            firebaseRoleService.asignarRolAFirebaseUser(uid, "DEFAULT");
            return "Usuario registrado con Google correctamente. UID: " + uid;

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Error al verificar token de Google: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error en registro con Google: " + e.getMessage());
        }
    }

    @Override
    public AuthResponseGoogleDto refreshGoogleToken(String refreshToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String firebaseRefreshUrl = "https://securetoken.googleapis.com/v1/token?key=" + firebaseApiKey;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("grant_type", "refresh_token");
            requestBody.put("refresh_token", refreshToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    firebaseRefreshUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> responseBody = responseEntity.getBody();

            String newIdToken = (String) responseBody.get("id_token");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(newIdToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            String rol = firebaseRoleService.obtenerRolDeUsuario(uid);

            AuthResponseGoogleDto responseDto = new AuthResponseGoogleDto();
            responseDto.setIdToken(newIdToken);
            responseDto.setEmail(email);
            responseDto.setLocalId(uid);
            responseDto.setRole(rol);

            return responseDto;

        } catch (Exception e) {
            throw new RuntimeException("Error al refrescar token: " + e.getMessage());
        }
    }
}