package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.ModelMapperConfig;
import Latam.Latam.work.hub.dtos.common.CompleteUserDataDto;
import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.UserService;
import Latam.Latam.work.hub.services.cloudinary.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapperConfig modelMapperConfig;
    private final CloudinaryService cloudinaryService;
    @Transactional
    public UserEntity createOrUpdateLocalUser(String email, String uid, String photoUrl, String name) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        UserEntity user;
        if (userOpt.isPresent()) {
            user = userOpt.get();

            if (!Objects.equals(user.getFirebaseUid(), uid)) {
                user.setFirebaseUid(uid);
                user.setPhotoUrl(photoUrl);
                user.setEmail(email);
                user.setName(name);
                user.setEnabled(true);
                user.setRegistrationDate(LocalDateTime.now());
            }
        } else {
            user = new UserEntity();
            user.setName(name);
            user.setEmail(email);
            user.setEnabled(true);
            user.setFirebaseUid(uid);
            user.setPhotoUrl(photoUrl);
            user.setRegistrationDate(LocalDateTime.now());
        }
        return userRepository.save(user);
    }



    @Override
    public UserEntity validateUserExists(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado con email: " + email));

        if (!user.isEnabled()) {
            throw new AuthException("Usuario deshabilitado");
        }

        return user;
    }

    @Override
    public CompleteUserDataDto getPersonalDataUser(String uid) {
        UserEntity user=this.userRepository.findByFirebaseUid(uid).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return modelMapperConfig.modelMapper().map(user,CompleteUserDataDto.class);
    }



    @Override
    public PersonalDataUserDto createOrUpdatePersonalDataUser(PersonalDataUserDto personalDataUserDto,String uid) {
        UserEntity user = userRepository.findByFirebaseUid(uid).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        user.setBirthDay(personalDataUserDto.getBirthDate());
        if (!user.isEnabled()) {
            throw  new IllegalStateException("Usuario deshabilitado");
        }
        modelMapperConfig.modelMapper().map(personalDataUserDto, user);

        userRepository.save(user);
        return personalDataUserDto;
    }

    @Transactional
    public UserEntity updateUserLoginData(UserEntity user) {
        user.setLastAccess(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public boolean uploadImagenProfile(String uid, MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("La imagen no puede estar vacía");
        }

        // Validate image type
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }

        // Validate image size (5MB max)
        if (image.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen no puede superar los 5MB");
        }

        // Find user
        UserEntity user = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        // Delete old image if exists
        String oldImageUrl = user.getPhotoUrl();
        if (oldImageUrl != null && oldImageUrl.contains("cloudinary.com")) {
            String publicId = cloudinaryService.extractPublicIdFromUrl(oldImageUrl);
            if (publicId != null) {
                cloudinaryService.deleteImage(publicId);
            }
        }

        // Upload new image
        String imgUrl = cloudinaryService.uploadProfileImage(image);
        user.setPhotoUrl(imgUrl);
        userRepository.save(user);

        log.info("Imagen de perfil actualizada correctamente para el usuario {}", user.getEmail());
        return true;
    }


}
