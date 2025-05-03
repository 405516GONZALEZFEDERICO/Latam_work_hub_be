package Latam.Latam.work.hub.services;

import org.springframework.stereotype.Service;

@Service
public interface MailService {
    void sendPaymentConfirmationEmail(String toEmail, String userName, String espacio, String fecha, double monto);
    /**
     * Envía una notificación al propietario del espacio sobre una nueva reserva
     */
    void sendBookingNotificationToOwner(String ownerEmail, String ownerName, String spaceName,
                                        String userName, String startDate, String endDate,
                                        String startTime, String endTime);

    /**
     * Envía un correo cuando la reserva ha sido completada
     */
    void sendBookingCompletedEmail(String userEmail, String userName, String spaceName);
}
