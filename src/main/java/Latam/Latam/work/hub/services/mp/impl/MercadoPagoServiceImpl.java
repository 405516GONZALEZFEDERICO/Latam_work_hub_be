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
            if (!"merchant_order".equalsIgnoreCase(topic) || resource == null) {
                return "Notificación ignorada: tipo no soportado";
            }

            String[] resourceParts = resource.split("/");
            if (resourceParts.length == 0) {
                return "Recurso inválido";
            }

            Long merchantOrderId = Long.valueOf(resourceParts[resourceParts.length - 1]);
            MerchantOrder merchantOrder = this.merchantOrderClient.get(merchantOrderId);

            if (!"paid".equalsIgnoreCase(merchantOrder.getOrderStatus())) {
                return "Orden no pagada";
            }

            if (merchantOrder.getPayments() == null || merchantOrder.getPayments().isEmpty()) {
                throw new RuntimeException("No se encontraron pagos en la orden");
            }

            InvoiceEntity invoiceEntity = this.invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada: " + invoiceId));

            // Verificar si la factura ya está procesada
            if (invoiceEntity.getStatus() == InvoiceStatus.PAID) {
                return "Factura ya procesada";
            }

            Long paymentId = merchantOrder.getPayments().get(0).getId();
            invoiceEntity.setStatus(InvoiceStatus.PAID);
            invoiceEntity.setPaymentId(paymentId);
            this.invoiceRepository.save(invoiceEntity);

            // Procesar según el tipo de factura
            if (invoiceEntity.getBooking() != null) {
                processBookingPayment(invoiceEntity);
            } else if (invoiceEntity.getRentalContract() != null) {
                processRentalContractPayment(invoiceEntity);
            }

            return "Notificación procesada exitosamente";
        } catch (Exception e) {            // Log del error
            throw new RuntimeException("Error procesando notificación: " + e.getMessage(), e);
        }
    }

    private void processBookingPayment(InvoiceEntity invoiceEntity) {
        BookingEntity booking = invoiceEntity.getBooking();
        bookingService.confirmBookingPayment(booking.getId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDate = invoiceEntity.getIssueDate().format(formatter);
        SpaceEntity space = invoiceEntity.getBooking().getSpace();
        space.setAvailable(false);
        spaceRepository.save(space);
        this.mailService.sendPaymentConfirmationEmail(
                booking.getUser().getEmail(),
                booking.getUser().getName(),
                booking.getSpace().getName(),
                formattedDate,
                invoiceEntity.getTotalAmount());
    }

    private void processRentalContractPayment(InvoiceEntity invoiceEntity) {
        RentalContractEntity contract = invoiceEntity.getRentalContract();

        if (contract.getContractStatus() == ContractStatus.PENDING) {
            contract.setContractStatus(ContractStatus.ACTIVE);
            rentalContractRepository.save(contract);

            SpaceEntity space = contract.getSpace();
            space.setAvailable(false);
            spaceRepository.save(space);
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

            // Check if invoice is in a refundable state
            if (invoice.getStatus() != InvoiceStatus.PAID) {
                throw new RuntimeException("La factura no está en estado pagado y no puede ser reembolsada");
            }

            // Get payment ID from invoice
            Long paymentId = invoice.getPaymentId();
            if (paymentId == null) {
                throw new RuntimeException("No se encontró ID de pago para esta factura");
            }

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

                // Update booking status
                BookingEntity booking = invoice.getBooking();
                booking.setStatus(BookingStatus.CANCELED);
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
                        invoice.getTotalAmount());

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