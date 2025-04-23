package Latam.Latam.work.hub.services.cloudinary.impl;

import Latam.Latam.work.hub.services.cloudinary.CloudinaryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {
    private final Cloudinary cloudinary;

    // Método para subir una sola imagen (anteriormente 'uploadProfileImage')
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

    // Método para subir múltiples imágenes (nuevo)
    @Override
    public List<String> uploadImages(List<MultipartFile> files) throws IOException {
        // Usamos stream para subir todas las imágenes de la lista y devolver una lista de URLs
        return files.stream()
                .map(file -> {
                    try {
                        return uploadProfileImage(file); // Llamamos a 'uploadProfileImage' para cada archivo
                    } catch (IOException e) {
                        log.error("Error uploading image: {}", file.getOriginalFilename(), e);
                        return null;
                    }
                })
                .filter(url -> url != null) // Filtramos cualquier URL nula (por si hubo un error)
                .toList();
    }

    // Método para eliminar una imagen (sin cambios)
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

    // Método para generar el public_id (sin cambios)
    private String generatePublicId() {
        return "profile_" + System.currentTimeMillis();
    }

    // Método para extraer el public_id desde la URL (sin cambios)
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
