package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.security.dtos.AuthResponseDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.services.AuthService;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.UserService;
import Latam.Latam.work.hub.services.rest.template.firebase.FirebaseAuthRestServiceImpl;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final FirebaseRoleService firebaseRoleService;
    private final FirebaseAuthRestServiceImpl authRestService;

    @Override
    @Transactional
    public String registerUser(String email, String password) {
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
            FirebaseAuth.getInstance().getUserByEmail(email);
            throw new AuthException("El email ya está registrado en el sistema de autenticación");
        } catch (FirebaseAuthException e) {
            if (!e.getAuthErrorCode().equals(AuthErrorCode.USER_NOT_FOUND)) {
                log.error("Error al verificar usuario en Firebase: {}", e.getMessage());
                throw new AuthException("Error al verificar usuario: " + e.getMessage());
            }
        }

        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setEmailVerified(false)
                    .setDisabled(false);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            String uid = userRecord.getUid();

            UserEntity user = userService.createOrUpdateLocalUser(email, uid, userRecord.getPhotoUrl(), userRecord.getDisplayName());

            if (!user.isEnabled()) {
                throw new AuthException("No se pudo habilitar el usuario correctamente");
            }

            firebaseRoleService.assignRolFirebaseUser(uid, "DEFAULT");

            return "Usuario registrado con éxito: " + uid;
        } catch (FirebaseAuthException e) {
            log.error("Error al registrar usuario en Firebase: {}", e.getMessage());
            throw new AuthException("Error al registrar usuario: " + e.getMessage());
        }
    }


    @Override
    @Transactional
    public AuthResponseDto login(String email, String password) {
        if (email == null || email.isEmpty()) {
            throw new AuthException("Email es requerido");
        }

        if (password == null || password.isEmpty()) {
            throw new AuthException("Contraseña es requerida");
        }

        try {
            userService.validateUserExists(email);

            Map<String, Object> responseBody = authRestService.signInWithEmailAndPassword(email, password);

            String idToken = (String) responseBody.get("idToken");
            String refreshToken = (String) responseBody.get("refreshToken");
            String uid = (String) responseBody.get("localId");

            FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRol(idToken);

            return AuthResponseDto.builder()
                    .idToken(idToken)
                    .refreshToken(refreshToken)
                    .expiresIn((String) responseBody.get("expiresIn"))
                    .role(userInfo.getRole())
                    .firebaseUid(uid)
                    .build();
        } catch (AuthException e) {
            throw e;
        } catch (FirebaseAuthException e) {
            log.error("Error de Firebase: {}", e.getMessage());
            throw new AuthException("Error de autenticación");
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            throw new AuthException("Error interno del servidor");
        }
    }


    @Override
    @Transactional
    public String retrievePassword(String email) {
        try {
            userService.validateUserExists(email);
            authRestService.sendPasswordResetEmail(email);
            return "Correo de recuperación enviado correctamente";
        } catch (Exception e) {
            log.error("Error al enviar correo de recuperación: {}", e.getMessage());
            throw new AuthException("Error al procesar solicitud de recuperación");
        }
    }



}
