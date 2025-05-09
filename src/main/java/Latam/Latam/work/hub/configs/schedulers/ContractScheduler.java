package Latam.Latam.work.hub.configs.schedulers;

import Latam.Latam.work.hub.services.RentalContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Programador de tareas relacionadas con contratos de alquiler
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractScheduler {

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
     * Sincroniza disponibilidad de espacios
     * Se ejecuta cada 6 horas
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void syncSpaceAvailability() {
        log.info("Sincronizando disponibilidad de espacios");
        try {
            rentalContractService.syncSpaceAvailability();
            log.info("Sincronización de disponibilidad completada");
        } catch (Exception e) {
            log.error("Error al sincronizar disponibilidad: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica contratos terminados y procesa devoluciones de depósitos
     * Se ejecuta cada día a las 4:00 AM
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void processCompletedContractsAndDeposits() {
        log.info("Iniciando verificación de contratos terminados y devoluciones de depósitos");
        try {
            rentalContractService.processCompletedContractsAndDeposits();
            log.info("Procesamiento de contratos terminados y depósitos completado");
        } catch (Exception e) {
            log.error("Error al procesar contratos terminados y depósitos: {}", e.getMessage(), e);
        }
    }

}