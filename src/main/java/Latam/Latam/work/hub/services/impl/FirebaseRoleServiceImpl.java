package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.enums.Permission;
import Latam.Latam.work.hub.enums.Role;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FirebaseRoleServiceImpl implements FirebaseRoleService {

    private static final Map<Role, List<Permission>> permisosPorRol = new HashMap<>();

    static {
        permisosPorRol.put(Role.ADMIN, Arrays.asList(
                Permission.CREAR_USUARIO,
                Permission.ELIMINAR_USUARIO,
                Permission.APROBAR_PROVEEDOR,
                Permission.ACTIVAR_CUENTA,
                Permission.VER_ESPACIOS,
                Permission.MODIFICAR_ESPACIO,
                Permission.VER_TRANSACCIONES,
                Permission.CANCELAR_RESERVA,
                Permission.GESTIONAR_REEMBOLSOS,
                Permission.VER_INFORMES,
                Permission.CONFIGURAR_SISTEMA
        ));

        permisosPorRol.put(Role.PROVEEDOR, Arrays.asList(
                Permission.VER_ESPACIOS,
                Permission.CREAR_ESPACIO,
                Permission.MODIFICAR_ESPACIO,
                Permission.ESTABLECER_PRECIO,
                Permission.GESTIONAR_CONTRATO,
                Permission.VER_PAGOS_RECIBIDOS,
                Permission.EMITIR_FACTURAS,
                Permission.VER_INFORMES_PROPIO
        ));

        permisosPorRol.put(Role.CLIENTE, Arrays.asList(
                Permission.BUSCAR_ESPACIOS,
                Permission.VER_ESPACIOS,
                Permission.VER_DETALLES_ESPACIO,
                Permission.CREAR_RESERVA,
                Permission.CANCELAR_RESERVA_CLIENTE,
                Permission.CALIFICAR_ESPACIO,
                Permission.REALIZAR_PAGO,
                Permission.VER_HISTORIAL_PAGOS,
                Permission.DESCARGAR_FACTURA
        ));
    }

//    @Override
//    public void asignarRolAFirebaseUser(String uid, String rol) throws FirebaseAuthException {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("role", rol);
//        FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
//    }
//    @Override
//    public List<String> obtenerPermisosDeUsuario(String uid) throws FirebaseAuthException {
//        UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
//        Map<String, Object> claims = userRecord.getCustomClaims();
//
//        if (claims != null && claims.containsKey("permissions")) {
//            Object permissionsObj = claims.get("permissions");
//            if (permissionsObj instanceof List<?>) {
//                return ((List<?>) permissionsObj).stream().map(Object::toString).toList();
//            }
//        }
//
//        return new ArrayList<>();
//    }
    @Override
    public String obtenerRolDeUsuario(String uid) throws FirebaseAuthException {
        UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
        Object rol = userRecord.getCustomClaims().get("role");
        return rol != null ? rol.toString() : "sin rol";
    }

    @Override
    public void asignarRolYPermisosAFirebaseUser(String uid, String rolStr) throws FirebaseAuthException {
        Role rol = Role.valueOf(rolStr.toUpperCase());
        List<Permission> permisos = permisosPorRol.getOrDefault(rol, Collections.emptyList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", rol.name()); // "ADMIN", "CLIENTE", etc.
        claims.put("permissions", permisos.stream()
                .map(Enum::name)
                .collect(Collectors.toList()));

        FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
    }



    @Override
    public FirebaseUserInfoDto verificarRolYPermisos(String idToken) throws FirebaseAuthException {
        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid = token.getUid();

        UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
        Map<String, Object> claims = userRecord.getCustomClaims();

        String email = token.getEmail();
        String role = (String) claims.getOrDefault("role", "sin rol");
        List<String> permissions = (List<String>) claims.getOrDefault("permissions", new ArrayList<>());

        return new FirebaseUserInfoDto(email, uid, role, permissions);
    }




}
