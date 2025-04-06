package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.AuthResponseGoogleDto;
import org.springframework.stereotype.Service;

@Service
public interface GoogleAuthService {

    AuthResponseGoogleDto loginWithGoogle(String idToken);

    String registerWithGoogle(String idToken);

    AuthResponseGoogleDto refreshGoogleToken(String refreshToken);
}
