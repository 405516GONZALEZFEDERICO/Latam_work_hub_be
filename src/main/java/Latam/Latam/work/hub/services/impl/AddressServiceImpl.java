package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.AddressEntity;
import Latam.Latam.work.hub.entities.CityEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.repositories.AddressRepository;
import Latam.Latam.work.hub.repositories.CityRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.AddressService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;

    @Override
    public AddressEntity updateAddress(Long id, AddressEntity addressDetails) {
        Optional<AddressEntity> addressOpt = addressRepository.findById(id);
        if (addressOpt.isPresent()) {
            AddressEntity address = addressOpt.get();
            address.setStreetName(addressDetails.getStreetName());
            address.setStreetNumber(addressDetails.getStreetNumber());
            address.setFloor(addressDetails.getFloor());
            address.setApartment(addressDetails.getApartment());
            address.setPostalCode(addressDetails.getPostalCode());
            address.setCity(addressDetails.getCity());
            return addressRepository.save(address);
        }
        throw new EntityNotFoundException("No se encontro la direccion solicitada");
    }



    @Override
    public AddressEntity saveAddressForCurrentUser(AddressEntity address, String uid) {
        UserEntity user = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        if (address.getCity() != null && address.getCity().getId() == 0) {
            CityEntity savedCity = cityRepository.save(address.getCity());
            address.setCity(savedCity);
        }

        AddressEntity savedAddress = addressRepository.save(address);

        user.setAddress(savedAddress);
        userRepository.save(user);

        return savedAddress;
    }

    @Override
    public AddressEntity getAddressByUserUid(String uid) {
        UserEntity user = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con uid: " + uid));

        AddressEntity address = user.getAddress();

        if (address == null) {
            throw new EntityNotFoundException("No se encontró dirección para el usuario con uid: " + uid);
        }

        return address;
    }
}
