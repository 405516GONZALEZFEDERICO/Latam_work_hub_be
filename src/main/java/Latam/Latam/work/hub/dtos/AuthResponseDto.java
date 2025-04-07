package Latam.Latam.work.hub.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String idToken;
    private String refreshToken;
    private String expiresIn;
    private String email;
    private String localId;
    private String role;
    private List<String> permissions; //
}
