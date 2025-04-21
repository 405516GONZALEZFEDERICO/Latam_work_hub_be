package Latam.Latam.work.hub.services.mp.impl;

import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import jakarta.transaction.Transactional;
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
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoServiceImpl.class);

    @Value("${mercadopago.marketplace.user.id}")
    private String marketplaceUserId;

    @Value("${mercadopago.webhook.url}")
    private String webhookUrl;

    @Value("${mercadopago.admin.fee.percentage}")
    private int adminFeePercentage;

    /**
     * Crea una preferencia de pago para una factura
     */
    @Transactional
    public String createInvoicePaymentPreference(Long invoiceId, String title, BigDecimal amount,
                                                 String buyerEmail, String sellerEmail) throws MPException, MPApiException {
        logger.info("Creando preferencia de pago para factura ID: {}", invoiceId);

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

        // Crear comprador
        PreferencePayerRequest payer = PreferencePayerRequest.builder()
                .email(buyerEmail)
                .build();

        // Metadata para seguimiento interno
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoice_id", invoiceId);
        metadata.put("buyer_email", buyerEmail);
        metadata.put("seller_email", sellerEmail);
        metadata.put("marketplace_user_id", marketplaceUserId);
        metadata.put("admin_fee", adminFee);

        // Crear la preferencia
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
                .payer(payer)
                .externalReference("INVOICE-" + invoiceId)
                // Metadata para tu propio seguimiento
                .metadata(metadata)
                // URLs de retorno después del pago
                .backUrls(PreferenceBackUrlsRequest.builder()
                        .success(webhookUrl + "/success")
                        .failure(webhookUrl + "/failure")
                        .pending(webhookUrl + "/pending")
                        .build())
                .autoReturn("approved")
                .notificationUrl(webhookUrl + "/notifications")
                .build();

        // Para la integración de marketplace, usa el método de procesamiento de la API de MercadoPago
        // correspondiente a tu versión del SDK. En versiones más recientes, esto se maneja por separado
        // después de crear la preferencia básica.

        // Crear la preferencia
        Preference preference = client.create(preferenceRequest);

        // Aquí puedes hacer la llamada adicional para asociar la preferencia con el marketplace
        // Esto depende de la versión específica de la API de MercadoPago que estés utilizando
        logger.info("Preferencia creada con ID: {}", preference.getId());

        return preference.getInitPoint();
    }

    /**
     * Procesa las notificaciones de pago
     */
    @Transactional
    public void processPaymentNotification(String notificationId, String notificationType) throws MPException, MPApiException {
        logger.info("Procesando notificación de pago con ID: {}", notificationId);

        if (!"payment".equals(notificationType)) {
            logger.info("Tipo de notificación no soportado: {}", notificationType);
            return;
        }

        // Validar que la notificación sea auténtica
        if (!validateNotificationAuthenticity(notificationId)) {
            logger.warn("Notificación no válida o no auténtica: {}", notificationId);
            return;
        }

        PaymentClient paymentClient = new PaymentClient();
        Long paymentId = Long.parseLong(notificationId);
        Payment mpPayment = paymentClient.get(paymentId);

        // Obtener metadata y externalReference
        Map<String, Object> metadata = mpPayment.getMetadata();
        String externalReference = mpPayment.getExternalReference();

        if (externalReference == null || !externalReference.startsWith("INVOICE-")) {
            logger.warn("Referencia externa no válida: {}", externalReference);
            return;
        }

        String invoiceIdStr = externalReference.replace("INVOICE-", "");
        Long invoiceId = Long.parseLong(invoiceIdStr);

        // Obtener emails para notificaciones
        String buyerEmail = metadata != null ? (String) metadata.get("buyer_email") : null;
        String sellerEmail = metadata != null ? (String) metadata.get("seller_email") : null;

        if (buyerEmail == null || sellerEmail == null) {
            logger.warn("Información de correo electrónico faltante en la notificación");
        }

        // Procesar según el estado del pago
        processPaymentByStatus(mpPayment, invoiceId, buyerEmail, sellerEmail);
    }

    private void processPaymentByStatus(Payment payment, Long invoiceId, String buyerEmail, String sellerEmail) {
        switch (payment.getStatus()) {
            case "approved":
                // Actualizar estado de la factura a pagada
                updateInvoiceStatus(invoiceId, "PAID");
                logger.info("Pago aprobado para factura {}", invoiceId);
                sendPaymentConfirmationEmails(invoiceId, buyerEmail, sellerEmail);
                break;

            case "rejected":
                updateInvoiceStatus(invoiceId, "REJECTED");
                logger.info("Pago rechazado para factura {}", invoiceId);
                sendPaymentFailureEmail(invoiceId, buyerEmail);
                break;

            case "pending":
                updateInvoiceStatus(invoiceId, "PENDING");
                logger.info("Pago pendiente para factura {}", invoiceId);
                sendPaymentPendingEmails(invoiceId, buyerEmail, sellerEmail);
                break;

            case "in_process":
                updateInvoiceStatus(invoiceId, "PROCESSING");
                logger.info("Pago en proceso para factura {}", invoiceId);
                break;

            default:
                logger.info("Estado de pago no manejado: {}", payment.getStatus());
                break;
        }
    }

    /**
     * Actualiza el estado de una factura
     * Este método debería interactuar con tu repositorio o servicio de facturas
     */
    private void updateInvoiceStatus(Long invoiceId, String status) {
        // Aquí implementa la lógica para actualizar el estado de la factura
        logger.info("Actualizando factura {} a estado {}", invoiceId, status);
        // invoiceRepository.updateStatus(invoiceId, status);
    }

    /**
     * Valida la autenticidad de una notificación de MercadoPago
     * Implementa la lógica de seguridad recomendada por MercadoPago
     */
    private boolean validateNotificationAuthenticity(String notificationId) {
        // Implementar verificación de seguridad según la documentación de MercadoPago
        // Por ejemplo, verificar firmas digitales, tokens, o realizar consultas de verificación
        logger.info("Validando notificación: {}", notificationId);
        return true; // Cambiar por la implementación real
    }

    // Métodos para enviar emails
    private void sendPaymentConfirmationEmails(Long invoiceId, String buyerEmail, String sellerEmail) {
        logger.info("Enviando confirmación de pago a {} y {} para factura {}",
                buyerEmail, sellerEmail, invoiceId);
        // Aquí se implementaría el envío real de emails
    }

    private void sendPaymentFailureEmail(Long invoiceId, String buyerEmail) {
        logger.info("Enviando notificación de fallo de pago a {} para factura {}",
                buyerEmail, invoiceId);
        // Aquí se implementaría el envío real de emails
    }

    private void sendPaymentPendingEmails(Long invoiceId, String buyerEmail, String sellerEmail) {
        logger.info("Enviando notificación de pago pendiente a {} y {} para factura {}",
                buyerEmail, sellerEmail, invoiceId);
        // Aquí se implementaría el envío real de emails
    }
}