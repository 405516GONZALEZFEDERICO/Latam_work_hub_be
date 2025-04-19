package Latam.Latam.work.hub.services.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {CloudinaryServiceImpl.class})
public class CloudinaryServiceImplTest {

    @MockitoBean
    private Cloudinary cloudinary;

    @MockitoBean
    private Uploader uploader;

    @MockitoSpyBean
    private CloudinaryServiceImpl cloudinaryService;

    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);

        mockFile = new MockMultipartFile(
                "test-image",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
    }

    @Test
    void uploadProfileImage_Success() throws IOException {
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/example/image/upload/v1/profile_pictures/profile_123");
        uploadResult.put("public_id", "profile_pictures/profile_123");

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        String result = cloudinaryService.uploadProfileImage(mockFile);

        assertEquals("https://res.cloudinary.com/example/image/upload/v1/profile_pictures/profile_123", result);
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void uploadProfileImage_ExceptionThrown() throws IOException {
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Upload error"));

        assertThrows(IOException.class, () -> cloudinaryService.uploadProfileImage(mockFile));
    }

    @Test
    void deleteImage_Success() throws IOException {
        Map<String, Object> deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");

        when(uploader.destroy(anyString(), anyMap())).thenReturn(deleteResult);

        boolean result = cloudinaryService.deleteImage("profile_pictures/profile_123");

        assertTrue(result);
        verify(uploader).destroy(eq("profile_pictures/profile_123"), anyMap());
    }

    @Test
    void deleteImage_ExceptionThrown() throws IOException {
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("Delete error"));

        boolean result = cloudinaryService.deleteImage("profile_pictures/profile_123");

        assertFalse(result);
    }

    @Test
    void extractPublicIdFromUrl_ValidUrl() {
        String url = "https://res.cloudinary.com/mycloud/image/upload/v1234/profile_pictures/image_123.jpg";

        String publicId = cloudinaryService.extractPublicIdFromUrl(url);

        assertEquals("profile_pictures/image_123", publicId);
    }

    @Test
    void extractPublicIdFromUrl_InvalidUrl() {
        String url = "https://example.com/invalid/image.jpg";

        String publicId = cloudinaryService.extractPublicIdFromUrl(url);

        assertNull(publicId);
    }

    @Test
    void generatePublicId_ReturnsExpectedFormat() throws IOException {

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://example.com/image.jpg");
        uploadResult.put("public_id", "any-id");

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        cloudinaryService.uploadProfileImage(mockFile);

        ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), optionsCaptor.capture());

        Map<String, Object> options = optionsCaptor.getValue();
        String publicId = (String) options.get("public_id");

        assertTrue(publicId.startsWith("profile_"), "Public ID should start with 'profile_'");
        assertTrue(publicId.length() > 8, "Public ID should contain a timestamp");
        assertTrue(publicId.substring(8).matches("\\d+"), "Characters after prefix should be numeric");
    }
}