package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.security.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserExtendedInfoDto;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.GoogleAuthService;
import Latam.Latam.work.hub.services.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FirebaseRoleService firebaseRoleService;
    @Override
    @Transactional
    public AuthResponseGoogleDto loginWithGoogle(String idToken) {
        try {
            FirebaseUserExtendedInfoDto userExtendedInfo = firebaseRoleService.getExtendedUserInfo(idToken);

            UserEntity user = userRepository.findByEmail(userExtendedInfo.getEmail())
                    .orElseThrow(() -> new AuthException("Usuario no encontrado"));

            if (!user.isEnabled()) {
                throw new AuthException("Usuario deshabilitado");
            }

            userService.updateUserLoginData(user);

            return AuthResponseGoogleDto.builder()
                    .idToken(idToken)
                    .email(userExtendedInfo.getEmail())
                    .localId(userExtendedInfo.getUid())
                    .name(userExtendedInfo.getName())
                    .photoUrl(userExtendedInfo.getPicture())
                    .role(userExtendedInfo.getRole())
                    .build();
        } catch (AuthException e) {
            log.error("Error al iniciar sesión con Google: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al iniciar sesión con Google: {}", e.getMessage());
            throw new AuthException("Error al iniciar sesión con Google: " + e.getMessage());
        }
    }



    @Override
    @Transactional
    public String registerWithGoogle(String idToken) {
        try {
            FirebaseUserExtendedInfoDto userExtendedInfo = firebaseRoleService.getExtendedUserInfo(idToken);
            Optional<UserEntity> userOpt = userRepository.findByEmail(userExtendedInfo.getEmail());
            if (userOpt.isPresent()) {
                if (!userOpt.get().isEnabled()) {
                    throw new AuthException("Esta cuenta está deshabilitada");
                }
                throw new AuthException("El usuario ya está registrado. Por favor, utilice el login con Google");
            }

            UserEntity user = userService.createOrUpdateLocalUser(
                    userExtendedInfo.getEmail(),
                    userExtendedInfo.getUid(),
                    userExtendedInfo.getPicture(),
                    userExtendedInfo.getName()
            );

            if (!user.isEnabled()) {
                throw new AuthException("No se pudo habilitar el usuario correctamente");
            }

            firebaseRoleService.assignRolFirebaseUser(userExtendedInfo.getUid(), "DEFAULT");
            return "Usuario registrado correctamente";
        } catch (AuthException e) {
            log.error("Error al registrar con Google: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al registrar con Google: {}", e.getMessage());
            throw new AuthException("Error al registrar con Google: " + e.getMessage());
        }
    }

}




