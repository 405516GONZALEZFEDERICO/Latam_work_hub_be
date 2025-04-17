package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.DocumentType;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class UserServiceImplTest {

    @MockitoBean
    private UserRepository userRepository;
    @MockitoSpyBean
    private UserServiceImpl userService;

    private UserEntity testUser;
    private PersonalDataUserDto testPersonalData;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirebaseUid("test-uid");
        testUser.setName("Test User");
        testUser.setEnabled(true);

        testPersonalData = new PersonalDataUserDto();
        testPersonalData.setName("Updated Name");
        testPersonalData.setBirthDate(LocalDate.of(1990, 1, 1));
        testPersonalData.setDocumentNumber("12345678");
        testPersonalData.setDocumentType(DocumentType.DNI);
        testPersonalData.setJobTitle("Developer");
        testPersonalData.setDepartment("IT");
    }

    @Test
    void createOrUpdateLocalUser_WhenUserExists_ShouldUpdateIfUidDifferent() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        String newUid = "new-uid";

        UserEntity result = userService.createOrUpdateLocalUser(
                "test@example.com", newUid, "http://photo.url", "Test User");

        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(UserEntity.class));
        assertEquals(newUid, result.getFirebaseUid());
    }

    @Test
    void createOrUpdateLocalUser_WhenUserExists_ShouldNotUpdateIfUidSame() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // Act
        UserEntity result = userService.createOrUpdateLocalUser(
                "test@example.com", "test-uid", "http://photo.url", "Test User");

        // Assert
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(UserEntity.class));
        assertEquals("test-uid", result.getFirebaseUid());
        // No debería actualizarse nada más que el lastAccess
    }

    @Test
    void createOrUpdateLocalUser_WhenUserDoesNotExist_ShouldCreateNew() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        UserEntity result = userService.createOrUpdateLocalUser(
                "new@example.com", "new-uid", "http://photo.url", "New User");

        verify(userRepository).findByEmail("new@example.com");
        verify(userRepository).save(any(UserEntity.class));
        assertEquals("new-uid", result.getFirebaseUid());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New User", result.getName());
        assertTrue(result.isEnabled());
        assertNotNull(result.getRegistrationDate());
    }

    @Test
    void validateUserExists_WhenUserExists_ShouldReturnUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        UserEntity result = userService.validateUserExists("test@example.com");

        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
    }

    @Test
    void validateUserExists_WhenUserDoesNotExist_ShouldThrowException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> {
            userService.validateUserExists("nonexistent@example.com");
        });

        assertTrue(exception.getMessage().contains("Usuario no encontrado con email"));
    }

    @Test
    void validateUserExists_WhenUserDisabled_ShouldThrowException() {
        testUser.setEnabled(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        AuthException exception = assertThrows(AuthException.class, () -> {
            userService.validateUserExists("test@example.com");
        });

        assertEquals("Usuario deshabilitado", exception.getMessage());
    }

    @Test
    void createOrUpdatePersonalDataUser_WhenUserExists_ShouldUpdateAndReturnDto() {
        when(userRepository.findByFirebaseUid(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        PersonalDataUserDto result = userService.createOrUpdatePersonalDataUser(testPersonalData, "test-uid");

        verify(userRepository).findByFirebaseUid("test-uid");
        verify(userRepository).save(testUser);
        assertNotNull(result);
    }

    @Test
    void createOrUpdatePersonalDataUser_WhenUserDoesNotExist_ShouldThrowException() {
        when(userRepository.findByFirebaseUid(anyString())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            userService.createOrUpdatePersonalDataUser(testPersonalData, "nonexistent-uid");
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void createOrUpdatePersonalDataUser_WhenUserDisabled_ShouldThrowException() {
        testUser.setEnabled(false);
        when(userRepository.findByFirebaseUid(anyString())).thenReturn(Optional.of(testUser));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.createOrUpdatePersonalDataUser(testPersonalData, "test-uid");
        });

        assertEquals("Usuario deshabilitado", exception.getMessage());
    }

    @Test
    void updateUserLoginData_ShouldUpdateLastAccessAndSave() {
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        UserEntity result = userService.updateUserLoginData(testUser);

        verify(userRepository).save(testUser);
        assertNotNull(result.getLastAccess());
    }
}