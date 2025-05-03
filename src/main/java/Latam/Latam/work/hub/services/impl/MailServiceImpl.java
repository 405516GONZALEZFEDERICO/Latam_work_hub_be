package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.services.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.w3c.tidy.Tidy;

@Service
@RequiredArgsConstructor
public class MailServiceImpl  implements MailService {
    private final JavaMailSender mailSender;

    private final Tidy tidy;

    @Value("${spring.mail.username}")
    private String EMAIL_FROM;


    @Override
    public void sendPaymentConfirmationEmail(String toEmail, String userName, String espacio, String fecha, double monto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Confirmación de Pago - LATAM Work Hub";
            String content = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                    <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                        <div style="text-align: center;">
                            <img src="https://i.imgur.com/EZ9pXJY.png" alt="LATAM Work Hub" style="width: 150px; margin-bottom: 20px;" />
                            <h2 style="color: #4CAF50;">¡Gracias por tu pago, %s!</h2>
                        </div>
            
                        <p style="font-size: 16px;">Tu reserva ha sido confirmada con éxito.</p>
            
                        <table style="width: 100%%; margin-top: 20px; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 10px; border-bottom: 1px solid #ddd;"><strong>Espacio:</strong></td>
                                <td style="padding: 10px; border-bottom: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border-bottom: 1px solid #ddd;"><strong>Fecha:</strong></td>
                                <td style="padding: 10px; border-bottom: 1px solid #ddd;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border-bottom: 1px solid #ddd;"><strong>Monto pagado:</strong></td>
                                <td style="padding: 10px; border-bottom: 1px solid #ddd;">$%.2f</td>
                            </tr>
                        </table>
            
                        <p style="margin-top: 20px; font-size: 15px;">¡Esperamos verte pronto!</p>
                        <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                    </div>
                </body>
                </html>
    """.formatted(userName, espacio, fecha, monto);


            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }


    @Override
    public void sendBookingNotificationToOwner(String ownerEmail, String ownerName, String spaceName, String userName, String startDate, String endDate, String startTime, String endTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(ownerEmail);
        message.setSubject("Nueva reserva en tu espacio - WorkHub");

        StringBuilder body = new StringBuilder();
        body.append(String.format("Hola %s,\n\n", ownerName));
        body.append(String.format("Te informamos que tu espacio '%s' ha sido reservado por %s.\n\n", spaceName, userName));
        body.append("Detalles de la reserva:\n");

        if (!startDate.isEmpty()) {
            body.append(String.format("- Fecha de inicio: %s\n", startDate));
        }

        if (!endDate.isEmpty()) {
            body.append(String.format("- Fecha de finalización: %s\n", endDate));
        }

        if (!startTime.isEmpty()) {
            body.append(String.format("- Hora de inicio: %s\n", startTime));
        }

        if (!endTime.isEmpty()) {
            body.append(String.format("- Hora de finalización: %s\n", endTime));
        }

        body.append("\nPuedes ver más detalles en tu cuenta de WorkHub.\n\n");
        body.append("Saludos,\nEl equipo de WorkHub");

        message.setText(body.toString());
        mailSender.send(message);
    }

    @Override
    public void sendBookingCompletedEmail(String userEmail, String userName, String spaceName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("Tu reserva ha finalizado - WorkHub");

        String body = String.format(
                "Hola %s,\n\n" +
                        "Tu reserva del espacio '%s' ha finalizado. Esperamos que hayas tenido una gran experiencia.\n\n" +
                        "¡Gracias por usar WorkHub!\n\n" +
                        "Saludos,\n" +
                        "El equipo de WorkHub",
                userName, spaceName
        );

        message.setText(body);
        mailSender.send(message);
    }
}
