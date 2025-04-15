package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.ModelMapperConfig;
import Latam.Latam.work.hub.dtos.common.CompleteUserDataDto;
import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapperConfig modelMapperConfig;
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
                user.setRole(user.getRole());
                user.setRegistrationDate(LocalDateTime.now());
            }
        } else {
            user = new UserEntity();
            user.setName(name);
            user.setEmail(email);
            user.setEnabled(true);
            user.setFirebaseUid(uid);
            user.setPhotoUrl(photoUrl);
            user.setRole(user.getRole());
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
    public CompleteUserDataDto getCompleteUserData() {
        return null;
    }

    @Override
    public PersonalDataUserDto createOrUpdatePersonalDataUser(PersonalDataUserDto personalDataUserDto,String uid) {
        UserEntity user = userRepository.findByFirebaseUid(uid).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

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


}
