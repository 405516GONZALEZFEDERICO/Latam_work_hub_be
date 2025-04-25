package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.mapper.ModelMapperConfig;
import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.DocumentType;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.cloudinary.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ModelMapperConfig modelMapperConfig;
    
    @Mock
    private CloudinaryService cloudinaryService;
    
    @InjectMocks
    @Spy
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
        // Set up modelMapper mock
        org.modelmapper.ModelMapper modelMapperMock = mock(org.modelmapper.ModelMapper.class);
        when(modelMapperConfig.modelMapper()).thenReturn(modelMapperMock);
        
        when(userRepository.findByFirebaseUid(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        PersonalDataUserDto result = userService.createOrUpdatePersonalDataUser(testPersonalData, "test-uid");

        verify(userRepository).findByFirebaseUid("test-uid");
        verify(userRepository).save(testUser);
        verify(modelMapperMock).map(testPersonalData, testUser);
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
    @Test
    void uploadImagenProfile_Success() throws IOException {
        String uid = "test-uid";
        MockMultipartFile mockImage = new MockMultipartFile(
                "profile-image",
                "profile.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(cloudinaryService.uploadProfileImage(any(MultipartFile.class))).thenReturn("https://cloudinary.com/new-image-url");

        when(userRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        boolean result = userService.uploadImagenProfile(uid, mockImage);

        assertTrue(result);
        verify(userRepository).findByFirebaseUid(uid);
        verify(cloudinaryService).uploadProfileImage(mockImage);
        verify(userRepository).save(testUser);
        assertEquals("https://cloudinary.com/new-image-url", testUser.getPhotoUrl());
    }

    @Test
    void uploadImagenProfile_WithExistingImage_ShouldDeleteOldImage() throws IOException {
        String uid = "test-uid";
        MockMultipartFile mockImage = new MockMultipartFile(
                "profile-image",
                "profile.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        testUser.setPhotoUrl("https://cloudinary.com/old-image-url");

        // Mock cloudinary service
        when(cloudinaryService.extractPublicIdFromUrl("https://cloudinary.com/old-image-url")).thenReturn("old-image-public-id");
        when(cloudinaryService.uploadProfileImage(any(MultipartFile.class))).thenReturn("https://cloudinary.com/new-image-url");

        when(userRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        boolean result = userService.uploadImagenProfile(uid, mockImage);

        assertTrue(result);
        verify(userRepository).findByFirebaseUid(uid);
        verify(cloudinaryService).extractPublicIdFromUrl("https://cloudinary.com/old-image-url");
        verify(cloudinaryService).deleteImage("old-image-public-id");
        verify(cloudinaryService).uploadProfileImage(mockImage);
        verify(userRepository).save(testUser);
        assertEquals("https://cloudinary.com/new-image-url", testUser.getPhotoUrl());
    }

    @Test
    void uploadImagenProfile_EmptyImage_ShouldThrowException() {
        // Arrange
        String uid = "test-uid";
        MockMultipartFile mockImage = new MockMultipartFile(
                "profile-image",
                "profile.jpg",
                "image/jpeg",
                new byte[0]  // Empty image
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.uploadImagenProfile(uid, mockImage);
        });

        assertEquals("La imagen no puede estar vacía", exception.getMessage());
    }

    @Test
    void uploadImagenProfile_InvalidContentType_ShouldThrowException() {
        // Arrange
        String uid = "test-uid";
        MockMultipartFile mockFile = new MockMultipartFile(
                "document",
                "document.pdf",
                "application/pdf", 
                "test content".getBytes()
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.uploadImagenProfile(uid, mockFile);
        });

        assertEquals("El archivo debe ser una imagen", exception.getMessage());
    }

    @Test
    void uploadImagenProfile_ExceedsMaxSize_ShouldThrowException() throws IOException {
        // Arrange
        String uid = "test-uid";

        MockMultipartFile mockLargeImage = mock(MockMultipartFile.class);
        when(mockLargeImage.isEmpty()).thenReturn(false);
        when(mockLargeImage.getContentType()).thenReturn("image/jpeg");
        when(mockLargeImage.getSize()).thenReturn(6 * 1024 * 1024L);  // 6MB

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.uploadImagenProfile(uid, mockLargeImage);
        });

        assertEquals("La imagen no puede superar los 5MB", exception.getMessage());
    }

    @Test
    void uploadImagenProfile_UserNotFound_ShouldThrowException() {
        String uid = "non-existent-uid";
        MockMultipartFile mockImage = new MockMultipartFile(
                "profile-image",
                "profile.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(userRepository.findByFirebaseUid(uid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            userService.uploadImagenProfile(uid, mockImage);
        });
    }
}