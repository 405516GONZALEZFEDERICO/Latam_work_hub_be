package Latam.Latam.work.hub.controllers;


import Latam.Latam.work.hub.dtos.common.CompleteUserDataDto;
import Latam.Latam.work.hub.dtos.common.DisableUserDto;
import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.dtos.common.ProviderTypeDto;
import Latam.Latam.work.hub.exceptions.AuthException;
import Latam.Latam.work.hub.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/personal-data")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR', 'ADMIN')")
    public ResponseEntity<PersonalDataUserDto> createOrUpdatePersonalData(
            @RequestBody PersonalDataUserDto personalDataUserDto,
            @RequestHeader("Authorization") String authorization) {

        String token = authorization.replace("Bearer ", "");
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid(); 

            PersonalDataUserDto result = userService.createOrUpdatePersonalDataUser(personalDataUserDto, uid);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (FirebaseAuthException e) {
            throw new AuthException("Token inválido o expirado: " + e.getMessage());
        }
    }


    @PatchMapping("/desactivate-account/{uid}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR', 'ADMIN')")
    public ResponseEntity<?> desactivateAccount(@PathVariable String uid) {
        try {
            boolean result = userService.desactivateAccount(uid);
            return ResponseEntity.ok(result);
        } catch (EntityNotFoundException e) {
            log.error("Error al desactivar cuenta: Usuario no encontrado - {}", uid);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado");
        } catch (Exception e) {
            log.error("Error inesperado al desactivar la cuenta: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al desactivar la cuenta");
        }
    }



    @PatchMapping("/activate-account/{uid}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR', 'ADMIN')")
    public ResponseEntity<?> activateAccount(@PathVariable String uid) {
        try {
            boolean result = userService.activateAccount(uid);
            return ResponseEntity.ok(result);
        } catch (EntityNotFoundException e) {
            log.error("Error al activar cuenta: Usuario no encontrado - {}", uid);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado");
        } catch (Exception e) {
            log.error("Error inesperado al activar la cuenta: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al activar la cuenta");
        }
    }

    @PostMapping("/{uid}/upload-img")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR', 'ADMIN')")
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable String uid,
            @RequestParam("image") MultipartFile image) {

        try {
            if (image.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity
                        .badRequest()
                        .body("La imagen no puede superar los 5MB");
            }

            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity
                        .badRequest()
                        .body("El archivo debe ser una imagen");
            }

            boolean result = userService.uploadImagenProfile(uid, image);
            return ResponseEntity.ok(result);

        } catch (EntityNotFoundException e) {
            log.error("Error al subir imagen: Usuario no encontrado - {}", uid);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado");

        } catch (IllegalArgumentException e) {
            log.error("Error de validación al subir imagen: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (IOException e) {
            log.error("Error al procesar la imagen: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar la imagen");

        } catch (Exception e) {
            log.error("Error inesperado al subir la imagen: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir la imagen");
        }
    }
    @GetMapping("/{uid}/get-personal-data")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR', 'ADMIN')")
    public ResponseEntity<CompleteUserDataDto> getPersonalData(@PathVariable String uid) {
        try {
            CompleteUserDataDto userData = this.userService.getPersonalDataUser(uid);
            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            log.error("Error al obtener datos personales del usuario: {}", e.getMessage());
            CompleteUserDataDto emptyDto = new CompleteUserDataDto();
            return ResponseEntity.ok(emptyDto);
        }
    }

    @GetMapping("/{uid}/get-provider-type")
    @PreAuthorize("hasAnyRole('PROVEEDOR')")
    public ResponseEntity<ProviderTypeDto> getProviderType(@PathVariable String uid) {
        try {
            ProviderTypeDto userData = this.userService.getProviderType(uid);
            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            log.error("Error al obtener el tipo de   usuario del proveedor: {}", e.getMessage());
            ProviderTypeDto providerTypeDto = new ProviderTypeDto();
            return ResponseEntity.ok(providerTypeDto);
        }
    }
    @GetMapping("/get-user-list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DisableUserDto>>getAllActivesUsers(@RequestParam String roleName) {
        return  ResponseEntity.ok(userService.getAllUsersActive(roleName));
    }
}
