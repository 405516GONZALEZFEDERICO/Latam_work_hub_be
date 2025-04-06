package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.TokenDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public interface TokenValidationService {
    TokenDto refrescarToken(HttpServletRequest request, HttpServletResponse response);
    boolean esTokenValido(String idToken);
}
