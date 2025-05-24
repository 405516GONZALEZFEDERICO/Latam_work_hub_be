package Latam.Latam.work.hub.services;
import Latam.Latam.work.hub.dtos.common.RoleChangeDto;
import Latam.Latam.work.hub.security.dtos.AuthResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    String registerUser(String email, String password);
    AuthResponseDto login(String email, String password);
    String retrievePassword(String email);

    String changeRol(RoleChangeDto roleChangeDto);
}
