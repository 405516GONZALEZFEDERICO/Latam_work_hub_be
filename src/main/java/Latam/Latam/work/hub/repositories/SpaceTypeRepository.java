package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.SpaceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpaceTypeRepository extends JpaRepository<SpaceTypeEntity, Long> {
}
