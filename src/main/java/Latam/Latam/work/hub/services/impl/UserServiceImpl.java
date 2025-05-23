package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.mapper.ModelMapperConfig;
import Latam.Latam.work.hub.dtos.common.CompleteUserDataDto;
import Latam.Latam.work.hub.dtos.common.DisableUserDto;
import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.dtos.common.ProviderTypeDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.UserService;
import Latam.Latam.work.hub.services.cloudinary.CloudinaryService;
import com.google.common.reflect.TypeToken;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapperConfig modelMapperConfig;
    private final CloudinaryService cloudinaryService;
    private final ModelMapper modelMapper;

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
        try {
            UserEntity user = this.userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
            return modelMapperConfig.modelMapper().map(user, CompleteUserDataDto.class);
        } catch (EntityNotFoundException e) {
            CompleteUserDataDto emptyDto = new CompleteUserDataDto();
            emptyDto.setName("");
            emptyDto.setEmail("");
            emptyDto.setPhotoUrl("");
            emptyDto.setBirthDate(null);
            emptyDto.setDepartment("");
            emptyDto.setDocumentNumber("");
            emptyDto.setJobTitle("");
            emptyDto.setDocumentType(null);
            return emptyDto;
        }
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



    @Override
    public UserEntity getUserByUid(String uid) {
        return userRepository.findByFirebaseUid(uid).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }



    @Override
    public ProviderTypeDto getProviderType(String uid) {
        UserEntity userEntity=this.userRepository.findByFirebaseUid(uid).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        ProviderTypeDto providerTypeDto = new ProviderTypeDto();
        providerTypeDto.setProviderType(String.valueOf(userEntity.getProviderType()));
        return  providerTypeDto;
    }

    @Override
    public List<DisableUserDto> getAllUsersActive( String roleName) {
        List<UserEntity> allUsers=this.userRepository.findUsersEnabledByRole(roleName);
        if (allUsers.isEmpty()) {
            throw new EntityNotFoundException("Usuarios  por rol activos no encontrados");
        }
        Type listType = new TypeToken<List<DisableUserDto>>(){}.getType();
        List<DisableUserDto>disableUserDtoList=modelMapperConfig.modelMapper().map(allUsers, listType);
        return disableUserDtoList;

    }
    @Override
    public boolean desactivateAccount(String uid) {
        Optional<UserEntity> user=this.userRepository.findByFirebaseUid(uid);
        if (user.isPresent()) {
            UserEntity userEntity = user.get();
            userEntity.setEnabled(false);
            userRepository.save(userEntity);
            return true;
        }
        return false;
    }
    @Override
    public boolean activateAccount(String uid) {
        Optional<UserEntity> user=this.userRepository.findByFirebaseUid(uid);
        if (user.isPresent()) {
            UserEntity userEntity = user.get();
            userEntity.setEnabled(true);
            userRepository.save(userEntity);
            return true;
        }
        return false;
    }

}
