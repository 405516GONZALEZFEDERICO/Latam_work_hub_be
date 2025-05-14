package Latam.Latam.work.hub.services.impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import Latam.Latam.work.hub.dtos.common.BookingDto;
import Latam.Latam.work.hub.dtos.common.BookingResponseDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.entities.SpaceEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.BookingType;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final InvoiceService invoiceService;
    private final MailService mailService;
    private final MercadoPagoService mercadoPagoService;
    private final InvoiceRepository invoiceRepository;
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    @Override
    @Transactional
    public String createBooking(BookingDto bookingDto) {
        try {
            var space = spaceRepository.findById(bookingDto.getSpaceId())
                    .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));

            var user = userRepository.findByFirebaseUid(bookingDto.getUid())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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

            var bookingEntity = new BookingEntity();
            bookingEntity.setActive(true);
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

            var savedBooking = bookingRepository.saveAndFlush(bookingEntity);
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

        // Verificar si la reserva ya está confirmada
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return; // No procesar nuevamente
        }

        // Actualizar estado de la reserva
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setActive(true);
        bookingRepository.save(booking);
        Optional<SpaceEntity> space= this.spaceRepository.findById(booking.getSpace().getId());
        if (space.isEmpty()){
            throw new RuntimeException("Espacio no encontrado");
        }
        space.get().setAvailable(false);
        this.spaceRepository.save(space.get());
        // Enviar email al dueño del espacio para notificarle de la reserva
        SpaceEntity spaced = booking.getSpace();
        mailService.sendBookingNotificationToOwner(
                spaced.getOwner().getEmail(),
                spaced.getOwner().getName(),
                spaced.getName(),
                booking.getUser().getName(),
                booking.getStartDate().toString(),
                booking.getEndDate() != null ? booking.getEndDate().toString() : "",
                booking.getInitHour() != null ? booking.getInitHour().toString() : "",
                booking.getEndHour() != null ? booking.getEndHour().toString() : ""
        );
    }

    @Override
    @Transactional
    public void cancelAndRefoundPayment(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // Validar que la cancelación sea con al menos 7 días de anticipación
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = booking.getStartDate();
        long daysUntilBooking = ChronoUnit.DAYS.between(now, startDate);

        if (daysUntilBooking < 7) {
            throw new RuntimeException("Solo se permiten cancelaciones con al menos 7 días de anticipación");
        }

        // Buscar la factura asociada a la reserva
        InvoiceEntity invoice = invoiceService.findByBookingId(bookingId);

        // Manejar el pago según el estado de la factura
        if (invoice != null && invoice.getStatus() == InvoiceStatus.PAID) {
            try {
                boolean refunded = mercadoPagoService.refundPayment(invoice.getId());
                if (!refunded) {
                    throw new RuntimeException("No se pudo procesar el reembolso para la factura asociada");
                }
            } catch (Exception e) {
                throw new RuntimeException("Error al intentar reembolsar el pago: " + e.getMessage(), e);
            }
        } else {
            // Si no hay factura o no está pagada, simplemente cancelamos la reserva
            booking.setStatus(BookingStatus.CANCELED);
            booking.setActive(false);
            bookingRepository.save(booking);

            // Marcar el espacio como disponible nuevamente
            SpaceEntity space = booking.getSpace();
            space.setAvailable(true);
            spaceRepository.save(space);

            // Actualizar el estado de la factura si existe
            if (invoice != null) {
                invoice.setStatus(InvoiceStatus.CANCELLED);

                invoiceRepository.save(invoice);
            }
        }
    }
    @Override
    @Transactional
    public void updateBookingsStatus() {
        ZoneId buenosAiresZoneId = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDateTime nowInBuenosAires = LocalDateTime.now(buenosAiresZoneId);

        log.debug("Ejecutando updateBookingsStatus con hora de Buenos Aires: {}", nowInBuenosAires);

        List<BookingEntity> bookingsToSave = new ArrayList<>();

        // Activar reservas
        List<BookingEntity> upcomingBookings = bookingRepository.findUpcomingBookings(nowInBuenosAires);
        for (BookingEntity booking : upcomingBookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                log.info("Activando Booking ID: {} (startDate: {})", booking.getId(), booking.getStartDate());
                booking.setStatus(BookingStatus.ACTIVE);
                booking.setActive(true);
                SpaceEntity space = booking.getSpace();
                space.setAvailable(false);
                spaceRepository.save(space);
                bookingsToSave.add(booking);
            }
        }

        // Completar reservas
        List<BookingEntity> activeBookings = bookingRepository.findByStatus(BookingStatus.ACTIVE);
        for (BookingEntity booking : activeBookings) {
            boolean shouldComplete = false;
            LocalDateTime effectiveBookingEndDateTime = null;

            switch (booking.getBookingType()) {
                case PER_HOUR:
                    // Asume que booking.endDate para PER_HOUR es el momento exacto de finalización.
                    // Si no, y dependes de startDate + endHour, ajusta aquí.
                    // Basado en la captura de DBeaver, endDate parece ser el timestamp completo.
                    effectiveBookingEndDateTime = booking.getEndDate();
                    if (effectiveBookingEndDateTime == null && booking.getStartDate() != null && booking.getEndHour() != null) {

                        log.warn("Booking PER_HOUR ID {} tiene endDate nulo, usando fallback con startDate y endHour.", booking.getId());
                        effectiveBookingEndDateTime = LocalDateTime.of(booking.getStartDate().toLocalDate(), booking.getEndHour());
                    }
                    break;
                case PER_DAY:
                    effectiveBookingEndDateTime = booking.getStartDate().toLocalDate().plusDays(1).atStartOfDay();
                    break;
                case PER_MONTH:
                    effectiveBookingEndDateTime = booking.getEndDate();
                    break;
                default:
                    log.warn("Tipo de reserva desconocido {} para booking ID {}", booking.getBookingType(), booking.getId());
                    continue;
            }

            if (effectiveBookingEndDateTime != null) {
                if (nowInBuenosAires.isAfter(effectiveBookingEndDateTime)) {
                    log.info("Completando Booking ID: {} (effectiveEnd: {})", booking.getId(), effectiveBookingEndDateTime);
                    shouldComplete = true;
                }
            } else {
                log.warn("No se pudo determinar effectiveBookingEndDateTime para Booking ID {}", booking.getId());
            }

            if (shouldComplete) {
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setActive(false);
                SpaceEntity space = booking.getSpace();
                space.setAvailable(true);
                spaceRepository.save(space);
                bookingsToSave.add(booking);

                     mailService.sendBookingCompletedEmail(
                         booking.getUser().getEmail(),
                         booking.getUser().getName(),
                         booking.getSpace().getName()
                     );
            }
        }

        if (!bookingsToSave.isEmpty()) {
            bookingRepository.saveAll(bookingsToSave);
            log.debug("Guardadas {} modificaciones de reservas.", bookingsToSave.size());
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

        try {
            String buyerEmail = booking.getUser().getEmail();
            String sellerEmail = booking.getSpace().getOwner().getEmail();
            String description = "Reserva de " + booking.getSpace().getName() + " - " +
                    booking.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            return mercadoPagoService.createInvoicePaymentPreference(
                    booking.getId(),
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
