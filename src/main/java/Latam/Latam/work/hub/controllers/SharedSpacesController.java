package Latam.Latam.work.hub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/espacios")
public class SharedSpacesController {
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERMISSION_VER_ESPACIOS', 'PERMISSION_BUSCAR_ESPACIOS')")
    public ResponseEntity<?> listarEspacios() {
        System.out.println("Hola 1");
        return null;
    }

    @GetMapping("/asd")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> crearEspacio() {
        return ResponseEntity.ok("hola wachin");
    }

    @PutMapping()
    @PreAuthorize("hasAuthority('PERMISSION_MODIFICAR_ESPACIO')")
    public ResponseEntity<?> modificarEspacio() {
        System.out.println("Hola 3");
        return null;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERMISSION_VER_ESPACIOS', 'PERMISSION_VER_DETALLES_ESPACIO')")
    public ResponseEntity<?> verDetallesEspacio() {
        System.out.println("Hola 4");
        return null;
    }
}
