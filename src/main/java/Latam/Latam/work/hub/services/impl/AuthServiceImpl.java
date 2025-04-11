package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.AuthResponseDto;
import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.dtos.UserDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.AuthService;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.UserService;
import Latam.Latam.work.hub.services.template.FirebaseAuthRestService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final FirebaseRoleService firebaseRoleService;
    private final ModelMapper modelMapper;
    private final FirebaseAuthRestService authRestService;
    private String firebaseApiKey = "AIzaSyB9b7mzFtoRDNB0YroNRe6tF9uQFGfvzXQ";


    /**
     * Obtiene el perfil del usuario autenticado
     */
    public UserDto obtenerPerfil(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException("No autorizado");
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        return modelMapper.map(user, UserDto.class);
    }



    /**
     * Registra un nuevo usuario
     */
    @Transactional
    public String registrarUsuario(String email, String password) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            throw new AuthException("Email inválido");
        }

        if (password == null || password.length() < 6) {
            throw new AuthException("Contraseña debe tener al menos 6 caracteres");
        }

        Optional<UserEntity> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new AuthException("El email ya está registrado");
        }

        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setEmailVerified(false)
                    .setDisabled(false);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            String uid = userRecord.getUid();

            // Crear usuario local con rol

            userService.createOrUpdateLocalUser(email, uid,userRecord.getPhotoUrl(),userRecord.getDisplayName());

            // Asignar rol en Firebase también
            firebaseRoleService.asignarRolAFirebaseUser(uid,"DEFAULT" );

            return "Usuario registrado con éxito: " + uid;
        } catch (FirebaseAuthException e) {
            log.error("Error al registrar usuario en Firebase: {}", e.getMessage());
            throw new AuthException("Error al registrar usuario: " + e.getMessage());
        }
    }


    /**
     * Inicia sesión con email y contraseña
     */
    public AuthResponseDto login(String email, String password) {
        // Validaciones básicas
        if (email == null || email.isEmpty()) {
            throw new AuthException("Email es requerido");
        }

        if (password == null || password.isEmpty()) {
            throw new AuthException("Contraseña es requerida");
        }

        try {
            // Verificar si el usuario existe localmente
            UserEntity user = userService.validateUserExists(email);

            // Autenticar con Firebase usando el servicio inyectado
            Map<String, Object> responseBody = authRestService.signInWithEmailAndPassword(email, password);

            String idToken = (String) responseBody.get("idToken");
            String uid = (String) responseBody.get("localId");

            // Obtener rol y permisos
            FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRol(idToken);

            // Construir respuesta
            return AuthResponseDto.builder()
                    .idToken(idToken)
                    .expiresIn((String) responseBody.get("expiresIn"))
                    .role(userInfo.getRole())
                    .build();
        } catch (AuthException e) {
            // Propagar excepciones de autenticación
            throw e;
        } catch (FirebaseAuthException e) {
            log.error("Error de Firebase: {}", e.getMessage());
            throw new AuthException("Error de autenticación");
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            throw new AuthException("Error interno del servidor");
        }
    }

    /**
     * Cierra sesión revocando los tokens
     */
    public String logout(String idToken) {
        try {
            String uid = FirebaseAuth.getInstance().verifyIdToken(idToken).getUid();
            FirebaseAuth.getInstance().revokeRefreshTokens(uid);
            return "Sesión cerrada correctamente";
        } catch (FirebaseAuthException e) {
            log.error("Error al cerrar sesión: {}", e.getMessage());
            throw new AuthException("Error al cerrar sesión");
        }
    }

    /**
     * Envía un correo de recuperación de contraseña
     */
    public String recuperarContrasenia(String email) {
        try {
            // Verificar que el usuario existe y está habilitado
            userService.validateUserExists(email);

            // Enviar correo de recuperación
            Map<String, Object> body = new HashMap<>();
            body.put("requestType", "PASSWORD_RESET");
            body.put("email", email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + firebaseApiKey;

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return "Correo de recuperación enviado correctamente";
            } else {
                throw new AuthException("No se pudo enviar el correo de recuperación");
            }
        } catch (Exception e) {
            log.error("Error al enviar correo de recuperación: {}", e.getMessage());
            throw new AuthException("Error al procesar solicitud de recuperación");
        }
    }


}
