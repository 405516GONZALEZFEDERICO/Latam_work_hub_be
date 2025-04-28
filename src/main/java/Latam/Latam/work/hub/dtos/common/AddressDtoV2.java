package Latam.Latam.work.hub.dtos.common;

import lombok.Data;
import lombok.Builder;
@Data
@Builder
public class AddressDtoV2 {
    private Long id;
    private String streetName;
    private String streetNumber;
    private String floor;
    private String apartment;
    private String postalCode;
    private Long cityId;
    private Long countryId;
    private String city;
    private String country;
}
