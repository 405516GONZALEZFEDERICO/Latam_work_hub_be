package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
}
