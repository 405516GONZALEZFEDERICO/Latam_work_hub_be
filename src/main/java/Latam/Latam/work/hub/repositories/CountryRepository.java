package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<CountryEntity, Long> {
}
