package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.entities.AddressEntity;
import Latam.Latam.work.hub.entities.CityEntity;
import Latam.Latam.work.hub.entities.CountryEntity;
import Latam.Latam.work.hub.services.AddressService;
import Latam.Latam.work.hub.services.CityService;
import Latam.Latam.work.hub.services.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class AdressController {

    private final AddressService addressService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private CityService cityService;

    @GetMapping("/countries")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<List<CountryEntity>> getAllCountries() {
        List<CountryEntity> countries = countryService.getAllCountries();
        return new ResponseEntity<>(countries, HttpStatus.OK);
    }

    @GetMapping("/cities/country/{countryId}")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<List<CityEntity>> getCitiesByCountry(@PathVariable Long countryId) {
        List<CityEntity> cities = cityService.getCitiesByCountry(countryId);
        return new ResponseEntity<>(cities, HttpStatus.OK);
    }

    @PostMapping("/addresses")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<AddressEntity> saveAddress(@RequestBody AddressEntity address,@RequestParam String uid) {
        AddressEntity savedAddress = addressService.saveAddressForCurrentUser(address,uid);
        return new ResponseEntity<>(savedAddress, HttpStatus.CREATED);
    }

    @PutMapping("/addresses/{id}")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<AddressEntity> updateAddress(@PathVariable Long id, @RequestBody AddressEntity address) {
        AddressEntity updatedAddress = addressService.updateAddress(id, address);
        return new ResponseEntity<>(updatedAddress, HttpStatus.OK);
    }

    @GetMapping("/addresses/")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<Optional<AddressEntity>>getAddressById(@RequestParam String uid ){
        return ResponseEntity.ok(Optional.ofNullable(addressService.getAddressByUserUid(uid))) ;
    }
}