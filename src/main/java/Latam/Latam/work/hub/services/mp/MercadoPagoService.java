package Latam.Latam.work.hub.services.mp;



import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import java.math.BigDecimal;

public interface MercadoPagoService {

    /**
     * Crea una preferencia de pago para una factura
     */
    String createInvoicePaymentPreference(Long invoiceId, String title, BigDecimal amount,
                                          String buyerEmail, String sellerEmail) throws MPException, MPApiException;

    /**
     * Procesa las notificaciones de pago
     */
    void processPaymentNotification(String notificationId, String notificationType) throws MPException, MPApiException;
}