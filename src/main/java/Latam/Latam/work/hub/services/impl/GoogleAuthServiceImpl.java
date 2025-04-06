package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.GoogleAuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserRepository userRepository;
    private final FirebaseRoleService firebaseRoleService;

    @Value("${firebase.api.key}")
    private String firebaseApiKey;

    @Override
    public AuthResponseGoogleDto loginWithGoogle(String idToken) {
        try {
            // Verificar el token ID de Google
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // Verificar si el usuario existe localmente
            boolean userExistsLocalmente = false;
            UserEntity user = null;

            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                user = userOpt.get();
                userExistsLocalmente = true;
                if (!user.isEnabled()) {
                    throw new RuntimeException("Usuario deshabilitado");
                }
            }

            // Si el usuario no existe localmente, crearlo
            if (!userExistsLocalmente) {
                user = createLocalUserFromFirebase(email, uid);
                try {
                    // Asignar rol de cliente por defecto a nuevos usuarios
                    firebaseRoleService.asignarRolYPermisosAFirebaseUser(uid, "CLIENTE");
                } catch (FirebaseAuthException e) {
                    throw new RuntimeException("Error al asignar rol: " + e.getMessage());
                }
            }

            // Obtener el rol del usuario
            String rol = firebaseRoleService.obtenerRolDeUsuario(uid);

            // Generar un nuevo token personalizado
            String customToken = FirebaseAuth.getInstance().createCustomToken(uid);

            // Intercambiar el token personalizado por un ID token
            AuthResponseGoogleDto responseDto = exchangeCustomTokenForIdToken(customToken, email, uid, rol);

            return responseDto;

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Error al verificar token de Google: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error interno del servidor: " + e.getMessage());
        }
    }

    @Override
    public String registerWithGoogle(String idToken) {
        try {
            // Verificar el token ID de Google
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();

            // Verificar si el usuario ya existe
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                return "El usuario ya está registrado";
            }

            // Crear el usuario en nuestra base de datos local
            UserEntity user = createLocalUserFromFirebase(email, uid);

            // Si hay información adicional disponible, actualizamos el usuario
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            }

            // Guardar el usuario actualizado si se modificó
            userRepository.save(user);

            // Asignar rol de CLIENTE por defecto
            firebaseRoleService.asignarRolYPermisosAFirebaseUser(uid, "CLIENTE");

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

            // Verificar el nuevo ID token para obtener los detalles del usuario
            String newIdToken = (String) responseBody.get("id_token");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(newIdToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // Obtener el rol del usuario
            String rol = firebaseRoleService.obtenerRolDeUsuario(uid);

            // Construir la respuesta
            AuthResponseGoogleDto responseDto = new AuthResponseGoogleDto();
            responseDto.setIdToken(newIdToken);
            responseDto.setRefreshToken((String) responseBody.get("refresh_token"));
            responseDto.setExpiresIn(String.valueOf(responseBody.get("expires_in")));
            responseDto.setEmail(email);
            responseDto.setLocalId(uid);
            responseDto.setRole(rol);
            responseDto.setGoogleAuth(true);

            return responseDto;

        } catch (Exception e) {
            throw new RuntimeException("Error al refrescar token: " + e.getMessage());
        }
    }

    private UserEntity createLocalUserFromFirebase(String email, String uid) {
        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        newUser.setEnabled(true);
        newUser.setFirebaseUid(uid);
        newUser.setLastPasswordUpdateDate(LocalDateTime.now());
        return userRepository.save(newUser);
    }

    private AuthResponseGoogleDto exchangeCustomTokenForIdToken(String customToken, String email, String uid, String rol) {
        RestTemplate restTemplate = new RestTemplate();
        String firebaseAuthUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=" + firebaseApiKey;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("token", customToken);
        requestBody.put("returnSecureToken", true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> responseEntity = restTemplate.exchange(
                firebaseAuthUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        Map<String, Object> responseBody = responseEntity.getBody();

        // Construir la respuesta de autenticación
        AuthResponseGoogleDto responseDto = new AuthResponseGoogleDto();
        responseDto.setIdToken((String) responseBody.get("idToken"));
        responseDto.setRefreshToken((String) responseBody.get("refreshToken"));
        responseDto.setExpiresIn((String) responseBody.get("expiresIn"));
        responseDto.setEmail(email);
        responseDto.setLocalId(uid);
        responseDto.setRole(rol);
        responseDto.setGoogleAuth(true);

        return responseDto;
    }
}