package Latam.Latam.work.hub.services.mp.impl;

import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoServiceImpl.class);

    @Value("${mercadopago.admin.fee.percentage}")
    private int adminFeePercentage;

    @Value("${mercadopago.success.url}")
    private String successUrl;

    @Value("${mercadopago.failure.url}")
    private String failureUrl;

    @Value("${mercadopago.pending.url}")
    private String pendingUrl;

    private final InvoiceRepository invoiceRepository;
    @Transactional
    public String createInvoicePaymentPreference(Long invoiceId, String title, BigDecimal amount,
                                                 String buyerEmail, String sellerEmail) throws MPException, MPApiException {
        logger.info("Creando preferencia de pago para factura ID: {}", invoiceId);

        try {
            PreferenceClient client = new PreferenceClient();

            // Crear ítem para la factura
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(title)
                    .quantity(1)
                    .unitPrice(amount)
                    .build();
            items.add(item);

            // Calcular la comisión del admin
            BigDecimal adminFee = amount
                    .multiply(BigDecimal.valueOf(adminFeePercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

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
            metadata.put("admin_fee", adminFee);

            // IMPORTANTE: Agregar las URLs de retorno
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();

            // Crear la preferencia CON URLs de retorno
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .externalReference("INVOICE-" + invoiceId)
                    .metadata(metadata)
//                    .backUrls(backUrls)
//                    .autoReturn("approved")
                    .build();

            // Crear la preferencia
            Preference preference = client.create(preferenceRequest);

            // Actualizar estado a ISSUED
            invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                invoice.setStatus(InvoiceStatus.ISSUED);
                invoiceRepository.save(invoice);
            });

            logger.info("Preferencia creada con ID: {}", preference.getId());
            return preference.getInitPoint();
        } catch (MPException | MPApiException e) {
            logger.error("Error creando preferencia de pago para factura ID: " + invoiceId, e);
            throw e;
        }
    }

}