package Latam.Latam.work.hub.configs.state.machine.contract;

import Latam.Latam.work.hub.services.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
 * Servicio para enviar notificaciones por email con reintentos
 * en caso de fallos temporales.
 */
@Service
@Slf4j
public class NotificationRetryService {


    @Autowired
    private MailService mailService;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 60000; // 1 minuto


    /**
     * Envía un email con reintentos
     * @param emailFunction Función que envía el email
     * @param errorMessage Mensaje en caso de error
     */
    public void sendEmailWithRetry(Runnable emailFunction, String errorMessage) {
        int attempts = 0;
        boolean sent = false;

        while (!sent && attempts < MAX_RETRIES) {
            attempts++;
            try {
                emailFunction.run();
                sent = true;
                log.info("Email enviado con éxito en el intento {}", attempts);
            } catch (Exception e) {
                log.error("Error al enviar email (intento {}): {}", attempts, e.getMessage(), e);

                if (attempts < MAX_RETRIES) {
                    try {
                        log.info("Reintentando en {} ms", RETRY_DELAY_MS);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!sent) {
            log.error("Fallo al enviar email después de {} intentos: {}", MAX_RETRIES, errorMessage);
        }
    }
}