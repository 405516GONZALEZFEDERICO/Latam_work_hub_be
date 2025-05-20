package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.security.dtos.FirebaseUserExtendedInfoDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.entities.RoleEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.repositories.RoleRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseRoleServiceImpl implements FirebaseRoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    private static final String DEFAULT_ROLE = "DEFAULT";


  
    @Override
    @Transactional
    public void assignRolFirebaseUser(String uid, String rolNombre) throws FirebaseAuthException {
        try {
            RoleEntity rolEntity = roleRepository.findByName(rolNombre)
                    .orElseThrow(() -> new AuthException("Rol no válido: " + rolNombre));

            // Actualizar usuario en base de datos
            UserEntity user = userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new AuthException("Usuario no encontrado con UID: " + uid));

            user.setRole(rolEntity); 
            user.setLastAccess(LocalDateTime.now());
            userRepository.save(user);

            // Actualizar claims en Firebase
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", rolEntity.getName());
            claims.put("updated_at", System.currentTimeMillis());

            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
            
            log.info("Rol {} asignado al usuario {} en DB y Firebase", rolEntity.getName(), uid);
        } catch (FirebaseAuthException e) {
            log.error("Error al asignar rol a usuario {}: {}", uid, e.getMessage());
            throw e;
        }
    }


    
    @Override
    public FirebaseUserInfoDto verificarRol(String idToken) throws FirebaseAuthException {
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = token.getUid();

            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            Map<String, Object> claims = userRecord.getCustomClaims();

            String email = token.getEmail();
            String role = claims != null && claims.containsKey("role")
                    ? claims.get("role").toString()
                    : DEFAULT_ROLE;

            UserEntity userEntity = userRepository.findByFirebaseUid(uid).orElse(null);

            if (userEntity == null) {
                createNewUserWithDefaultRole(uid, email, token.getName(), token.getPicture());
                role = DEFAULT_ROLE;
            } else {
                userEntity.setLastAccess(LocalDateTime.now());
                userRepository.save(userEntity);
                
                // Obtener el rol desde la base de datos
                String dbRole = userEntity.getRole().getName();
                
                // Si el rol en la base de datos es diferente del rol en los claims, actualizar claims
                if (!dbRole.equals(role)) {
                    log.info("Actualizando rol en Firebase para uid {}: de {} a {}", uid, role, dbRole);
                    Map<String, Object> updatedClaims = new HashMap<>();
                    updatedClaims.put("role", dbRole);
                    updatedClaims.put("updated_at", System.currentTimeMillis());
                    
                    FirebaseAuth.getInstance().setCustomUserClaims(uid, updatedClaims);
                    role = dbRole;
                }
            }

            return FirebaseUserInfoDto.builder()
                    .email(email)
                    .uid(uid)
                    .role(role)
                    .build();
        } catch (FirebaseAuthException e) {
            log.error("Error al verificar token: {}", e.getMessage());
            throw e;
        }
    }


    @Transactional
    @Override
    public void createNewUserWithDefaultRole(String uid, String email, String name, String photoUrl) {
        try {
            RoleEntity defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                    .orElseThrow(() -> new AuthException("Rol DEFAULT no encontrado"));

            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            newUser.setFirebaseUid(uid);
            newUser.setName(name);
            newUser.setPhotoUrl(photoUrl);
            newUser.setEnabled(true);
            newUser.setRegistrationDate(LocalDateTime.now());
            newUser.setLastAccess(LocalDateTime.now());
            newUser.setRole(defaultRole);

            userRepository.save(newUser);

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", DEFAULT_ROLE);
            claims.put("updated_at", System.currentTimeMillis());

            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);

            log.info("Nuevo usuario creado con UID: {} y rol DEFAULT", uid);
        } catch (Exception e) {
            log.error("Error al crear usuario: {}", e.getMessage());
            throw new AuthException("Error al crear usuario", e);
        }
    }
    @Override
    public FirebaseUserExtendedInfoDto getExtendedUserInfo(String idToken) {
        try {
            // Validar token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decodedToken.getEmail();
            String uid = decodedToken.getUid();

            // Obtener información adicional del usuario
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            String name = userRecord.getDisplayName() != null ? userRecord.getDisplayName() : "";
            String picture = userRecord.getPhotoUrl() != null ? userRecord.getPhotoUrl() : "";

            // Obtener el rol (si se implementa diferente en tu servicio actual, ajustar esta parte)
            FirebaseUserInfoDto basicInfo = verificarRol(idToken);
            String role = basicInfo.getRole();

            return FirebaseUserExtendedInfoDto.builder()
                    .email(email)
                    .uid(uid)
                    .name(name)
                    .picture(picture)
                    .role(role)
                    .build();
        } catch (FirebaseAuthException e) {
            log.error("Error al validar token de Firebase: {}", e.getMessage());
            throw new AuthException("Token inválido: " + e.getMessage());
        }
    }

    @Override
    public boolean existsByEmailAndRole(String email, String roleName) {
        try {
            UserEntity user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                return false;
            }

            return user.getRole().getName().equalsIgnoreCase(roleName);
        } catch (Exception e) {
            log.error("Error al verificar email y rol: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getEmailFromToken(String token) throws FirebaseAuthException {
        try {
            // Decodificar el token para obtener la información del usuario
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

            // Obtener el email del token decodificado
            String email = decodedToken.getEmail();

            // Verificar que el email existe
            if (email == null || email.isEmpty()) {
                log.error("Token no contiene email");
                throw new AuthException("Token inválido: no contiene email");
            }

            return email;
        } catch (FirebaseAuthException e) {
            log.error("Error al verificar token de Firebase: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al obtener email del token: {}", e.getMessage());
            throw new AuthException("Error al procesar el token");
        }
    }


}
