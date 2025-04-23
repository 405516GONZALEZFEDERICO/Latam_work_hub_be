package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.AmenityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmenityRepository extends JpaRepository<AmenityEntity, Long> {
    AmenityEntity findByName(String name);
}
