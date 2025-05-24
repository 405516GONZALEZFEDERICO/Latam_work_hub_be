package Latam.Latam.work.hub.configs.schedulers;

import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.services.RentalContractService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private  final RentalContractRepository rentalContractRepository;

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
            // Esta funcionalidad podría agregarse al servicio de contratos o facturas
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
     * Actualiza el estado de los espacios según contratos activos
     * Se ejecuta cada día a las 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateSpaceStatus() {
        log.info("Iniciando actualización de estado de espacios");
        try {
            rentalContractService.updateSpaceStatuses();
            log.info("Actualización de estado de espacios completada");
        } catch (Exception e) {
            log.error("Error al actualizar estados de espacios: {}", e.getMessage(), e);
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
     * Se ejecuta cada día a las 4:00 AM
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

    @Scheduled(fixedRate = 1000)
    public void updateConfirmedToActiveContracts() {
        try {
            rentalContractService.updateConfirmedToActiveContracts();
        } catch (Exception e) {
            log.error("Error al actualizar contratos CONFIRMADOS a ACTIVOS: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void updateSpacesAvailability() {

        List<SpaceEntity> spaces = spaceRepository.findAll();
        LocalDate today = LocalDate.now();

        for (SpaceEntity space : spaces) {
            Optional<RentalContractEntity> activeContract = rentalContractRepository.findBySpaceIdAndContractStatusAndEndDateGreaterThanEqual(
                    space.getId(),
                    ContractStatus.ACTIVE,
                    today
            );

            // Actualizar disponibilidad
            boolean shouldBeAvailable = !activeContract.isPresent();
            if (space.getAvailable() != shouldBeAvailable) {
                space.setAvailable(shouldBeAvailable);
                spaceRepository.save(space);
            }
        }
    }


}