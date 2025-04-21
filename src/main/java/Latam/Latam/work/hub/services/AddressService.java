package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.entities.AddressEntity;
import org.springframework.stereotype.Service;


@Service
public interface AddressService {
    AddressEntity updateAddress(Long id, AddressEntity addressDetails);
    AddressEntity saveAddressForCurrentUser(AddressEntity address,String uid);
    AddressEntity getAddressByUserUid(String uid);
}
