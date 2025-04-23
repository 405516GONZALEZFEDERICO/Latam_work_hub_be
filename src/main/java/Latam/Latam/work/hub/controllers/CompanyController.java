package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.CompanyInfoDto;
import Latam.Latam.work.hub.services.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping("/select-provider-type")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<CompanyInfoDto> companyInfoDto(@RequestParam String uid, @RequestBody CompanyInfoDto companyInfoDto){
      return ResponseEntity.ok(this.companyService.createOrUpdateCompanyUser(uid, companyInfoDto));
    }

    @GetMapping("/get-info-company")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<CompanyInfoDto> getCompanyInfo(@RequestParam String uid) {
        try {
            CompanyInfoDto companyInfo = this.companyService.getInfoCompany(uid);
            return ResponseEntity.ok(companyInfo);
        } catch (Exception e) {
            log.error("Error al obtener información de la empresa: {}", e.getMessage());
            // Devolver un DTO vacío con estado 200
            CompanyInfoDto emptyDto = new CompanyInfoDto();
            emptyDto.setName("");
            emptyDto.setLegalName("");
            emptyDto.setTaxId("");
            emptyDto.setPhone("");
            emptyDto.setEmail("");
            emptyDto.setWebsite("");
            emptyDto.setContactPerson("");
            return ResponseEntity.ok(emptyDto);
        }
    }


}
