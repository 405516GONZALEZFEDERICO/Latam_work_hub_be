package Latam.Latam.work.hub.services;


import Latam.Latam.work.hub.dtos.AuthResponseDto;
import Latam.Latam.work.hub.dtos.UserDto;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    UserDto obtenerPerfil(HttpServletRequest request);
    String registrarUsuario(String email, String password);
    AuthResponseDto login(String email, String password);
    String logout(String refreshToken);
    String recuperarContrasenia(String email);
}
