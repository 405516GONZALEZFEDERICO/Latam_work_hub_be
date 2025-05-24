package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.security.dtos.AuthResponseDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.UserService;
import Latam.Latam.work.hub.services.firebase.impl.FirebaseAuthRestServiceImpl;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AuthServiceImplTest {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FirebaseRoleService firebaseRoleService;

    @MockitoBean
    private FirebaseAuthRestServiceImpl authRestService;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoSpyBean
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void registerUser_ConDatosValidos_DebeRegistrarUsuario() throws FirebaseAuthException {
        String email = "test@example.com";
        String password = "password123";
        String uid = "firebase-uid-123";

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email);
        userEntity.setEnabled(true);

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn(uid);
        when(userRecord.getPhotoUrl()).thenReturn("https://example.com/photo.jpg");
        when(userRecord.getDisplayName()).thenReturn("Test User");

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            FirebaseAuthException authException = mock(FirebaseAuthException.class);
            when(authException.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
            when(firebaseAuth.getUserByEmail(email)).thenThrow(authException);

            when(firebaseAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(userRecord);

            when(userService.createOrUpdateLocalUser(eq(email), eq(uid), anyString(), anyString())).thenReturn(userEntity);

            String result = authService.registerUser(email, password);

            assertTrue(result.contains(uid));
            verify(firebaseAuth).createUser(any(UserRecord.CreateRequest.class));
            verify(firebaseRoleService).assignRolFirebaseUser(uid, "DEFAULT");
            verify(userService).createOrUpdateLocalUser(eq(email), eq(uid), anyString(), anyString());
        }
    }

    @Test
    void registerUser_ConEmailInvalido_DebeLanzarExcepcion() {
        String email = "";
        String password = "password123";

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.registerUser(email, password);
        });

        assertEquals("Email inválido", exception.getMessage());
    }

    @Test
    void registerUser_ConPasswordDemasiadoCorta_DebeLanzarExcepcion() {
        String email = "test@example.com";
        String password = "12345";

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.registerUser(email, password);
        });

        assertEquals("Contraseña debe tener al menos 6 caracteres", exception.getMessage());
    }

    @Test
    void registerUser_ConUsuarioExistente_DebeLanzarExcepcion() {
        String email = "existing@example.com";
        String password = "password123";

        UserEntity existingUser = new UserEntity();
        existingUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.registerUser(email, password);
        });

        assertEquals("El email ya está registrado", exception.getMessage());
    }

    @Test
    void login_ConCredencialesValidas_DebeRetornarToken() throws FirebaseAuthException {
        String email = "test@example.com";
        String password = "password123";
        String uid = "firebase-uid-123";
        String idToken = "firebase-token-123";
        String refreshToken = "refresh-token-123";

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email);

        Map<String, Object> loginResponse = new HashMap<>();
        loginResponse.put("idToken", idToken);
        loginResponse.put("refreshToken", refreshToken);
        loginResponse.put("localId", uid);
        loginResponse.put("expiresIn", "3600");

        FirebaseUserInfoDto userInfo = new FirebaseUserInfoDto();
        userInfo.setRole("USER");

        when(userService.validateUserExists(email)).thenReturn(userEntity);
        when(authRestService.signInWithEmailAndPassword(email, password)).thenReturn(loginResponse);
        when(firebaseRoleService.verificarRol(idToken)).thenReturn(userInfo);

        AuthResponseDto result = authService.login(email, password);

        assertNotNull(result);
        assertEquals(idToken, result.getIdToken());
        assertEquals(refreshToken, result.getRefreshToken());
        assertEquals("USER", result.getRole());
        assertEquals(uid, result.getFirebaseUid());

        verify(userService).validateUserExists(email);
        verify(authRestService).signInWithEmailAndPassword(email, password);
        verify(firebaseRoleService).verificarRol(idToken);
    }

    @Test
    void login_ConEmailVacio_DebeLanzarExcepcion() {
        String email = "";
        String password = "password123";

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login(email, password);
        });

        assertEquals("Email es requerido", exception.getMessage());
    }

    @Test
    void login_ConPasswordVacio_DebeLanzarExcepcion() {
        String email = "test@example.com";
        String password = "";

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login(email, password);
        });

        assertEquals("Contraseña es requerida", exception.getMessage());
    }

    @Test
    void login_ConCredencialesInvalidas_DebeLanzarExcepcion() {
        String email = "test@example.com";
        String password = "password123";

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email);

        when(userService.validateUserExists(email)).thenReturn(userEntity);
        when(authRestService.signInWithEmailAndPassword(email, password)).thenThrow(new AuthException("Credenciales inválidas"));

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login(email, password);
        });

        assertEquals("Credenciales inválidas", exception.getMessage());

        verify(userService).validateUserExists(email);
        verify(authRestService).signInWithEmailAndPassword(email, password);
    }

    @Test
    void retrievePassword_ConEmailValido_DebeEnviarCorreo() {
        String email = "test@example.com";

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email);

        when(userService.validateUserExists(email)).thenReturn(userEntity);

        String result = authService.retrievePassword(email);

        assertEquals("Correo de recuperación enviado correctamente", result);

        verify(userService).validateUserExists(email);
        verify(authRestService).sendPasswordResetEmail(email);
    }

    @Test
    void retrievePassword_ConErrorEnProceso_DebeLanzarExcepcion() {
        String email = "test@example.com";

        when(userService.validateUserExists(email)).thenThrow(new RuntimeException("Error interno"));

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.retrievePassword(email);
        });

        assertTrue(exception.getMessage().contains("Error al procesar solicitud"));

        verify(userService).validateUserExists(email);
    }
}