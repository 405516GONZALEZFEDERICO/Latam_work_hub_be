package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

@Service
public interface FirebaseRoleService {
    String obtenerRolDeUsuario(String uid) throws FirebaseAuthException ;
    void cambiarRolDeUsuario(String uid, String nuevoRol) throws FirebaseAuthException;
    void sincronizarRolesConFirebase();
    FirebaseUserInfoDto verificarRol(String idToken) throws FirebaseAuthException;
    void createNewUserWithDefaultRole(String uid, String email, String name, String photoUrl);
    void asignarRolAFirebaseUser(String uid, String rolNombre) throws FirebaseAuthException;
}

