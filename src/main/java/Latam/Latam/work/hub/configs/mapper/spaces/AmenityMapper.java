package Latam.Latam.work.hub.configs.mapper.spaces;


import Latam.Latam.work.hub.dtos.common.AmenityDto;
import Latam.Latam.work.hub.entities.AmenityEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AmenityMapper {

    public static AmenityDto toDto(AmenityEntity entity) {
        if (entity == null) {
            return null;
        }

        return AmenityDto.builder()
                .name(entity.getName())
                .price(entity.getPrice())
                .build();
    }

    public static List<AmenityDto> toDtoList(List<AmenityEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(AmenityMapper::toDto)
                .toList();
    }
}
