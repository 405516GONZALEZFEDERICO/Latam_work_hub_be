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
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.w3c.tidy.Tidy;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailServiceImpl  implements MailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String EMAIL_FROM;

    /**
     * Método auxiliar para crear la estructura HTML base de los emails
     */
    private String createEmailTemplate(String title, String titleColor, String content) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px; margin: 0;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h2 style="color: %s; margin: 0; font-size: 24px;">%s</h2>
                    </div>
                    %s
                    <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; text-align: center;">
                        <p style="font-size: 14px; color: #555; margin: 0;">El equipo de <strong>LATAM Work Hub</strong></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(titleColor, title, content);
    }

    @Override
    public void sendPaymentConfirmationEmail(String toEmail, String userName, String espacio, String fecha, double monto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Confirmación de Pago - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">¡Gracias por tu pago, <strong>%s</strong>!</p>
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
                """.formatted(userName, espacio, fecha, monto);

            String htmlContent = createEmailTemplate("¡Pago Confirmado!", "#4CAF50", content);

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendBookingNotificationToOwner(String ownerEmail, String ownerName, String spaceName, String userName, String startDate, String endDate, String startTime, String endTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Nueva reserva en tu espacio - LATAM Work Hub";
            
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(String.format("<p style=\"font-size: 16px;\">Hola <strong>%s</strong>,</p>", ownerName));
            contentBuilder.append(String.format("<p style=\"font-size: 16px;\">Te informamos que tu espacio '<strong>%s</strong>' ha sido reservado por <strong>%s</strong>.</p>", spaceName, userName));
            
            contentBuilder.append("<div style=\"background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0;\">");
            contentBuilder.append("<p style=\"margin: 5px 0; font-weight: bold;\">Detalles de la reserva:</p>");
            
            if (!startDate.isEmpty()) {
                contentBuilder.append(String.format("<p style=\"margin: 5px 0;\">• Fecha de inicio: %s</p>", startDate));
            }
            if (!endDate.isEmpty()) {
                contentBuilder.append(String.format("<p style=\"margin: 5px 0;\">• Fecha de finalización: %s</p>", endDate));
            }
            if (!startTime.isEmpty()) {
                contentBuilder.append(String.format("<p style=\"margin: 5px 0;\">• Hora de inicio: %s</p>", startTime));
            }
            if (!endTime.isEmpty()) {
                contentBuilder.append(String.format("<p style=\"margin: 5px 0;\">• Hora de finalización: %s</p>", endTime));
            }
            contentBuilder.append("</div>");
            
            contentBuilder.append("<p style=\"font-size: 15px;\">Puedes ver más detalles en tu cuenta de LATAM Work Hub.</p>");

            String htmlContent = createEmailTemplate("Nueva Reserva", "#2196F3", contentBuilder.toString());

            helper.setTo(ownerEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendBookingCompletedEmail(String userEmail, String userName, String spaceName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Tu reserva ha finalizado - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Tu reserva del espacio '<strong>%s</strong>' ha finalizado. Esperamos que hayas tenido una gran experiencia.</p>
                
                <div style="background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold; color: #1976d2;">¡Gracias por usar LATAM Work Hub!</p>
                    <p style="margin: 5px 0;">Esperamos verte de nuevo pronto.</p>
                </div>
                """.formatted(userName, spaceName);

            String htmlContent = createEmailTemplate("Reserva Finalizada", "#FF9800", content);

            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendBookingRefundConfirmationEmail(String userEmail, String userName, String spaceName, Double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Confirmación de Reembolso - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te confirmamos que hemos procesado el reembolso por la cancelación de tu reserva del espacio "<strong>%s</strong>".</p>
                
                <div style="background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles del reembolso:</p>
                    <p style="margin: 5px 0;">Monto reembolsado: <strong>$%.2f</strong></p>
                    <p style="margin: 5px 0;">El reembolso se procesará a través del mismo método de pago utilizado para la reserva.</p>
                </div>
                
                <p style="font-size: 14px; color: #777;">Según la política de tu entidad bancaria, el reembolso puede tardar entre 3 y 15 días hábiles en reflejarse en tu cuenta.</p>
                <p style="margin-top: 20px; font-size: 15px;">Gracias por tu comprensión.</p>
                """.formatted(userName, spaceName, amount);

            String htmlContent = createEmailTemplate("Reembolso Procesado", "#3498DB", content);

            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo de reembolso: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendNewContractNotificationToOwner(String ownerEmail, String ownerName, String spaceName, String tenantName, String startDate, String endDate, String monthlyAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Nuevo contrato creado - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Se ha creado un nuevo contrato para tu espacio '<strong>%s</strong>'.</p>
                
                <div style="background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles del contrato:</p>
                    <p style="margin: 5px 0;">• Inquilino: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Fecha de inicio: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Fecha de finalización: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Monto mensual: <strong>$%s</strong></p>
                </div>
                
                <p style="font-size: 15px;">Puedes ver más detalles accediendo a tu cuenta.</p>
                """.formatted(ownerName, spaceName, tenantName, startDate, endDate, monthlyAmount);

            String htmlContent = createEmailTemplate("Nuevo Contrato", "#4CAF50", content);

            helper.setTo(ownerEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendNewInvoiceNotification(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String dueDate, String totalAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Nueva factura disponible - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Se ha generado una nueva factura para tu contrato del espacio '<strong>%s</strong>'.</p>
                
                <div style="background-color: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles de la factura:</p>
                    <p style="margin: 5px 0;">• Número de factura: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Fecha de vencimiento: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Total a pagar: <strong>$%s</strong></p>
                </div>
                
                <p style="font-size: 15px;">Por favor, realiza el pago antes de la fecha de vencimiento.</p>
                <p style="font-size: 15px;">Gracias por usar LATAM Work Hub.</p>
                """.formatted(tenantName, spaceName, invoiceNumber, dueDate, totalAmount);

            String htmlContent = createEmailTemplate("Nueva Factura", "#FF9800", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPaymentReminderEmail(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String dueDate, String totalAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Recordatorio de pago - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te recordamos que la factura número <strong>%s</strong> correspondiente al espacio '<strong>%s</strong>' vence el <strong>%s</strong>.</p>
                
                <div style="background-color: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Información del pago:</p>
                    <p style="margin: 5px 0;">• Total a pagar: <strong>$%s</strong></p>
                    <p style="margin: 5px 0;">• Fecha de vencimiento: <strong>%s</strong></p>
                </div>
                
                <p style="font-size: 15px;">Por favor, realiza el pago a tiempo para evitar cargos adicionales.</p>
                <p style="font-size: 15px;">Gracias por usar LATAM Work Hub.</p>
                """.formatted(tenantName, invoiceNumber, spaceName, dueDate, totalAmount, dueDate);

            String htmlContent = createEmailTemplate("Recordatorio de Pago", "#FF9800", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendContractExpirationReminder(String tenantEmail, String tenantName, String spaceName, String endDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Tu contrato está por vencer - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te informamos que tu contrato del espacio '<strong>%s</strong>' finalizará el <strong>%s</strong>.</p>
                
                <div style="background-color: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">¿Deseas renovar tu contrato?</p>
                    <p style="margin: 5px 0;">Si deseas renovarlo, por favor contáctanos o renueva desde tu cuenta.</p>
                </div>
                
                <p style="font-size: 15px;">Gracias por usar LATAM Work Hub.</p>
                """.formatted(tenantName, spaceName, endDate);

            String htmlContent = createEmailTemplate("Contrato por Vencer", "#FF9800", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendContractCancellationNotification(String tenantEmail, String tenantName, String spaceName, String startDate, String endDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Tu contrato ha sido cancelado - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te informamos que tu contrato del espacio '<strong>%s</strong>' ha sido cancelado.</p>
                
                <div style="background-color: #fde8e8; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles del contrato cancelado:</p>
                    <p style="margin: 5px 0;">• Período: <strong>%s - %s</strong></p>
                </div>
                
                <p style="font-size: 15px;">Gracias por usar LATAM Work Hub.</p>
                """.formatted(tenantName, spaceName, startDate, endDate);

            String htmlContent = createEmailTemplate("Contrato Cancelado", "#e74c3c", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendOwnerContractCancellationNotification(String ownerEmail, String ownerName, String spaceName, String tenantName, String startDate, String endDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Contrato cancelado - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">El contrato del espacio '<strong>%s</strong>' con el inquilino <strong>%s</strong> ha sido cancelado.</p>
                
                <div style="background-color: #fde8e8; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles del contrato cancelado:</p>
                    <p style="margin: 5px 0;">• Inquilino: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Período: <strong>%s - %s</strong></p>
                </div>
                """.formatted(ownerName, spaceName, tenantName, tenantName, startDate, endDate);

            String htmlContent = createEmailTemplate("Contrato Cancelado", "#e74c3c", content);

            helper.setTo(ownerEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendContractRenewalNotification(String tenantEmail, String tenantName, String spaceName, String months, String newEndDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Tu contrato ha sido renovado - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Tu contrato del espacio '<strong>%s</strong>' ha sido renovado exitosamente.</p>
                
                <div style="background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles de la renovación:</p>
                    <p style="margin: 5px 0;">• Duración de la renovación: <strong>%s meses</strong></p>
                    <p style="margin: 5px 0;">• Nueva fecha de finalización: <strong>%s</strong></p>
                </div>
                
                <p style="font-size: 15px;">Gracias por seguir confiando en LATAM Work Hub.</p>
                """.formatted(tenantName, spaceName, months, newEndDate);

            String htmlContent = createEmailTemplate("Contrato Renovado", "#4CAF50", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendOwnerContractRenewalNotification(String ownerEmail, String ownerName, String spaceName, String tenantName, String months, String newEndDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Contrato renovado - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">El contrato del espacio '<strong>%s</strong>' con el inquilino <strong>%s</strong> ha sido renovado.</p>
                
                <div style="background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles de la renovación:</p>
                    <p style="margin: 5px 0;">• Inquilino: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Duración: <strong>%s meses</strong></p>
                    <p style="margin: 5px 0;">• Nueva fecha de finalización: <strong>%s</strong></p>
                </div>
                
                <p style="font-size: 15px;">Gracias por seguir usando LATAM Work Hub.</p>
                """.formatted(ownerName, spaceName, tenantName, tenantName, months, newEndDate);

            String htmlContent = createEmailTemplate("Contrato Renovado", "#4CAF50", content);

            helper.setTo(ownerEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendRentalPaymentConfirmation(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String paymentDate, String amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Confirmación de Pago de Alquiler - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Hemos recibido correctamente tu pago de alquiler para el espacio "<strong>%s</strong>".</p>
                
                <div style="background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles del pago:</p>
                    <p style="margin: 5px 0;">• Factura: <strong>#%s</strong></p>
                    <p style="margin: 5px 0;">• Fecha de pago: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Monto pagado: <strong>$%s</strong></p>
                </div>
                
                <p style="font-size: 14px; color: #777;">La factura y el recibo están disponibles en tu cuenta.</p>
                <p style="margin-top: 20px; font-size: 15px;">¡Gracias por tu puntualidad!</p>
                """.formatted(tenantName, spaceName, invoiceNumber, paymentDate, amount);

            String htmlContent = createEmailTemplate("¡Pago Confirmado!", "#4CAF50", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo de confirmación de pago de alquiler: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendDepositRefundNotification(String tenantEmail, String tenantName, String spaceName, String refundAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Reembolso de Depósito - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te confirmamos que hemos procesado el reembolso del depósito correspondiente a tu contrato del espacio "<strong>%s</strong>".</p>
                
                <div style="background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles del reembolso:</p>
                    <p style="margin: 5px 0;">• Monto reembolsado: <strong>$%s</strong></p>
                    <p style="margin: 5px 0;">• El reembolso se procesará a través del mismo método de pago utilizado para el depósito.</p>
                </div>
                
                <p style="font-size: 14px; color: #777;">Según la política de tu entidad bancaria, el reembolso puede tardar entre 3 y 15 días hábiles en reflejarse en tu cuenta.</p>
                <p style="margin-top: 20px; font-size: 15px;">Gracias por haber confiado en nosotros.</p>
                """.formatted(tenantName, spaceName, refundAmount);

            String htmlContent = createEmailTemplate("Reembolso de Depósito Procesado", "#3498DB", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo de reembolso de depósito: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendAutoRenewalSetupNotification(String tenantEmail, String tenantName, String spaceName,
                                                 String endDate, String renewalMonths, String status) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Configuración de Renovación Automática - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te confirmamos que la configuración de renovación automática para tu contrato del espacio "<strong>%s</strong>" ha sido <strong>%s</strong>.</p>
                
                <div style="background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles de la configuración:</p>
                    <p style="margin: 5px 0;">• Fecha actual de finalización del contrato: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Duración de la renovación: <strong>%s meses</strong></p>
                </div>
                
                <p style="font-size: 14px; color: #777;">Puedes modificar esta configuración en cualquier momento desde tu cuenta.</p>
                <p style="margin-top: 20px; font-size: 15px;">Gracias por confiar en nosotros.</p>
                """.formatted(tenantName, spaceName, status, endDate, renewalMonths);

            String htmlContent = createEmailTemplate("Renovación Automática " + status, "#3498DB", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo de configuración de renovación automática: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendUpcomingAutoRenewalNotification(String tenantEmail, String tenantName, String spaceName,
                                                    String endDate, String renewalMonths) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Renovación Automática Próxima - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te recordamos que tu contrato del espacio "<strong>%s</strong>" está configurado para renovarse automáticamente.</p>
                
                <div style="background-color: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Información importante:</p>
                    <p style="margin: 5px 0;">• Fecha actual de finalización: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• El contrato se renovará automáticamente por: <strong>%s meses</strong></p>
                    <p style="margin: 10px 0; font-weight: bold; color: #f57c00;">Si deseas cancelar la renovación automática, debes hacerlo antes de la fecha de finalización.</p>
                </div>
                
                <p style="font-size: 14px; color: #777;">Puedes gestionar la configuración de renovación automática desde tu cuenta en cualquier momento.</p>
                <p style="margin-top: 20px; font-size: 15px;">Gracias por confiar en nosotros.</p>
                """.formatted(tenantName, spaceName, endDate, renewalMonths);

            String htmlContent = createEmailTemplate("Aviso de Renovación Automática", "#FF9800", content);

            helper.setTo(tenantEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo de aviso de renovación automática: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendOverdueInvoiceNotification(String email, String name, String spaceName,
                                               String amount, String dueDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Factura Vencida - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te notificamos que tienes una factura vencida del espacio "<strong>%s</strong>".</p>

                <div style="background-color: #fde8e8; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles de la factura:</p>
                    <p style="margin: 5px 0;">• Monto vencido: <strong>$%s</strong></p>
                    <p style="margin: 5px 0;">• Fecha de vencimiento: <strong>%s</strong></p>
                </div>

                <p style="font-size: 15px;">Por favor, realiza el pago lo antes posible para evitar recargos adicionales.</p>
                <p style="margin-top: 20px; font-size: 14px;">Si ya realizaste el pago, por favor ignora este mensaje.</p>
                """.formatted(name, spaceName, amount, dueDate);

            String htmlContent = createEmailTemplate("Aviso de Factura Vencida", "#e74c3c", content);

            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar notificación de factura vencida: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendOwnerOverdueInvoiceNotification(String email, String ownerName, String spaceName,
                                                    String tenantName, String amount, String dueDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Notificación de Pago Vencido - LATAM Work Hub";
            String content = """
                <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                <p style="font-size: 16px;">Te informamos que existe un pago vencido correspondiente a tu espacio "<strong>%s</strong>".</p>

                <div style="background-color: #fde8e8; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0; font-weight: bold;">Detalles:</p>
                    <p style="margin: 5px 0;">• Inquilino: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">• Monto vencido: <strong>$%s</strong></p>
                    <p style="margin: 5px 0;">• Fecha de vencimiento: <strong>%s</strong></p>
                </div>

                <p style="font-size: 15px;">Nuestro equipo está gestionando el cobro de este pago.</p>
                <p style="margin-top: 20px; font-size: 14px;">Te mantendremos informado sobre la situación.</p>
                """.formatted(ownerName, spaceName, tenantName, amount, dueDate);

            String htmlContent = createEmailTemplate("Aviso de Pago Vencido", "#e74c3c", content);

            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar notificación de pago vencido al propietario: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendOwnerAndTenantAutoRenewalNotification(
            String tenantEmail,
            String ownerEmail,
            String spaceName,
            Boolean isAutoRenewal
    ) {
        String autoRenewalStatus = Boolean.TRUE.equals(isAutoRenewal) ? "activada" : "desactivada";

        String tenantContent = """
            <p style="font-size: 16px;">Hola,</p>
            <p style="font-size: 16px;">Te informamos que la renovación automática del espacio <strong>%s</strong> ha sido <strong>%s</strong>.</p>
            
            <div style="background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                <p style="margin: 5px 0; font-weight: bold;">Estado actual:</p>
                <p style="margin: 5px 0;">Renovación automática: <strong>%s</strong></p>
            </div>
            
            <p style="font-size: 15px;">Si tienes alguna duda, no dudes en contactarnos.</p>
            """.formatted(spaceName, autoRenewalStatus, autoRenewalStatus);

        String ownerContent = """
            <p style="font-size: 16px;">Hola,</p>
            <p style="font-size: 16px;">Te notificamos que la renovación automática del espacio <strong>%s</strong> ha sido <strong>%s</strong> por el inquilino.</p>
            
            <div style="background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                <p style="margin: 5px 0; font-weight: bold;">Estado actual:</p>
                <p style="margin: 5px 0;">Renovación automática: <strong>%s</strong></p>
            </div>
            
            <p style="font-size: 15px;">Podés revisar los detalles en tu panel de administración.</p>
            """.formatted(spaceName, autoRenewalStatus, autoRenewalStatus);

        try {
            String tenantHtml = createEmailTemplate("Actualización de Renovación Automática", "#2c3e50", tenantContent);
            String ownerHtml = createEmailTemplate("Actualización de Renovación Automática", "#2c3e50", ownerContent);
            
            sendEmail(tenantEmail, "Actualización de Renovación Automática", tenantHtml);
            sendEmail(ownerEmail, "Actualización de Renovación Automática", ownerHtml);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar correos de notificación");
        }
    }

    private void sendEmail(String to, String subject, String content) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(EMAIL_FROM);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true para HTML

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo");
        }
    }
}
