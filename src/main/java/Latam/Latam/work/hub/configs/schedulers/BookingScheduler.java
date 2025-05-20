package Latam.Latam.work.hub.configs.schedulers;

import Latam.Latam.work.hub.services.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Programador de tareas para actualizar el estado de las reservas
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingService bookingService;

 /**
  * Actualiza el estado de todas las reservas cada 1 segundo
  * La disponibilidad de los espacios se actualiza automáticamente cuando
  * una reserva comienza o termina.
  */
 @Scheduled(fixedRate = 1000) // 1 segundo en milisegundos
 public void updateBookingsStatus() {
     try {
         bookingService.updateBookingsStatus();
     } catch (Exception e) {
         log.error("Error al actualizar estado de reservas: {}", e.getMessage(), e);
     }
 }
}
