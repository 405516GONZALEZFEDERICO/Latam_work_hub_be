package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.CityEntity;
import Latam.Latam.work.hub.repositories.CityRepository;
import Latam.Latam.work.hub.services.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {
    private final CityRepository cityRepository;
    @Override
    public List<CityEntity> getCitiesByCountry(Long countryId) {
        return cityRepository.findByCountryId(countryId);
    }
}
