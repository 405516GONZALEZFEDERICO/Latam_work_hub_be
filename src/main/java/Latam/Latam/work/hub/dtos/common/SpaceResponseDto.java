package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceResponseDto {
    private Long id;
    private String name;
    private String description;
    private Double pricePerHour;
    private Double pricePerDay;
    private Double pricePerMonth;
    private Integer capacity;
    private Double area;
    private Boolean active;
    private Boolean available;
    private AddressDto address;
    private String spaceType;
    private List<AmenityDto> amenities;
    private List<String> photoUrl;
}
