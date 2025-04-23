package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.AmenityDto;
import Latam.Latam.work.hub.dtos.common.SpaceDto;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    @Override
    public boolean createSpace(SpaceDto spaceDto, List<MultipartFile> images) throws Exception {
        try {
            // 1. Crear y guardar el SpaceEntity
            SpaceEntity spaceEntity = modelMapper.map(spaceDto, SpaceEntity.class);



            spaceEntity.setOwner(userService.getUserByUid(spaceDto.getUid()));
            spaceEntity.setActive(true);
            spaceEntity.setAvailable(true);
    
            spaceEntity.setType(spaceTypeRepository.findByName(spaceDto.getType().getName()));
            AddressEntity addressEntity = modelMapper.map(spaceDto.getAddress(), AddressEntity.class);
            addressRepository.save(addressEntity);
            spaceEntity.setAddress(addressEntity);
    
            if (spaceDto.getAmenities() != null) {
                List<AmenityEntity> savedAmenities = new ArrayList<>();
                for (AmenityDto amenityDto : spaceDto.getAmenities()) {
                    AmenityEntity amenityEntity = amenityRepository.findByName(amenityDto.getName());
                    if (amenityEntity == null) {
                        amenityEntity = modelMapper.map(amenityDto, AmenityEntity.class);
                        amenityEntity = amenityRepository.save(amenityEntity);
                    }
                    savedAmenities.add(amenityEntity);
                }
                spaceEntity.setAmenities(savedAmenities);
            }
    
            spaceRepository.save(spaceEntity);
    
            // 2. Subir las imágenes a Cloudinary y guardar las entidades
            List<String> imageUrls = cloudinaryService.uploadImages(images);  // Usar el método 'uploadImages' para subir varias imágenes
    
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
    public boolean updateSpace(SpaceDto spaceDto) {
        throw new UnsupportedOperationException("Unimplemented method 'updateSpace'");
    }

    @Override
    public boolean deleteSpace(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteSpace'");
    }

    @Override
    public SpaceDto getSpaceById(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'getSpaceById'");
    }

    @Override
    public List<SpaceDto> getAllSpaces() {
        throw new UnsupportedOperationException("Unimplemented method 'getAllSpaces'");
    }

    @Override
    public List<SpaceDto> getSpacesByUserUid(String uid) {
        throw new UnsupportedOperationException("Unimplemented method 'getSpacesByUserUid'");
    }
}
