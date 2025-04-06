package Latam.Latam.work.hub.dtos;

import lombok.Data;

@Data
public class AuthResponseDto {
    private String idToken;
    private String refreshToken;
    private String expiresIn;
    private String email;
    private String localId;
    private String role;
}
