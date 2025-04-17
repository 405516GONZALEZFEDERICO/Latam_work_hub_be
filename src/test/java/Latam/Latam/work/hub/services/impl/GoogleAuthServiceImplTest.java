package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.security.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserExtendedInfoDto;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.UserService;
import com.google.firebase.auth.FirebaseAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class GoogleAuthServiceImplTest {

    @MockitoSpyBean
    private GoogleAuthServiceImpl googleAuthService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private FirebaseRoleService firebaseRoleService;

    @Captor
    ArgumentCaptor<UserEntity> userEntityCaptor;
    @Captor
    ArgumentCaptor<String> stringCaptor;

    private FirebaseUserExtendedInfoDto firebaseInfo;
    private UserEntity existingUserEnabled;
    private UserEntity existingUserDisabled;
    private UserEntity newUser;
    private String idToken;

    @BeforeEach
    void setUp() {
        idToken = "testIdToken123";
        firebaseInfo = FirebaseUserExtendedInfoDto.builder()
                .uid("firebaseUid123")
                .email("test@example.com")
                .name("Test User")
                .picture("http://example.com/photo.jpg")
                .role("USER")
                .build();

        existingUserEnabled = new UserEntity();
        existingUserEnabled.setId(1L);
        existingUserEnabled.setEmail("test@example.com");
        existingUserEnabled.setFirebaseUid("firebaseUid123");
        existingUserEnabled.setName("Test User");
        existingUserEnabled.setEnabled(true);

        existingUserDisabled = new UserEntity();
        existingUserDisabled.setId(2L);
        existingUserDisabled.setEmail("disabled@example.com");
        existingUserDisabled.setFirebaseUid("firebaseUid456");
        existingUserDisabled.setName("Disabled User");
        existingUserDisabled.setEnabled(false);

        newUser = new UserEntity();
        newUser.setId(3L);
        newUser.setEmail(firebaseInfo.getEmail());
        newUser.setFirebaseUid(firebaseInfo.getUid());
        newUser.setName(firebaseInfo.getName());
        newUser.setPhotoUrl(firebaseInfo.getPicture());
        newUser.setEnabled(true);
    }


    @Test
    @DisplayName("Login Exitoso con Google - Usuario existente y habilitado")
    void loginWithGoogle_Success() {
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenReturn(firebaseInfo);
        when(userRepository.findByEmail(firebaseInfo.getEmail())).thenReturn(Optional.of(existingUserEnabled));
        when(userService.updateUserLoginData(any(UserEntity.class))).thenReturn(existingUserEnabled);

        AuthResponseGoogleDto response = googleAuthService.loginWithGoogle(idToken);

        assertNotNull(response);
        assertEquals(idToken, response.getIdToken());
        assertEquals(firebaseInfo.getEmail(), response.getEmail());
        assertEquals(firebaseInfo.getUid(), response.getLocalId());
        assertEquals(firebaseInfo.getName(), response.getName());
        assertEquals(firebaseInfo.getPicture(), response.getPhotoUrl());
        assertEquals(firebaseInfo.getRole(), response.getRole());

        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, times(1)).findByEmail(firebaseInfo.getEmail());
        verify(userService, times(1)).updateUserLoginData(userEntityCaptor.capture());
        assertEquals(existingUserEnabled.getEmail(), userEntityCaptor.getValue().getEmail());
    }

    @Test
    @DisplayName("Login con Google Fallido - Usuario no encontrado en BD local")
    void loginWithGoogle_UserNotFound() {
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenReturn(firebaseInfo);
        when(userRepository.findByEmail(firebaseInfo.getEmail())).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> {
            googleAuthService.loginWithGoogle(idToken);
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, times(1)).findByEmail(firebaseInfo.getEmail());
        verify(userService, never()).updateUserLoginData(any());
    }

    @Test
    @DisplayName("Login con Google Fallido - Usuario encontrado pero deshabilitado")
    void loginWithGoogle_UserDisabled() {
        firebaseInfo.setEmail(existingUserDisabled.getEmail());
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenReturn(firebaseInfo);
        when(userRepository.findByEmail(firebaseInfo.getEmail())).thenReturn(Optional.of(existingUserDisabled));

        AuthException exception = assertThrows(AuthException.class, () -> {
            googleAuthService.loginWithGoogle(idToken);
        });

        assertEquals("Usuario deshabilitado", exception.getMessage());

        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, times(1)).findByEmail(firebaseInfo.getEmail());
        verify(userService, never()).updateUserLoginData(any()); // No debería llamarse
    }

    @Test
    @DisplayName("Login con Google Fallido - Error de Firebase")
    void loginWithGoogle_FirebaseError() {
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenThrow(new RuntimeException("Firebase connection failed"));

        AuthException exception = assertThrows(AuthException.class, () -> {
            googleAuthService.loginWithGoogle(idToken);
        });

        assertTrue(exception.getMessage().contains("Error al iniciar sesión con Google:"));
        assertTrue(exception.getMessage().contains("Firebase connection failed"));

        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userService, never()).updateUserLoginData(any());
    }


    @Test
    @DisplayName("Registro Exitoso con Google - Nuevo usuario")
    void registerWithGoogle_Success() throws FirebaseAuthException {
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenReturn(firebaseInfo);
        when(userRepository.findByEmail(firebaseInfo.getEmail())).thenReturn(Optional.empty());
        when(userService.createOrUpdateLocalUser(
                firebaseInfo.getEmail(),
                firebaseInfo.getUid(),
                firebaseInfo.getPicture(),
                firebaseInfo.getName()
        )).thenReturn(newUser);

        doNothing().when(firebaseRoleService).assignRolFirebaseUser(firebaseInfo.getUid(), "DEFAULT");

        String result = googleAuthService.registerWithGoogle(idToken);

        assertEquals("Usuario registrado correctamente", result);

        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, times(1)).findByEmail(firebaseInfo.getEmail());
        verify(userService, times(1)).createOrUpdateLocalUser(
                firebaseInfo.getEmail(),
                firebaseInfo.getUid(),
                firebaseInfo.getPicture(),
                firebaseInfo.getName()
        );
        verify(firebaseRoleService, times(1)).assignRolFirebaseUser(firebaseInfo.getUid(), "DEFAULT");
    }

    @Test
    @DisplayName("Registro con Google Fallido - Usuario ya existe y está habilitado")
    void registerWithGoogle_UserAlreadyExistsEnabled() throws FirebaseAuthException {
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenReturn(firebaseInfo);
        when(userRepository.findByEmail(firebaseInfo.getEmail())).thenReturn(Optional.of(existingUserEnabled));

        AuthException exception = assertThrows(AuthException.class, () -> {
            googleAuthService.registerWithGoogle(idToken);
        });

        assertEquals("El usuario ya está registrado. Por favor, utilice el login con Google", exception.getMessage());

        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, times(1)).findByEmail(firebaseInfo.getEmail());
        verify(userService, never()).createOrUpdateLocalUser(anyString(), anyString(), anyString(), anyString());
        verify(firebaseRoleService, never()).assignRolFirebaseUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Registro con Google Fallido - Usuario ya existe y está deshabilitado")
    void registerWithGoogle_UserAlreadyExistsDisabled() throws FirebaseAuthException {
        firebaseInfo.setEmail(existingUserDisabled.getEmail());
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenReturn(firebaseInfo);
        when(userRepository.findByEmail(firebaseInfo.getEmail())).thenReturn(Optional.of(existingUserDisabled));

        AuthException exception = assertThrows(AuthException.class, () -> {
            googleAuthService.registerWithGoogle(idToken);
        });

        assertEquals("Esta cuenta está deshabilitada", exception.getMessage());

        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, times(1)).findByEmail(firebaseInfo.getEmail());
        verify(userService, never()).createOrUpdateLocalUser(anyString(), anyString(), anyString(), anyString());
        verify(firebaseRoleService, never()).assignRolFirebaseUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Registro con Google Fallido - userService crea un usuario deshabilitado")
    void registerWithGoogle_CreatedUserNotEnabled() throws FirebaseAuthException {
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenReturn(firebaseInfo);
        when(userRepository.findByEmail(firebaseInfo.getEmail())).thenReturn(Optional.empty());
        UserEntity createdDisabledUser = new UserEntity();
        createdDisabledUser.setEnabled(false);
        when(userService.createOrUpdateLocalUser(
                firebaseInfo.getEmail(),
                firebaseInfo.getUid(),
                firebaseInfo.getPicture(),
                firebaseInfo.getName()
        )).thenReturn(createdDisabledUser);

        AuthException exception = assertThrows(AuthException.class, () -> {
            googleAuthService.registerWithGoogle(idToken);
        });

        assertEquals("No se pudo habilitar el usuario correctamente", exception.getMessage());

        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, times(1)).findByEmail(firebaseInfo.getEmail());
        verify(userService, times(1)).createOrUpdateLocalUser(anyString(), anyString(), anyString(), anyString());
        verify(firebaseRoleService, never()).assignRolFirebaseUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Registro con Google Fallido - Error de Firebase al obtener info")
    void registerWithGoogle_FirebaseError() throws FirebaseAuthException {
        when(firebaseRoleService.getExtendedUserInfo(idToken)).thenThrow(new RuntimeException("Firebase connection failed"));

        AuthException exception = assertThrows(AuthException.class, () -> {
            googleAuthService.registerWithGoogle(idToken);
        });

        assertTrue(exception.getMessage().contains("Error al registrar con Google:"));
        assertTrue(exception.getMessage().contains("Firebase connection failed"));


        verify(firebaseRoleService, times(1)).getExtendedUserInfo(idToken);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userService, never()).createOrUpdateLocalUser(anyString(), anyString(), anyString(), anyString());
        verify(firebaseRoleService, never()).assignRolFirebaseUser(anyString(), anyString());
    }
}