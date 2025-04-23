package Latam.Latam.work.hub.services.cloudinary;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface CloudinaryService {
    String uploadProfileImage(MultipartFile file) throws IOException;
    boolean deleteImage(String publicId) throws IOException;
    String extractPublicIdFromUrl(String cloudinaryUrl);
    List<String> uploadImages(List<MultipartFile> files) throws IOException;
}
