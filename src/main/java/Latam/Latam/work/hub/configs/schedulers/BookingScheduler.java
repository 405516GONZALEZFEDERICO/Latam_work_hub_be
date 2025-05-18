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
  * Actualiza el estado de todas las reservas cada 30 segundos
  * La disponibilidad de los espacios se actualiza automáticamente cuando
  * una reserva comienza o termina.
  */
 @Scheduled(fixedRate = 30000) // 30 segundos en milisegundos
 public void updateBookingsStatus() {
     log.info("Iniciando actualización programada de estado de reservas");
     try {
         bookingService.updateBookingsStatus();
         log.info("Actualización de estado de reservas completada con éxito");
     } catch (Exception e) {
         log.error("Error al actualizar estado de reservas: {}", e.getMessage(), e);
     }
 }
}
