package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.BookingDto;
import Latam.Latam.work.hub.dtos.common.BookingResponseDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.RentalContractEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.BookingType;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.BookingService;
import Latam.Latam.work.hub.services.InvoiceService;
import Latam.Latam.work.hub.services.MailService;
import Latam.Latam.work.hub.services.mp.MercadoPagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final InvoiceService invoiceService;
    private final MailService mailService;
    private final MercadoPagoService mercadoPagoService;
    private final InvoiceRepository invoiceRepository;
    private final RentalContractRepository rentalContractRepository;
    @Override
    @Transactional
    public String createBooking(BookingDto bookingDto) {
        try {
            var space = spaceRepository.findById(bookingDto.getSpaceId())
                    .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));

            var user = userRepository.findByFirebaseUid(bookingDto.getUid())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar disponibilidad básica del espacio para reservas
            boolean isAvailableForPeriod = isSpaceAvailableForPeriod(
                    space.getId(),
                    bookingDto.getStartDate(),
                    bookingDto.getEndDate(),
                    bookingDto.getInitHour(),
                    bookingDto.getEndHour()
            );
            if (!isAvailableForPeriod) {
                throw new RuntimeException("El espacio no está disponible para el período solicitado");
            }

            // Validar que no haya contratos activos o confirmados que se solapen
            LocalDate startDate = bookingDto.getStartDate().toLocalDate();
            LocalDate endDate = bookingDto.getEndDate() != null ? 
                bookingDto.getEndDate().toLocalDate() : 
                bookingDto.getStartDate().toLocalDate();
            
            try {
                validateContractAndBookingOverlap(space.getId(), startDate, endDate);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e.getMessage());
            }

            BookingEntity bookingEntity = new BookingEntity();
            // La reserva se crea como INACTIVE y solo se activa cuando llegue su fecha de inicio
            bookingEntity.setActive(false);
            bookingEntity.setSpace(space);
            bookingEntity.setUser(user);
            bookingEntity.setInitHour(bookingDto.getInitHour());
            bookingEntity.setEndHour(bookingDto.getEndHour());
            bookingEntity.setStartDate(bookingDto.getStartDate());
            bookingEntity.setEndDate(bookingDto.getEndDate());
            bookingEntity.setCounterPersons(bookingDto.getCounterPersons());
            bookingEntity.setTotalAmount(bookingDto.getTotalAmount());
            bookingEntity.setStatus(BookingStatus.PENDING_PAYMENT);
            BookingType bookingType=determineBookingType(bookingDto);
              if (bookingType == BookingType.PER_DAY) {
                  bookingEntity.setEndDate(bookingDto.getStartDate()
                      .plusDays(1)
                      .withHour(bookingDto.getStartDate().getHour())
                      .withMinute(bookingDto.getStartDate().getMinute())
                      .withSecond(bookingDto.getStartDate().getSecond()));
              }
            bookingEntity.setBookingType(determineBookingType(bookingDto));

            BookingEntity savedBooking = bookingRepository.saveAndFlush(bookingEntity);
            return invoiceService.createInvoice(savedBooking);

        } catch (Exception e) {
            throw new RuntimeException("Error al crear la reserva: " + e.getMessage(), e);
        }
    }

    private BookingType determineBookingType(BookingDto bookingDto) {
        if (bookingDto.getInitHour() != null) {
            return BookingType.PER_HOUR;
        } else if (bookingDto.getStartDate() != null && bookingDto.getEndDate() == null) {
            return BookingType.PER_DAY;
        }
        return BookingType.PER_MONTH;
    }


    /**
     * Verifica si un espacio está disponible para un período específico
     */
    private boolean isSpaceAvailableForPeriod(Long spaceId, LocalDateTime startDate, LocalDateTime endDate,
                                              LocalTime initHour, LocalTime endHour) {
        List<BookingEntity> overlappingBookings = bookingRepository.findOverlappingBookings(
                spaceId,
                startDate,
                endDate,
                initHour,
                endHour);

        return overlappingBookings.isEmpty();
    }

    @Override
    @Transactional
    public void confirmBookingPayment(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        System.out.println("Confirmando pago de la reserva ID: " + bookingId);
        
        // Verificar si la reserva ya está confirmada
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            System.out.println("Reserva ya confirmada, no se procesa nuevamente");
            return; // No procesar nuevamente
        }

        // Actualizar estado de la reserva a CONFIRMED (pero NO active)
        booking.setStatus(BookingStatus.CONFIRMED);
        // NO marcar como active = true hasta que llegue la fecha de inicio
        bookingRepository.save(booking);
        System.out.println("Reserva actualizada a estado CONFIRMED");
        
        // El espacio se marcará como no disponible cuando la reserva se active por el scheduler
        
        // Enviar email al dueño del espacio para notificarle de la reserva
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        SpaceEntity space = booking.getSpace();
        mailService.sendBookingNotificationToOwner(
                space.getOwner().getEmail(),
                space.getOwner().getName(),
                space.getName(),
                booking.getUser().getName(),
                booking.getStartDate().format(dateFormatter),
                booking.getEndDate() != null ? booking.getEndDate().format(dateFormatter) : "",
                booking.getInitHour() != null ? booking.getInitHour().format(timeFormatter) : "",
                booking.getEndHour() != null ? booking.getEndHour().format(timeFormatter) : ""
        );
    }

    @Override
    @Transactional
    public void cancelAndRefoundPayment(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // Permitir cancelaciones sin restricción de días para reservas pendientes de pago
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            // Validar que la cancelación sea con al menos 7 días de anticipación solo para reservas ya confirmadas
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = booking.getStartDate();
            long daysUntilBooking = ChronoUnit.DAYS.between(now, startDate);

            if (daysUntilBooking < 7) {
                throw new RuntimeException("Solo se permiten cancelaciones con al menos 7 días de anticipación para reservas confirmadas");
            }
        }

        // Buscar la factura asociada a la reserva
        InvoiceEntity invoice = invoiceRepository.findByBookingId(bookingId)
                .orElse(null);

        // Asegurarse de que el espacio quede disponible
        SpaceEntity space = booking.getSpace();
        space.setAvailable(true);
        spaceRepository.save(space);

        // Guardar el status original antes de cambiar a CANCELED
        BookingStatus originalStatus = booking.getStatus();
        
        // Actualizar la entidad de reserva
        booking.setStatus(BookingStatus.CANCELED);
        booking.setActive(false);
        
        // Setear refund amount si la reserva fue pagada (independientemente del estado de la factura)
        // Esto incluye reservas CONFIRMED, ACTIVE, COMPLETED que se cancelan con reembolso
        if (originalStatus == BookingStatus.CONFIRMED || 
            originalStatus == BookingStatus.ACTIVE || 
            originalStatus == BookingStatus.COMPLETED) {
            booking.setRefundAmount(booking.getTotalAmount()); // Guardamos el reembolso completo
            System.out.println("Seteando refund amount: " + booking.getTotalAmount() + " para reserva " + booking.getId());
        } else {
            booking.setRefundAmount(0.0); // No hay reembolso si no se había pagado (PENDING_PAYMENT)
            System.out.println("No hay reembolso para reserva " + booking.getId() + " - no estaba pagada (status original: " + originalStatus + ")");
        }
        
        bookingRepository.save(booking);
        
        // Procesar la factura si existe
        if (invoice != null) {
            try {
                // Intentamos el reembolso MercadoPago si hay paymentId
                if (invoice.getPaymentId() != null) {
                    try {
                        mercadoPagoService.refundPayment(invoice.getId());
                    } catch (Exception refundException) {
                        // Si falla el reembolso, simplemente lo ignoramos
                        // y continuamos con la cancelación
                        System.out.println("Error al intentar reembolsar el pago: " + refundException.getMessage());
                    }
                }
                
                // Siempre actualizamos el estado de la factura a cancelada
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoiceRepository.save(invoice);
                
            } catch (Exception e) {
                // Garantizamos que la factura esté cancelada, incluso si hay otros errores
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoiceRepository.save(invoice);
            }
        }
    }
    @Override
    @Transactional
    public void updateBookingsStatus() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Actualizando estado de reservas a las {}", now);

        // Activar reservas cuya fecha de inicio ha llegado
        List<BookingEntity> upcomingBookings = bookingRepository.findUpcomingBookings(now);
        log.info("Se encontraron {} reservas próximas a activar", upcomingBookings.size());
        for (BookingEntity booking : upcomingBookings) {
            try {
                SpaceEntity space = booking.getSpace();
                booking.setStatus(BookingStatus.ACTIVE);
                booking.setActive(true);
                bookingRepository.saveAndFlush(booking); // Forzar flush

                if (space != null) {
                    space.setAvailable(false);
                    spaceRepository.saveAndFlush(space); // Forzar flush
                    log.info("Espacio {} actualizado: available={}", space.getId(), space.getAvailable());
                }
            } catch (Exception e) {
                log.error("Error actualizando reserva {}: {}", booking.getId(), e.getMessage());
            }
        }
        // Completar reservas según su tipo
        List<BookingEntity> activeBookings = bookingRepository.findByStatus(BookingStatus.ACTIVE);
        for (BookingEntity booking : activeBookings) {
            boolean shouldComplete = false;

            switch (booking.getBookingType()) {
                case PER_HOUR:
                    // Completar si la hora actual es posterior a la hora de fin
                    LocalDateTime bookingEndDateTime = booking.getStartDate()
                            .withHour(booking.getEndHour().getHour())
                            .withMinute(booking.getEndHour().getMinute());
                    shouldComplete = now.isAfter(bookingEndDateTime);
                    break;

                case PER_DAY:
                    // Completar si la fecha actual es posterior al día de la reserva
                    LocalDateTime endOfBookingDay = booking.getStartDate()
                            .plusDays(1)
                            .withHour(0)
                            .withMinute(0)
                            .withSecond(0);
                    shouldComplete = now.isAfter(endOfBookingDay);
                    break;

                case PER_MONTH:
                    // Completar si la fecha actual es posterior a la fecha de fin
                    shouldComplete = booking.getEndDate() != null && now.isAfter(booking.getEndDate());
                    break;
            }

            if (shouldComplete) {
                System.out.println("Completando reserva ID: " + booking.getId());
                
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setActive(false);
                bookingRepository.save(booking); // Guardar cada reserva inmediatamente

                SpaceEntity space = booking.getSpace();
                if (space != null) {
                    System.out.println("Marcando espacio ID: " + space.getId() + " como disponible al completar la reserva");
                    space.setAvailable(true);
                    spaceRepository.saveAndFlush(space); // Guardar cada espacio inmediatamente
                }

                mailService.sendBookingCompletedEmail(
                        booking.getUser().getEmail(),
                        booking.getUser().getName(),
                        booking.getSpace().getName()
                );
            }
        }
        
        // Ya no es necesario guardar en lote, ya que guardamos cada entidad inmediatamente después de modificarla
        // bookingRepository.saveAll(upcomingBookings);
        // bookingRepository.saveAll(activeBookings);
    }

    @Override
    public void validateContractAndBookingOverlap(Long spaceId, LocalDate startDate, LocalDate endDate) {
        // Verificar contratos activos Y confirmados
        List<RentalContractEntity> activeContracts = rentalContractRepository
                .findBySpaceIdAndDateRange(spaceId, startDate, endDate, ContractStatus.ACTIVE);
        
        List<RentalContractEntity> confirmedContracts = rentalContractRepository
                .findBySpaceIdAndDateRange(spaceId, startDate, endDate, ContractStatus.CONFIRMED);

        if (!activeContracts.isEmpty() || !confirmedContracts.isEmpty()) {
            throw new IllegalArgumentException("El espacio tiene un contrato activo o confirmado durante el período seleccionado");
        }

        // Verificar reservas confirmadas o activas
        List<BookingEntity> overlappingBookings = bookingRepository
                .findBySpaceIdAndDateRangeAndStatuses(
                        spaceId,
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59),
                        List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE)
                );

        if (!overlappingBookings.isEmpty()) {
            throw new IllegalArgumentException("El espacio tiene reservas durante el período seleccionado");
        }
    }

    @Override
    public Page<BookingResponseDto> getUserBookings(String uid, BookingStatus status, Pageable pageable) {
        Page<BookingEntity> bookingsPage = bookingRepository
                .findByUserFirebaseUidAndStatus(uid, status, pageable);

        return bookingsPage.map(booking -> {
            BookingResponseDto dto = new BookingResponseDto();
            // Mapeo de la reserva
            dto.setId(booking.getId());
            dto.setStartDate(booking.getStartDate());
            dto.setEndDate(booking.getEndDate());
            dto.setInitHour(booking.getInitHour());
            dto.setEndHour(booking.getEndHour());
            dto.setBookingType(booking.getBookingType().name());
            dto.setStatus(booking.getStatus());
            dto.setCounterPersons(booking.getCounterPersons());
            dto.setTotalAmount(booking.getTotalAmount());

            // Mapeo del espacio
            dto.setSpaceId(booking.getSpace().getId());
            dto.setSpaceName(booking.getSpace().getName());
            String spaceAddress = String.format("%s %s, %s, %s, %s, %s",
                    booking.getSpace().getAddress().getStreetName(),
                    booking.getSpace().getAddress().getStreetNumber(),
                    booking.getSpace().getAddress().getCity().getName(),
                    booking.getSpace().getAddress().getCity().getDivisionName(),
                    booking.getSpace().getAddress().getCity().getCountry().getName(),
                    booking.getSpace().getAddress().getPostalCode()
            );
            dto.setSpaceAddress(spaceAddress);
            dto.setSpaceType(booking.getSpace().getType().getName());
            dto.setCityName(booking.getSpace().getAddress().getCity().getName());
            dto.setCountryName(booking.getSpace().getAddress().getCity().getCountry().getName());

            return dto;
        });
    }
    @Override
    public String generateBookingPaymentLink(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("La reserva no está pendiente de pago");
        }

        // Buscar la factura asociada a la reserva
        InvoiceEntity invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada para la reserva"));

        try {
            String buyerEmail = booking.getUser().getEmail();
            String sellerEmail = booking.getSpace().getOwner().getEmail();
            String description = "Reserva de " + booking.getSpace().getName() + " - " +
                    booking.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            // Usar el invoiceId en lugar del bookingId
            return mercadoPagoService.createInvoicePaymentPreference(
                    invoice.getId(),  // Cambio aquí: usar invoice.getId() en lugar de booking.getId()
                    description,
                    BigDecimal.valueOf(booking.getTotalAmount()),
                    buyerEmail,
                    sellerEmail
            );
        } catch (MPException | MPApiException e) {
            throw new RuntimeException("Error al generar el link de pago: " + e.getMessage());
        }
    }

}
