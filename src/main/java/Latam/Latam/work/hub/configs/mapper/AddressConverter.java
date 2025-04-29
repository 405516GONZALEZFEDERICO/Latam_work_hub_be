package Latam.Latam.work.hub.configs.mapper;
import Latam.Latam.work.hub.dtos.common.SpaceDto;
import Latam.Latam.work.hub.entities.AddressEntity;
import Latam.Latam.work.hub.entities.CityEntity;
import Latam.Latam.work.hub.repositories.CityRepository;
import Latam.Latam.work.hub.repositories.CountryRepository;
import org.springframework.stereotype.Component;

@Component
public class AddressConverter {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    public AddressConverter(CityRepository cityRepository, CountryRepository countryRepository) {
        this.cityRepository = cityRepository;
        this.countryRepository = countryRepository;
    }

    public AddressEntity convertToAddressEntity(SpaceDto spaceDto) {
        AddressEntity addressEntity = new AddressEntity();

        addressEntity.setStreetName(spaceDto.getStreetName());
        addressEntity.setStreetNumber(spaceDto.getStreetNumber());
        addressEntity.setFloor(spaceDto.getFloor());
        addressEntity.setApartment(spaceDto.getApartment());
        addressEntity.setPostalCode(spaceDto.getPostalCode());

        if (spaceDto.getCityId() != null) {
            CityEntity city = cityRepository.findById(spaceDto.getCityId())
                    .orElseThrow(() -> new RuntimeException("Ciudad no encontrada"));
            addressEntity.setCity(city);
        }

        return addressEntity;
    }


}