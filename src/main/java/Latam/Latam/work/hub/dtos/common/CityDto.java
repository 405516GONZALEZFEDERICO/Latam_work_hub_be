package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityDto {
    private String name;
    private String divisionName;
    private String divisionType;
    private CountryDto country;
}
