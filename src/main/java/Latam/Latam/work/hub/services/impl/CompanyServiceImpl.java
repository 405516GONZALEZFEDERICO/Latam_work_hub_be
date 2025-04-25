package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.mapper.ModelMapperConfig;
import Latam.Latam.work.hub.dtos.common.CompanyInfoDto;
import Latam.Latam.work.hub.entities.CompanyEntity;
import Latam.Latam.work.hub.entities.CountryEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.ProviderType;
import Latam.Latam.work.hub.repositories.CompanyRepository;
import Latam.Latam.work.hub.repositories.CountryRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.CompanyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ModelMapperConfig modelMapperConfig;
    private final CountryRepository countryRepository;
    @Override
    @Transactional
    public CompanyInfoDto createOrUpdateCompanyUser(String uid, CompanyInfoDto companyInfoDto) {
        if (companyInfoDto == null) {
            throw new IllegalArgumentException("CompanyInfoDto cannot be null");
        }

        UserEntity user = this.userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Si es CLIENTE, solo se maneja la asociación de compañía
        if (user.getRole().equals("CLIENTE")) {
            return handleClientCompany(user, companyInfoDto);
        }

        // Para PROVEEDOR, validar y manejar el tipo de proveedor
        if (companyInfoDto.getProviderType() == null || companyInfoDto.getProviderType().isEmpty()) {
            throw new IllegalArgumentException("ProviderType is required for providers");
        }

        if (ProviderType.INDIVIDUAL.name().equals(companyInfoDto.getProviderType())) {
            return handleIndividualProvider(user, companyInfoDto);
        } else if (ProviderType.COMPANY.name().equals(companyInfoDto.getProviderType())) {
            return handleCompanyProvider(user, companyInfoDto);
        } else {
            throw new IllegalArgumentException("ProviderType not valid: " + companyInfoDto.getProviderType());
        }
    }
    private CompanyInfoDto handleClientCompany(UserEntity user, CompanyInfoDto companyInfoDto) {
        CompanyEntity companyEntity = user.getCompany() != null ? user.getCompany() : new CompanyEntity();

        updateCompanyFromDto(companyEntity, companyInfoDto);
        companyEntity.setRegistrationDate(companyEntity.getRegistrationDate() != null ?
                companyEntity.getRegistrationDate() : LocalDateTime.now());
        companyEntity.setActive(true);

        companyEntity = companyRepository.save(companyEntity);
        user.setCompany(companyEntity);
        userRepository.save(user);

        return convertCompanyEntityToDto(companyEntity);
    }
    /**
     * Maneja la lógica para proveedores de tipo individual
     */
    private CompanyInfoDto handleIndividualProvider(UserEntity user, CompanyInfoDto companyInfoDto) {
        user.setProviderType(ProviderType.INDIVIDUAL);
        user.setCompany(null);
        userRepository.save(user);
        return companyInfoDto;
    }

    /**
     * Maneja la lógica para proveedores de tipo empresa
     */


    private CompanyInfoDto handleCompanyProvider(UserEntity user, CompanyInfoDto companyInfoDto) {
        if (companyInfoDto.getName() == null || companyInfoDto.getName().isEmpty()) {
            throw new IllegalArgumentException("Company information is required for COMPANY provider type");
        }

        user.setProviderType(ProviderType.COMPANY);
        CompanyEntity companyEntity = user.getCompany() != null ? user.getCompany() : new CompanyEntity();

        updateCompanyFromDto(companyEntity, companyInfoDto);
        companyEntity.setRegistrationDate(companyEntity.getRegistrationDate() != null ? companyEntity.getRegistrationDate() : LocalDateTime.now());
        companyEntity.setActive(true);

        companyEntity = companyRepository.save(companyEntity);
        user.setCompany(companyEntity);
        userRepository.save(user);

        return convertCompanyEntityToDto(companyEntity);
    }

    /**
     * Actualiza los datos de la entidad de compañía desde el DTO
     */
    private void updateCompanyFromDto(CompanyEntity companyEntity, CompanyInfoDto companyInfoDto) {
        companyEntity.setName(companyInfoDto.getName());
        companyEntity.setLegalName(companyInfoDto.getLegalName());
        companyEntity.setTaxId(companyInfoDto.getTaxId());
        companyEntity.setPhone(companyInfoDto.getPhone());
        companyEntity.setEmail(companyInfoDto.getEmail());
        companyEntity.setWebsite(companyInfoDto.getWebsite());
        companyEntity.setContactPerson(companyInfoDto.getContactPerson());
        companyEntity.setRegistrationDate(LocalDateTime.now());
        companyEntity.setActive(true);

        // Buscar y asignar el país
        if (companyInfoDto.getCountry() != null) {
            CountryEntity countryEntity = countryRepository
                    .findById(companyInfoDto.getCountry().longValue())
                    .orElseThrow(() -> new IllegalArgumentException("País no encontrado con ID: " + companyInfoDto.getCountry()));
            companyEntity.setCountry(countryEntity);
        } else {
            companyEntity.setCountry(null);
        }
    }

    /**
     * Convierte la entidad de compañía a DTO usando ModelMapper de forma segura
     */
    private CompanyInfoDto convertCompanyEntityToDto(CompanyEntity companyEntity) {
        // Primero crear el DTO sin el campo country
        CompanyInfoDto resultDto = new CompanyInfoDto();

        // Mapear manualmente los campos básicos
        resultDto.setName(companyEntity.getName());
        resultDto.setLegalName(companyEntity.getLegalName());
        resultDto.setTaxId(companyEntity.getTaxId());
        resultDto.setPhone(companyEntity.getPhone());
        resultDto.setEmail(companyEntity.getEmail());
        resultDto.setWebsite(companyEntity.getWebsite());
        resultDto.setContactPerson(companyEntity.getContactPerson());

        // Establecer el tipo de proveedor
        resultDto.setProviderType(ProviderType.COMPANY.name());

        // Mapear el ID del país si existe
        if (companyEntity.getCountry() != null) {
            resultDto.setCountry(companyEntity.getCountry().getId().intValue());
        }

        return resultDto;
    }

    @Override
    public CompanyInfoDto getInfoCompany(String uid) {
        UserEntity user = this.userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CompanyEntity companyEntity = user.getCompany();
        if(companyEntity == null){
            CompanyInfoDto emptyDto = new CompanyInfoDto();
            emptyDto.setName("");
            emptyDto.setLegalName("");
            emptyDto.setTaxId("");
            emptyDto.setPhone("");
            emptyDto.setEmail("");
            emptyDto.setWebsite("");
            emptyDto.setContactPerson("");
            if ("PROVEEDOR".equals(user.getRole())) {
                emptyDto.setProviderType(user.getProviderType() != null ?
                        user.getProviderType().name() : "");
            }
            return emptyDto;
        }

        return convertCompanyEntityToDto(companyEntity);
    }
}
