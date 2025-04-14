package Latam.Latam.work.hub.security.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirebaseUserInfoDto {
    private String email;
    private String uid;
    private String role;

}