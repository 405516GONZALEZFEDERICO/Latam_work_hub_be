package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.security.dtos.AuthResponseDto;
import Latam.Latam.work.hub.security.dtos.AuthResponseGoogleDto;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.dtos.common.RoleAssignmentRequestDto;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.services.AuthService;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import Latam.Latam.work.hub.services.GoogleAuthService;
import com.google.firebase.auth.FirebaseAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final FirebaseRoleService firebaseRoleService;
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

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String email, @RequestParam String password) {
        String result = authService.registerUser(email, password);
        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestParam String email, @RequestParam String password) {
        return ResponseEntity.ok(authService.login(email, password));
    }


    @PostMapping("/roles/assign")
    @PreAuthorize("hasRole('DEFAULT')")
        public ResponseEntity<?> assignRoleToUser(@RequestBody RoleAssignmentRequestDto request) {
        try {
            firebaseRoleService.asignarRolAFirebaseUser(request.getUid(), request.getRoleName());
            return ResponseEntity.ok().body(
                    Map.of(
                            "success", true,
                            "message", "Rol " + request.getRoleName() + " asignado correctamente al usuario"
                    )
            );
        } catch (FirebaseAuthException e) {
            log.error("Error al asignar rol: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "success", false,
                            "message", "Error al asignar rol: " + e.getMessage()
                    )
            );
        } catch (AuthException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }
    @GetMapping("/recuperar-contrasenia")
    public ResponseEntity<String> recuperarContrasenia(@RequestParam String email) {
        return ResponseEntity.ok(authService.getPasswordForgoted(email));
    }

    @GetMapping("/verificar-rol")
    public ResponseEntity<FirebaseUserInfoDto> verificarRol(@RequestParam String idToken) {
        try {
            FirebaseUserInfoDto response = firebaseRoleService.verificarRol(idToken);
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
    }





}
