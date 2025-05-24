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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final String CURRENCY = "ARS";

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
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(title)
                    .quantity(1)
                    .unitPrice(amount)
                    .currencyId(CURRENCY)
                    .build();
            items.add(item);

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

            invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                invoice.setStatus(InvoiceStatus.ISSUED);
                invoiceRepository.save(invoice);
            });

            Preference preference = preferenceClient.create(preferenceRequest);
            return preference.getInitPoint();
        } catch (MPException | MPApiException e) {
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
        // Actualizar estado de la factura
        System.out.println("Actualizando factura " + invoiceEntity.getId() + " con paymentId: " + paymentId);
        invoiceEntity.setStatus(InvoiceStatus.PAID);
        invoiceEntity.setPaymentId(paymentId);
        this.invoiceRepository.save(invoiceEntity);
        
        // Procesar según el tipo de factura
        if (invoiceEntity.getBooking() != null) {
            System.out.println("Procesando pago de reserva para factura: " + invoiceEntity.getId());
            processBookingPayment(invoiceEntity);
        } else if (invoiceEntity.getRentalContract() != null) {
            System.out.println("Procesando pago de contrato para factura: " + invoiceEntity.getId());
            processRentalContractPayment(invoiceEntity);
        } else {
            System.err.println("La factura " + invoiceEntity.getId() + " no tiene ni reserva ni contrato asociado");
        }
        
        System.out.println("Pago procesado exitosamente para factura: " + invoiceEntity.getId());
        return "Pago procesado exitosamente";
    }

    private void processBookingPayment(InvoiceEntity invoiceEntity) {
        BookingEntity booking = invoiceEntity.getBooking();
        
        try {
            System.out.println("Procesando pago para reserva ID: " + booking.getId());
            
            // Forzamos la actualización de la reserva independientemente de su estado actual
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setActive(true);
            bookingRepository.save(booking);
            System.out.println("Reserva actualizada a estado CONFIRMED");
    
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String formattedDate = invoiceEntity.getIssueDate().format(formatter);
            
            // Obtener el espacio fresco de la base de datos
            Long spaceId = booking.getSpace().getId();
            SpaceEntity space = spaceRepository.findById(spaceId)
                    .orElseThrow(() -> new RuntimeException("Espacio no encontrado ID: " + spaceId));
                    
            System.out.println("Estado actual del espacio ID " + space.getId() + ": available=" + space.getAvailable());
            
            // Marcar el espacio como no disponible
            space.setAvailable(false);
            spaceRepository.save(space);
            
            // Verificar que el espacio se haya actualizado correctamente
            SpaceEntity verifiedSpace = spaceRepository.findById(space.getId()).orElse(null);
            if (verifiedSpace != null) {
                System.out.println("Estado del espacio después de actualizar: available=" + verifiedSpace.getAvailable());
            }
            
            // Informamos al servicio de reservas sobre el pago confirmado
            try {
                System.out.println("Notificando al servicio de reservas sobre el pago confirmado");
                bookingService.confirmBookingPayment(booking.getId());
            } catch (Exception e) {
                // Loguear el error para diagnóstico
                System.err.println("Error al confirmar el pago de la reserva a través del servicio: " + e.getMessage());
                e.printStackTrace();
                // La actualización directa de la entidad ya se realizó, así que podemos continuar
            }
            
            this.mailService.sendPaymentConfirmationEmail(
                    booking.getUser().getEmail(),
                    booking.getUser().getName(),
                    booking.getSpace().getName(),
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