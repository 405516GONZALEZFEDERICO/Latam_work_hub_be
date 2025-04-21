package Latam.Latam.work.hub.services.rest.template.firebase;

import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.services.rest.template.firebase.impl.FirebaseAuthRestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class FirebaseAuthRestServiceImplTest {
    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoSpyBean
    private FirebaseAuthRestServiceImpl firebaseAuthRestServiceImpl;

    private final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(firebaseAuthRestServiceImpl, "firebaseApiKey", API_KEY);
    }

    @Test
    void signInWithEmailAndPassword() {
        String email = "prueba@example.com";
        String password = "password123";
        String expectedUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;

        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("idToken", "token-prueba");
        successResponse.put("email", email);
        successResponse.put("localId", "usuario123");
        ResponseEntity<Map> successResponseEntity = new ResponseEntity<>(successResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<Class<Map>>any()
        )).thenReturn(successResponseEntity);

        Map<String, Object> result = firebaseAuthRestServiceImpl.signInWithEmailAndPassword(email, password);

        assertNotNull(result);
        assertEquals("token-prueba", result.get("idToken"));
        assertEquals(email, result.get("email"));

        verify(restTemplate, times(1)).exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<Class<Map>>any()
        );

        String errorJson = "{\"error\":{\"message\":\"INVALID_PASSWORD\"}}";
        HttpClientErrorException authException = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                errorJson.getBytes(),
                null
        );

        when(restTemplate.exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<Class<Map>>any()
        )).thenThrow(authException);

        AuthException thrownException = assertThrows(AuthException.class, () -> {
            firebaseAuthRestServiceImpl.signInWithEmailAndPassword(email, password);
        });

        assertTrue(thrownException.getMessage().contains("Credenciales inválidas"));
    }

    @Test
    void sendPasswordResetEmail() {
        String email = "reseteo@example.com";
        String expectedUrl = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + API_KEY;

        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("email", email);
        ResponseEntity<Map> successResponseEntity = new ResponseEntity<>(successResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<Class<Map>>any()
        )).thenReturn(successResponseEntity);

        Map<String, Object> result = firebaseAuthRestServiceImpl.sendPasswordResetEmail(email);

        assertNotNull(result);
        assertEquals(email, result.get("email"));

        verify(restTemplate, times(1)).exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<Class<Map>>any()
        );

        String errorJson = "{\"error\":{\"message\":\"EMAIL_NOT_FOUND\"}}";
        HttpClientErrorException emailException = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                errorJson.getBytes(),
                null
        );

        when(restTemplate.exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<Class<Map>>any()
        )).thenThrow(emailException);

        AuthException thrownException = assertThrows(AuthException.class, () -> {
            firebaseAuthRestServiceImpl.sendPasswordResetEmail(email);
        });

        assertTrue(thrownException.getMessage().contains("Error al enviar correo de recuperación"));
    }
}