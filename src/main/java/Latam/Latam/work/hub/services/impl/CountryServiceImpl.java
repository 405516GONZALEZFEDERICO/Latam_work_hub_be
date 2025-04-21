package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.entities.CountryEntity;
import Latam.Latam.work.hub.repositories.CountryRepository;
import Latam.Latam.work.hub.services.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {
    private final CountryRepository countryRepository;

    @Override
    public List<CountryEntity> getAllCountries() {
        return countryRepository.findAll();
    }

}
