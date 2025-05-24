package Latam.Latam.work.hub.services.firebase;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface FirebaseAuthRestService {
    Map<String, Object> signInWithEmailAndPassword(String email, String password);
    Map<String, Object> sendPasswordResetEmail(String email);
}
