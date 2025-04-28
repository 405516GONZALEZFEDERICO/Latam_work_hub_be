package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    @Query(value = "SELECT * FROM FACTURAS " +
            "WHERE invoice_number LIKE :prefix " +
            "ORDER BY invoice_number DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<InvoiceEntity> findTopByInvoiceNumberLikeOrderByInvoiceNumberDesc(@Param("prefix") String prefix);


    @Modifying
    @Query("UPDATE InvoiceEntity i SET i.status = :status WHERE i.id = :invoiceId")
    void updateStatus(@Param("invoiceId") Long invoiceId, @Param("status") InvoiceStatus status);
}
