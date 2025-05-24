package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.mapper.AddressConverter;
import Latam.Latam.work.hub.configs.mapper.spaces.SpaceMapper;
import Latam.Latam.work.hub.dtos.common.AmenityDto;
import Latam.Latam.work.hub.dtos.common.SpaceDto;
import Latam.Latam.work.hub.dtos.common.FiltersSpaceDto;
import Latam.Latam.work.hub.dtos.common.SpaceResponseDto;
import Latam.Latam.work.hub.entities.AddressEntity;
import Latam.Latam.work.hub.entities.AmenityEntity;
import Latam.Latam.work.hub.entities.ImageEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.entities.SpaceTypeEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.repositories.AddressRepository;
import Latam.Latam.work.hub.repositories.AmenityRepository;
import Latam.Latam.work.hub.repositories.ImageRepository;  // Añadir repositorio para las imágenes
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.SpaceTypeRepository;
import Latam.Latam.work.hub.services.SpaceService;
import Latam.Latam.work.hub.services.UserService;
import Latam.Latam.work.hub.services.cloudinary.CloudinaryService;  // Importar el servicio de Cloudinary
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
            SpaceTypeEntity spaceTypeEntity=this.spaceTypeRepository.findById(spaceDto.getType().getId()).orElse(null);
            spaceEntity.setType(spaceTypeEntity);
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
    public boolean updateSpace(Long spaceId, SpaceDto spaceDto, List<MultipartFile> images) throws Exception {
        try {
            // Find existing space
            SpaceEntity spaceEntity = spaceRepository.findById(spaceId)
                    .orElseThrow(() -> new Exception("Espacio no encontrado con ID: " + spaceId));

            // Verify ownership if needed
            if (!spaceEntity.getOwner().getFirebaseUid().equals(spaceDto.getUid())) {
                throw new Exception("No tiene permisos para actualizar este espacio");
            }

            // Update basic properties
            spaceEntity.setName(spaceDto.getName());
            spaceEntity.setDescription(spaceDto.getDescription());
            spaceEntity.setCapacity(spaceDto.getCapacity());
            spaceEntity.setArea(spaceDto.getArea());
            spaceEntity.setPricePerHour(spaceDto.getPricePerHour());
            spaceEntity.setPricePerDay(spaceDto.getPricePerDay());
            spaceEntity.setPricePerMonth(spaceDto.getPricePerMonth());
            spaceEntity.setUpdatedDateTime(LocalDateTime.now());

            // Update space type if changed
            if (spaceDto.getType() != null && spaceDto.getType().getName() != null) {
                spaceEntity.setType(spaceTypeRepository.findByName(spaceDto.getType().getName()));
                if (spaceEntity.getType() == null) {
                    throw new Exception("Tipo de espacio no encontrado");
                }
            }

            // Update address
            AddressEntity addressEntity = spaceEntity.getAddress();
            if (addressEntity == null) {
                addressEntity = new AddressEntity();
                spaceEntity.setAddress(addressEntity);
            }

            // Update address fields using converter
            AddressEntity updatedAddress = addressConverter.convertToAddressEntity(spaceDto);
            addressEntity.setStreetName(updatedAddress.getStreetName());
            addressEntity.setStreetNumber(updatedAddress.getStreetNumber());
            addressEntity.setFloor(updatedAddress.getFloor());
            addressEntity.setApartment(updatedAddress.getApartment());
            addressEntity.setPostalCode(updatedAddress.getPostalCode());
            addressEntity.setCity(updatedAddress.getCity());

            // Save updated address
            addressRepository.save(addressEntity);

            // Update amenities
            if (spaceDto.getAmenities() != null) {
                // Clear existing amenities relationship (not deleting the amenities themselves)
                spaceEntity.getAmenities().clear();

                List<AmenityEntity> updatedAmenities = new ArrayList<>();
                for (AmenityDto amenityDto : spaceDto.getAmenities()) {
                    AmenityEntity amenityEntity = amenityRepository.findByName(amenityDto.getName());
                    if (amenityEntity == null) {
                        // Create new amenity if it doesn't exist
                        amenityEntity = new AmenityEntity();
                        amenityEntity.setName(amenityDto.getName());
                        amenityEntity.setPrice(amenityDto.getPrice());
                        amenityEntity = amenityRepository.save(amenityEntity);
                    } else {
                        // Update existing amenity price if it changed
                        if (amenityEntity.getPrice() != amenityDto.getPrice()) {
                            amenityEntity.setPrice(amenityDto.getPrice());
                            amenityRepository.save(amenityEntity);
                        }
                    }
                    updatedAmenities.add(amenityEntity);
                }
                spaceEntity.setAmenities(updatedAmenities);
            }

            // Save the updated space entity
            spaceRepository.save(spaceEntity);

            // Handle images if provided
            if (images != null && !images.isEmpty()) {
                // Optionally, delete existing images if needed
                List<ImageEntity> existingImages = imageRepository.findBySpaceId(spaceId);
                if (!existingImages.isEmpty()) {
                    // Delete image files from Cloudinary
                    for (ImageEntity image : existingImages) {
                        String publicId =  cloudinaryService.extractPublicIdFromUrl(image.getUrl());
                        if (publicId != null && !publicId.isEmpty()) {
                            cloudinaryService.deleteImage(publicId);
                        }
                    }

                    // Delete image records from database
                    imageRepository.deleteAll(existingImages);
                }

                // Upload and save new images
                List<String> imageUrls = cloudinaryService.uploadImages(images);
                for (String imageUrl : imageUrls) {
                    ImageEntity imageEntity = new ImageEntity();
                    imageEntity.setUrl(imageUrl);
                    imageEntity.setSpace(spaceEntity);
                    imageRepository.save(imageEntity);
                }
            }

            return true;
        } catch (Exception e) {
            throw new Exception("Error al actualizar el espacio: " + e.getMessage(), e);
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

    @Override
    public Page<SpaceResponseDto> findSpacesByOwnerUid(String uid, FiltersSpaceDto filters, Pageable pageable) {
        try {
            Page<SpaceEntity> spacesPage = spaceRepository.findSpacesByOwnerUid(
                    uid,
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
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener los espacios del proveedor: " + e.getMessage(), e);
        }
    }
    /**
     * Elimina un espacio y sus imágenes asociadas de Cloudinary.
     *
     * @param spaceId ID del espacio a eliminar.
     * @param uid     UID del usuario que intenta eliminar el espacio.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    @Override
    public boolean deleteSpace(Long spaceId, String uid) {
        try {
            SpaceEntity spaceToBeDeleted = this.spaceRepository
                    .findById(spaceId)
                    .orElseThrow(() -> new EntityNotFoundException("Espacio no encontrado"));

            if (!spaceToBeDeleted.getOwner().isEnabled() ||
                    !spaceToBeDeleted.getOwner().getFirebaseUid().equals(uid)) {
                throw new RuntimeException("No tiene permisos para eliminar este espacio");
            }


            spaceToBeDeleted.setDeleted(true);

            // Eliminar el espacio
            this.spaceRepository.save(spaceToBeDeleted);
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el espacio: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SpaceResponseDto> getAllActiveSpaces() {
        List<SpaceEntity> activeSpaces = spaceRepository.findAllActiveSpaces();

        return activeSpaces.stream()
                .map(spaceMapper::toDto)
                .toList();
    }

    @Override
    public Boolean deactivateSpace(Long id, String userUid) {
        UserEntity admin=this.userService.getUserByUid(userUid);
        if (admin!=null){
            SpaceEntity spaceEntity = this.spaceRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Espacio no encontrado"));
            spaceEntity.setActive(false);
            spaceEntity.setAvailable(false);
            this.spaceRepository.save(spaceEntity);
            return true;
        }
        return null;
    }

    @Override
    public Boolean activateSpace(Long id, String userUid) {
        UserEntity admin=this.userService.getUserByUid(userUid);
        if (admin!=null){
            SpaceEntity spaceEntity = this.spaceRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Espacio no encontrado"));
            spaceEntity.setActive(true);
            spaceEntity.setAvailable(true);
            this.spaceRepository.save(spaceEntity);
            return true;
        }
        return null;
    }


}
