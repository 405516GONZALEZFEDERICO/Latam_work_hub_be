package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.enums.DocumentType;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private FirebaseToken firebaseToken;

    @MockitoSpyBean
    private UserController userController;

    private PersonalDataUserDto testPersonalData;
    private final String TEST_TOKEN = "test-token";
    private final String TEST_UID = "test-uid";

    @BeforeEach
    void setUp() {
        testPersonalData = new PersonalDataUserDto();
        testPersonalData.setName("Test User");
        testPersonalData.setBirthDate(LocalDate.of(1990, 1, 1));
        testPersonalData.setDocumentNumber("12345678");
        testPersonalData.setDocumentType(DocumentType.DNI);
        testPersonalData.setJobTitle("Developer");
        testPersonalData.setDepartment("IT");
    }

    @Test
    @WithMockUser(roles = {"CLIENTE","PROVEEDOR"})
    void createOrUpdatePersonalData_Success() throws FirebaseAuthException {
        try (MockedStatic<FirebaseAuth> mockedFirebaseAuth = Mockito.mockStatic(FirebaseAuth.class)) {
            mockedFirebaseAuth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
            when(firebaseToken.getUid()).thenReturn(TEST_UID);
            when(userService.createOrUpdatePersonalDataUser(any(PersonalDataUserDto.class), anyString()))
                    .thenReturn(testPersonalData);

            ResponseEntity<PersonalDataUserDto> response = userController.createOrUpdatePersonalData(
                    testPersonalData, "Bearer " + TEST_TOKEN);

            verify(firebaseAuth).verifyIdToken(TEST_TOKEN);
            verify(userService).createOrUpdatePersonalDataUser(testPersonalData, TEST_UID);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(testPersonalData, response.getBody());
        }
    }

    @Test
    @WithMockUser(roles = {"CLIENTE","PROVEEDOR"})
    void createOrUpdatePersonalData_InvalidToken() throws FirebaseAuthException {
        try (MockedStatic<FirebaseAuth> mockedFirebaseAuth = Mockito.mockStatic(FirebaseAuth.class)) {
            mockedFirebaseAuth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            FirebaseAuthException mockException = Mockito.mock(FirebaseAuthException.class);
            when(mockException.getMessage()).thenReturn("Token inválido o expirado");

            when(firebaseAuth.verifyIdToken(anyString())).thenThrow(mockException);

            AuthException exception = assertThrows(AuthException.class, () -> {
                userController.createOrUpdatePersonalData(testPersonalData, "Bearer " + TEST_TOKEN);
            });

            verify(firebaseAuth).verifyIdToken(TEST_TOKEN);
            verifyNoInteractions(userService);
            assertTrue(exception.getMessage().contains("Token inválido o expirado"));
        }
    }
}