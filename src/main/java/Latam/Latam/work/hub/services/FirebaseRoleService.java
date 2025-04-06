package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;


@Service
public interface FirebaseRoleService {
//    void asignarRolAFirebaseUser(String uid, String rol) throws FirebaseAuthException;
//    List<String> obtenerPermisosDeUsuario(String uid) throws FirebaseAuthException;

    String obtenerRolDeUsuario(String uid) throws FirebaseAuthException;

    void asignarRolYPermisosAFirebaseUser(String uid, String rol) throws FirebaseAuthException;


    FirebaseUserInfoDto verificarRolYPermisos(String idToken) throws FirebaseAuthException;


}
