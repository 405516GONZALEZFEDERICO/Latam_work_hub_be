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

    import java.math.BigDecimal;

    @Service
    @RequiredArgsConstructor
    public class MailServiceImpl  implements MailService {
        private final JavaMailSender mailSender;
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


        @Override
        public void sendBookingRefundConfirmationEmail(String userEmail, String userName, String spaceName, Double amount) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String subject = "Confirmación de Reembolso - LATAM Work Hub";
                String content = """
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                        <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                            <div style="text-align: center;">
                                <h2 style="color: #3498DB;">Reembolso Procesado</h2>
                            </div>
                
                            <p style="font-size: 16px;">Hola %s,</p>
                            
                            <p style="font-size: 16px;">Te confirmamos que hemos procesado el reembolso por la cancelación de tu reserva del espacio "%s".</p>
                            
                            <div style="background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                <p style="margin: 5px 0; font-weight: bold;">Detalles del reembolso:</p>
                                <p style="margin: 5px 0;">Monto reembolsado: $%.2f</p>
                                <p style="margin: 5px 0;">El reembolso se procesará a través del mismo método de pago utilizado para la reserva.</p>
                            </div>
                            
                            <p style="font-size: 14px; color: #777;">Según la política de tu entidad bancaria, el reembolso puede tardar entre 3 y 15 días hábiles en reflejarse en tu cuenta.</p>
                
                            <p style="margin-top: 20px; font-size: 15px;">Gracias por tu comprensión.</p>
                            <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                        </div>
                    </body>
                    </html>
                """.formatted(userName, spaceName, amount);


                helper.setTo(userEmail);
                helper.setSubject(subject);
                helper.setText(content, true);

                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Error al enviar correo de reembolso: " + e.getMessage(), e);
            }
        }

        @Override
        public void sendNewContractNotificationToOwner(String ownerEmail, String ownerName, String spaceName, String tenantName, String startDate, String endDate, String monthlyAmount) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(ownerEmail);
            message.setSubject("Nuevo contrato creado - LATAM Work Hub");

            String body = String.format(
                    "Hola %s,\n\n" +
                            "Se ha creado un nuevo contrato para tu espacio '%s'.\n\n" +
                            "Inquilino: %s\n" +
                            "Fecha de inicio: %s\n" +
                            "Fecha de finalización: %s\n" +
                            "Monto mensual: $%s\n\n" +
                            "Puedes ver más detalles accediendo a tu cuenta.\n\n" +
                            "Saludos,\n" +
                            "El equipo de LATAM Work Hub",
                    ownerName, spaceName, tenantName, startDate, endDate, monthlyAmount
            );

            message.setText(body);
            mailSender.send(message);
        }
        @Override
        public void sendNewInvoiceNotification(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String dueDate, String totalAmount) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenantEmail);
            message.setSubject("Nueva factura disponible - LATAM Work Hub");

            String body = String.format(
                    "Hola %s,\n\n" +
                            "Se ha generado una nueva factura para tu contrato del espacio '%s'.\n\n" +
                            "Número de factura: %s\n" +
                            "Fecha de vencimiento: %s\n" +
                            "Total a pagar: $%s\n\n" +
                            "Por favor, realiza el pago antes de la fecha de vencimiento.\n\n" +
                            "Gracias por usar LATAM Work Hub.",
                    tenantName, spaceName, invoiceNumber, dueDate, totalAmount
            );

            message.setText(body);
            mailSender.send(message);
        }
        @Override
        public void sendPaymentReminderEmail(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String dueDate, String totalAmount) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenantEmail);
            message.setSubject("Recordatorio de pago - LATAM Work Hub");

            String body = String.format(
                    "Hola %s,\n\n" +
                            "Te recordamos que la factura número %s correspondiente al espacio '%s' vence el %s.\n\n" +
                            "Total a pagar: $%s\n\n" +
                            "Por favor, realiza el pago a tiempo para evitar cargos adicionales.\n\n" +
                            "Gracias por usar LATAM Work Hub.",
                    tenantName, invoiceNumber, spaceName, dueDate, totalAmount
            );

            message.setText(body);
            mailSender.send(message);
        }
        @Override
        public void sendContractExpirationReminder(String tenantEmail, String tenantName, String spaceName, String endDate) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenantEmail);
            message.setSubject("Tu contrato está por vencer - LATAM Work Hub");

            String body = String.format(
                    "Hola %s,\n\n" +
                            "Te informamos que tu contrato del espacio '%s' finalizará el %s.\n\n" +
                            "Si deseas renovarlo, por favor contáctanos o renueva desde tu cuenta.\n\n" +
                            "Gracias por usar LATAM Work Hub.",
                    tenantName, spaceName, endDate
            );

            message.setText(body);
            mailSender.send(message);
        }
        @Override
        public void sendContractCancellationNotification(String tenantEmail, String tenantName, String spaceName, String startDate, String endDate) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenantEmail);
            message.setSubject("Tu contrato ha sido cancelado - LATAM Work Hub");

            String body = String.format(
                    "Hola %s,\n\n" +
                            "Te informamos que tu contrato del espacio '%s' ha sido cancelado.\n\n" +
                            "Período: %s - %s\n\n" +
                            "Gracias por usar LATAM Work Hub.",
                    tenantName, spaceName, startDate, endDate
            );

            message.setText(body);
            mailSender.send(message);
        }
        @Override
        public void sendOwnerContractCancellationNotification(String ownerEmail, String ownerName, String spaceName, String tenantName, String startDate, String endDate) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(ownerEmail);
            message.setSubject("Contrato cancelado - LATAM Work Hub");

            String body = String.format(
                    "Hola %s,\n\n" +
                            "El contrato del espacio '%s' con el inquilino %s ha sido cancelado.\n\n" +
                            "Período: %s - %s\n\n" +
                            "Saludos,\n" +
                            "El equipo de LATAM Work Hub",
                    ownerName, spaceName, tenantName, startDate, endDate
            );

            message.setText(body);
            mailSender.send(message);
        }
        @Override
        public void sendContractRenewalNotification(String tenantEmail, String tenantName, String spaceName, String months, String newEndDate) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenantEmail);
            message.setSubject("Tu contrato ha sido renovado - LATAM Work Hub");

            String body = String.format(
                    "Hola %s,\n\n" +
                            "Tu contrato del espacio '%s' ha sido renovado por %s meses.\n" +
                            "Nueva fecha de finalización: %s\n\n" +
                            "Gracias por seguir confiando en LATAM Work Hub.",
                    tenantName, spaceName, months, newEndDate
            );

            message.setText(body);
            mailSender.send(message);
        }
        @Override
        public void sendOwnerContractRenewalNotification(String ownerEmail, String ownerName, String spaceName, String tenantName, String months, String newEndDate) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(ownerEmail);
            message.setSubject("Contrato renovado - LATAM Work Hub");

            String body = String.format(
                    "Hola %s,\n\n" +
                            "El contrato del espacio '%s' con el inquilino %s ha sido renovado por %s meses.\n" +
                            "Nueva fecha de finalización: %s\n\n" +
                            "Gracias por seguir usando LATAM Work Hub.",
                    ownerName, spaceName, tenantName, months, newEndDate
            );

            message.setText(body);
            mailSender.send(message);
        }
        @Override
        public void sendContractActivationNotification(String tenantEmail, String tenantName, String spaceName, String startDate, String endDate, String monthlyAmount) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String subject = "Contrato Activado - LATAM Work Hub";
                String content = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center;">
                        <h2 style="color: #4CAF50;">¡Tu contrato ha sido activado!</h2>
                    </div>
        
                    <p style="font-size: 16px;">Hola %s,</p>
                    
                    <p style="font-size: 16px;">Te confirmamos que tu contrato para el espacio "%s" ha sido activado exitosamente.</p>
                    
                    <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 5px 0; font-weight: bold;">Detalles del contrato:</p>
                        <p style="margin: 5px 0;">Fecha de inicio: %s</p>
                        <p style="margin: 5px 0;">Fecha de finalización: %s</p>
                        <p style="margin: 5px 0;">Monto mensual: $%s</p>
                    </div>
                    
                    <p style="font-size: 15px;">Podrás acceder a todos los detalles de tu contrato desde tu cuenta en nuestra plataforma.</p>
        
                    <p style="margin-top: 20px; font-size: 15px;">¡Gracias por confiar en nosotros!</p>
                    <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                </div>
            </body>
            </html>
        """.formatted(tenantName, spaceName, startDate, endDate, monthlyAmount);

                helper.setTo(tenantEmail);
                helper.setSubject(subject);
                helper.setText(content, true);

                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Error al enviar correo de activación de contrato: " + e.getMessage(), e);
            }
        }

        @Override
        public void sendOwnerContractActivationNotification(String ownerEmail, String ownerName, String spaceName, String tenantName, String startDate, String endDate) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String subject = "Contrato Activado - LATAM Work Hub";
                String content = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center;">
                        <h2 style="color: #4CAF50;">¡Contrato Activado!</h2>
                    </div>
        
                    <p style="font-size: 16px;">Hola %s,</p>
                    
                    <p style="font-size: 16px;">Te informamos que el contrato para tu espacio "%s" ha sido activado correctamente.</p>
                    
                    <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 5px 0; font-weight: bold;">Detalles del contrato:</p>
                        <p style="margin: 5px 0;">Inquilino: %s</p>
                        <p style="margin: 5px 0;">Fecha de inicio: %s</p>
                        <p style="margin: 5px 0;">Fecha de finalización: %s</p>
                    </div>
                    
                    <p style="font-size: 15px;">Puedes ver todos los detalles accediendo a tu cuenta en nuestra plataforma.</p>
        
                    <p style="margin-top: 20px; font-size: 15px;">Gracias por usar nuestros servicios.</p>
                    <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                </div>
            </body>
            </html>
        """.formatted(ownerName, spaceName, tenantName, startDate, endDate);

                helper.setTo(ownerEmail);
                helper.setSubject(subject);
                helper.setText(content, true);

                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Error al enviar correo de activación de contrato al propietario: " + e.getMessage(), e);
            }
        }

        @Override
        public void sendRentalPaymentConfirmation(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String paymentDate, String amount) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String subject = "Confirmación de Pago de Alquiler - LATAM Work Hub";
                String content = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center;">
                        <h2 style="color: #4CAF50;">¡Pago Confirmado!</h2>
                    </div>
        
                    <p style="font-size: 16px;">Hola %s,</p>
                    
                    <p style="font-size: 16px;">Hemos recibido correctamente tu pago de alquiler para el espacio "%s".</p>
                    
                    <div style="background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 5px 0; font-weight: bold;">Detalles del pago:</p>
                        <p style="margin: 5px 0;">Factura: #%s</p>
                        <p style="margin: 5px 0;">Fecha de pago: %s</p>
                        <p style="margin: 5px 0;">Monto pagado: $%s</p>
                    </div>
                    
                    <p style="font-size: 14px; color: #777;">La factura y el recibo están disponibles en tu cuenta.</p>
        
                    <p style="margin-top: 20px; font-size: 15px;">¡Gracias por tu puntualidad!</p>
                    <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                </div>
            </body>
            </html>
        """.formatted(tenantName, spaceName, invoiceNumber, paymentDate, amount);

                helper.setTo(tenantEmail);
                helper.setSubject(subject);
                helper.setText(content, true);

                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Error al enviar correo de confirmación de pago de alquiler: " + e.getMessage(), e);
            }
        }
        /**
         * Envía notificación de reembolso de depósito al inquilino
         */
        @Override
        public void sendDepositRefundNotification(String tenantEmail, String tenantName, String spaceName, String refundAmount) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String subject = "Reembolso de Depósito - LATAM Work Hub";
                String content = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center;">
                        <h2 style="color: #3498DB;">Reembolso de Depósito Procesado</h2>
                    </div>
        
                    <p style="font-size: 16px;">Hola %s,</p>
                    
                    <p style="font-size: 16px;">Te confirmamos que hemos procesado el reembolso del depósito correspondiente a tu contrato del espacio "%s".</p>
                    
                    <div style="background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 5px 0; font-weight: bold;">Detalles del reembolso:</p>
                        <p style="margin: 5px 0;">Monto reembolsado: $%s</p>
                        <p style="margin: 5px 0;">El reembolso se procesará a través del mismo método de pago utilizado para el depósito.</p>
                    </div>
                    
                    <p style="font-size: 14px; color: #777;">Según la política de tu entidad bancaria, el reembolso puede tardar entre 3 y 15 días hábiles en reflejarse en tu cuenta.</p>
        
                    <p style="margin-top: 20px; font-size: 15px;">Gracias por haber confiado en nosotros.</p>
                    <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                </div>
            </body>
            </html>
        """.formatted(tenantName, spaceName, refundAmount);

                helper.setTo(tenantEmail);
                helper.setSubject(subject);
                helper.setText(content, true);

                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Error al enviar correo de reembolso de depósito: " + e.getMessage(), e);
            }
        }


        /**
         * Envía notificación sobre la configuración de renovación automática
         */
        @Override
        public void sendAutoRenewalSetupNotification(String tenantEmail, String tenantName, String spaceName,
                                                     String endDate, String renewalMonths, String status) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String subject = "Configuración de Renovación Automática - LATAM Work Hub";
                String content = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center;">
                        <h2 style="color: #3498DB;">Renovación Automática %s</h2>
                    </div>
        
                    <p style="font-size: 16px;">Hola %s,</p>
                    
                    <p style="font-size: 16px;">Te confirmamos que la configuración de renovación automática para tu contrato del espacio "%s" ha sido %s.</p>
                    
                    <div style="background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 5px 0; font-weight: bold;">Detalles de la configuración:</p>
                        <p style="margin: 5px 0;">Fecha actual de finalización del contrato: %s</p>
                        <p style="margin: 5px 0;">Duración de la renovación: %s meses</p>
                    </div>
                    
                    <p style="font-size: 14px; color: #777;">Puedes modificar esta configuración en cualquier momento desde tu cuenta.</p>
        
                    <p style="margin-top: 20px; font-size: 15px;">Gracias por confiar en nosotros.</p>
                    <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                </div>
            </body>
            </html>
        """.formatted(status, tenantName, spaceName, status, endDate, renewalMonths);

                helper.setTo(tenantEmail);
                helper.setSubject(subject);
                helper.setText(content, true);

                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Error al enviar correo de configuración de renovación automática: " + e.getMessage(), e);
            }
        }

        /**
         * Envía notificación sobre una renovación automática próxima
         */
        @Override
        public void sendUpcomingAutoRenewalNotification(String tenantEmail, String tenantName, String spaceName,
                                                        String endDate, String renewalMonths) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String subject = "Renovación Automática Próxima - LATAM Work Hub";
                String content = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center;">
                        <h2 style="color: #FF9800;">Aviso de Renovación Automática</h2>
                    </div>
        
                    <p style="font-size: 16px;">Hola %s,</p>
                    
                    <p style="font-size: 16px;">Te recordamos que tu contrato del espacio "%s" está configurado para renovarse automáticamente.</p>
                    
                    <div style="background-color: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 5px 0; font-weight: bold;">Información importante:</p>
                        <p style="margin: 5px 0;">Fecha actual de finalización: %s</p>
                        <p style="margin: 5px 0;">El contrato se renovará automáticamente por: %s meses</p>
                        <p style="margin: 10px 0; font-weight: bold;">Si deseas cancelar la renovación automática, debes hacerlo antes de la fecha de finalización.</p>
                    </div>
                    
                    <p style="font-size: 14px; color: #777;">Puedes gestionar la configuración de renovación automática desde tu cuenta en cualquier momento.</p>
        
                    <p style="margin-top: 20px; font-size: 15px;">Gracias por confiar en nosotros.</p>
                    <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                </div>
            </body>
            </html>
        """.formatted(tenantName, spaceName, endDate, renewalMonths);

                helper.setTo(tenantEmail);
                helper.setSubject(subject);
                helper.setText(content, true);

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
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center;">
                        <h2 style="color: #e74c3c;">Aviso de Factura Vencida</h2>
                    </div>

                    <p style="font-size: 16px;">Hola %s,</p>

                    <p style="font-size: 16px;">Te notificamos que tienes una factura vencida del espacio "%s".</p>

                    <div style="background-color: #fde8e8; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 5px 0; font-weight: bold;">Detalles de la factura:</p>
                        <p style="margin: 5px 0;">Monto vencido: $%s</p>
                        <p style="margin: 5px 0;">Fecha de vencimiento: %s</p>
                    </div>

                    <p style="font-size: 15px;">Por favor, realiza el pago lo antes posible para evitar recargos adicionales.</p>

                    <p style="margin-top: 20px; font-size: 14px;">Si ya realizaste el pago, por favor ignora este mensaje.</p>
                    <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                </div>
            </body>
            </html>
        """.formatted(name, spaceName, amount, dueDate);

                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(content, true);

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
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
                    <div style="text-align: center;">
                        <h2 style="color: #e74c3c;">Aviso de Pago Vencido</h2>
                    </div>

                    <p style="font-size: 16px;">Hola %s,</p>

                    <p style="font-size: 16px;">Te informamos que existe un pago vencido correspondiente a tu espacio "%s".</p>

                    <div style="background-color: #fde8e8; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 5px 0; font-weight: bold;">Detalles:</p>
                        <p style="margin: 5px 0;">Inquilino: %s</p>
                        <p style="margin: 5px 0;">Monto vencido: $%s</p>
                        <p style="margin: 5px 0;">Fecha de vencimiento: %s</p>
                    </div>

                    <p style="font-size: 15px;">Nuestro equipo está gestionando el cobro de este pago.</p>

                    <p style="margin-top: 20px; font-size: 14px;">Te mantendremos informado sobre la situación.</p>
                    <p style="font-size: 14px; color: #555;">El equipo de <strong>LATAM Work Hub</strong></p>
                </div>
            </body>
            </html>
        """.formatted(ownerName, spaceName, tenantName, amount, dueDate);

                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(content, true);

                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Error al enviar notificación de pago vencido al propietario: " + e.getMessage(), e);
            }
        }



    }
