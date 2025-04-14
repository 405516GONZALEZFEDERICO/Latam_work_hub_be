package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.CompleteUserDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.UserService;
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




    public UserEntity validateUserExists(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado con email: " + email));

        if (!user.isEnabled()) {
            throw new AuthException("Usuario deshabilitado");
        }

        return user;
    }

    @Override
    public CompleteUserDto getCompleteUserData() {
        return null;
    }

}
