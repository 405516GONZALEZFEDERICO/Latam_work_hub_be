package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.security.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.GoogleAuthService;
import Latam.Latam.work.hub.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
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
    @Transactional
    public AuthResponseGoogleDto loginWithGoogle(String idToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String photoUrl = decodedToken.getPicture();

            UserEntity user = userService.createOrUpdateLocalUser(email, uid, photoUrl, name);
            FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRol(idToken);

            return AuthResponseGoogleDto.builder()
                    .idToken(idToken)
                    .email(email)
                    .localId(uid)
                    .role(userInfo.getRole())
                    .name(name)
                    .photoUrl(photoUrl)
                    .build();

        } catch (FirebaseAuthException e) {
            throw new AuthException("Token de Google inválido o expirado", e);
        }
    }


    @Override
    public String registerWithGoogle(String idToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();

            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                return "El usuario ya está registrado";
            }

           userService.createOrUpdateLocalUser(email, uid, picture, name);

            firebaseRoleService.asignarRolAFirebaseUser(uid, "DEFAULT");
            return "Usuario registrado con Google correctamente. UID: " + uid;

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Error al verificar token de Google: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error en registro con Google: " + e.getMessage());
        }
    }



}