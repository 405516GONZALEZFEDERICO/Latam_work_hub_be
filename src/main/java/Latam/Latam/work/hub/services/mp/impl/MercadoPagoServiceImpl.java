package Latam.Latam.work.hub.services.mp.impl;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.enums.InvoiceType;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.services.BookingService;
import Latam.Latam.work.hub.services.MailService;
import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoServiceImpl.class);
    private static final String CURRENCY = "ARS";
    
    // Cache para evitar procesamiento duplicado simultáneo
    private final Set<String> processingPayments = new HashSet<>();

    @Value("${front.url}")
    private String WEB_URL;

    @Value("${back.url}")
    private String BACK_URL;

    @Value("${mercadopago.access.token}")
    private String MP_TOKEN;

    @Autowired
    private  InvoiceRepository invoiceRepository;
    @Autowired
    private RentalContractRepository rentalContractRepository;
    @Autowired
    private  MerchantOrderClient merchantOrderClient;

    @Autowired
    private  PreferenceClient preferenceClient;

    @Autowired
    private PaymentRefundClient paymentRefundClient;

    @Autowired
    private  MailService mailService;

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    @Lazy
    private  BookingService bookingService;

    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(MP_TOKEN);
    }

    @Transactional
    public String createInvoicePaymentPreference(Long invoiceId, String title, BigDecimal amount,
                                                 String buyerEmail, String sellerEmail) throws MPException, MPApiException {
        try {
            log.info("=== INICIO createInvoicePaymentPreference ===");
            log.info("Parámetros recibidos - InvoiceID: {}, Title: {}, Amount: {}, Buyer: {}, Seller: {}", 
                    invoiceId, title, amount, buyerEmail, sellerEmail);

            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(title)
                    .quantity(1)
                    .unitPrice(amount)
                    .currencyId(CURRENCY)
                    .build();
            items.add(item);

            log.info("Item de preferencia creado - UnitPrice: {}", amount);

            String buyerEmailTest = "test_user_1440077709@testuser.com";
            String sellerEmailTest = "test_user_11222044@testuser.com";

            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .email(buyerEmailTest)
                    .build();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("invoice_id", invoiceId);
            metadata.put("buyer_email", buyerEmailTest);
            metadata.put("seller_email", sellerEmailTest);

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(WEB_URL)
                    .failure(WEB_URL)
                    .build();

            String notificationUrl = BACK_URL + "/api/payments/notifications/" + invoiceId;
            OffsetDateTime expirationDateFrom = LocalDateTime.now()
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();

            OffsetDateTime expirationDateTo = LocalDateTime.now()
                    .plusHours(24)
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference("INVOICE-" + invoiceId)
                    .expires(true)
                    .expirationDateFrom(expirationDateFrom)
                    .expirationDateTo(expirationDateTo)
                    .metadata(metadata)
                    .autoReturn("approved")
                    .build();

            log.info("PreferenceRequest configurado para factura ID: {}", invoiceId);

            invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                log.info("Actualizando estado de factura ID {} a ISSUED - TotalAmount en BD: {}", 
                        invoiceId, invoice.getTotalAmount());
                invoice.setStatus(InvoiceStatus.ISSUED);
                invoiceRepository.save(invoice);
            });

            Preference preference = preferenceClient.create(preferenceRequest);
            log.info("Preferencia creada exitosamente - ID: {}, InitPoint: {}", 
                    preference.getId(), preference.getInitPoint());
            log.info("=== FIN createInvoicePaymentPreference ===");
            
            return preference.getInitPoint();
        } catch (MPException | MPApiException e) {
            log.error("Error al crear preferencia de pago para factura {}: {}", invoiceId, e.getMessage());
            throw e;
        }
    }
    @Override
    @Transactional
    public String receiveNotification(String topic, String resource, Long invoiceId) throws MPException, MPApiException {
        try {
            System.out.println("Recibida notificación MercadoPago - Topic: " + topic + ", Resource: " + resource + ", InvoiceId: " + invoiceId);
            
            // Si no hay topic o resource, ignoramos la notificación
            if (topic == null || resource == null) {
                System.out.println("Notificación ignorada: tipo no soportado o recurso nulo");
                return "Notificación ignorada: datos incompletos";
            }
            
            InvoiceEntity invoiceEntity = this.invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada: " + invoiceId));
            
            // Procesar según el tipo de notificación
            switch (topic.toLowerCase()) {
                case "merchant_order":
                    return processMerchantOrderNotification(resource, invoiceEntity);
                case "payment":
                    return processPaymentNotification(resource, invoiceEntity);
                default:
                    System.out.println("Tipo de notificación no soportada: " + topic);
                    return "Tipo de notificación no soportada";
            }
        } catch (Exception e) {
            // Log del error detallado
            System.err.println("Error procesando notificación para factura " + invoiceId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error procesando notificación: " + e.getMessage(), e);
        }
    }
    
    private String processMerchantOrderNotification(String resource, InvoiceEntity invoiceEntity) throws MPException, MPApiException {
        try {
            String[] resourceParts = resource.split("/");
            if (resourceParts.length == 0) {
                System.out.println("Recurso de merchant_order inválido: " + resource);
                return "Recurso inválido";
            }
            
            Long merchantOrderId = Long.valueOf(resourceParts[resourceParts.length - 1]);
            System.out.println("Obteniendo merchant order: " + merchantOrderId);
            MerchantOrder merchantOrder = this.merchantOrderClient.get(merchantOrderId);
            
            System.out.println("Estado de la orden merchant: " + merchantOrder.getOrderStatus());
            
            if (!"paid".equalsIgnoreCase(merchantOrder.getOrderStatus())) {
                System.out.println("Orden no pagada. Estado actual: " + merchantOrder.getOrderStatus());
                return "Orden no pagada";
            }
            
            if (merchantOrder.getPayments() == null || merchantOrder.getPayments().isEmpty()) {
                System.err.println("No se encontraron pagos en la orden: " + merchantOrderId);
                throw new RuntimeException("No se encontraron pagos en la orden");
            }
            
            // Procesamos el pago usando el paymentId del merchant_order
            Long paymentId = merchantOrder.getPayments().get(0).getId();
            return processPaymentSuccess(paymentId, invoiceEntity);
        } catch (Exception e) {
            System.err.println("Error procesando notificación merchant_order: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private String processPaymentNotification(String paymentId, InvoiceEntity invoiceEntity) throws MPException, MPApiException {
        try {
            System.out.println("Procesando notificación de pago: " + paymentId);
            
            // Obtener el estado del pago desde la API de MercadoPago
            var payment = paymentClient.get(Long.valueOf(paymentId));
            String status = payment.getStatus();
            
            System.out.println("Estado del pago " + paymentId + ": " + status);
            
            // Sólo procesar pagos aprobados
            if ("approved".equalsIgnoreCase(status)) {
                return processPaymentSuccess(Long.valueOf(paymentId), invoiceEntity);
            } else {
                System.out.println("Pago no aprobado. Estado actual: " + status);
                return "Pago no aprobado";
            }
        } catch (Exception e) {
            System.err.println("Error procesando notificación payment: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private String processPaymentSuccess(Long paymentId, InvoiceEntity invoiceEntity) {
        String cacheKey = "payment_" + paymentId + "_invoice_" + invoiceEntity.getId();
        
        // VERIFICACIÓN DE CACHE: Evitar procesamiento simultáneo del mismo pago
        synchronized (processingPayments) {
            if (processingPayments.contains(cacheKey)) {
                System.out.println("Pago " + paymentId + " para factura " + invoiceEntity.getId() + " ya se está procesando");
                return "Pago ya en procesamiento";
            }
            processingPayments.add(cacheKey);
        }
        
        try {
            // VERIFICACIÓN ATÓMICA: Recargar la factura desde la BD para evitar race conditions
            InvoiceEntity freshInvoice = invoiceRepository.findById(invoiceEntity.getId())
                    .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + invoiceEntity.getId()));
            
            // Verificar si la factura ya está pagada para evitar procesamiento duplicado
            if (freshInvoice.getStatus() == InvoiceStatus.PAID) {
                System.out.println("Factura " + freshInvoice.getId() + " ya está pagada, no se procesa nuevamente");
                return "Factura ya procesada";
            }
            
            // Verificar si ya tiene el mismo paymentId para evitar duplicados
            if (freshInvoice.getPaymentId() != null && freshInvoice.getPaymentId().equals(paymentId)) {
                System.out.println("Factura " + freshInvoice.getId() + " ya tiene este paymentId: " + paymentId);
                return "Pago ya procesado";
            }
        
        // Actualizar estado de la factura
        System.out.println("Actualizando factura " + freshInvoice.getId() + " con paymentId: " + paymentId);
        freshInvoice.setStatus(InvoiceStatus.PAID);
        freshInvoice.setPaymentId(paymentId);
        this.invoiceRepository.save(freshInvoice);
        
        // Procesar según el tipo de factura
        if (freshInvoice.getBooking() != null) {
            System.out.println("Procesando pago de reserva para factura: " + freshInvoice.getId());
            processBookingPayment(freshInvoice);
        } else if (freshInvoice.getRentalContract() != null) {
            System.out.println("Procesando pago de contrato para factura: " + freshInvoice.getId());
            processRentalContractPayment(freshInvoice);
        } else {
            System.err.println("La factura " + freshInvoice.getId() + " no tiene ni reserva ni contrato asociado");
        }
        
            System.out.println("Pago procesado exitosamente para factura: " + freshInvoice.getId());
            return "Pago procesado exitosamente";
        } finally {
            // Limpiar cache al finalizar (exitoso o con error)
            synchronized (processingPayments) {
                processingPayments.remove(cacheKey);
            }
        }
    }

    private void processBookingPayment(InvoiceEntity invoiceEntity) {
        BookingEntity booking = invoiceEntity.getBooking();
        
        try {
            System.out.println("Procesando pago para reserva ID: " + booking.getId());
            
            // VERIFICACIÓN ATÓMICA: Recargar la reserva desde la BD para evitar race conditions
            BookingEntity freshBooking = bookingRepository.findById(booking.getId())
                    .orElseThrow(() -> new RuntimeException("Reserva no encontrada: " + booking.getId()));
            
            // Verificar si la reserva ya está confirmada para evitar procesamiento duplicado
            if (freshBooking.getStatus() == BookingStatus.CONFIRMED || 
                freshBooking.getStatus() == BookingStatus.ACTIVE ||
                freshBooking.getStatus() == BookingStatus.COMPLETED) {
                System.out.println("Reserva " + freshBooking.getId() + " ya está en estado: " + freshBooking.getStatus() + ", no se procesa nuevamente el pago");
                return; // No procesar nuevamente
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String formattedDate = invoiceEntity.getIssueDate().format(formatter);
            
            // Informamos al servicio de reservas sobre el pago confirmado
            // Este método ya maneja todo: status CONFIRMED, NO active hasta la fecha correcta
            try {
                System.out.println("Notificando al servicio de reservas sobre el pago confirmado");
                bookingService.confirmBookingPayment(freshBooking.getId());
            } catch (Exception e) {
                // Loguear el error para diagnóstico
                System.err.println("Error al confirmar el pago de la reserva a través del servicio: " + e.getMessage());
                e.printStackTrace();
                throw e; // Re-lanzar para que la transacción se revierta
            }
            
            // Solo enviar email si la reserva fue procesada exitosamente
            this.mailService.sendPaymentConfirmationEmail(
                    freshBooking.getUser().getEmail(),
                    freshBooking.getUser().getName(),
                    freshBooking.getSpace().getName(),
                    formattedDate,
                    invoiceEntity.getTotalAmount());
        } catch (Exception e) {
            // Capturar cualquier error inesperado, loguearlo y lanzarlo nuevamente para que la transacción se revierta si es necesario
            System.err.println("Error crítico al procesar el pago de la reserva: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al procesar el pago: " + e.getMessage(), e);
        }
    }

    private void processRentalContractPayment(InvoiceEntity invoiceEntity) {
        RentalContractEntity contract = invoiceEntity.getRentalContract();

        if (contract.getContractStatus() == ContractStatus.PENDING) {
            if (!contract.getStartDate().atStartOfDay().isBefore(LocalDateTime.now().toLocalDate().atStartOfDay())) {
                contract.setContractStatus(ContractStatus.CONFIRMED);
            }
            if (contract.getStartDate().atStartOfDay().isEqual(LocalDateTime.now().toLocalDate().atStartOfDay())) {
                contract.setContractStatus(ContractStatus.ACTIVE);
            }
            rentalContractRepository.save(contract);
        }
        UserEntity tenant = contract.getTenant();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        mailService.sendRentalPaymentConfirmation(
                tenant.getEmail(),
                tenant.getName(),
                contract.getSpace().getName(),
                invoiceEntity.getInvoiceNumber(),
                invoiceEntity.getIssueDate().format(formatter),
                invoiceEntity.getTotalAmount().toString()
        );
    }

    @Override
    @Transactional
    public boolean refundPayment(Long invoiceId) throws MPException, MPApiException {
        try {
            InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

            // Get payment ID from invoice
            Long paymentId = invoice.getPaymentId();
            if (paymentId == null) {
                throw new RuntimeException("No se encontró ID de pago para esta factura, no se puede reembolsar");
            }
            
            // Obtenemos la reserva asociada
            BookingEntity booking = invoice.getBooking();
            if (booking == null) {
                throw new RuntimeException("No hay reserva asociada a esta factura");
            }
            
            // Permitimos reembolsos aunque la factura esté en otro estado siempre que tenga paymentId
            // Esto ayuda en casos donde el estado puede estar desincronizado pero existe un pago real

            String idempotencyKey = UUID.randomUUID().toString();

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .customHeaders(Collections.singletonMap("X-Idempotency-Key", idempotencyKey))
                    .build();

            // Create a full refund (null amount for full refund)
            com.mercadopago.resources.payment.PaymentRefund refund = paymentClient.refund(paymentId, null, requestOptions);

            // Check if refund was created successfully - check if we got a non-null response
            if (refund != null) {
                // Update invoice status
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoiceRepository.save(invoice);

                // Update booking status and refund amount
                booking.setStatus(BookingStatus.CANCELED);
                booking.setRefundAmount(booking.getTotalAmount()); // Guardamos el monto reembolsado en la reserva
                bookingRepository.save(booking);

                // Mark the space as available again
                SpaceEntity space = booking.getSpace();
                space.setAvailable(true);
                spaceRepository.save(space);

                // Send refund confirmation email
                mailService.sendBookingRefundConfirmationEmail(
                        booking.getUser().getEmail(),
                        booking.getUser().getName(),
                        booking.getSpace().getName(),
                        booking.getTotalAmount());

                return true;
            } else {
                throw new RuntimeException("No se pudo crear el reembolso");
            }
        } catch (MPException | MPApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar el reembolso: " + e.getMessage(), e);
        }
    }

}