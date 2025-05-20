package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.CompleteUserDataDto;
import Latam.Latam.work.hub.dtos.common.DisableUserDto;
import Latam.Latam.work.hub.dtos.common.PersonalDataUserDto;
import Latam.Latam.work.hub.dtos.common.ProviderTypeDto;
import Latam.Latam.work.hub.entities.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface UserService {
    UserEntity createOrUpdateLocalUser(String email, String uid, String urlImg,String name);
    UserEntity validateUserExists(String email);
    CompleteUserDataDto getPersonalDataUser(String uid);
    PersonalDataUserDto createOrUpdatePersonalDataUser(PersonalDataUserDto personalDataUserDto,String uid);
    UserEntity updateUserLoginData(UserEntity user);
    boolean uploadImagenProfile(String uid, MultipartFile image) throws IOException;
    UserEntity getUserByUid(String uid);
    boolean desactivateAccount(String uid);
    ProviderTypeDto getProviderType(String uid);

    List<DisableUserDto>getAllUsersActive(String rolName);

    boolean activateAccount(String uid);
}
