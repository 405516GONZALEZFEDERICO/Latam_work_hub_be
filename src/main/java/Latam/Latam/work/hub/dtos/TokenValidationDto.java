package Latam.Latam.work.hub.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenValidationDto {
    private boolean valido;
    private String mensaje;
}
