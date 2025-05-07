package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.PendingInvoiceDto;
import Latam.Latam.work.hub.dtos.common.RentalContractDto;
import Latam.Latam.work.hub.dtos.common.RentalContractResponseDto;
import Latam.Latam.work.hub.dtos.common.ContractStateChangeDto;
import Latam.Latam.work.hub.dtos.common.InvoiceHistoryDto;
import Latam.Latam.work.hub.dtos.common.AutoRenewalDto;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.services.RentalContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rental-contracts")
@RequiredArgsConstructor
public class RentalContractController {

    private final RentalContractService rentalContractService;

    /**
     * Crea un nuevo contrato de alquiler
     * @param contractDto Datos del contrato
     * @return URL para realizar el pago inicial
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<String> createRentalContract(@RequestBody RentalContractDto contractDto) {
        String paymentUrl = rentalContractService.createRentalContract(contractDto);
        return ResponseEntity.ok(paymentUrl);
    }

    /**
     * Obtiene todos los contratos de alquiler de un usuario
     * @return Lista de contratos
     */
    @GetMapping("/user/{uid}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<RentalContractResponseDto>> getUserContracts(
            @PathVariable String uid,
            @RequestParam(required = false) ContractStatus status,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<RentalContractResponseDto> contracts = rentalContractService.getUserContracts(uid, status, pageable);
        return ResponseEntity.ok(contracts);
    }

    /**
     * Obtiene las facturas pendientes de un contrato
     * @param contractId ID del contrato
     * @return Lista de facturas pendientes
     */
    @GetMapping("/{contractId}/pending-invoices")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<PendingInvoiceDto>> getPendingInvoices(@PathVariable Long contractId) {
        List<PendingInvoiceDto> invoices = rentalContractService.getPendingInvoices(contractId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Obtiene la última factura de un contrato y genera el link de pago
     * @param contractId ID del contrato
     * @return URL para realizar el pago
     */
    @GetMapping("/{contractId}/current-invoice/payment")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<String> getCurrentInvoicePaymentLink(@PathVariable Long contractId) {
        String paymentUrl = rentalContractService.generateCurrentInvoicePaymentLink(contractId);
        return ResponseEntity.ok(paymentUrl);
    }

    /**
     * Cancelación de un contrato de alquiler
     * @param contractId ID del contrato
     * @return Mensaje de confirmación
     */
    @PostMapping("/{contractId}/cancel")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<String> cancelContract(@PathVariable Long contractId) {
        rentalContractService.cancelContract(contractId);
        return ResponseEntity.ok("Contrato cancelado exitosamente");
    }

    /**
     * Renovación de un contrato de alquiler
     * @param contractId ID del contrato
     * @param months Número de meses a renovar
     * @return URL para realizar el pago
     */
    @PostMapping("/{contractId}/renew")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<String> renewContract(@PathVariable Long contractId, @RequestParam Integer months) {
        String paymentUrl = rentalContractService.renewContract(contractId, months);
        return ResponseEntity.ok(paymentUrl);
    }

    /**
     * Obtiene el historial de cambios de estado de un contrato
     * @param contractId ID del contrato
     * @return Lista de cambios de estado
     */
    @GetMapping("/{contractId}/history")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<ContractStateChangeDto>> getContractHistory(@PathVariable Long contractId) {
        List<ContractStateChangeDto> history = rentalContractService.getContractHistory(contractId);
        return ResponseEntity.ok(history);
    }

    /**
     * Obtiene el historial completo de facturas de un contrato
     * @param contractId ID del contrato
     * @return Lista de facturas
     */
    @GetMapping("/{contractId}/invoices")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<InvoiceHistoryDto>> getContractInvoices(@PathVariable Long contractId) {
        List<InvoiceHistoryDto> invoices = rentalContractService.getContractInvoices(contractId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Configura la renovación automática de un contrato
     * @param contractId ID del contrato
     * @param autoRenewalDto Datos de configuración de renovación automática
     * @return Mensaje de confirmación
     */
    @PostMapping("/{contractId}/auto-renewal")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<String> setupAutoRenewal(
            @PathVariable Long contractId,
            @RequestBody AutoRenewalDto autoRenewalDto) {
        boolean success = rentalContractService.setupAutoRenewal(
                contractId,
                autoRenewalDto.isAutoRenew(),
                autoRenewalDto.getRenewalMonths()
        );
        return ResponseEntity.ok(success ? "Configuración de renovación automática actualizada" : "Error al configurar renovación automática");
    }

    /**
     * Obtiene los detalles de la política de cancelación para un contrato
     * @param contractId ID del contrato
     * @return Detalles de la política de cancelación
     */
    @GetMapping("/{contractId}/cancellation-policy")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Map<String, Object>> getCancellationPolicy(@PathVariable Long contractId) {
        Map<String, Object> policyDetails = rentalContractService.getCancellationPolicyDetails(contractId);
        return ResponseEntity.ok(policyDetails);
    }


    @PostMapping("/invoices/{invoiceId}/payment")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<String> generateInvoicePaymentLink(@PathVariable Long invoiceId) {
        String paymentUrl = rentalContractService.generateInvoicePaymentLink(invoiceId);
        return ResponseEntity.ok(paymentUrl);
    }
}
