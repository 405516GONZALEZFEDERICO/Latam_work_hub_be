package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.CompanyInfoDto;
import org.springframework.stereotype.Service;

@Service
public interface CompanyService {
    CompanyInfoDto createOrUpdateCompanyUser(String uid,CompanyInfoDto companyInfoDto);

    CompanyInfoDto getInfoCompany(String uid);

}
