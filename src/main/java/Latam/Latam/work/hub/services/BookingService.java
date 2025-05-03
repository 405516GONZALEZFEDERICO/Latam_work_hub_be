package Latam.Latam.work.hub.services;
import Latam.Latam.work.hub.dtos.common.BookingDto;
import org.springframework.stereotype.Service;


@Service
public interface BookingService {
    String createBooking(BookingDto bookingDto);
    void confirmBookingPayment(Long bookingId);
    void completeBooking(Long bookingId);
    void cancelBooking(Long bookingId);
    void updateBookingsStatus();
}
