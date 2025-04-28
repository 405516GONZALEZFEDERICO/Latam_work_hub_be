package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.AddressConverter;
import Latam.Latam.work.hub.configs.mapper.spaces.AddressMapper;
import Latam.Latam.work.hub.configs.mapper.spaces.SpaceMapper;
import Latam.Latam.work.hub.dtos.common.AddressDtoV2;
import Latam.Latam.work.hub.dtos.common.AmenityDto;
import Latam.Latam.work.hub.dtos.common.SpaceDto;
import Latam.Latam.work.hub.dtos.common.FiltersSpaceDto;
import Latam.Latam.work.hub.dtos.common.SpaceResponseDto;
import Latam.Latam.work.hub.entities.AddressEntity;
import Latam.Latam.work.hub.entities.AmenityEntity;
import Latam.Latam.work.hub.entities.ImageEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.repositories.AddressRepository;
import Latam.Latam.work.hub.repositories.AmenityRepository;
import Latam.Latam.work.hub.repositories.ImageRepository;  // Añadir repositorio para las imágenes
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.SpaceTypeRepository;
import Latam.Latam.work.hub.services.SpaceService;
import Latam.Latam.work.hub.services.UserService;
import Latam.Latam.work.hub.services.cloudinary.CloudinaryService;  // Importar el servicio de Cloudinary
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpaceServiceImpl implements SpaceService {
    
    private final SpaceRepository spaceRepository;
    private final AddressRepository addressRepository;
    private final SpaceTypeRepository spaceTypeRepository;
    private final AmenityRepository amenityRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;  
    private final ImageRepository imageRepository;
    private final AddressConverter addressConverter;
    private final SpaceMapper spaceMapper;
    @Override
    public boolean createSpace(SpaceDto spaceDto, List<MultipartFile> images) throws Exception {
        try {
            SpaceEntity spaceEntity = new SpaceEntity();

            // Map basic properties
            spaceEntity.setName(spaceDto.getName());
            spaceEntity.setDescription(spaceDto.getDescription());
            spaceEntity.setCapacity(spaceDto.getCapacity());
            spaceEntity.setArea(spaceDto.getArea());
            spaceEntity.setPricePerHour(spaceDto.getPricePerHour());
            spaceEntity.setPricePerDay(spaceDto.getPricePerDay());
            spaceEntity.setPricePerMonth(spaceDto.getPricePerMonth());

            spaceEntity.setOwner(userService.getUserByUid(spaceDto.getUid()));
            spaceEntity.setActive(true);
            spaceEntity.setAvailable(true);
            spaceEntity.setCreatedDateTime(LocalDateTime.now());

            spaceEntity.setType(spaceTypeRepository.findByName(spaceDto.getType().getName()));
            if (spaceEntity.getType() == null) {
                throw new Exception("Tipo de espacio no encontrado");
            }

            // Convertir y guardar la dirección
            AddressEntity addressEntity = addressConverter.convertToAddressEntity(spaceDto);
            addressRepository.save(addressEntity);
            spaceEntity.setAddress(addressEntity);

            // Save address first to get ID
            addressRepository.save(addressEntity);
            spaceEntity.setAddress(addressEntity);

            // Handle amenities
            if (spaceDto.getAmenities() != null) {
                List<AmenityEntity> savedAmenities = new ArrayList<>();
                for (AmenityDto amenityDto : spaceDto.getAmenities()) {
                    AmenityEntity amenityEntity = amenityRepository.findByName(amenityDto.getName());
                    if (amenityEntity == null) {
                        // Create new amenity if it doesn't exist
                        amenityEntity = new AmenityEntity();
                        amenityEntity.setName(amenityDto.getName());
                        amenityEntity.setPrice(amenityDto.getPrice());
                        amenityEntity = amenityRepository.save(amenityEntity);
                    }
                    savedAmenities.add(amenityEntity);
                }
                spaceEntity.setAmenities(savedAmenities);
            }

            // Save the space entity
            spaceRepository.save(spaceEntity);

            // 2. Upload and save images
            List<String> imageUrls = cloudinaryService.uploadImages(images);

            for (String imageUrl : imageUrls) {
                ImageEntity imageEntity = new ImageEntity();
                imageEntity.setUrl(imageUrl);
                imageEntity.setSpace(spaceEntity);
                imageRepository.save(imageEntity);
            }

            return true;
        } catch (Exception e) {
            throw new Exception("Error al crear el espacio", e);
        }
    }

    @Override
    public Page<SpaceResponseDto> findSpacesFiltered(FiltersSpaceDto filters, Pageable pageable) {
        Page<SpaceEntity> spacesPage = spaceRepository.findActiveAvailableSpacesWithoutAmenityFilters(
                filters.getPricePerHour(),
                filters.getPricePerDay(),
                filters.getPricePerMonth(),
                filters.getArea(),
                filters.getCapacity(),
                filters.getSpaceTypeId(),
                filters.getCityId(),
                filters.getCountryId(),
                filters.getAmenityIds(),
                pageable
        );

        return spacesPage.map(spaceMapper::toDto);
    }

    @Override
    public SpaceResponseDto findSpaceById(Long id) {
        return this.spaceRepository.findById(id).map(spaceMapper::toDto).orElse(null);
    }
}
