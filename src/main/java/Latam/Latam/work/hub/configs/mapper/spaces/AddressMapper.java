package Latam.Latam.work.hub.configs.mapper.spaces;

import Latam.Latam.work.hub.dtos.common.AddressDto;
import Latam.Latam.work.hub.entities.AddressEntity;
import Latam.Latam.work.hub.entities.CityEntity;
import Latam.Latam.work.hub.repositories.CityRepository;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    private final CityRepository cityRepository;

    public AddressMapper(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public AddressDto toDto(AddressEntity entity) {
        if (entity == null) {
            return null;
        }

        String cityName = null;
        String countryName = null;

        // Verifica si hay una ciudad asociada
        CityEntity city = entity.getCity();
        if (city != null) {
            cityName = city.getName();
            // Si la ciudad tiene un país asociado, obtén su nombre
            if (city.getCountry() != null) {
                countryName = city.getCountry().getName();
            }
        }

        return AddressDto.builder()
                .id(entity.getId())
                .streetName(entity.getStreetName())
                .streetNumber(entity.getStreetNumber())
                .floor(entity.getFloor())
                .apartment(entity.getApartment())
                .postalCode(entity.getPostalCode())
                .city(cityName)
                .country(countryName)
                .build();
    }
}