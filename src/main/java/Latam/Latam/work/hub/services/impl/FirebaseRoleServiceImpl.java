package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.entities.PermissionEntity;
import Latam.Latam.work.hub.entities.RoleEntity;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.RolePermissionService;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.ListUsersPage;
import com.google.firebase.auth.UserRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseRoleServiceImpl implements FirebaseRoleService {

    private final RolePermissionService rolePermissionService;

    private String firebaseApiKey = "AIzaSyB9b7mzFtoRDNB0YroNRe6tF9uQFGfvzXQ";

    /**
     * Obtiene el rol de un usuario en Firebase
     */
    public String obtenerRolDeUsuario(String uid) throws FirebaseAuthException {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            Map<String, Object> claims = userRecord.getCustomClaims();

            return claims != null && claims.containsKey("role")
                    ? claims.get("role").toString()
                    : "sin_rol";
        } catch (FirebaseAuthException e) {
            log.error("Error al obtener rol de usuario {}: {}", uid, e.getMessage());
            throw e;
        }
    }

    /**
     * Asigna un rol y sus permisos a un usuario en Firebase
     */
    @Transactional
    public void asignarRolYPermisosAFirebaseUser(String uid, String rolNombre) throws FirebaseAuthException {
        try {
            RoleEntity rolEntity = rolePermissionService.getRoleByName(rolNombre);
            if (rolEntity == null) {
                throw new AuthException("Rol no válido: " + rolNombre);
            }

            List<PermissionEntity> permisos = rolePermissionService.getPermissionsForRole(rolNombre);

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", rolEntity.getName());
            claims.put("permissions", permisos.stream()
                    .map(PermissionEntity::getName)
                    .collect(Collectors.toList()));
            claims.put("updated_at", System.currentTimeMillis());

            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
            log.info("Rol {} y {} permisos asignados al usuario {}", rolEntity.getName(), permisos.size(), uid);
        } catch (FirebaseAuthException e) {
            log.error("Error al asignar rol a usuario {}: {}", uid, e.getMessage());
            throw e;
        }
    }

    /**
     * Verifica el token y obtiene información del usuario incluyendo su rol y permisos
     */
    public FirebaseUserInfoDto verificarRolYPermisos(String idToken) throws FirebaseAuthException {
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = token.getUid();

            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            Map<String, Object> claims = userRecord.getCustomClaims();

            String email = token.getEmail();
            String role = claims != null && claims.containsKey("role")
                    ? claims.get("role").toString()
                    : "sin_rol";

            @SuppressWarnings("unchecked")
            List<String> permissions = claims != null && claims.containsKey("permissions")
                    ? (List<String>) claims.get("permissions")
                    : new ArrayList<>();

            return FirebaseUserInfoDto.builder()
                    .email(email)
                    .uid(uid)
                    .role(role)
                    .permissions(permissions)
                    .build();
        } catch (FirebaseAuthException e) {
            log.error("Error al verificar token: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void actualizarPermisosDeUsuariosPorRol(RoleEntity role) {
        if (role == null) {
            throw new AuthException("El rol no puede ser nulo");
        }

        actualizarPermisosDeUsuariosPorRol(role.getName());
    }

    /**
     * Actualiza los permisos de todos los usuarios con un rol específico
     */
    @Transactional
    public void actualizarPermisosDeUsuariosPorRol(String roleName) {
        try {
            log.info("Iniciando actualización de permisos para usuarios con rol {}", roleName);
            ListUsersPage page = FirebaseAuth.getInstance().listUsers(null);
            int usuariosActualizados = 0;

            while (page != null) {
                for (ExportedUserRecord user : page.getValues()) {
                    Map<String, Object> claims = user.getCustomClaims();
                    if (claims != null && claims.containsKey("role") &&
                            roleName.equals(claims.get("role").toString())) {
                        asignarRolYPermisosAFirebaseUser(user.getUid(), roleName);
                        usuariosActualizados++;
                    }
                }

                page = page.getNextPage();
            }

            log.info("Actualizados permisos de {} usuarios con rol {}", usuariosActualizados, roleName);
        } catch (FirebaseAuthException e) {
            log.error("Error al actualizar permisos de usuarios por rol {}: {}", roleName, e.getMessage());
            throw new AuthException("Error al actualizar permisos: " + e.getMessage(), e);
        }
    }
}
