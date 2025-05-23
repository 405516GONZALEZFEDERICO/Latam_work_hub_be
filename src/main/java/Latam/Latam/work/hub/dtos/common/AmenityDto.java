package Latam.Latam.work.hub.dtos.common;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AmenityDto {
    private Long id;
    private String name;
    private Double price;
}
