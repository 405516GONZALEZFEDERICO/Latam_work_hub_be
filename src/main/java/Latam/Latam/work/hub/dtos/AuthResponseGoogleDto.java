package Latam.Latam.work.hub.dtos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseGoogleDto {
    private String idToken;
    private String refreshToken;
    private String expiresIn;
    private String email;
    private String localId;
    private String role;
    private boolean googleAuth = false;
}
