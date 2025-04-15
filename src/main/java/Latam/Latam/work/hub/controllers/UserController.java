package Latam.Latam.work.hub.controllers;


import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/personal-data")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR')")
    public ResponseEntity<PersonalDataUserDto> createOrUpdatePersonalData(
            @RequestBody PersonalDataUserDto personalDataUserDto,
            @RequestHeader("Authorization") String authorization) {

        // Extraer el token JWT del encabezado
        String token = authorization.replace("Bearer ", "");

        try {
            // Verificar y decodificar el token para obtener el UID
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid(); // Esto extrae el UID del token

            PersonalDataUserDto result = userService.createOrUpdatePersonalDataUser(personalDataUserDto, uid);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (FirebaseAuthException e) {
            throw new AuthException("Token inválido o expirado: " + e.getMessage());
        }
    }
}
