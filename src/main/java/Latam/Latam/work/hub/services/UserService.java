package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.entities.UserEntity;

public interface UserService {
    UserEntity createOrUpdateLocalUser(String email, String uid, String rol);
    UserEntity validateUserExists(String email);
}
