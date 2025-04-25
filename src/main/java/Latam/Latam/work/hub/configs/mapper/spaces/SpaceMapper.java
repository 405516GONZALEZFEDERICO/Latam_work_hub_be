package Latam.Latam.work.hub.configs.mapper.spaces;

import Latam.Latam.work.hub.dtos.common.SpaceResponseDto;
import Latam.Latam.work.hub.entities.SpaceEntity;
import org.springframework.stereotype.Component;

@Component
public class SpaceMapper {

    private final AddressMapper addressMapper;

    public SpaceMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public SpaceResponseDto toDto(SpaceEntity entity) {
        if (entity == null) {
            return null;
        }

        return SpaceResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .pricePerHour(entity.getPricePerHour())
                .pricePerDay(entity.getPricePerDay())
                .pricePerMonth(entity.getPricePerMonth())
                .capacity(entity.getCapacity())
                .area(entity.getArea())
                .active(entity.getActive())
                .available(entity.getAvailable())
                .address(addressMapper.toDto(entity.getAddress()))
                .spaceType(entity.getType() != null ? entity.getType().getName() : null)
                .amenities(AmenityMapper.toDtoList(entity.getAmenities()))
                .photoUrl(entity.getImages() != null ?
                        entity.getImages().stream()
                                .map(image -> image.getUrl())
                                .toList() :
                        null)
                .build();
    }
}