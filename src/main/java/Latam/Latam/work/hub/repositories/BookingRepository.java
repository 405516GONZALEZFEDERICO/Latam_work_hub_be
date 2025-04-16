package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
}
