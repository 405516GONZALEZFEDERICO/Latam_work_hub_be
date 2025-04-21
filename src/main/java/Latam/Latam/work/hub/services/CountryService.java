package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.entities.CountryEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface CountryService {
    List<CountryEntity> getAllCountries();
}
