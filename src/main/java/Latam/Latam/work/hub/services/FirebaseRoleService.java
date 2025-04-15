package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.security.dtos.FirebaseUserExtendedInfoDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

@Service
public interface FirebaseRoleService {

    FirebaseUserInfoDto verificarRol(String idToken) throws FirebaseAuthException;
    void createNewUserWithDefaultRole(String uid, String email, String name, String photoUrl);
    void assignRolFirebaseUser(String uid, String rolNombre) throws FirebaseAuthException;
    FirebaseUserExtendedInfoDto getExtendedUserInfo(String idToken);
}

