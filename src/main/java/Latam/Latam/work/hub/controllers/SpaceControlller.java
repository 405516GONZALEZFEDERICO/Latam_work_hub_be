package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.FiltersSpaceDto;
import Latam.Latam.work.hub.dtos.common.SpaceDto;

import Latam.Latam.work.hub.dtos.common.SpaceResponseDto;
import Latam.Latam.work.hub.services.SpaceService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceControlller {
    private final SpaceService spaceService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<Boolean> createSpace(
            @RequestPart("space") SpaceDto spaceDto,
            @RequestPart("images") List<MultipartFile> images
    ) throws Exception {
        return ResponseEntity.ok(spaceService.createSpace(spaceDto, images));
    }



    @GetMapping("/search")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<SpaceResponseDto>> getActiveAvailableSpaces(
            @RequestParam(required = false) Double pricePerHour,
            @RequestParam(required = false) Double pricePerDay,
            @RequestParam(required = false) Double pricePerMonth,
            @RequestParam(required = false) Double area,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) Long spaceTypeId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) List<Long> amenityIds,
            @PageableDefault(size = 10) Pageable pageable) {

        FiltersSpaceDto filters = FiltersSpaceDto.builder()
                .pricePerHour(pricePerHour)
                .pricePerDay(pricePerDay)
                .pricePerMonth(pricePerMonth)
                .area(area)
                .capacity(capacity)
                .spaceTypeId(spaceTypeId)
                .cityId(cityId)
                .countryId(countryId)
                .amenityIds(amenityIds)
                .build();

        return ResponseEntity.ok(spaceService.findSpacesFiltered(filters, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaceResponseDto> getSpaceById(@PathVariable Long id) {
        SpaceResponseDto space = spaceService.findSpaceById(id);
        return ResponseEntity.ok(space);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<Boolean> updateSpace(
            @PathVariable("id") Long spaceId,
            @RequestPart("space") SpaceDto spaceDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws Exception {
        return ResponseEntity.ok(spaceService.updateSpace(spaceId, spaceDto, images));
    }

    @GetMapping("/provider/spaces")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<Page<SpaceResponseDto>> getProviderSpaces(
            @RequestParam String uid,
            @RequestParam(required = false) Double pricePerHour,
            @RequestParam(required = false) Double pricePerDay,
            @RequestParam(required = false) Double pricePerMonth,
            @RequestParam(required = false) Double area,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) Long spaceTypeId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) List<Long> amenityIds,
            @PageableDefault(size = 10) Pageable pageable) {
        try {
            FiltersSpaceDto filters = FiltersSpaceDto.builder()
                    .pricePerHour(pricePerHour)
                    .pricePerDay(pricePerDay)
                    .pricePerMonth(pricePerMonth)
                    .area(area)
                    .capacity(capacity)
                    .spaceTypeId(spaceTypeId)
                    .cityId(cityId)
                    .countryId(countryId)
                    .amenityIds(amenityIds)
                    .build();

            Page<SpaceResponseDto> spaces = spaceService.findSpacesByOwnerUid(uid, filters, pageable);
            return ResponseEntity.ok(spaces);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<Boolean> deleteSpace(@PathVariable Long id, @RequestParam String userUid) {
        return ResponseEntity.ok(spaceService.deleteSpace(id, userUid));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> deactivateSpace(@PathVariable Long id, @RequestParam String userUid) {
        return   ResponseEntity.ok(spaceService.deactivateSpace(id, userUid));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> activateSpace(@PathVariable Long id, @RequestParam String userUid) {
        return   ResponseEntity.ok(spaceService.activateSpace(id, userUid));
    }
    @GetMapping("/spaces-list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SpaceResponseDto>> getAllActiveSpaces() {
        return ResponseEntity.ok(spaceService.getAllActiveSpaces());
    }
}
