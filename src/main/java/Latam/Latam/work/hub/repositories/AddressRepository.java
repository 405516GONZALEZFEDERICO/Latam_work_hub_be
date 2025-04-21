package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository  extends JpaRepository<AddressEntity, Long> {
}
