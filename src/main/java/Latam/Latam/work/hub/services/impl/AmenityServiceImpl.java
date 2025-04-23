package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.AmenityDto;
import Latam.Latam.work.hub.entities.AmenityEntity;
import Latam.Latam.work.hub.repositories.AmenityRepository;
import Latam.Latam.work.hub.services.AmenityService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {
    private final AmenityRepository amenityRepository;
    private final ModelMapper modelMapper;
    @Override
    public List<AmenityDto> getAllAmenities() {
    List<AmenityDto>amenityDtos=new ArrayList<>();
        List<AmenityEntity>amenityEntities=this.amenityRepository.findAll();
        if (amenityEntities.size()>0) {
            for (AmenityEntity amenityEntity : amenityEntities) {
                AmenityDto amenityDto = modelMapper.map(amenityEntity, AmenityDto.class);
              amenityDtos.add(amenityDto);
            }
            return amenityDtos;
        }else throw new RuntimeException("No amenities found");
    }
}
