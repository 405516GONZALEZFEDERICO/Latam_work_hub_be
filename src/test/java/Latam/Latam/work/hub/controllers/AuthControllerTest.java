package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.RoleAssignmentRequestDto;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.security.dtos.AuthResponseDto;
import Latam.Latam.work.hub.security.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.services.AuthService;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.GoogleAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoSpyBean
    private AuthController a;
    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private FirebaseRoleService firebaseRoleService;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String GOOGLE_ID_TOKEN = "google-id-token";
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "Test123!";

    private AuthResponseGoogleDto authResponseGoogleDto;
    private AuthResponseDto authResponseDto;
    private FirebaseUserInfoDto firebaseUserInfoDto;
    private RoleAssignmentRequestDto roleAssignmentRequestDto;
    private FirebaseAuthException firebaseAuthException;
    @TestConfiguration
    public class MockFirebaseConfig {
        @Bean
        @Primary // This will override the real Firebase config
        public FirebaseRoleService firebaseRoleService() {
            return Mockito.mock(FirebaseRoleService.class);
        }
    }
    @BeforeEach
    void setUp() {
        authResponseGoogleDto = AuthResponseGoogleDto.builder()
                .idToken("test-token")
                .email(TEST_EMAIL)
                .localId("test-local-id")
                .role("DEFAULT")
                .name("Test User")
                .photoUrl("https://example.com/photo.jpg")
                .build();

        authResponseDto = AuthResponseDto.builder()
                .idToken("test-token")
                .expiresIn("3600")
                .role("DEFAULT")
                .firebaseUid("firebase-uid-123")
                .refreshToken("refresh-token-123")
                .build();

        firebaseUserInfoDto = FirebaseUserInfoDto.builder()
                .email(TEST_EMAIL)
                .uid("test-uid")
                .role("DEFAULT")
                .build();

        roleAssignmentRequestDto = new RoleAssignmentRequestDto();
        roleAssignmentRequestDto.setUid("test-uid");
        roleAssignmentRequestDto.setRoleName("ADMIN");

        // Create a proper FirebaseAuthException for testing
        firebaseAuthException = new FirebaseAuthException(
                ErrorCode.INVALID_ARGUMENT,
                "Firebase authentication error",
                null,
                null,
                AuthErrorCode.INVALID_ID_TOKEN
        );
    }

    @Test
    void loginWithGoogle_Success() throws Exception {
        when(googleAuthService.loginWithGoogle(GOOGLE_ID_TOKEN)).thenReturn(authResponseGoogleDto);

        mockMvc.perform(post("/api/auth/google/login")
                        .param("idToken", GOOGLE_ID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idToken").value("test-token"))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.localId").value("test-local-id"))
                .andExpect(jsonPath("$.role").value("DEFAULT"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/photo.jpg"));
    }

    @Test
    void loginWithGoogle_Unauthorized() throws Exception {
        when(googleAuthService.loginWithGoogle(GOOGLE_ID_TOKEN)).thenThrow(new AuthException("Invalid token"));

        mockMvc.perform(post("/api/auth/google/login")
                        .param("idToken", GOOGLE_ID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerWithGoogle_Success() throws Exception {
        when(googleAuthService.registerWithGoogle(GOOGLE_ID_TOKEN)).thenReturn("Usuario registrado exitosamente");

        mockMvc.perform(post("/api/auth/google/register")
                        .param("idToken", GOOGLE_ID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario registrado exitosamente"));
    }

    @Test
    void registerWithGoogle_BadRequest() throws Exception {
        when(googleAuthService.registerWithGoogle(GOOGLE_ID_TOKEN)).thenThrow(new AuthException("Email already in use"));

        mockMvc.perform(post("/api/auth/google/register")
                        .param("idToken", GOOGLE_ID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already in use"));
    }

    @Test
    void register_Success() throws Exception {
        when(authService.registerUser(TEST_EMAIL, TEST_PASSWORD)).thenReturn("Usuario registrado exitosamente");

        mockMvc.perform(post("/api/auth/register")
                        .param("email", TEST_EMAIL)
                        .param("password", TEST_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario registrado exitosamente"));
    }

    @Test
    void login_Success() throws Exception {
        when(authService.login(TEST_EMAIL, TEST_PASSWORD)).thenReturn(authResponseDto);

        mockMvc.perform(post("/api/auth/login")
                        .param("email", TEST_EMAIL)
                        .param("password", TEST_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idToken").value("test-token"))
                .andExpect(jsonPath("$.expiresIn").value("3600"))
                .andExpect(jsonPath("$.role").value("DEFAULT"))
                .andExpect(jsonPath("$.firebaseUid").value("firebase-uid-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"));
    }

    @Test
    void assignRoleToUser_Unauthorized() throws Exception {
        RoleAssignmentRequestDto request = new RoleAssignmentRequestDto("user123", "ADMIN");

        mockMvc.perform(post("/api/auth/roles/assign")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void assignRoleToUser_Success() throws Exception {
        Authentication auth = new TestingAuthenticationToken("user", "password", "ROLE_DEFAULT");
        SecurityContextHolder.getContext().setAuthentication(auth);

        Mockito.doNothing().when(firebaseRoleService).assignRolFirebaseUser(roleAssignmentRequestDto.getUid(), roleAssignmentRequestDto.getRoleName());

        ResponseEntity<?> response = a.assignRoleToUser(roleAssignmentRequestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(true, responseBody.get("success"));
        assertEquals("Rol ADMIN asignado correctamente al usuario", responseBody.get("message"));
    }


    @Test
    void assignRoleToUser_AuthException() throws Exception {
        Authentication auth = new TestingAuthenticationToken("user", "password", "ROLE_DEFAULT");
        SecurityContextHolder.getContext().setAuthentication(auth);

        String errorMessage = "Usuario no autorizado para esta operación";
        doThrow(new AuthException(errorMessage))
                .when(firebaseRoleService).assignRolFirebaseUser(anyString(), anyString());

        ResponseEntity<?> response = a.assignRoleToUser(roleAssignmentRequestDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(false, responseBody.get("success"));
        assertEquals(errorMessage, responseBody.get("message"));
    }


    @Test
    void assignRoleToUser_FirebaseAuthError_CompleteTest() throws Exception {
        Authentication auth = new TestingAuthenticationToken("user", "password", "ROLE_DEFAULT");
        SecurityContextHolder.getContext().setAuthentication(auth);

        doThrow(firebaseAuthException)
                .when(firebaseRoleService).assignRolFirebaseUser(anyString(), anyString());

        ResponseEntity<?> response = a.assignRoleToUser(roleAssignmentRequestDto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(false, responseBody.get("success"));
        assertTrue(((String) responseBody.get("message")).contains("Error al asignar rol"));
    }




    @Test
    void assignRoleToUser_FirebaseAuthError() throws Exception {
        Authentication auth = new TestingAuthenticationToken("user", "password", "ROLE_DEFAULT");
        SecurityContextHolder.getContext().setAuthentication(auth);

        doThrow(firebaseAuthException)
                .when(firebaseRoleService).assignRolFirebaseUser(anyString(), anyString());

        ResponseEntity<?> response = a.assignRoleToUser(roleAssignmentRequestDto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void recuperarContrasenia_Success() throws Exception {
        when(authService.retrievePassword(TEST_EMAIL)).thenReturn("Email de recuperación enviado");

        mockMvc.perform(get("/api/auth/recuperar-contrasenia")
                        .param("email", TEST_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Email de recuperación enviado"));
    }

    @Test
    void verificarRol_Success() throws Exception {
        when(firebaseRoleService.verificarRol(GOOGLE_ID_TOKEN)).thenReturn(firebaseUserInfoDto);

        mockMvc.perform(get("/api/auth/verificar-rol")
                        .param("idToken", GOOGLE_ID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.uid").value("test-uid"))
                .andExpect(jsonPath("$.role").value("DEFAULT"));
    }

    @Test
    void verificarRol_Unauthorized() throws Exception {
        when(firebaseRoleService.verificarRol(GOOGLE_ID_TOKEN)).thenThrow(firebaseAuthException);

        mockMvc.perform(get("/api/auth/verificar-rol")
                        .param("idToken", GOOGLE_ID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}