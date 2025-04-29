package Latam.Latam.work.hub.services.mp.impl;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.services.MailService;
import com.mercadopago.client.merchantorder.MerchantOrderClient;

import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.merchantorder.MerchantOrderPayment;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.swing.text.DateFormatter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final String CURRENCY = "ARS";

    @Value("${front.url}")
    private String WEB_URL;

    @Value("${back.url}")
    private String BACK_URL;

    @Value("${mercadopago.access.token}")
    private String MP_TOKEN;

    private final InvoiceRepository invoiceRepository;

    private final MerchantOrderClient merchantOrderClient;

    private final PreferenceClient preferenceClient;

    private final PaymentRefundClient paymentRefundClient;

    private final MailService mailService;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(MP_TOKEN);
    }

    @Transactional
    public String createInvoicePaymentPreference(Long invoiceId, String title, BigDecimal amount,
                                                 String buyerEmail, String sellerEmail) throws MPException, MPApiException {
        try {


            // Crear ítem para la factura
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(title)
                    .quantity(1)
                    .unitPrice(amount)
                    .currencyId(CURRENCY)
                    .build();
            items.add(item);

            // Usar cuentas de prueba
            String buyerEmailTest = "test_user_153291871@testuser.com";
            String sellerEmailTest = "test_user_868623445@testuser.com";

            // Crear comprador
            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .email(buyerEmailTest)
                    .build();

            // Metadata para seguimiento interno
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("invoice_id", invoiceId);
            metadata.put("buyer_email", buyerEmailTest);
            metadata.put("seller_email", sellerEmailTest);

//             Configurar URLs de retorno
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(WEB_URL + "/home/reservas")
                    .failure(WEB_URL + "/home/reservas")    // URL para pagos fallidos
                    .pending(WEB_URL + "/home/reservas")    // URL para pagos pendientes
                    .build();

            // URL de notificación
            String notificationUrl = BACK_URL + "/api/payments/notifications/" + invoiceId;

            // Crear la preferencia
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference("INVOICE-" + invoiceId)
                    .metadata(metadata)
                    .autoReturn("all")
                    .build();

            // Crear la preferencia
            Preference preference = preferenceClient.create(preferenceRequest);

            // Actualizar estado a ISSUED
            invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                invoice.setStatus(InvoiceStatus.ISSUED);
                invoiceRepository.save(invoice);
            });

            return preference.getInitPoint();
        } catch (MPException | MPApiException e) {
            throw e;
        }
    }

    @Override
    public String receiveNotification(String topic, String resource, Long invoiceId) throws MPException, MPApiException {
        if ("merchant_order".equalsIgnoreCase(topic) && resource != null) {
            String[] resourceParts = resource.split("/");
            Long merchantOrderId = Long.valueOf(resourceParts[resourceParts.length - 1]);
            MerchantOrder merchantOrder = this.merchantOrderClient.get(merchantOrderId);

            if ("paid".equalsIgnoreCase(merchantOrder.getOrderStatus())) {
                InvoiceEntity invoiceEntity = this.invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));
                invoiceEntity.setStatus(InvoiceStatus.PAID);
                this.invoiceRepository.save(invoiceEntity);

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