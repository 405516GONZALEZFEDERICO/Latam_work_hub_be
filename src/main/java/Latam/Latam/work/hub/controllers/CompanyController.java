package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.CompanyInfoDto;
import Latam.Latam.work.hub.services.CompanyService;
import lombok.RequiredArgsConstructor;
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
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping("/select-provider-type")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<CompanyInfoDto> companyInfoDto(@RequestParam String uid, @RequestBody CompanyInfoDto companyInfoDto){
      return ResponseEntity.ok(this.companyService.createOrUpdateCompanyUser(uid, companyInfoDto));
    }

    @GetMapping("/get-info-company")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public  ResponseEntity<CompanyInfoDto>getCompanyInfo(@RequestParam String uid){
        return ResponseEntity.ok(this.companyService.getInfoCompany(uid));
    }


}
