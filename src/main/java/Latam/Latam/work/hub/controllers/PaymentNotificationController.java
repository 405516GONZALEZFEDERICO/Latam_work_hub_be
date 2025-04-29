package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/notifications")
@RequiredArgsConstructor
public class PaymentNotificationController {

    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/{invoiceId}")
    public ResponseEntity<String> handleMercadoPagoNotification(@PathVariable Long invoiceId,
                                                                @RequestBody(required = false) Map<String, Object> body) {
        try {
            String topic = (String) body.get("topic");
            String resource = (String) body.get("resource");

            return ResponseEntity.ok(mercadoPagoService.receiveNotification(topic, resource, invoiceId));


        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing notification");
        }
    }
}