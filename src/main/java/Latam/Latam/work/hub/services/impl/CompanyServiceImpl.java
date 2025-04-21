package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.ModelMapperConfig;
import Latam.Latam.work.hub.dtos.common.CompanyInfoDto;
import Latam.Latam.work.hub.entities.CompanyEntity;
import Latam.Latam.work.hub.entities.CountryEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.ProviderType;
import Latam.Latam.work.hub.repositories.CompanyRepository;
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
    @Override
    @Transactional
    public CompanyInfoDto createOrUpdateCompanyUser(String uid, CompanyInfoDto companyInfoDto) {
        if (companyInfoDto == null || companyInfoDto.getProviderType() == null || companyInfoDto.getProviderType().isEmpty()) {
            throw new IllegalArgumentException("CompanyInfoDto or providerType cannot be null or empty");
        }

        UserEntity user = this.userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (ProviderType.INDIVIDUAL.name().equals(companyInfoDto.getProviderType())) {
            return handleIndividualProvider(user, companyInfoDto);
        } else if (ProviderType.COMPANY.name().equals(companyInfoDto.getProviderType())) {
            return handleCompanyProvider(user, companyInfoDto);
        } else {
            throw new IllegalArgumentException("ProviderType not valid: " + companyInfoDto.getProviderType());
        }
    }

    /**
     * Maneja la lógica para proveedores de tipo individual
     */
    private CompanyInfoDto handleIndividualProvider(UserEntity user, CompanyInfoDto companyInfoDto) {
        user.setProviderType(ProviderType.INDIVIDUAL);
        // Si el usuario ya tenía una compañía asociada, desasociarla
        user.setCompany(null);
        userRepository.save(user);
        // Devolver los datos básicos para proveedores individuales
        return companyInfoDto;
    }

    /**
     * Maneja la lógica para proveedores de tipo empresa
     */
    private CompanyInfoDto handleCompanyProvider(UserEntity user, CompanyInfoDto companyInfoDto) {
        user.setProviderType(ProviderType.COMPANY);

        // Verificar si el usuario ya tiene una compañía
        CompanyEntity companyEntity;
        boolean isNewCompany = false;

        if (user.getCompany() != null) {
            // Actualizar la compañía existente
            companyEntity = user.getCompany();
        } else {
            // Crear una nueva compañía
            companyEntity = new CompanyEntity();
            companyEntity.setRegistrationDate(LocalDateTime.now());
            companyEntity.setActive(true);
            isNewCompany = true;
        }

        // Actualizar los datos de la compañía
        updateCompanyFromDto(companyEntity, companyInfoDto);

        // Guardar la compañía
        companyEntity = this.companyRepository.save(companyEntity);

        // Asignar la compañía al usuario y guardar
        user.setCompany(companyEntity);
        this.userRepository.save(user);

        // Convertir entidad a DTO para respuesta
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

        // Asignar el país manualmente
        if (companyInfoDto.getCountry() != null) {
            CountryEntity countryEntity = new CountryEntity();
            countryEntity.setId(companyInfoDto.getCountry().longValue());
            companyEntity.setCountry(countryEntity);
        } else {
            companyEntity.setCountry(null);
        }
    }

    /**
     * Convierte la entidad de compañía a DTO usando ModelMapper de forma segura
     */
    private CompanyInfoDto convertCompanyEntityToDto(CompanyEntity companyEntity) {
        // Utilizamos ModelMapper pero con configuración para evitar problemas de optimistic locking
        CompanyInfoDto resultDto = this.modelMapperConfig.modelMapper().map(companyEntity, CompanyInfoDto.class);

        // Asegurarnos de que el ProviderType esté correctamente establecido
        resultDto.setProviderType(ProviderType.COMPANY.name());

        // Ajustar el campo de país para evitar problemas de mapeo
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
            throw new IllegalArgumentException("Company not found");
        }

        return convertCompanyEntityToDto(companyEntity);
    }
}
