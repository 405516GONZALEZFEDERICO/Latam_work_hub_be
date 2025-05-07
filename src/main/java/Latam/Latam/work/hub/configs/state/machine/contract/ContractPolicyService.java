package Latam.Latam.work.hub.configs.state.machine.contract;

import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.enums.ContractStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class ContractPolicyService {

    // Período de notificación requerido (en días) antes de cancelar
    private static final int CANCELLATION_NOTICE_DAYS = 30;
    public static final int NOTICE_PERIOD_DAYS = 30;

    /**
     * Verifica si la cancelación cumple con la política de cancelación
     * @param contract Contrato a cancelar
     * @return true si se permite la cancelación
     */
    public boolean canCancelContract(RentalContractEntity contract) {
        LocalDate today = LocalDate.now();
        ContractStatus status = contract.getContractStatus();

        // Si está pendiente de pago, se puede cancelar inmediatamente
        if (status == ContractStatus.PENDING) {
            return true;
        }

        // Si está activo, verificar el período de notificación
        if (status == ContractStatus.ACTIVE) {
            // Calcular fecha efectiva de cancelación
            LocalDate effectiveCancellationDate = today.plusDays(CANCELLATION_NOTICE_DAYS);

            // Si la fecha de fin ya está más cerca que el período de notificación,
            // no tiene sentido cancelar (simplemente esperar a que termine)
            if (effectiveCancellationDate.isAfter(contract.getEndDate())) {
                return false;
            }

            return true;
        }

        return false;
    }

    /**
     * Calcula el monto de depósito a devolver basado en condiciones del contrato
     * @param contract Contrato cancelado
     * @return Porcentaje del depósito a devolver (0.0 - 1.0)
     */
    public double calculateDepositRefundPercentage(RentalContractEntity contract) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = contract.getStartDate();
        LocalDate endDate = contract.getEndDate();
        ContractStatus status = contract.getContractStatus();

        // Si nunca estuvo activo, devolver 100%
        if (status == ContractStatus.PENDING) {
            return 1.0;
        }

        // Si acaba de empezar (menos de 1 mes), retener 50%
        if (ChronoUnit.DAYS.between(startDate, today) < 30) {
            return 0.5;
        }

        // Si está cercano al fin (último 20% del período), no devolver nada
        double totalDuration = ChronoUnit.DAYS.between(startDate, endDate);
        double elapsed = ChronoUnit.DAYS.between(startDate, today);
        double completionPercentage = elapsed / totalDuration;

        if (completionPercentage > 0.8) {
            return 0.0;
        }

        // En otros casos, devolver parte proporcional al tiempo restante
        return 1.0 - completionPercentage;
    }

    /**
     * Verifica si el contrato es elegible para renovación automática
     * @param contract Contrato a verificar
     * @return true si es elegible
     */
    public boolean isEligibleForAutoRenewal(RentalContractEntity contract) {
        // Solo contratos activos cerca de su vencimiento
        if (contract.getContractStatus() != ContractStatus.ACTIVE) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = contract.getEndDate();

        // Verificar si está dentro del período de renovación (30 días antes del fin)
        long daysUntilExpiration = ChronoUnit.DAYS.between(today, endDate);
        return daysUntilExpiration <= 30 && daysUntilExpiration > 0;
    }
}