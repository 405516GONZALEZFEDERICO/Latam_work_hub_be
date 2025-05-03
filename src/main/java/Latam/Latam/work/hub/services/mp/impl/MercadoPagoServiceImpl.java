package Latam.Latam.work.hub.services.mp.impl;

import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.services.BookingService;
import Latam.Latam.work.hub.services.MailService;
import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private  MerchantOrderClient merchantOrderClient;

    @Autowired
    private  PreferenceClient preferenceClient;

    @Autowired
    private  MailService mailService;

    @Autowired
    @Lazy
    private  BookingService bookingService;

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

            String buyerEmailTest = "test_user_153291871@testuser.com";
            String sellerEmailTest = "test_user_868623445@testuser.com";

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

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference("INVOICE-" + invoiceId)
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
        if ("merchant_order".equalsIgnoreCase(topic) && resource != null) {
            String[] resourceParts = resource.split("/");
            Long merchantOrderId = Long.valueOf(resourceParts[resourceParts.length - 1]);
            MerchantOrder merchantOrder = this.merchantOrderClient.get(merchantOrderId);

            if ("paid".equalsIgnoreCase(merchantOrder.getOrderStatus())) {
                InvoiceEntity invoiceEntity = this.invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

                // Actualizar estado de la factura
                invoiceEntity.setStatus(InvoiceStatus.PAID);
                this.invoiceRepository.save(invoiceEntity);

                // Confirmar la reserva (en lugar de activarla directamente aquí)
                if (invoiceEntity.getBooking() != null) {
                    bookingService.confirmBookingPayment(invoiceEntity.getBooking().getId());
                }

                // Enviar email de confirmación al cliente
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String formattedDate = invoiceEntity.getIssueDate().format(formatter);

                this.mailService.sendPaymentConfirmationEmail(
                        invoiceEntity.getBooking().getUser().getEmail(),
                        invoiceEntity.getBooking().getUser().getName(),
                        invoiceEntity.getBooking().getSpace().getName(),
                        formattedDate,
                        invoiceEntity.getTotalAmount());
            }
        }
        return "Notification Recibida";
    }
}