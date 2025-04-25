package Latam.Latam.work.hub.dtos.common;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiltersSpaceDto {
    private Double pricePerHour;
    private Double pricePerDay;
    private Double pricePerMonth;
    private Double area;
    private Integer capacity;
    private Long countryId;
    private Long cityId;
    private Long spaceTypeId;
    private List<Long> amenityIds;
}