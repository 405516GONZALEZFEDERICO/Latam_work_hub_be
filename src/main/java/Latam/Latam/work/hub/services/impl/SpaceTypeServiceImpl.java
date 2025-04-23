package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.SpaceTypeDto;
import Latam.Latam.work.hub.entities.SpaceTypeEntity;
import Latam.Latam.work.hub.repositories.SpaceTypeRepository;
import Latam.Latam.work.hub.services.SpaceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpaceTypeServiceImpl implements SpaceTypeService {
    private final SpaceTypeRepository spaceTypeRepository;
    @Override
    public List<SpaceTypeDto> getAllSpacesTypes() {
        List<SpaceTypeEntity> spaceTypes =this.spaceTypeRepository.findAll();
        List<SpaceTypeDto> spaceTypeDtos = new ArrayList<>();
        if (spaceTypes != null && !spaceTypes.isEmpty()) {
            for (SpaceTypeEntity spaceType : spaceTypes) {
                SpaceTypeDto spaceTypeDto = new SpaceTypeDto();
                spaceTypeDto.setName(spaceType.getName());
                spaceTypeDtos.add(spaceTypeDto);
            }
            return spaceTypeDtos;
        } else {
            throw new RuntimeException("No space types found");
        }
    }
}
