package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<CityEntity, Long> {
    CityEntity findByName(String name);
    List<CityEntity>findByCountryId(Long countryId);
}
