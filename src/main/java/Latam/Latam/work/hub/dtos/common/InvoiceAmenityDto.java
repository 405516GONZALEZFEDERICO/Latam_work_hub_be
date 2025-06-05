package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceAmenityDto {
    private Long amenityId;
    private String amenityName;
    private Double price;
}
