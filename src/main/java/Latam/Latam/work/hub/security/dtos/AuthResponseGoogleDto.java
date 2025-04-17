package Latam.Latam.work.hub.security.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseGoogleDto {
    private String idToken;
    private String email;
    private String localId;
    private String role;
    private String name;
    private String photoUrl;
}