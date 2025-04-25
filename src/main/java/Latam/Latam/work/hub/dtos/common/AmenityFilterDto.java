package Latam.Latam.work.hub.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityFilterDto {
    private Long id;
    private String name;
    private String description;
}
