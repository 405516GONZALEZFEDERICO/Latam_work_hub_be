package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.AmenityDto;
import Latam.Latam.work.hub.services.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/amenities")
public class AmenityController {
    private final AmenityService amenityService;
    @GetMapping("/all")
    @PreAuthorize("hasRole('PROVEEDOR') || hasRole('CLIENTE')")
    public ResponseEntity<List<AmenityDto>> getAllAmenities() {
        List<AmenityDto> amenities = amenityService.getAllAmenities();
        return ResponseEntity.ok(amenities);
    }
}
