package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.*;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface RentalContractService {
    Map<String, Object> getCancellationPolicyDetails(Long contractId);
    boolean setupAutoRenewal(Long contractId, boolean autoRenew, Integer renewalMonths);
    /**
     * Crea un nuevo contrato de alquiler y genera la primera factura
     * @param contractDto Datos del contrato
     * @return URL para realizar el pago inicial
     */
    String createRentalContract(RentalContractDto contractDto);

    /**
     * Obtiene todos los contratos de un usuario por su UID de Firebase
     * @param uid UID de Firebase del usuario
     * @return Lista de contratos
     */
//    List<RentalContractResponseDto> getUserContracts(String uid);

    /**
     * Obtiene las facturas pendientes de un contrato
     * @param contractId ID del contrato
     * @return Lista de facturas pendientes
     */
    List<PendingInvoiceDto> getPendingInvoices(Long contractId);

    /**
     * Genera el link de pago para la factura actual de un contrato
     * @param contractId ID del contrato
     * @return URL para realizar el pago
     */
    String generateCurrentInvoicePaymentLink(Long contractId);

    /**
     * Obtiene el historial de cambios de estado de un contrato
     * @param contractId ID del contrato
     * @return Lista de cambios de estado
     */
    List<ContractStateChangeDto> getContractHistory(Long contractId);

    /**
     * Obtiene el historial completo de facturas de un contrato
     * @param contractId ID del contrato
     * @return Lista de facturas
     */
    List<InvoiceHistoryDto> getContractInvoices(Long contractId);

    void checkOverdueInvoices();
    /**
     * Genera las facturas mensuales para todos los contratos activos
     * Este método será llamado por un scheduler
     */
    void generateMonthlyInvoices();

    /**
     * Cancela un contrato de alquiler
     * @param contractId ID del contrato
     */
    void cancelContract(Long contractId);

    /**
     * Renueva un contrato de alquiler
     * @param contractId ID del contrato
     * @param months Número de meses a renovar
     * @return URL para realizar el pago
     */
    String renewContract(Long contractId, Integer months);
    void processAutoRenewals();
    void syncSpaceAvailability();
    void updateSpaceStatuses();
    void processExpiringContracts();



    Page<RentalContractResponseDto> getUserContracts(
            String uid,
            ContractStatus status,
            Pageable pageable);
    String generateInvoicePaymentLink(Long invoiceId);
    void processCompletedContractsAndDeposits();
}