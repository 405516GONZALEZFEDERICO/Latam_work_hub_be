package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.TokenDto;
import org.springframework.stereotype.Service;

@Service
public interface TokenValidationService {
    TokenDto refrescarToken(String refreshToken);
    boolean esTokenValido(String idToken);
    TokenDto exchangeCustomTokenForIdToken(String customToken);
}