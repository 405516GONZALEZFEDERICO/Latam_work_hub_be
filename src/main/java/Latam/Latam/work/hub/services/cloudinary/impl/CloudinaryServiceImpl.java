package Latam.Latam.work.hub.services.cloudinary.impl;

import Latam.Latam.work.hub.services.cloudinary.CloudinaryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {
    private final Cloudinary cloudinary;

    @Override
    public String uploadProfileImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "profile_pictures",
                        "public_id", generatePublicId()));

        String imageUrl = uploadResult.get("secure_url").toString();
        String publicId = uploadResult.get("public_id").toString();

        log.info("Image uploaded successfully to Cloudinary. Public ID: {}", publicId);
        return imageUrl;
    }

    @Override
    public boolean deleteImage(String publicId) throws IOException {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted successfully from Cloudinary. Public ID: {}", publicId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting image from Cloudinary. Public ID: {}", publicId, e);
            return false;
        }
    }

    private String generatePublicId() {
        return "profile_" + System.currentTimeMillis();
    }
    public String extractPublicIdFromUrl(String imageUrl) {
        try {
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            String pathAfterUpload = imageUrl.substring(uploadIndex + 8);

            if (pathAfterUpload.startsWith("v")) {
                int slashAfterVersion = pathAfterUpload.indexOf("/");
                if (slashAfterVersion != -1) {
                    pathAfterUpload = pathAfterUpload.substring(slashAfterVersion + 1);
                }
            }

            int extensionIndex = pathAfterUpload.lastIndexOf(".");
            if (extensionIndex != -1) {
                pathAfterUpload = pathAfterUpload.substring(0, extensionIndex);
            }

            return pathAfterUpload;
        } catch (Exception e) {
            log.error("Error extracting public ID from URL: {}", imageUrl, e);
            return null;
        }
    }
}
