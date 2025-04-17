package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.RoleEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.RoleRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.security.dtos.FirebaseUserExtendedInfoDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class FirebaseRoleServiceImplTest {

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoSpyBean
    private FirebaseRoleServiceImpl firebaseRoleService;

    private static final String DEFAULT_ROLE = "DEFAULT";
    private static final String TEST_UID = "test-uid-123";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // Configuración básica para los mocks
        RoleEntity defaultRole = new RoleEntity();
        defaultRole.setName(DEFAULT_ROLE);
        when(roleRepository.findByName(DEFAULT_ROLE)).thenReturn(Optional.of(defaultRole));
    }

    @Test
    void assignRolFirebaseUser_ConRolValido_DebeAsignarRol() throws FirebaseAuthException {
        String roleName = "ADMIN";
        roleEntity.setName(roleName);

        UserEntity userEntity = new UserEntity();
        userEntity.setFirebaseUid(TEST_UID);
        userEntity.setEmail(TEST_EMAIL);

        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(roleEntity));
        when(userRepository.findByFirebaseUid(TEST_UID)).thenReturn(Optional.of(userEntity));

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            doNothing().when(firebaseAuth).setCustomUserClaims(eq(TEST_UID), any(Map.class));

            firebaseRoleService.assignRolFirebaseUser(TEST_UID, roleName);

            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(firebaseAuth).setCustomUserClaims(eq(TEST_UID), claimsCaptor.capture());

            Map<String, Object> capturedClaims = claimsCaptor.getValue();
            assertEquals(roleName, capturedClaims.get("role"));
            assertTrue(capturedClaims.containsKey("updated_at"));

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());

            UserEntity savedUser = userCaptor.getValue();
            assertEquals(roleEntity, savedUser.getRole());
            assertNotNull(savedUser.getLastAccess());
        }
    }

    @Test
    void assignRolFirebaseUser_ConRolInvalido_DebeLanzarExcepcion() {
        String invalidRoleName = "INVALID_ROLE";
        when(roleRepository.findByName(invalidRoleName)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> {
            firebaseRoleService.assignRolFirebaseUser(TEST_UID, invalidRoleName);
        });

        assertTrue(exception.getMessage().contains("Rol no válido"));
        verify(roleRepository).findByName(invalidRoleName);
    }

    @Test
    void assignRolFirebaseUser_ConUsuarioNoEncontrado_DebeLanzarExcepcion() throws FirebaseAuthException {
        String roleName = "ADMIN";
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(roleName);

        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(roleEntity));
        when(userRepository.findByFirebaseUid(TEST_UID)).thenReturn(Optional.empty());

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            doNothing().when(firebaseAuth).setCustomUserClaims(eq(TEST_UID), any(Map.class));

            AuthException exception = assertThrows(AuthException.class, () -> {
                firebaseRoleService.assignRolFirebaseUser(TEST_UID, roleName);
            });

            assertTrue(exception.getMessage().contains("Usuario no encontrado"));
            verify(roleRepository).findByName(roleName);
            verify(userRepository).findByFirebaseUid(TEST_UID);
        }
    }

    @Test
    void verificarRol_ConTokenValido_DebeRetornarInfoUsuario() throws FirebaseAuthException {
        // Arrange
        String idToken = "valid-id-token";
        String roleName = "USER";

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn(TEST_UID);
        when(firebaseToken.getEmail()).thenReturn(TEST_EMAIL);
        when(firebaseToken.getName()).thenReturn("Test User");
        when(firebaseToken.getPicture()).thenReturn("https://example.com/photo.jpg");

        UserRecord userRecord = mock(UserRecord.class);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", roleName);
        when(userRecord.getCustomClaims()).thenReturn(claims);

        UserEntity userEntity = new UserEntity();
        userEntity.setFirebaseUid(TEST_UID);
        userEntity.setEmail(TEST_EMAIL);

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken(idToken)).thenReturn(firebaseToken);
            when(firebaseAuth.getUser(TEST_UID)).thenReturn(userRecord);
            when(userRepository.findByFirebaseUid(TEST_UID)).thenReturn(Optional.of(userEntity));

            FirebaseUserInfoDto result = firebaseRoleService.verificarRol(idToken);

            assertNotNull(result);
            assertEquals(TEST_UID, result.getUid());
            assertEquals(TEST_EMAIL, result.getEmail());
            assertEquals(roleName, result.getRole());

            verify(firebaseAuth).verifyIdToken(idToken);
            verify(firebaseAuth).getUser(TEST_UID);
            verify(userRepository).findByFirebaseUid(TEST_UID);
            verify(userRepository).save(any(UserEntity.class));
        }
    }

    @Test
    void verificarRol_ConUsuarioNoExistente_DebeCrearNuevoUsuario() throws FirebaseAuthException {
        // Arrange
        String idToken = "valid-id-token";
        String name = "New User";
        String photoUrl = "https://example.com/photo.jpg";

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn(TEST_UID);
        when(firebaseToken.getEmail()).thenReturn(TEST_EMAIL);
        when(firebaseToken.getName()).thenReturn(name);
        when(firebaseToken.getPicture()).thenReturn(photoUrl);

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getCustomClaims()).thenReturn(null);

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken(idToken)).thenReturn(firebaseToken);
            when(firebaseAuth.getUser(TEST_UID)).thenReturn(userRecord);
            when(userRepository.findByFirebaseUid(TEST_UID)).thenReturn(Optional.empty());

            doNothing().when(firebaseRoleService).createNewUserWithDefaultRole(TEST_UID, TEST_EMAIL, name, photoUrl);

            FirebaseUserInfoDto result = firebaseRoleService.verificarRol(idToken);

            assertNotNull(result);
            assertEquals(TEST_UID, result.getUid());
            assertEquals(TEST_EMAIL, result.getEmail());
            assertEquals(DEFAULT_ROLE, result.getRole());

            verify(firebaseAuth).verifyIdToken(idToken);
            verify(firebaseAuth).getUser(TEST_UID);
            verify(userRepository).findByFirebaseUid(TEST_UID);
            verify(firebaseRoleService).createNewUserWithDefaultRole(TEST_UID, TEST_EMAIL, name, photoUrl);
        }
    }

    @Test
    void verificarRol_ConErrorFirebase_DebePropagaExcepcion() throws FirebaseAuthException {
        String idToken = "invalid-token";

        FirebaseAuthException mockedException = mock(FirebaseAuthException.class);
        when(mockedException.getMessage()).thenReturn("Token inválido");


        FirebaseAuthException exception = assertThrows(FirebaseAuthException.class, () -> {
            firebaseRoleService.verificarRol(idToken);
        });

        assertEquals("Token inválido", exception.getMessage());
    }

    @Test
    void createNewUserWithDefaultRole_DebeCrearUsuarioConRolPorDefecto() throws FirebaseAuthException {
        String name = "New User";
        String photoUrl = "https://example.com/photo.jpg";
        RoleEntity defaultRole = new RoleEntity();
        defaultRole.setName(DEFAULT_ROLE);

        when(roleRepository.findByName(DEFAULT_ROLE)).thenReturn(Optional.of(defaultRole));

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            doNothing().when(firebaseAuth).setCustomUserClaims(eq(TEST_UID), any(Map.class));

            firebaseRoleService.createNewUserWithDefaultRole(TEST_UID, TEST_EMAIL, name, photoUrl);

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());

            UserEntity savedUser = userCaptor.getValue();
            assertEquals(TEST_UID, savedUser.getFirebaseUid());
            assertEquals(TEST_EMAIL, savedUser.getEmail());
            assertEquals(name, savedUser.getName());
            assertEquals(photoUrl, savedUser.getPhotoUrl());
            assertTrue(savedUser.isEnabled());
            assertNotNull(savedUser.getRegistrationDate());
            assertNotNull(savedUser.getLastAccess());
            assertEquals(defaultRole, savedUser.getRole());

            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(firebaseAuth).setCustomUserClaims(eq(TEST_UID), claimsCaptor.capture());

            Map<String, Object> capturedClaims = claimsCaptor.getValue();
            assertEquals(DEFAULT_ROLE, capturedClaims.get("role"));
            assertTrue(capturedClaims.containsKey("updated_at"));
        }
    }


    @Test
    void getExtendedUserInfo_ConTokenValido_DebeRetornarInfoExtendida() throws FirebaseAuthException {
        String idToken = "valid-id-token";
        String name = "Test User";
        String photoUrl = "https://example.com/photo.jpg";
        String role = "USER";

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn(TEST_UID);
        when(firebaseToken.getEmail()).thenReturn(TEST_EMAIL);

        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getDisplayName()).thenReturn(name);
        when(userRecord.getPhotoUrl()).thenReturn(photoUrl);

        FirebaseUserInfoDto basicInfo = FirebaseUserInfoDto.builder()
                .uid(TEST_UID)
                .email(TEST_EMAIL)
                .role(role)
                .build();

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken(idToken)).thenReturn(firebaseToken);
            when(firebaseAuth.getUser(TEST_UID)).thenReturn(userRecord);

            doReturn(basicInfo).when(firebaseRoleService).verificarRol(idToken);

            FirebaseUserExtendedInfoDto result = firebaseRoleService.getExtendedUserInfo(idToken);

            assertNotNull(result);
            assertEquals(TEST_UID, result.getUid());
            assertEquals(TEST_EMAIL, result.getEmail());
            assertEquals(name, result.getName());
            assertEquals(photoUrl, result.getPicture());
            assertEquals(role, result.getRole());

            verify(firebaseAuth).verifyIdToken(idToken);
            verify(firebaseAuth).getUser(TEST_UID);
            verify(firebaseRoleService).verificarRol(idToken);
        }
    }
}