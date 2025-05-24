package Latam.Latam.work.hub.services;
import Latam.Latam.work.hub.dtos.common.BookingDto;
import Latam.Latam.work.hub.dtos.common.BookingResponseDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;


@Service
public interface BookingService {
    String createBooking(BookingDto bookingDto);
    void confirmBookingPayment(Long bookingId);
    void cancelAndRefoundPayment(Long bookingId);
    void updateBookingsStatus();
    void validateContractAndBookingOverlap(Long spaceId, LocalDate startDate, LocalDate endDate);
    String generateBookingPaymentLink(Long bookingId);
   Page<BookingResponseDto> getUserBookings(String uid, BookingStatus status, Pageable pageable);

}
