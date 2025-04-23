package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.SpaceTypeDto;
import Latam.Latam.work.hub.services.SpaceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/space-types")
public class SpaceTypeController {
    private final SpaceTypeService spaceTypeService;
    @GetMapping("/all")
    @PreAuthorize("hasRole('PROVEEDOR') || hasRole('CLIENTE')")
    public ResponseEntity<List<SpaceTypeDto>> getAllAmenities() {
        List<SpaceTypeDto> spaceTypeDtos = spaceTypeService.getAllSpacesTypes();
        return ResponseEntity.ok(spaceTypeDtos);
    }
}
