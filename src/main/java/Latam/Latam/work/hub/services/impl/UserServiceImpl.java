package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.RoleEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.RoleRepository;
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
private final RoleRepository roleRepository;
    /**
     * Crea o actualiza un usuario local a partir de datos de Firebase
     */
    @Transactional
    public UserEntity createOrUpdateLocalUser(String email, String uid, String role) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);

        UserEntity user;
        if (userOpt.isPresent()) {
            user = userOpt.get();

            if (!Objects.equals(user.getFirebaseUid(), uid)) {
                user.setFirebaseUid(uid);
            }



        } else {
            user = new UserEntity();
            user.setEmail(email);
            user.setEnabled(true);
            user.setFirebaseUid(uid);
            user.setLastPasswordUpdateDate(LocalDateTime.now());


        }

        // Asignar rol si se proporciona
        if (role != null && !role.isBlank()) {
            RoleEntity rolEntity = roleRepository.findByName(role.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + role));
            user.setRole(rolEntity);
        }

        return userRepository.save(user);
    }



    /**
     * Verifica si un usuario existe y está habilitado
     */
    public UserEntity validateUserExists(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Usuario no encontrado con email: " + email));

        if (!user.isEnabled()) {
            throw new AuthException("Usuario deshabilitado");
        }

        return user;
    }
}
