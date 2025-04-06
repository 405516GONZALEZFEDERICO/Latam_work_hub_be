package Latam.Latam.work.hub.controllers;


import Latam.Latam.work.hub.dtos.AuthResponseDto;
import Latam.Latam.work.hub.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.dtos.TokenDto;
import Latam.Latam.work.hub.dtos.TokenValidationDto;
import Latam.Latam.work.hub.dtos.UserDto;
import Latam.Latam.work.hub.services.AuthService;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.GoogleAuthService;
import Latam.Latam.work.hub.services.TokenValidationService;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final FirebaseRoleService firebaseRoleService;
    private final TokenValidationService tokenValidationService;
    private final GoogleAuthService googleAuthService;

    @PostMapping("/google/login")
    public ResponseEntity<AuthResponseGoogleDto> loginWithGoogle(@RequestParam String idToken) {
        try {
            AuthResponseGoogleDto response = googleAuthService.loginWithGoogle(idToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
    }

    @PostMapping("/google/register")
    public ResponseEntity<String> registerWithGoogle(@RequestParam String idToken) {
        try {
            String response = googleAuthService.registerWithGoogle(idToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
    @PostMapping("/google/refresh")
    public ResponseEntity<AuthResponseGoogleDto> refreshGoogleToken(@RequestParam String refreshToken) {
        try {
            AuthResponseGoogleDto response = googleAuthService.refreshGoogleToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncUser(HttpServletRequest request) {
        return ResponseEntity.ok(authService.syncUser(request));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String email, @RequestParam String password) {
        return ResponseEntity.ok(authService.registrarUsuario(email, password));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestParam String email, @RequestParam String password) {
        return ResponseEntity.ok(authService.login(email, password));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.logout(refreshToken));
    }

    @GetMapping("/perfil")
    public ResponseEntity<UserDto> obtenerPerfil(HttpServletRequest request) {
        return ResponseEntity.ok(authService.obtenerPerfil(request));
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<TokenDto> refreshToken(HttpServletRequest request) {
        TokenDto tokenDto = tokenValidationService.refrescarToken(request, null);
        return ResponseEntity.ok(tokenDto);
    }

    @GetMapping("/recuperar-contrasenia")
    public ResponseEntity<String> recoverPassword(@RequestParam String email) {
        return ResponseEntity.ok(authService.recuperarContrasenia(email));
    }

    @GetMapping("/verificar-rol")
    public ResponseEntity<FirebaseUserInfoDto> verificarRol(@RequestParam String idToken) {
        try {
            FirebaseUserInfoDto response = firebaseRoleService.verificarRolYPermisos(idToken);
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
    }
    @GetMapping("/validar-token")
    public ResponseEntity<TokenValidationDto> validarToken(@RequestParam String idToken) {
        boolean valido = tokenValidationService.esTokenValido(idToken);
        if (valido) {
            return ResponseEntity.ok(new TokenValidationDto(true, "Token válido."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TokenValidationDto(false, "Token inválido o expirado."));
        }
    }


}
