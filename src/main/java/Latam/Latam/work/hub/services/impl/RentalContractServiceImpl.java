package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.configs.state.machine.contract.ContractPolicyService;
import Latam.Latam.work.hub.configs.state.machine.contract.NotificationRetryService;
import Latam.Latam.work.hub.configs.state.machine.contract.StateMachineContract;
import Latam.Latam.work.hub.dtos.common.InvoiceAmenityDto;
import Latam.Latam.work.hub.dtos.common.InvoiceHistoryDto;
import Latam.Latam.work.hub.dtos.common.PendingInvoiceDto;
import Latam.Latam.work.hub.dtos.common.RentalContractDto;
import Latam.Latam.work.hub.dtos.common.RentalContractResponseDto;
import Latam.Latam.work.hub.dtos.common.isAutoRenewalDto;
import Latam.Latam.work.hub.dtos.common.PaymentRequestDto;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.enums.InvoiceType;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.BookingService;
import Latam.Latam.work.hub.services.InvoiceService;
import Latam.Latam.work.hub.services.MailService;
import Latam.Latam.work.hub.services.RentalContractService;
import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static Latam.Latam.work.hub.configs.state.machine.contract.ContractPolicyService.NOTICE_PERIOD_DAYS;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalContractServiceImpl implements RentalContractService {

    private final RentalContractRepository rentalContractRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final MercadoPagoService mercadoPagoService;
    private final MailService mailService;
    // Nuevos servicios
    private final StateMachineContract stateMachine;
    private final ContractPolicyService policyService;
    private final NotificationRetryService notificationRetryService;
    private final BookingService bookingService;
    @Override
    @Transactional
    public void checkOverdueInvoices() {
        LocalDate today = LocalDate.now();

        // Buscar facturas vencidas no pagadas
        List<InvoiceEntity> overdueInvoices = invoiceRepository.findByDueDateBeforeAndPaidFalse(today);

        for (InvoiceEntity invoice : overdueInvoices) {
            RentalContractEntity contract = (RentalContractEntity) invoice.getRentalContract();
            UserEntity tenant = contract.getTenant();
            UserEntity owner = contract.getSpace().getOwner();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String formattedDueDate = invoice.getDueDate().format(formatter);

            // Notificar al inquilino
            notificationRetryService.sendEmailWithRetry(
                    () -> mailService.sendOverdueInvoiceNotification(
                            tenant.getEmail(),
                            tenant.getName(),
                            contract.getSpace().getName(),
                            invoice.getTotalAmount().toString(),
                            formattedDueDate
                    ),
                    "No se pudo notificar al inquilino sobre la factura vencida"
            );

            // Notificar al propietario
            notificationRetryService.sendEmailWithRetry(
                    () -> mailService.sendOwnerOverdueInvoiceNotification(
                            owner.getEmail(),
                            owner.getName(),
                            contract.getSpace().getName(),
                            tenant.getName(),
                            invoice.getTotalAmount().toString(),
                            formattedDueDate
                    ),
                    "No se pudo notificar al propietario sobre la factura vencida"
            );
        }
    }
    @Override
    @Transactional
    public String createRentalContract(RentalContractDto contractDto) {
        try {
            // Obtener espacio y usuario
            SpaceEntity space = spaceRepository.findById(contractDto.getSpaceId())
                    .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));

            UserEntity tenant = userRepository.findByFirebaseUid(contractDto.getUid())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar disponibilidad del espacio
            if (!space.getAvailable()) {
                throw new RuntimeException("El espacio no está disponible para alquiler");
            }

            // Validar solapamiento de contratos
            validateContractOverlap(space.getId(), contractDto.getStartDate(), 
                contractDto.getStartDate().plusMonths(contractDto.getDurationMonths()));

            // Validar montos mínimos/máximos
            validateContractAmounts(contractDto.getMonthlyAmount(), contractDto.getDepositAmount());

            // Validar período mínimo de contrato
            validateContractDuration(contractDto.getDurationMonths());

            // Crear entidad de contrato
            RentalContractEntity contract = new RentalContractEntity();
            contract.setSpace(space);
            contract.setTenant(tenant);
            contract.setStartDate(contractDto.getStartDate());
            contract.setEndDate(contractDto.getStartDate().plusMonths(contractDto.getDurationMonths()));
            contract.setMonthlyAmount(contractDto.getMonthlyAmount());
            contract.setDepositAmount(contractDto.getDepositAmount());
            contract.setDurationMonths(Double.valueOf(contractDto.getDurationMonths()));
            contract.setContractStatus(ContractStatus.DRAFT);
            contract.setDepositAmount(contractDto.getDepositAmount());


            // Guardar contrato
            RentalContractEntity savedContract = rentalContractRepository.saveAndFlush(contract);

            // Usar máquina de estados para transicionar
            stateMachine.transition(savedContract, ContractStatus.PENDING);
            rentalContractRepository.save(savedContract);

            // Establecer monto inicial (incluye depósito + primer mes + amenities)
            double initialAmount = contractDto.getMonthlyAmount() + contractDto.getDepositAmount();
            
            // Agregar precio de amenities si están seleccionadas
            if (contractDto.getAmenitiesPrice() != null && contractDto.getAmenitiesPrice() > 0) {
                initialAmount += contractDto.getAmenitiesPrice();
                log.info("Contrato con amenities - Mensual: ${}, Depósito: ${}, Amenities: ${}, Total inicial: ${}", 
                        contractDto.getMonthlyAmount(), contractDto.getDepositAmount(), 
                        contractDto.getAmenitiesPrice(), initialAmount);
            } else {
                log.info("Contrato sin amenities - Mensual: ${}, Depósito: ${}, Total inicial: ${}", 
                        contractDto.getMonthlyAmount(), contractDto.getDepositAmount(), initialAmount);
            }
            
            savedContract.setAmount(initialAmount);
            log.info("Monto establecido en contrato ID {}: ${}", savedContract.getId(), savedContract.getAmount());

            // Generar factura inicial
            String paymentUrl = invoiceService.createInvoice(savedContract);
            log.info("Factura inicial creada para contrato ID {}", savedContract.getId());

            // Verificar el monto de la factura creada
            InvoiceEntity createdInvoice = getCurrentInvoice(savedContract.getId());
            if (createdInvoice != null) {
                log.info("Factura creada - ID: {}, TotalAmount: ${}, Status: {}", 
                        createdInvoice.getId(), createdInvoice.getTotalAmount(), createdInvoice.getStatus());
            } else {
                log.warn("No se pudo obtener la factura recién creada para el contrato ID {}", savedContract.getId());
            }

            // Notificar al propietario
            notifyOwnerAboutNewContract(savedContract);

            return paymentUrl;
        } catch (Exception e) {
            log.error("Error al crear contrato de alquiler: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear el contrato de alquiler: " + e.getMessage(), e);
        }
    }

@Override
public isAutoRenewalDto isAutoRenewal(Long contractId) {
    Optional<RentalContractEntity> rentalContractEntity = this.rentalContractRepository.findById(contractId);
    if (!rentalContractEntity.isPresent()) {
        throw new RuntimeException("El contrato de alquiler no existe");
    }
    boolean isAutoRenewal = rentalContractEntity.get().isAutoRenewal();
    if (isAutoRenewal) {
        return new isAutoRenewalDto(true, rentalContractEntity.get().getRenewalMonths());
    } else {
        return new isAutoRenewalDto(null, null);
    }
}

    @Override
    public Boolean updateIsAutoRenewal(Long contractId, Boolean isAutoRenewal) {
        Optional<RentalContractEntity> rentalContractEntity=this.rentalContractRepository.findById(contractId);
        if(rentalContractEntity.isEmpty()){
            throw new RuntimeException("El contrato de alquiler no existe");
        }
        rentalContractEntity.get().setAutoRenewal(isAutoRenewal);
        rentalContractRepository.save(rentalContractEntity.get());
        mailService.sendOwnerAndTenantAutoRenewalNotification(
                rentalContractEntity.get().getTenant().getEmail(),
                rentalContractEntity.get().getSpace().getOwner().getEmail(),
                rentalContractEntity.get().getSpace().getName(),
                isAutoRenewal
        );
        return Boolean.TRUE;
    }

    /**
     * Valida que no haya solapamiento de contratos para el mismo espacio
     */
    private void validateContractOverlap(Long spaceId, LocalDate startDate, LocalDate endDate) {
        List<RentalContractEntity> existingContracts = rentalContractRepository.findBySpaceId(spaceId);
        
        for (RentalContractEntity existingContract : existingContracts) {
            if (existingContract.getContractStatus() == ContractStatus.ACTIVE ||
                existingContract.getContractStatus() == ContractStatus.PENDING ||
                    existingContract.getContractStatus()==ContractStatus.CONFIRMED)  {
                
                if ((startDate.isBefore(existingContract.getEndDate()) && 
                     endDate.isAfter(existingContract.getStartDate()))) {
                    throw new RuntimeException("El espacio ya tiene un contrato activo para el período solicitado");
                }
            }
        }
    }

    /**
     * Valida los montos del contrato
     */
    private void validateContractAmounts(double monthlyAmount, double depositAmount) {
        // Monto mensual mínimo: 100
        if (monthlyAmount < 100) {
            throw new RuntimeException("El monto mensual debe ser al menos $100");
        }

        // Depósito máximo: 3 meses de alquiler
        if (depositAmount > monthlyAmount * 3) {
            throw new RuntimeException("El depósito no puede exceder 3 meses de alquiler");
        }

        // Depósito mínimo: 1 mes de alquiler
        if (depositAmount < monthlyAmount) {
            throw new RuntimeException("El depósito debe ser al menos 1 mes de alquiler");
        }
    }

    /**
     * Valida la duración del contrato
     */
    private void validateContractDuration(int months) {
        // Duración mínima: 3 meses
        if (months < 3) {
            throw new RuntimeException("La duración mínima del contrato es de 3 meses");
        }

        // Duración máxima: 24 meses
        if (months > 24) {
            throw new RuntimeException("La duración máxima del contrato es de 24 meses");
        }
    }

    /**
     * Obtiene los detalles de la política de cancelación para un contrato específico
     * @param contractId ID del contrato
     * @return Mapa con los detalles de la política de cancelación
     */
    @Override
    public Map<String, Object> getCancellationPolicyDetails(Long contractId) {
        // Retrieve the contract
        RentalContractEntity contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));
        if(contract.getContractStatus().equals(ContractStatus.PENDING)){
            throw new RuntimeException("El contrato no está en un estado que permita obtener la política de cancelación");
        }
        // Get current date for calculations
        LocalDate today = LocalDate.now();

        // Initialize response map
        Map<String, Object> policyDetails = new HashMap<>();

        // Basic policy rules
        // Since CANCELLATION_NOTICE_DAYS might be private, we'll use a constant value of 30 days
        // which matches what's in the ContractPolicyService
        policyDetails.put("noticePeriodDays", ContractPolicyService.NOTICE_PERIOD_DAYS);
        policyDetails.put("contractStatus", contract.getContractStatus().toString());

        // Calculate if cancellation is allowed based on policy
        boolean canCancel = policyService.canCancelContract(contract);
        policyDetails.put("canCancel", canCancel);

        // Calculate deposit refund information
        double depositRefundPercentage = policyService.calculateDepositRefundPercentage(contract);
        double refundAmount = contract.getDepositAmount() * depositRefundPercentage;

        policyDetails.put("depositAmount", contract.getDepositAmount());
        policyDetails.put("refundPercentage", depositRefundPercentage);
        policyDetails.put("refundAmount", refundAmount);

        // Add date information
        policyDetails.put("contractStartDate", contract.getStartDate());
        policyDetails.put("contractEndDate", contract.getEndDate());

        // Calculate effective cancellation date
        LocalDate effectiveCancellationDate = today.plusDays(NOTICE_PERIOD_DAYS);
        policyDetails.put("effectiveCancellationDate", effectiveCancellationDate);

        // If unable to cancel, provide reason
        if (!canCancel) {
            String reason;
            if (contract.getContractStatus() != ContractStatus.ACTIVE &&
                    contract.getContractStatus() != ContractStatus.PENDING) {
                reason = "El contrato no está en un estado que permita cancelación";
            } else if (effectiveCancellationDate.isAfter(contract.getEndDate())) {
                reason = "El período de preaviso supera la fecha de finalización del contrato";
            } else {
                reason = "No se puede cancelar el contrato según la política actual";
            }
            policyDetails.put("cancellationRestrictionReason", reason);
        }

        // Calculate the remaining time on the contract
        long daysRemaining = ChronoUnit.DAYS.between(today, contract.getEndDate());
        policyDetails.put("daysRemaining", daysRemaining);

        // Calculate what percentage of the contract period has been completed
        long totalContractDays = ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate());
        double completionPercentage = (double) (totalContractDays - daysRemaining) / totalContractDays;
        policyDetails.put("completionPercentage", completionPercentage);

        return policyDetails;
    }

    /**
     * Notifica al propietario sobre el nuevo contrato
     */
    private void notifyOwnerAboutNewContract(RentalContractEntity contract) {
        UserEntity owner = contract.getSpace().getOwner();
        UserEntity tenant = contract.getTenant();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        final String ownerEmail = owner.getEmail();
        final String ownerName = owner.getName();
        final String spaceName = contract.getSpace().getName();
        final String tenantName = tenant.getName();
        final String startDate = contract.getStartDate().format(formatter);
        final String endDate = contract.getEndDate().format(formatter);
        final String monthlyAmount = contract.getMonthlyAmount().toString();

        notificationRetryService.sendEmailWithRetry(
                () -> mailService.sendNewContractNotificationToOwner(
                        ownerEmail, ownerName, spaceName, tenantName, startDate, endDate, monthlyAmount
                ),
                "No se pudo notificar al propietario sobre el nuevo contrato ID: " + contract.getId()
        );
    }


    public List<PendingInvoiceDto> getPendingInvoices(Long contractId) {
        return invoiceRepository.findByRentalContractIdAndStatus(contractId, InvoiceStatus.ISSUED)
                .stream()
                .map(invoice -> new PendingInvoiceDto(
                        invoice.getId(),
                        invoice.getDescription(),
                        invoice.getTotalAmount(),
                        invoice.getIssueDate(),
                        invoice.getDueDate(),
                        invoice.getStatus().toString(),
                        invoice.getInvoiceNumber(),
                        invoice.getRentalContract().getSpace().getAmenities().stream()
                                .map(amenity -> new InvoiceAmenityDto(
                                        amenity.getId(),
                                        amenity.getName(),
                                        amenity.getPrice()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la factura actual (última factura) de un contrato
     */
    private InvoiceEntity getCurrentInvoice(Long contractId) {
        return invoiceRepository.findTopByRentalContractIdOrderByIssueDateDesc(contractId)
                .orElse(null);
    }

    @Override
    @Transactional
    public String generateCurrentInvoicePaymentLink(Long contractId, PaymentRequestDto paymentRequest) {
        RentalContractEntity contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));

        // Obtener la factura más reciente
        InvoiceEntity currentInvoice = getCurrentInvoice(contractId);
        if (currentInvoice == null) {
            throw new RuntimeException("No hay facturas pendientes para este contrato");
        }

        // Usar el monto total calculado por el frontend (incluye amenities)
        Double totalAmountFromFrontend = paymentRequest.getTotalAmount();
        if (totalAmountFromFrontend == null || totalAmountFromFrontend <= 0) {
            throw new RuntimeException("El monto total debe ser mayor a cero");
        }

        log.info("=== INICIO generateCurrentInvoicePaymentLink ===");
        log.info("Contrato ID: {}, Factura ID: {}", contractId, currentInvoice.getId());
        log.info("Monto recibido del frontend: ${}", totalAmountFromFrontend);
        log.info("Estado del contrato: {}, Tipo de factura: {}", contract.getContractStatus(), currentInvoice.getType());

        // VERIFICAR SI ES LA FACTURA INICIAL (incluye depósito)
        boolean isInitialInvoice = (contract.getContractStatus() == ContractStatus.PENDING || 
                                   contract.getContractStatus() == ContractStatus.CONFIRMED) &&
                                   currentInvoice.getType() == InvoiceType.CONTRACT;
        
        // El frontend ya calcula e incluye TODOS los montos (mensual + depósito + amenities)
        // No necesitamos sumar nada adicional
        Double finalAmount = totalAmountFromFrontend;
        
        if (isInitialInvoice) {
            log.info("Factura inicial detectada - Usando monto completo del frontend: ${} (incluye mensual + depósito + amenities)", 
                    finalAmount);
        } else {
            log.info("Factura mensual - Usando monto del frontend: ${}", finalAmount);
        }

        // Validación de rango esperado
        Double expectedMinAmount = contract.getMonthlyAmount();
        Double expectedMaxAmount = contract.getMonthlyAmount() + contract.getDepositAmount() + 1000; // +1000 para amenities
        
        if (isInitialInvoice && (finalAmount < expectedMinAmount || finalAmount > expectedMaxAmount)) {
            log.warn("ADVERTENCIA: Monto de factura fuera del rango esperado para factura inicial. " +
                    "Monto: ${}, Rango esperado: ${} - ${}", 
                    finalAmount, expectedMinAmount, expectedMaxAmount);
        }

        try {
            // Actualizar la factura con el monto correcto del frontend
            currentInvoice.setTotalAmount(finalAmount);
            currentInvoice.setStatus(InvoiceStatus.ISSUED);
            
            // Actualizar descripción si se proporciona
            String description = paymentRequest.getDescription();
            if (description != null && !description.trim().isEmpty()) {
                currentInvoice.setDescription(description);
            }
            
            invoiceRepository.save(currentInvoice);
            log.info("Factura actualizada - ID: {}, TotalAmount: ${}, Status: {}", 
                    currentInvoice.getId(), currentInvoice.getTotalAmount(), currentInvoice.getStatus());

            // Establecer el monto en el contrato para cumplir con Billable
            contract.setAmount(finalAmount);

            String buyerEmail = contract.getTenant().getEmail();
            String sellerEmail = contract.getSpace().getOwner().getEmail();

            log.info("Generando preferencia de pago - Monto: {}, Comprador: {}, Vendedor: {}", 
                    finalAmount, buyerEmail, sellerEmail);

            String paymentUrl = mercadoPagoService.createInvoicePaymentPreference(
                    currentInvoice.getId(),
                    currentInvoice.getDescription() != null ? currentInvoice.getDescription() : 
                        (isInitialInvoice ? "Pago inicial contrato: " : "Pago mensual de alquiler: ") + contract.getSpace().getName(),
                    BigDecimal.valueOf(finalAmount),
                    buyerEmail,
                    sellerEmail
            );
            
            log.info("=== FIN generateCurrentInvoicePaymentLink - URL generada exitosamente ===");
            return paymentUrl;
            
        } catch (MPException | MPApiException e) {
            log.error("Error al generar el link de pago: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el link de pago: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void generateMonthlyInvoices() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        // Verificar si es el primer día del mes para generar facturas
        if (today.equals(firstDayOfMonth)) {
            List<RentalContractEntity> activeContracts = rentalContractRepository.findActiveContractsForBilling(today);

            for (RentalContractEntity contract : activeContracts) {
                try {
                    // Verificar si ya existe una factura para este mes
                    LocalDateTime startOfMonth = firstDayOfMonth.atStartOfDay();
                    LocalDateTime endOfMonth = firstDayOfMonth.plusMonths(1).atStartOfDay().minusSeconds(1);

                    boolean invoiceExists = invoiceRepository.existsByRentalContractIdAndIssueDateBetween(
                            contract.getId(), startOfMonth, endOfMonth);

                    if (!invoiceExists) {
                        createMonthlyInvoice(contract);
                    }
                } catch (Exception e) {
                    log.error("Error al generar factura mensual para contrato ID {}: {}",
                            contract.getId(), e.getMessage(), e);
                }
            }
        }

        // Enviar recordatorios de pago para facturas que vencen en 5 días
        sendPaymentReminders();

        // Verificar contratos por vencer
        checkExpiringContracts();
    }

    /**
     * Crea una factura mensual para un contrato usando el InvoiceService
     */
    private void createMonthlyInvoice(RentalContractEntity contract) {
        try {
            // Establecer el monto mensual en el contrato para cumplir con Billable
            contract.setAmount(contract.getMonthlyAmount());

            // Usar el invoiceService para crear la factura
            String paymentUrl = invoiceService.createInvoice(contract);

            // Obtener la factura recién creada para notificar al inquilino
            InvoiceEntity savedInvoice = getCurrentInvoice(contract.getId());

            // Actualizamos la descripción y la fecha de vencimiento específicas para facturas mensuales
            if (savedInvoice != null) {
                savedInvoice.setDescription("Alquiler mensual - " + contract.getSpace().getName());
                savedInvoice.setDueDate(LocalDateTime.now().plusDays(10)); // 10 días para pago mensual
                invoiceRepository.save(savedInvoice);

                // Notificar al inquilino sobre la nueva factura
                notifyTenantAboutNewInvoice(savedInvoice);
            }
        } catch (MPException | MPApiException e) {
            log.error("Error al crear factura mensual: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear factura mensual: " + e.getMessage(), e);
        }
    }

    /**
     * Notifica al inquilino sobre la nueva factura
     */
    private void notifyTenantAboutNewInvoice(InvoiceEntity invoice) {
        RentalContractEntity contract = (RentalContractEntity) invoice.getRentalContract();
        UserEntity tenant = contract.getTenant();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        mailService.sendNewInvoiceNotification(
                tenant.getEmail(),
                tenant.getName(),
                contract.getSpace().getName(),
                invoice.getInvoiceNumber(),
                invoice.getDueDate().format(formatter),
                invoice.getTotalAmount().toString()
        );
    }

    /**
     * Envía recordatorios de pago para facturas que vencen pronto
     */
    private void sendPaymentReminders() {
        LocalDateTime reminderDate = LocalDateTime.now().plusDays(5);
        List<InvoiceEntity> expiringInvoices = invoiceRepository.findInvoicesExpiringOn(reminderDate.toLocalDate());

        for (InvoiceEntity invoice : expiringInvoices) {
            if (invoice.getStatus() != InvoiceStatus.PAID) {
                RentalContractEntity contract = (RentalContractEntity) invoice.getRentalContract();
                UserEntity tenant = contract.getTenant();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                mailService.sendPaymentReminderEmail(
                        tenant.getEmail(),
                        tenant.getName(),
                        contract.getSpace().getName(),
                        invoice.getInvoiceNumber(),
                        invoice.getDueDate().format(formatter),
                        invoice.getTotalAmount().toString()
                );
            }
        }
    }

    /**
     * Verifica contratos que están por vencer
     */
    private void checkExpiringContracts() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);

        List<RentalContractEntity> expiringContracts = rentalContractRepository.findContractsAboutToExpire(
                today, thirtyDaysFromNow);

        for (RentalContractEntity contract : expiringContracts) {
            UserEntity tenant = contract.getTenant();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Notificar al inquilino que su contrato está por vencer
            mailService.sendContractExpirationReminder(
                    tenant.getEmail(),
                    tenant.getName(),
                    contract.getSpace().getName(),
                    contract.getEndDate().format(formatter)
            );
        }
    }

    @Override
    @Transactional
    public void cancelContract(Long contractId) {
        RentalContractEntity contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));

        // Verificar política de cancelación
        if (!policyService.canCancelContract(contract)) {
            throw new RuntimeException("No se puede cancelar este contrato debido a la política de cancelación");
        }

        try {
            // Usar máquina de estados para la transición
            stateMachine.transition(contract, ContractStatus.CANCELLED);
            rentalContractRepository.save(contract);

            // Marcar el espacio como disponible
            SpaceEntity space = contract.getSpace();
            space.setAvailable(true);
            spaceRepository.save(space);

            // Cancelar facturas pendientes
            List<InvoiceEntity> pendingInvoices = invoiceRepository.findPendingInvoicesByContractId(contractId);
            for (InvoiceEntity invoice : pendingInvoices) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
                // Establecer monto reembolsado si la factura ya estaba pagada
                if (invoice.getStatus() == InvoiceStatus.PAID) {
                    invoice.setRefundAmount(invoice.getTotalAmount());
                }
            }
            invoiceRepository.saveAll(pendingInvoices);

            // Calcular reembolso del depósito si aplica
            if (contract.getDepositAmount() > 0) {
                double refundPercentage = policyService.calculateDepositRefundPercentage(contract);
                if (refundPercentage > 0) {
                    processDepositRefund(contract, refundPercentage);
                }
            }

            // Notificar a las partes involucradas
            notifyContractCancellation(contract);
        } catch (IllegalStateException e) {
            // Error específico de transición de estado inválida
            log.error("Error en transición de estado: {}", e.getMessage());
            throw new RuntimeException("No se puede cancelar el contrato en su estado actual: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al cancelar contrato: {}", e.getMessage(), e);
            throw new RuntimeException("Error al cancelar el contrato: " + e.getMessage());
        }
    }

    /**
     * Procesa el reembolso del depósito
     */
    private void processDepositRefund(RentalContractEntity contract, double refundPercentage) {
        // Aquí implementaríamos la lógica de reembolso real
        double refundAmount = contract.getDepositAmount() * refundPercentage;

        log.info("Procesando reembolso de depósito para contrato ID {}: ${} ({:.0f}%)",
                contract.getId(), refundAmount, refundPercentage * 100);
       Optional<RentalContractEntity> rentalContractEntity= this.rentalContractRepository.findById(contract.getId());
       if (rentalContractEntity.isPresent()) {
           rentalContractEntity.get().setDepositRefounded(true);
           rentalContractEntity.get().setDepositRefundedAmount(refundAmount);
           rentalContractEntity.get().setDepositRefoundDate(LocalDateTime.now());
           rentalContractRepository.save(rentalContractEntity.get());
           
           // Buscar y actualizar las facturas relacionadas con el depósito (generalmente la primera factura)
           List<InvoiceEntity> contractInvoices = invoiceRepository.findByRentalContractId(contract.getId());
           if (!contractInvoices.isEmpty()) {
               // Encontrar la factura del depósito (generalmente la primera)
               InvoiceEntity depositInvoice = contractInvoices.stream()
                   .filter(i -> i.getDescription() != null && i.getDescription().toLowerCase().contains("depósito"))
                   .findFirst()
                   .orElse(null);
               
               if (depositInvoice != null && depositInvoice.getStatus() == InvoiceStatus.PAID) {
                   depositInvoice.setRefundAmount(refundAmount);
                   invoiceRepository.save(depositInvoice);
                   log.info("Actualizada factura de depósito ID {} con reembolso: ${}", depositInvoice.getId(), refundAmount);
               }
           }
       }

        // Aquí se integraría con la pasarela de pagos para procesar el reembolso
        // Por ahora, solo notificamos sobre el reembolso
        notifyDepositRefund(contract, refundAmount);
    }

    private void notifyDepositRefund(RentalContractEntity contract, double refundAmount) {
        UserEntity tenant = contract.getTenant();
        final String tenantEmail = tenant.getEmail();
        final String tenantName = tenant.getName();
        final String spaceName = contract.getSpace().getName();
        final String refundAmountStr = String.format("%.2f", refundAmount);

        mailService.sendDepositRefundNotification(
                tenantEmail, tenantName, spaceName, refundAmountStr
        );
    }

    /**
     * Notifica la cancelación del contrato a las partes involucradas
     */
    private void notifyContractCancellation(RentalContractEntity contract) {
        UserEntity tenant = contract.getTenant();
        UserEntity owner = contract.getSpace().getOwner();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        final String tenantEmail = tenant.getEmail();
        final String tenantName = tenant.getName();
        final String ownerEmail = owner.getEmail();
        final String ownerName = owner.getName();
        final String spaceName = contract.getSpace().getName();
        final String startDate = contract.getStartDate().format(formatter);
        final String endDate = contract.getEndDate().format(formatter);

        // Notificar al inquilino
        mailService.sendContractCancellationNotification(
                tenantEmail, tenantName, spaceName, startDate, endDate
        );

        // Notificar al propietario
        mailService.sendOwnerContractCancellationNotification(
                ownerEmail, ownerName, spaceName, tenantName, startDate, endDate
        );
    }

    @Override
    @Transactional
    public String renewContract(Long contractId, Integer months) {
        RentalContractEntity contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));

        try {
            // Verificar elegibilidad para renovación
            if (!policyService.isEligibleForAutoRenewal(contract)) {
                throw new RuntimeException("Este contrato no es elegible para renovación en este momento");
            }

            // Usar máquina de estados para la transición
            stateMachine.transition(contract, ContractStatus.RENEWAL);

            // Actualizar fecha de fin del contrato
            LocalDate newEndDate = contract.getEndDate().plusMonths(months);
            contract.setEndDate(newEndDate);
            contract.setDurationMonths(contract.getDurationMonths() + months);

            // Volver a estado activo después de renovar
            stateMachine.transition(contract, ContractStatus.ACTIVE);
            rentalContractRepository.save(contract);

            // Notificar a las partes
            notifyContractRenewal(contract, months);

            return "Contrato renovado con éxito hasta " + newEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (IllegalStateException e) {
            log.error("Error en transición de estado: {}", e.getMessage());
            throw new RuntimeException("No se puede renovar el contrato en su estado actual: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al renovar contrato: {}", e.getMessage(), e);
            throw new RuntimeException("Error al renovar el contrato: " + e.getMessage());
        }
    }
    @Override
    @Transactional
    public boolean setupAutoRenewal(Long contractId, boolean autoRenew, Integer renewalMonths) {
        RentalContractEntity contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));

        contract.setAutoRenewal(autoRenew);
        contract.setRenewalMonths(renewalMonths);
        rentalContractRepository.save(contract);

        // Notificar la configuración de renovación automática
        notifyAutoRenewalSetup(contract);

        return true;
    }
    /**
     * Notifica la configuración de renovación automática
     */
    private void notifyAutoRenewalSetup(RentalContractEntity contract) {
        UserEntity tenant = contract.getTenant();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        final String tenantEmail = tenant.getEmail();
        final String tenantName = tenant.getName();
        final String spaceName = contract.getSpace().getName();
        final String endDate = contract.getEndDate().format(formatter);
        final String renewalMonths = String.valueOf(contract.getRenewalMonths());

        notificationRetryService.sendEmailWithRetry(
                () -> mailService.sendAutoRenewalSetupNotification(
                        tenantEmail, tenantName, spaceName, endDate, renewalMonths,
                        contract.isAutoRenewal() ? "activada" : "desactivada"
                ),
                "No se pudo notificar al inquilino sobre la configuración de renovación automática"
        );
    }
    @Transactional
    public void processAutoRenewals() {
        LocalDate today = LocalDate.now();
        LocalDate renewalThreshold = today.plusDays(7);

        List<RentalContractEntity> contractsToRenew = rentalContractRepository
                .findContractsForAutoRenewal(today, renewalThreshold);

        for (RentalContractEntity contract : contractsToRenew) {
            try {
                if (contract.isAutoRenewal() && contract.getRenewalMonths() != null) {
                    String tenantEmail = contract.getTenant().getEmail();
                    String tenantName = contract.getTenant().getName();
                    String spaceName = contract.getSpace().getName();
                    String endDate = contract.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    String renewalMonths = contract.getRenewalMonths().toString();

                    notificationRetryService.sendEmailWithRetry(
                            () -> mailService.sendUpcomingAutoRenewalNotification(
                                    tenantEmail, tenantName, spaceName, endDate, renewalMonths
                            ),
                            "No se pudo notificar al inquilino sobre la renovación automática próxima"
                    );

                    log.info("Notificación de renovación automática enviada para contrato ID: {}", contract.getId());
                }
            } catch (Exception e) {
                log.error("Error al procesar renovación automática para contrato ID {}: {}",
                        contract.getId(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void executeAutoRenewals() {
        LocalDate today = LocalDate.now();
        LocalDate executionThreshold = today.plusDays(1);

        List<RentalContractEntity> contractsDueForRenewal = rentalContractRepository
                .findContractsForAutoRenewalExecution(today, executionThreshold);

        for (RentalContractEntity contract : contractsDueForRenewal) {
            try {
                if (contract.isAutoRenewal() && contract.getRenewalMonths() != null) {
                    renewContract(contract.getId(), contract.getRenewalMonths());
                    log.info("Auto-renewed contract ID: {}", contract.getId());
                }
            } catch (Exception e) {
                log.error("Error auto-renewing contract ID {}: {}",
                        contract.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Notifica la renovación del contrato a las partes involucradas
     */
    private void notifyContractRenewal(RentalContractEntity contract, Integer months) {
        UserEntity tenant = contract.getTenant();
        UserEntity owner = contract.getSpace().getOwner();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Notificar al inquilino
        mailService.sendContractRenewalNotification(
                tenant.getEmail(),
                tenant.getName(),
                contract.getSpace().getName(),
                months.toString(),
                contract.getEndDate().format(formatter)
        );

        // Notificar al propietario
        mailService.sendOwnerContractRenewalNotification(
                owner.getEmail(),
                owner.getName(),
                contract.getSpace().getName(),
                tenant.getName(),
                months.toString(),
                contract.getEndDate().format(formatter)
        );
    }

    /**
     * @deprecated Este método está siendo reemplazado por la lógica consolidada en ContractScheduler.
     * Se mantiene por compatibilidad, pero la lógica principal está en updateContractsAndSpacesStatus()
     */
    @Override
    @Deprecated
    public void updateSpaceStatuses() {
        log.warn("updateSpaceStatuses() está deprecado. Usar ContractScheduler.updateContractsAndSpacesStatus()");
        List<RentalContractEntity> activeContracts = rentalContractRepository
                .findByContractStatus(ContractStatus.ACTIVE);

        for (RentalContractEntity contract : activeContracts) {
            SpaceEntity space = contract.getSpace();
            space.setAvailable(false);
            spaceRepository.save(space);
        }
    }

    @Override
    public void processExpiringContracts() {
        LocalDate expirationThreshold = LocalDate.now().plusDays(7);
        List<RentalContractEntity> expiringContracts = rentalContractRepository
                .findExpiringContracts(expirationThreshold);

        for (RentalContractEntity contract : expiringContracts) {
            if (!contract.isAutoRenewal()) {
                SpaceEntity space = contract.getSpace();
                space.setAvailable(true);
                spaceRepository.save(space);

                mailService.sendContractExpirationReminder(
                        contract.getTenant().getEmail(),
                        contract.getTenant().getName(),
                        contract.getSpace().getName(),
                        contract.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
            }
        }
    }

    @Override
    public Page<RentalContractResponseDto> getUserContracts(String uid, ContractStatus status, Pageable pageable) {
        Page<RentalContractEntity> contractsPage = rentalContractRepository
                .findByUserFirebaseUidAndStatus(uid, status, pageable);

        return contractsPage.map(contract -> {
            RentalContractResponseDto dto = new RentalContractResponseDto();
            // Mapeo del contrato
            dto.setId(contract.getId());
            dto.setStartDate(contract.getStartDate());
            dto.setEndDate(contract.getEndDate());
            dto.setMonthlyAmount(contract.getMonthlyAmount());
            dto.setDepositAmount(contract.getDepositAmount());
            dto.setStatus(contract.getContractStatus());

            // Mapeo del espacio
            SpaceEntity space = contract.getSpace();
            dto.setSpaceId(space.getId());
            dto.setSpaceName(space.getName());
            String spaceAddres= space.getAddress().getStreetName() + " " +
                    space.getAddress().getStreetNumber().toString() + ", " +
                    space.getAddress().getCity().getName() + ", " +
                    space.getAddress().getCity().getDivisionName() + ", " +
                    space.getAddress().getCity().getCountry().getName() + ", " +
                    space.getAddress().getPostalCode();
            dto.setSpaceAddress(spaceAddres);
            dto.setSpaceDescription(space.getDescription());
            dto.setSpaceArea(space.getArea());
            dto.setSpaceCapacity(space.getCapacity());
            dto.setPricePerHour(space.getPricePerHour());
            dto.setPricePerDay(space.getPricePerDay());
            dto.setPricePerMonth(space.getPricePerMonth());
            dto.setSpaceType(space.getType().getName());
            dto.setCityName(space.getAddress().getCity().getName());
            dto.setCountryName(space.getAddress().getCity().getCountry().getName());
            dto.setOwnerName(space.getOwner().getName());

            // Mapeo de la factura pendiente
            InvoiceEntity currentInvoice = invoiceRepository
                    .findTopByRentalContractIdOrderByIssueDateDesc(contract.getId())
                    .orElse(null);

            if (currentInvoice != null &&
                    (currentInvoice.getStatus() == InvoiceStatus.DRAFT ||
                            currentInvoice.getStatus() == InvoiceStatus.ISSUED)) {
                dto.setHasCurrentInvoicePending(true);
                dto.setCurrentInvoiceNumber(currentInvoice.getInvoiceNumber());
                dto.setCurrentInvoiceDueDate(currentInvoice.getDueDate().toLocalDate());
            } else {
                dto.setHasCurrentInvoicePending(false);
            }

            return dto;
        });
    }

    @Override
    public String generateInvoicePaymentLink(Long invoiceId) {
        log.info("=== INICIO generateInvoicePaymentLink para factura ID: {} ===", invoiceId);
        
        // Buscar la factura una sola vez
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        
        log.info("Factura encontrada - ID: {}, TotalAmount: {}, Status: {}", 
                invoice.getId(), invoice.getTotalAmount(), invoice.getStatus());

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("La factura ya está pagada");
        }

        // Validar que es una factura de contrato
        if (invoice.getRentalContract() == null) {
            throw new RuntimeException("Esta factura no está asociada a un contrato de alquiler");
        }

        RentalContractEntity rentalContract = invoice.getRentalContract();
        log.info("Contrato asociado - ID: {}, MonthlyAmount: {}, DepositAmount: {}", 
                rentalContract.getId(), rentalContract.getMonthlyAmount(), rentalContract.getDepositAmount());

        Long spaceID = rentalContract.getSpace().getId();
        LocalDate startDate = rentalContract.getStartDate();
        LocalDate endDate = startDate.plusMonths(rentalContract.getDurationMonths().longValue());

        // Validar solapamiento de contratos y reservas
        try {
            bookingService.validateContractAndBookingOverlap(spaceID, startDate, endDate);
            log.info("Validación de solapamiento exitosa");
        } catch (Exception e) {
            log.warn("Error en validación de solapamiento: {}", e.getMessage());
            // No lanzamos excepción aquí para permitir el pago de facturas existentes
        }

        // Re-verificar el monto de la factura después de las validaciones
        InvoiceEntity refreshedInvoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada tras validación"));
        
        log.info("Factura re-verificada - TotalAmount: {} (era: {})", 
                refreshedInvoice.getTotalAmount(), invoice.getTotalAmount());

        // Usar la factura refrescada para asegurar datos actuales
        Double finalAmount = refreshedInvoice.getTotalAmount();
        
        if (finalAmount == null || finalAmount <= 0) {
            throw new RuntimeException("El monto de la factura no es válido: " + finalAmount);
        }

        // Validación adicional: verificar que el monto esté en un rango razonable
        // basado en los datos del contrato
        Double expectedMinAmount = rentalContract.getMonthlyAmount();
        Double expectedMaxAmount = rentalContract.getMonthlyAmount() + rentalContract.getDepositAmount() + 1000; // +1000 para amenities
        
        if (finalAmount < expectedMinAmount || finalAmount > expectedMaxAmount) {
            log.error("ADVERTENCIA: Monto de factura fuera del rango esperado. " +
                    "Factura: {}, Rango esperado: {} - {}, Mensual: {}, Depósito: {}", 
                    finalAmount, expectedMinAmount, expectedMaxAmount, 
                    rentalContract.getMonthlyAmount(), rentalContract.getDepositAmount());
        }

        // Verificar que el monto no haya cambiado durante el proceso
        if (!finalAmount.equals(invoice.getTotalAmount())) {
            log.warn("ADVERTENCIA: El monto de la factura cambió durante el proceso de {} a {}", 
                    invoice.getTotalAmount(), finalAmount);
        }

        try {
            String buyerEmail = rentalContract.getTenant().getEmail();
            String sellerEmail = rentalContract.getSpace().getOwner().getEmail();

            log.info("Generando preferencia de pago - Monto: {}, Comprador: {}, Vendedor: {}", 
                    finalAmount, buyerEmail, sellerEmail);

            String paymentUrl = mercadoPagoService.createInvoicePaymentPreference(
                    refreshedInvoice.getId(),
                    refreshedInvoice.getDescription(),
                    BigDecimal.valueOf(finalAmount),
                    buyerEmail,
                    sellerEmail
            );
            
            log.info("=== FIN generateInvoicePaymentLink - URL generada exitosamente ===");
            return paymentUrl;
            
        } catch (MPException | MPApiException e) {
            log.error("Error al generar el link de pago: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el link de pago: " + e.getMessage());
        }
    }



    @Override
    public List<InvoiceHistoryDto> getContractInvoices(Long contractId) {
        RentalContractEntity contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado"));

        List<InvoiceEntity> invoices = invoiceRepository.findByRentalContractId(contractId);
        
        return invoices.stream()
                .map(invoice -> new InvoiceHistoryDto(
                        invoice.getId(),
                        invoice.getInvoiceNumber(),
                        BigDecimal.valueOf(invoice.getTotalAmount()),
                        invoice.getIssueDate(),
                        invoice.getDueDate(),
                        invoice.getStatus(),
                        invoice.getType(),
                        invoice.getDescription()
                ))
                .collect(Collectors.toList());
    }


    @Transactional
    public void processCompletedContractsAndDeposits() {
        // Buscar contratos que terminaron hoy
        List<RentalContractEntity> completedContracts = rentalContractRepository
                .findByEndDateAndContractStatusAndDepositRefunded(
                        LocalDate.now(),
                        ContractStatus.ACTIVE,
                        false
                );

        for (RentalContractEntity contract : completedContracts) {
            try {
                // Verificar que no haya deudas pendientes
                if (!hasUnpaidInvoices(contract)) {
                    // Procesar la devolución del depósito
                    processDepositRefund(contract, 1.0); // 100% si terminó normalmente

                    // Actualizar estado del contrato
                    contract.setContractStatus(ContractStatus.TERMINATED);
                    contract.setDepositRefounded(true);
                    contract.setDepositRefoundDate(LocalDateTime.now());
                    rentalContractRepository.save(contract);

                    log.info("Contrato {} completado y depósito devuelto", contract.getId());
                } else {
                    log.warn("Contrato {} tiene facturas pendientes, no se puede devolver el depósito",
                            contract.getId());
                }
            } catch (Exception e) {
                log.error("Error procesando contrato {}: {}", contract.getId(), e.getMessage());
            }
        }
    }

    @Override
    public void updateConfirmedToActiveContracts() {
        log.warn("updateConfirmedToActiveContracts() está deprecado. Usar ContractScheduler.updateContractsAndSpacesStatus()");
        LocalDate today = LocalDate.now();

        // Usar el nuevo método que incluye contratos atrasados
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

    private boolean hasUnpaidInvoices(RentalContractEntity contract) {
        List<InvoiceEntity> unpaidInvoices = invoiceRepository.findPendingInvoicesByContractId(contract.getId());
        return !unpaidInvoices.isEmpty();
    }

}