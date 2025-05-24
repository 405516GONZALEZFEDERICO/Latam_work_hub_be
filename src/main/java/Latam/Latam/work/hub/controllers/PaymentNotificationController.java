package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/notifications")
@RequiredArgsConstructor
public class PaymentNotificationController {

    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/{invoiceId}")
    public ResponseEntity<String> handleMercadoPagoNotification(
            @PathVariable Long invoiceId,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Map<String, String> allParams,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            System.out.println("Recibida notificación para factura: " + invoiceId);
            System.out.println("Query params: " + allParams);
            System.out.println("Request body: " + (body != null ? body : "vacío"));
            
            // Intentamos extraer topic y resource de varias fuentes posibles
            String effectiveTopic = null;
            String effectiveResource = null;
            
            // 1. Intentar desde el body
            if (body != null) {
                if (effectiveTopic == null) {
                    effectiveTopic = (String) body.get("topic");
                }
                if (effectiveResource == null) {
                    effectiveResource = (String) body.get("resource");
                }
                
                // 2. En notificaciones de payments a veces viene el dato en data.id
                if (effectiveResource == null && body.get("data") instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data.get("id") != null) {
                        effectiveResource = data.get("id").toString();
                    }
                }
            }
            
            // 3. Intentar desde query params
            if (effectiveTopic == null) {
                effectiveTopic = topic != null ? topic : type; // MercadoPago usa 'topic' o 'type'
            }
            if (effectiveResource == null && id != null) {
                effectiveResource = id; // Para notificaciones de payment, el id es el resource
            }
            
            // Log de información extraída
            System.out.println("Topic extraído: " + effectiveTopic);
            System.out.println("Resource extraído: " + effectiveResource);
            
            // Enviar a procesar
            return ResponseEntity.ok(mercadoPagoService.receiveNotification(effectiveTopic, effectiveResource, invoiceId));
        } catch (Exception e) {
            System.err.println("Error procesando notificación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing notification: " + e.getMessage());
        }
    }
}