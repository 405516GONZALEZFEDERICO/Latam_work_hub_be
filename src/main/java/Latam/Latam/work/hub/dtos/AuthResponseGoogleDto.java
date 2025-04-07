package Latam.Latam.work.hub.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseGoogleDto {
    private String idToken;
    private String refreshToken;
    private String expiresIn;
    private String email;
    private String localId;
    private String role;
    private List<String> permissions;
    private String name;
    private String photoUrl;
    private boolean isNewUser;
}
