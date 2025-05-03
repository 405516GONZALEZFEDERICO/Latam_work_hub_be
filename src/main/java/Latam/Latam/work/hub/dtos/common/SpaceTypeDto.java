package Latam.Latam.work.hub.dtos.common;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTypeDto {
    private Long id;
    private String name;
}
