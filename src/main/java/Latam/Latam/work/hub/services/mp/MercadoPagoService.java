package Latam.Latam.work.hub.services.mp;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
@Service
public interface MercadoPagoService {
    String createInvoicePaymentPreference(Long invoiceId, String title, BigDecimal amount,
                                          String buyerEmail, String sellerEmail) throws MPException, MPApiException;

    String receiveNotification(String topic, String resource, Long userId) throws MPException, MPApiException;

    boolean refundPayment(Long invoiceId) throws MPException, MPApiException;
}