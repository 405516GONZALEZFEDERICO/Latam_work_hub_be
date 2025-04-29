package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
    @Query("SELECT i FROM ImageEntity i WHERE i.space.id = :spaceId")
    List<ImageEntity> findBySpaceId(@Param("spaceId") Long spaceId);
}
