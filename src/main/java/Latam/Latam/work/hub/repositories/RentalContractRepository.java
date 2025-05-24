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
import java.util.Optional;

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
     * Encuentra contratos por espacio y estado
     */
    @Query("SELECT rc FROM RentalContractEntity rc WHERE rc.contractStatus = :status AND rc.startDate = :startDate")
    List<RentalContractEntity> findByContractStatusAndStartDate(@Param("status") ContractStatus status, @Param("startDate") LocalDate startDate);


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


    @Query(value = """
    SELECT DISTINCT rc
    FROM RentalContractEntity rc
    JOIN FETCH rc.space s
    JOIN FETCH s.owner o
    JOIN FETCH rc.tenant t
    WHERE (:startDate IS NULL OR rc.startDate BETWEEN :startDate AND :endDate)
    AND (:status IS NULL OR rc.contractStatus = :status)
""",
            countQuery = """
    SELECT COUNT(DISTINCT rc)
    FROM RentalContractEntity rc
    JOIN rc.space s
    JOIN s.owner o
    JOIN rc.tenant t
    WHERE (:startDate IS NULL OR rc.startDate BETWEEN :startDate AND :endDate)
    AND (:status IS NULL OR rc.contractStatus = :status)
""")
  Page<RentalContractEntity> findContractsForReport(
          @Param("startDate") LocalDate startDate,
          @Param("endDate") LocalDate endDate,
          @Param("status") ContractStatus status,
          Pageable pageable
  );

    // Query para Alertas simplificada
    @Query(value = "SELECT rc " +
            "FROM RentalContractEntity rc " +
            "JOIN FETCH rc.space s " +
            "JOIN FETCH rc.tenant t " +
            "WHERE rc.contractStatus = :activeStatus " +
            "AND rc.endDate BETWEEN :today AND :expiryLimitDate",
            countQuery = "SELECT COUNT(rc) FROM RentalContractEntity rc " + // No necesita todos los JOINs para el count
                    "WHERE rc.contractStatus = :activeStatus " +
                    "AND rc.endDate BETWEEN :today AND :expiryLimitDate")
    Page<RentalContractEntity> findExpiringContractsForAlerts( // Devuelve Entidades
                                                               @Param("today") LocalDate today,
                                                               @Param("expiryLimitDate") LocalDate expiryLimitDate,
                                                               @Param("activeStatus") ContractStatus activeStatus,
                                                               Pageable pageable
    );


    @Query("SELECT COUNT(rc) FROM RentalContractEntity rc " +
            "WHERE rc.space.owner.id = :providerId " +
            "AND rc.contractStatus = 'ACTIVE'")
    Long countActiveContractsByProviderId(@Param("providerId") Long providerId);

    @Query("SELECT COUNT(rc) FROM RentalContractEntity rc " +
            "WHERE rc.tenant.id = :tenantId " +
            "AND rc.contractStatus = 'ACTIVE'")
    Long countActiveContractsByTenantId(@Param("tenantId") Long tenantId);



    /**
     * Cuenta los contratos según su estado.
     * Usado para el KPI de "Contratos Activos".
     */
    @Query("SELECT COUNT(rc) FROM RentalContractEntity rc WHERE rc.contractStatus = :status")
    long countByContractStatus(@Param("status") ContractStatus status);

    /**
     * Cuenta los contratos activos o confirmados que vencerán en un rango de fechas futuro.
     * Usado para el KPI de "Contratos Próximos a Vencer".
     */
    @Query("SELECT COUNT(rc) FROM RentalContractEntity rc WHERE rc.contractStatus IN :statuses AND rc.endDate >= :currentDate AND rc.endDate <= :futureDate")
    long countActiveOrConfirmedContractsExpiringBetween(
            @Param("statuses") List<ContractStatus> statuses, // e.g., ACTIVE, CONFIRMED
            @Param("currentDate") LocalDate currentDate,
            @Param("futureDate") LocalDate futureDate
    );

    @Query("SELECT r.space.id, COUNT(r.id) " +
            "FROM RentalContractEntity r " +
            "WHERE r.space.id IN :spaceIds " +
            "AND r.contractStatus IN ('ACTIVE', 'PENDING','CANCELLED','CONFIRMED') " +
            "GROUP BY r.space.id")
    List<Object[]> countActiveRentalContractForSpaces(@Param("spaceIds") List<Long> spaceIds);


    @Query("""
    SELECT s.type.name as spaceType, COUNT(rc.id) as contractCount
    FROM RentalContractEntity rc
    JOIN rc.space s
    WHERE rc.contractStatus = 'ACTIVE'
    GROUP BY s.type.name
""")
    List<Object[]> findContractsCountBySpaceType();

    Optional<RentalContractEntity> findBySpaceIdAndContractStatusAndEndDateGreaterThanEqual(
            Long spaceId,
            ContractStatus status,
            LocalDate date
    );
}