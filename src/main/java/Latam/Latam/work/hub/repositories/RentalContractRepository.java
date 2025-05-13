package Latam.Latam.work.hub.repositories;

import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContractEntity, Long> {

    /**
     * Encuentra todos los contratos de un inquilino por su ID
     */
    List<RentalContractEntity> findByTenantId(Long tenantId);

    /**
     * Encuentra todos los contratos activos que necesitan facturación mensual
     */
    @Query("SELECT rc FROM RentalContractEntity rc WHERE rc.contractStatus = 'ACTIVE' " +
            "AND rc.endDate >= :currentDate")
    List<RentalContractEntity> findActiveContractsForBilling(LocalDate currentDate);

    /**
     * Encuentra contratos por espacio
     */
    List<RentalContractEntity> findBySpaceId(Long spaceId);

    /**
     * Encuentra contratos por usuario (Firebase UID)
     */
    @Query("SELECT rc FROM RentalContractEntity rc " +
            "JOIN FETCH rc.tenant u " +
            "JOIN FETCH rc.space s " +
            "JOIN FETCH s.owner " +
            "JOIN FETCH s.type " +
            "JOIN FETCH s.address.city c " +
            "JOIN FETCH c.country " +
            "WHERE u.firebaseUid = :uid " +
            "AND (:status IS NULL OR rc.contractStatus = :status)")
    Page<RentalContractEntity> findByUserFirebaseUidAndStatus(
            @Param("uid") String uid,
            @Param("status") ContractStatus status,
            Pageable pageable);

    /**
     * Encuentra contratos que están por vencer en los próximos días
     */
    @Query("SELECT rc FROM RentalContractEntity rc WHERE rc.contractStatus = 'ACTIVE' " +
            "AND rc.endDate BETWEEN :startDate AND :endDate")
    List<RentalContractEntity> findContractsAboutToExpire(LocalDate startDate, LocalDate endDate);

    @Query("SELECT c FROM RentalContractEntity c " +
            "WHERE c.autoRenewal = true " +
            "AND c.contractStatus = 'ACTIVE' " +
            "AND c.endDate BETWEEN :today AND :threshold " +
            "ORDER BY c.endDate ASC")
    List<RentalContractEntity> findContractsForAutoRenewal(LocalDate today, LocalDate threshold);
    /**
     * Encuentra contratos por estado
     */
    List<RentalContractEntity> findByContractStatus(ContractStatus status);

    @Query("SELECT c FROM RentalContractEntity c WHERE c.endDate <= :threshold " +
            "AND c.contractStatus = 'ACTIVE'")
    List<RentalContractEntity> findExpiringContracts(@Param("threshold") LocalDate threshold);


    boolean existsBySpaceAndContractStatusIn(SpaceEntity space, Collection<ContractStatus> statuses);

    @Query("SELECT c FROM RentalContractEntity c " +
            "WHERE c.endDate = :endDate " +
            "AND c.contractStatus = :status " +
            "AND c.depositRefounded = :depositRefunded")
    List<RentalContractEntity> findByEndDateAndContractStatusAndDepositRefunded(
            LocalDate endDate,
            ContractStatus status,
            boolean depositRefunded);

    @Query("SELECT c FROM RentalContractEntity c " +
            "WHERE c.autoRenewal = true " +
            "AND c.contractStatus = 'ACTIVE' " +
            "AND c.endDate BETWEEN :today AND :threshold")
    List<RentalContractEntity> findContractsForAutoRenewalExecution(
            LocalDate today,
            LocalDate threshold);
}