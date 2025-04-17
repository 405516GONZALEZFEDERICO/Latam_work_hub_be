package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.RentalContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface RentalContractRepository extends JpaRepository<RentalContractEntity, Long> {
}
