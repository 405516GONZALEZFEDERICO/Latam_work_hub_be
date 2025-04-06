package Latam.Latam.work.hub.dtos;

import lombok.Data;

@Data
public class UserDto {
    private String email;
    private boolean enabled;
    private String firebaseUid;
}