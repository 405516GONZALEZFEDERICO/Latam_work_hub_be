package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.entities.RoleEntity;
import com.google.firebase.auth.FirebaseAuthException;


public interface FirebaseRoleService {
    String obtenerRolDeUsuario(String uid) throws FirebaseAuthException ;
    void asignarRolYPermisosAFirebaseUser(String uid, String rolStr)throws FirebaseAuthException;
    FirebaseUserInfoDto verificarRolYPermisos(String idToken)  throws FirebaseAuthException;
    void actualizarPermisosDeUsuariosPorRol(RoleEntity role);
}
