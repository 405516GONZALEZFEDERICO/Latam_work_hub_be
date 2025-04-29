package Latam.Latam.work.hub.services;

import org.springframework.stereotype.Service;

@Service
public interface MailService {
    void sendPaymentConfirmationEmail(String toEmail, String userName, String espacio, String fecha, double monto);
}
