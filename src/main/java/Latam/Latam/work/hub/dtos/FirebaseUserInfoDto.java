package Latam.Latam.work.hub.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseUserInfoDto {
    private String email;
    private String uid;
    private String role;
    private List<String> permissions;
}