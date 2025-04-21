package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private MercadoPagoService mercadoPagoService;

    /**
     * Crea una preferencia de pago para una factura
     */
    @PostMapping("/create-preference")
    @PreAuthorize("hasRole('CLIENTE') || hasRole('PROVEEDOR')")
    public ResponseEntity<?> createPaymentPreference(
            @RequestParam("invoiceId") Long invoiceId,
            @RequestParam("title") String title,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("buyerEmail") String buyerEmail,
            @RequestParam("sellerEmail") String sellerEmail) {

        try {
            String preferenceUrl = mercadoPagoService.createInvoicePaymentPreference(
                    invoiceId, title, amount, buyerEmail, sellerEmail);

            Map<String, String> response = new HashMap<>();
            response.put("preferenceUrl", preferenceUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al crear preferencia de pago", e);
            return ResponseEntity.badRequest().body("Error al crear preferencia: " + e.getMessage());
        }
    }

    /**
     * Endpoint para recibir notificaciones de Mercado Pago (webhook)
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestParam(value = "id", required = false) String notificationId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data_id", required = false) String dataId) {

        logger.info("Webhook recibido - topic: {}, type: {}, id: {}, data_id: {}",
                topic, type, notificationId, dataId);

        try {
            // Determinar qué ID procesar según el formato de la notificación
            String idToProcess = null;
            String notificationType = null;

            if ("payment".equals(topic)) {
                idToProcess = notificationId;
                notificationType = topic;
            } else if ("merchant_order".equals(topic)) {
                // Procesar orden de comercio si es necesario
                return ResponseEntity.ok("merchant_order recibido");
            } else if (type != null && "payment".equals(type)) {
                idToProcess = dataId;
                notificationType = type;
            }

            if (idToProcess != null) {
                mercadoPagoService.processPaymentNotification(idToProcess, notificationType);
                return ResponseEntity.ok("Notificación procesada correctamente");
            } else {
                logger.warn("No se pudo determinar el ID a procesar");
                return ResponseEntity.ok("Notificación recibida pero no procesada");
            }
        } catch (MPException | MPApiException e) {
            logger.error("Error al procesar notificación", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Maneja redirecciones después del pago (success)
     */
    @GetMapping("/success")
    public RedirectView paymentSuccess(
            @RequestParam(value = "collection_id", required = false) String paymentId,
            @RequestParam(value = "collection_status", required = false) String status,
            @RequestParam(value = "external_reference", required = false) String externalReference,
            @RequestParam(value = "preference_id", required = false) String preferenceId) {

        logger.info("Pago exitoso - payment_id: {}, status: {}, external_reference: {}",
                paymentId, status, externalReference);

        // Extraer el ID de la factura del external_reference
        String invoiceIdStr = externalReference.replace("INVOICE-", "");

        // Redirigir a la página de éxito en el frontend
        return new RedirectView("/payment-success?invoiceId=" + invoiceIdStr);
    }

    /**
     * Maneja redirecciones después del pago (failure)
     */
    @GetMapping("/failure")
    public RedirectView paymentFailure(
            @RequestParam(value = "collection_id", required = false) String paymentId,
            @RequestParam(value = "collection_status", required = false) String status,
            @RequestParam(value = "external_reference", required = false) String externalReference) {

        logger.info("Pago fallido - payment_id: {}, status: {}, external_reference: {}",
                paymentId, status, externalReference);

        // Extraer el ID de la factura
        String invoiceIdStr = externalReference.replace("INVOICE-", "");

        // Redirigir a la página de fallo en el frontend
        return new RedirectView("/payment-failed?invoiceId=" + invoiceIdStr);
    }

    /**
     * Maneja redirecciones después del pago (pending)
     */
    @GetMapping("/pending")
    public RedirectView paymentPending(
            @RequestParam(value = "collection_id", required = false) String paymentId,
            @RequestParam(value = "collection_status", required = false) String status,
            @RequestParam(value = "external_reference", required = false) String externalReference) {

        logger.info("Pago pendiente - payment_id: {}, status: {}, external_reference: {}",
                paymentId, status, externalReference);

        // Extraer el ID de la factura
        String invoiceIdStr = externalReference.replace("INVOICE-", "");

        // Redirigir a la página de pendiente en el frontend
        return new RedirectView("/payment-pending?invoiceId=" + invoiceIdStr);
    }
}