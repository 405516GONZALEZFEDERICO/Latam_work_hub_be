package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.CompleteUserDto;
import Latam.Latam.work.hub.entities.UserEntity;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    UserEntity createOrUpdateLocalUser(String email, String uid, String urlImg,String name);
    UserEntity validateUserExists(String email);

    CompleteUserDto getCompleteUserData();


}
