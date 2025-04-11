package Latam.Latam.work.hub.dtos;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class TokenDto {
    private String token;
    private String refreshToken;
    private String expiresIn;
    private String role;
}

