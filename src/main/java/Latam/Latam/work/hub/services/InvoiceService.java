package Latam.Latam.work.hub.services;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.springframework.stereotype.Service;

@Service
public interface InvoiceService {
    <T extends Billable> String createInvoice(T entity) throws MPException, MPApiException;
}