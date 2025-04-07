package Latam.Latam.work.hub.dtos;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class TokenDto {
    private String token;          // El ID Token (JWT)
    private String refreshToken;   // El nuevo refresh token
    private String expiresIn;      // Tiempo de expiración en segundos
}

