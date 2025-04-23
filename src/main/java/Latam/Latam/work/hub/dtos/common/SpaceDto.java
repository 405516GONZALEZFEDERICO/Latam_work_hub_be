package Latam.Latam.work.hub.dtos.common;

import Latam.Latam.work.hub.entities.AddressEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpaceDto {
    private String name;
    private String description;
    private Integer capacity;
    private Double area;
    private Double pricePerHour;
    private Double pricePerDay;
    private Double pricePerMonth;
    private String uid;
    private List<AmenityDto> amenities;
    private SpaceTypeDto type;
    private AddressEntity address;
}
