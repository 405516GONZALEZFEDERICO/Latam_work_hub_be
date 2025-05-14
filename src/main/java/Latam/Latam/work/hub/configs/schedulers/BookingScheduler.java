package Latam.Latam.work.hub.configs.schedulers;

import Latam.Latam.work.hub.services.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedRate = 30000)
    public void updateBookingsStatus() {
        ZoneId buenosAiresZoneId = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDateTime serverTimeUTC = LocalDateTime.now();
        LocalDateTime buenosAiresTime = LocalDateTime.now(buenosAiresZoneId);

        log.info("Iniciando actualización programada de estado de reservas. Hora Servidor (UTC probable): {}. Hora Buenos Aires usada: {}", serverTimeUTC, buenosAiresTime);
        try {
            bookingService.updateBookingsStatus();
            log.info("Actualización de estado de reservas completada con éxito.");
        } catch (Exception e) {
            log.error("Error al actualizar estado de reservas: {}", e.getMessage(), e);
        }
    }
}