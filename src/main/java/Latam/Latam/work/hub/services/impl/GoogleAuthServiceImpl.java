package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.dtos.TokenDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.GoogleAuthService;
import Latam.Latam.work.hub.services.TokenValidationService;
import Latam.Latam.work.hub.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserService userService;
    private final FirebaseRoleService firebaseRoleService;
    private final TokenValidationService tokenValidationService;

    /**
     * Inicia sesión con Google
     */
    @Transactional
    public AuthResponseGoogleDto loginWithGoogle(String idToken) {
        try {
            // Verificar token de Google
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            // Crear o actualizar usuario local
            UserEntity user = userService.createOrUpdateLocalUser(email, uid, name);

            // Verificar si tiene rol asignado, si no, asignarle CLIENTE
            String rol = firebaseRoleService.obtenerRolDeUsuario(uid);
            if ("sin_rol".equals(rol)) {
                firebaseRoleService.asignarRolYPermisosAFirebaseUser(uid, "CLIENTE");
                rol = "CLIENTE";
            }

            // Generar token personalizado y obtener info
            String customToken = FirebaseAuth.getInstance().createCustomToken(uid);
            TokenDto tokenInfo = tokenValidationService.exchangeCustomTokenForIdToken(customToken);

            // Obtener permisos actualizados
            FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRolYPermisos(tokenInfo.getToken());

            // Construir respuesta
            return AuthResponseGoogleDto.builder()
                    .idToken(tokenInfo.getToken())
                    .refreshToken(tokenInfo.getRefreshToken())
                    .expiresIn(tokenInfo.getExpiresIn())
                    .email(email)
                    .localId(uid)
                    .role(userInfo.getRole())
                    .permissions(userInfo.getPermissions())
                    .name(name)
                    .build();

// El resto del GoogleAuthService debería cerrarse así:
        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String registerWithGoogle(String idToken) {
        return null;
    }

    @Override
    public AuthResponseGoogleDto refreshGoogleToken(String refreshToken) {
        return null;
    }
}