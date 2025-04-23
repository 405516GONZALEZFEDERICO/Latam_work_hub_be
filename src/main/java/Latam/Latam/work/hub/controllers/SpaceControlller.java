package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.SpaceDto;
import Latam.Latam.work.hub.services.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    
}
