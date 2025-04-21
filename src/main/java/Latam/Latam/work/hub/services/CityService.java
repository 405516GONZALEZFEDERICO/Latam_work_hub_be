package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.entities.CityEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CityService {
    List<CityEntity> getCitiesByCountry(Long countryId) ;
}
