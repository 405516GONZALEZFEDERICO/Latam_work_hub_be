package Latam.Latam.work.hub.configs.schedulers;

import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.services.RentalContractService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Programador de tareas relacionadas con contratos de alquiler
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractScheduler {
    private final SpaceRepository spaceRepository;
    private final BookingRepository bookingRepository;
    private final RentalContractRepository rentalContractRepository;
    private final RentalContractService rentalContractService;

    /**
     * Genera facturas mensuales para contratos activos
     * Se ejecuta cada día a las 2:00 AM para verificar si es el primer día del mes
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateMonthlyInvoices() {
        log.info("Iniciando generación programada de facturas mensuales");
        try {
            rentalContractService.generateMonthlyInvoices();
            log.info("Generación de facturas mensuales completada con éxito");
        } catch (Exception e) {
            log.error("Error al generar facturas mensuales: {}", e.getMessage(), e);
        }
    }

    /**
     * Verificar facturas vencidas y enviar notificaciones
     * Se ejecuta cada día a las 10:00 AM
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void checkOverdueInvoices() {
        log.info("Iniciando verificación de facturas vencidas");
        try {
            rentalContractService.checkOverdueInvoices();
            log.info("Verificación de facturas vencidas completada con éxito");
        } catch (Exception e) {
            log.error("Error al verificar facturas vencidas: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica contratos para renovación automática
     * Se ejecuta cada día a las 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkAutoRenewals() {
        log.info("Iniciando verificación de renovaciones automáticas");
        try {
            rentalContractService.processAutoRenewals();
            log.info("Verificación de renovaciones automáticas completada con éxito");
        } catch (Exception e) {
            log.error("Error al verificar renovaciones automáticas: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica contratos próximos a vencer para liberar espacios
     * Se ejecuta cada día a las 3:00 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkExpiringContracts() {
        log.info("Verificando contratos próximos a vencer");
        try {
            rentalContractService.processExpiringContracts();
            log.info("Verificación de contratos próximos a vencer completada");
        } catch (Exception e) {
            log.error("Error al verificar contratos próximos a vencer: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica contratos terminados y procesa devoluciones de depósitos
     * Se ejecuta cada día a las 6:00 AM
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void processCompletedContractsAndDeposits() {
        log.info("Iniciando verificación de contratos terminados y devoluciones de depósitos");
        try {
            rentalContractService.processCompletedContractsAndDeposits();
            log.info("Procesamiento de contratos terminados y depósitos completado");
        } catch (Exception e) {
            log.error("Error al procesar contratos terminados y depósitos: {}", e.getMessage(), e);
        }
    }

    /**
     * Ejecuta las renovaciones automáticas de contratos
     * Se ejecuta cada día a las 10:30 AM (después de checkAutoRenewals)
     */
    @Scheduled(cron = "0 30 10 * * ?")
    public void executeAutoRenewals() {
        log.info("Iniciando ejecución de renovaciones automáticas");
        try {
            rentalContractService.executeAutoRenewals();
            log.info("Ejecución de renovaciones automáticas completada con éxito");
        } catch (Exception e) {
            log.error("Error al ejecutar renovaciones automáticas: {}", e.getMessage(), e);
        }
    }

    /**
     * Actualiza estados de contratos y disponibilidad de espacios
     * Consolidada toda la lógica de actualización en un solo método
     * Se ejecuta cada 5 minutos para mantener la consistencia sin sobrecargar el sistema
     */
    @Scheduled(fixedRate = 5000) // 5 minutos
    @Transactional
    public void updateContractsAndSpacesStatus() {
        try {
            LocalDate today = LocalDate.now();
            log.debug("Iniciando actualización consolidada de contratos y espacios");

            // 1. Actualizar contratos CONFIRMADOS a ACTIVOS (incluye contratos atrasados)
            updateConfirmedToActiveContracts(today);

            // 2. Actualizar disponibilidad de todos los espacios
            updateSpacesAvailability(today);

            log.debug("Actualización consolidada completada");
        } catch (Exception e) {
            log.error("Error en actualización consolidada de contratos y espacios: {}", e.getMessage(), e);
        }
    }

    /**
     * Actualiza contratos CONFIRMADOS a ACTIVOS
     * Incluye contratos que debían empezar en el pasado (para casos donde la app no estaba corriendo)
     */
    private void updateConfirmedToActiveContracts(LocalDate today) {
        // Buscar contratos CONFIRMADOS que ya debían haber empezado (startDate <= today)
        List<RentalContractEntity> confirmedContracts = rentalContractRepository
                .findByContractStatusAndStartDateLessThanEqual(ContractStatus.CONFIRMED, today);

        if (confirmedContracts.isEmpty()) {
            return;
        }

        log.info("Actualizando {} contratos de CONFIRMADO a ACTIVO", confirmedContracts.size());

        for (RentalContractEntity contract : confirmedContracts) {
            contract.setContractStatus(ContractStatus.ACTIVE);
            rentalContractRepository.save(contract);
            log.debug("Contrato {} actualizado a ACTIVO (inicio: {})", 
                     contract.getId(), contract.getStartDate());
        }
    }

    /**
     * Actualiza la disponibilidad de todos los espacios basándose en contratos y reservas activas
     */
    private void updateSpacesAvailability(LocalDate today) {
        List<SpaceEntity> spaces = spaceRepository.findAll();

        for (SpaceEntity space : spaces) {
            // Verificar si hay contratos activos
            Optional<RentalContractEntity> activeContract = rentalContractRepository
                    .findBySpaceIdAndContractStatusAndEndDateGreaterThanEqual(
                            space.getId(),
                            ContractStatus.ACTIVE,
                            today
                    );

            // Verificar si hay reservas activas o confirmadas
            boolean hasActiveBookings = bookingRepository.existsBySpaceIdAndStatusIn(
                    space.getId(),
                    List.of(BookingStatus.ACTIVE, BookingStatus.CONFIRMED)
            );

            // El espacio debería estar disponible solo si NO hay contratos activos Y NO hay reservas activas/confirmadas
            boolean shouldBeAvailable = !activeContract.isPresent() && !hasActiveBookings;
            
            if (space.getAvailable() != shouldBeAvailable) {
                space.setAvailable(shouldBeAvailable);
                spaceRepository.save(space);
                log.debug("Espacio {} actualizado: available={} (contrato activo: {}, reservas activas: {})", 
                         space.getId(), shouldBeAvailable, activeContract.isPresent(), hasActiveBookings);
            }
        }
    }
}