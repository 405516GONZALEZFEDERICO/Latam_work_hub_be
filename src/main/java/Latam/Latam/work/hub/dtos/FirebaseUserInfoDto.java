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
public class FirebaseUserInfoDto {
    private String email;
    private String uid;
    private String role;
    private List<String> permissions;
}