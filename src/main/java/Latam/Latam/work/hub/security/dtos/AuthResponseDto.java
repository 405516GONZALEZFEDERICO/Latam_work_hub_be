package Latam.Latam.work.hub.security.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String idToken;
    private String expiresIn;
    private String role;
    private String firebaseUid;
    private String refreshToken;
}
