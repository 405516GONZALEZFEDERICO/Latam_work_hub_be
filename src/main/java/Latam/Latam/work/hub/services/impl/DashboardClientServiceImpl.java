package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientKpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientMonthlySpendingDto;
import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientBookingsByTypeDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.DashboardClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardClientServiceImpl implements DashboardClientService {
    private static final Logger log = LoggerFactory.getLogger(DashboardClientServiceImpl.class);

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final RentalContractRepository rentalContractRepository;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<BookingStatus> COMPLETED_BOOKING_STATUSES = Arrays.asList(
            BookingStatus.COMPLETED
    );
    private static final List<BookingStatus> UPCOMING_BOOKING_STATUSES = Arrays.asList(
            BookingStatus.CONFIRMED, BookingStatus.ACTIVE
    );

    @Override
    public ClientKpiCardsDto getKpiCardsData(String clientUid) {
        log.debug("Fetching KPI cards data for client: {}", clientUid);

        UserEntity client = userRepository.findByFirebaseUid(clientUid)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Total de reservas
        long totalBookings = bookingRepository.countByUserId(client.getId());

        // Contratos activos
        long activeContracts = rentalContractRepository.countActiveContractsByTenantId(client.getId());

        // Reservas completadas
        long completedBookings = bookingRepository.countByUserIdAndStatuses(
                client.getId(), COMPLETED_BOOKING_STATUSES
        );

        // Gasto de los últimos 30 días
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();
        
        // === GASTOS BRUTOS ===
        // RESERVAS: Contamos directamente desde BookingEntity (NO desde InvoiceEntity para evitar duplicación)
        Double bookingGrossSpending = bookingRepository.sumGrossSpendingByClientInDateRange(
                client.getId(), thirtyDaysAgo, now
        );
        // CONTRATOS: Solo facturas de contratos (NO de reservas para evitar duplicación)
        Double contractGrossSpending = invoiceRepository.sumGrossSpendingFromContractsByClientId(
                client.getId(), thirtyDaysAgo, now
        );
        
        // DEBUG LOGS para gastos brutos
        System.out.println("=== DEBUG GASTOS BRUTOS CLIENTE " + client.getId() + " ===");
        System.out.println("Periodo: " + thirtyDaysAgo + " a " + now);
        System.out.println("Gastos reservas: " + bookingGrossSpending);
        System.out.println("Gastos contratos: " + contractGrossSpending);
        
        Double totalGrossSpent = (bookingGrossSpending != null ? bookingGrossSpending : 0.0) + 
                                (contractGrossSpending != null ? contractGrossSpending : 0.0);
        
        System.out.println("Total gastos brutos: " + totalGrossSpent);
        
        // === REEMBOLSOS RECIBIDOS ===
        // Solo contar reembolsos reales procesados
        Double contractDepositRefunds = rentalContractRepository.sumDepositRefundsByClientId(
                client.getId(), thirtyDaysAgo, now
        );
        Double bookingRefunds = bookingRepository.sumTotalRefundsByClientInDateRange(
                client.getId(), thirtyDaysAgo, now
        );
        // Reembolsos en facturas SOLO de contratos (NO de reservas para evitar duplicación)
        Double contractInvoiceRefunds = invoiceRepository.sumInvoiceRefundsFromContractsByClientId(
                client.getId(), thirtyDaysAgo, now
        );
        
        // DEBUG LOGS
        System.out.println("=== DEBUG REEMBOLSOS CLIENTE " + client.getId() + " ===");
        System.out.println("Periodo: " + thirtyDaysAgo + " a " + now);
        System.out.println("Reembolsos depósitos contratos: " + contractDepositRefunds);
        System.out.println("Reembolsos reservas: " + bookingRefunds);
        System.out.println("Reembolsos facturas contratos: " + contractInvoiceRefunds);
        
        Double totalRefunds = (contractDepositRefunds != null ? contractDepositRefunds : 0.0) + 
                             (bookingRefunds != null ? bookingRefunds : 0.0) +
                             (contractInvoiceRefunds != null ? contractInvoiceRefunds : 0.0);
        
        System.out.println("Total reembolsos: " + totalRefunds);
        System.out.println("=== FIN DEBUG REEMBOLSOS ===");
        
        // === GASTOS NETOS ===
        Double totalNetSpent = totalGrossSpent - totalRefunds;
        
        System.out.println("Total gastos netos: " + totalNetSpent);
        System.out.println("=== FIN DEBUG GASTOS BRUTOS ===");
        
        // Método anterior para compatibilidad
        Double bookingSpending = bookingRepository.sumSpendingByClientInDateRange(
                client.getId(), thirtyDaysAgo, now
        );
        Double contractSpending = invoiceRepository.sumSpendingByClientId(
                client.getId(), thirtyDaysAgo, now
        );
        
        Double totalSpent = (bookingSpending != null ? bookingSpending : 0.0) + 
                           (contractSpending != null ? contractSpending : 0.0);

        // Próximas reservas
        long upcomingBookings = bookingRepository.countByUserIdAndStatuses(
                client.getId(), UPCOMING_BOOKING_STATUSES
        );

        return ClientKpiCardsDto.builder()
                .totalBookings(totalBookings)
                .activeContracts(activeContracts)
                .completedBookings(completedBookings)
                // Nuevos campos diferenciados
                .totalGrossSpentLast30Days(totalGrossSpent)
                .totalNetSpentLast30Days(totalNetSpent)
                .totalRefundsLast30Days(totalRefunds)
                // Campo deprecated para compatibilidad
                .totalSpentLast30Days(totalNetSpent) // Usamos neto para mantener consistencia
                .upcomingBookings(upcomingBookings)
                .build();
    }

    @Override
    public List<ClientMonthlySpendingDto> getMonthlySpending(String clientUid, int lastNMonths) {
        log.debug("Fetching monthly spending for client: {} for the last {} months", clientUid, lastNMonths);
        
        UserEntity client = userRepository.findByFirebaseUid(clientUid)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        int validLastNMonths = Math.max(1, lastNMonths);
        LocalDateTime startDateOfRange = LocalDate.now().minusMonths(validLastNMonths - 1L).withDayOfMonth(1).atStartOfDay();

        Map<YearMonth, Double> spendingByMonth = new HashMap<>();
        
        // Gastos en reservas
        List<Object[]> bookingSpending = bookingRepository.findMonthlySpendingByClient(client.getId(), startDateOfRange);
        if (bookingSpending != null) {
            for (Object[] row : bookingSpending) {
                if (row != null && row.length == 3 && row[0] instanceof Number && row[1] instanceof Number && row[2] instanceof Number) {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    double spending = ((Number) row[2]).doubleValue();
                    
                    YearMonth yearMonth = YearMonth.of(year, month);
                    spendingByMonth.merge(yearMonth, spending, Double::sum);
                }
            }
        }
        
        // Gastos en contratos
        List<Object[]> contractSpending = invoiceRepository.findMonthlySpendingByClient(client.getId(), startDateOfRange);
        if (contractSpending != null) {
            for (Object[] row : contractSpending) {
                if (row != null && row.length == 3 && row[0] instanceof Number && row[1] instanceof Number && row[2] instanceof Number) {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    double spending = ((Number) row[2]).doubleValue();
                    
                    YearMonth yearMonth = YearMonth.of(year, month);
                    spendingByMonth.merge(yearMonth, spending, Double::sum);
                }
            }
        }

        List<ClientMonthlySpendingDto> result = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        
        for (int i = validLastNMonths - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            Double spending = spendingByMonth.getOrDefault(targetMonth, 0.0);
            result.add(new ClientMonthlySpendingDto(
                    targetMonth.format(YEAR_MONTH_FORMATTER), 
                    spending
            ));
        }
        
        return result;
    }

    @Override
    public List<ClientBookingsByTypeDto> getBookingsBySpaceType(String clientUid) {
        log.debug("Fetching bookings by space type for client: {}", clientUid);
        
        UserEntity client = userRepository.findByFirebaseUid(clientUid)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<Object[]> bookingsByTypeData = bookingRepository.findBookingCountBySpaceTypeForClient(client.getId());
        
        return bookingsByTypeData.stream()
                .filter(row -> row != null && row.length == 2 && row[0] instanceof String && row[1] instanceof Long)
                .map(row -> new ClientBookingsByTypeDto((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }
} 