package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.dashboard.admin.KpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.MonthlyRevenueDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.PeakHoursDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.ReservationsBySpaceTypeDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.ReservationsByZoneDto;
import Latam.Latam.work.hub.entities.BookingEntity;
import Latam.Latam.work.hub.entities.InvoiceEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.enums.InvoiceStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.DashboardAdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardAdminServiceImpl implements DashboardAdminService {
    private static final Logger log = LoggerFactory.getLogger(DashboardAdminServiceImpl.class);

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final RentalContractRepository rentalContractRepository;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<BookingStatus> RELEVANT_BOOKING_STATUSES_FOR_COUNT = Arrays.asList(
            BookingStatus.CONFIRMED, BookingStatus.ACTIVE, BookingStatus.PENDING_PAYMENT
    );
    private static final List<BookingStatus> RELEVANT_BOOKING_STATUSES_FOR_PEAK_HOURS = Arrays.asList(
            BookingStatus.CONFIRMED, BookingStatus.ACTIVE, BookingStatus.COMPLETED
    );
    private static final int DAYS_AHEAD_FOR_EXPIRING_CONTRACTS = 30;


    private static final String ROLE_NAME_CLIENTE = "CLIENTE";
    private static final String ROLE_NAME_PROVEEDOR = "PROVEEDOR";

    @Override
    public KpiCardsDto getKpiCardsData() {
        log.debug("Fetching KPI cards data");

        long activeClients = userRepository.countActiveUsersByRole(ROLE_NAME_CLIENTE);
        long activeProviders = userRepository.countActiveUsersByRole(ROLE_NAME_PROVEEDOR);
        long publishedSpaces = spaceRepository.countPublishedSpaces();

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        long reservationsThisMonth = bookingRepository.countReservationsInDateRangeWithStatuses(
                startOfMonth, endOfMonth, RELEVANT_BOOKING_STATUSES_FOR_COUNT
        );

        // Calcular ingresos total de los últimos 30 días
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();
        
        log.info("Calculando ingresos desde {} hasta {}", 
                thirtyDaysAgo.format(DateTimeFormatter.ISO_DATE_TIME), 
                now.format(DateTimeFormatter.ISO_DATE_TIME));
        
        // Ingresos de reservas (ya resta reembolsos y procesa canceladas)
        Double bookingRevenue = bookingRepository.sumTotalRevenueByDateRange(thirtyDaysAgo, now);
        log.info("Ingresos de reservas (últimos 30 días): {} (null significa que no hay datos)", bookingRevenue);
        
        // Ingresos de contratos (ya resta reembolsos y procesa canceladas)
        Double invoiceRevenue = invoiceRepository.sumTotalAmountByDateRange(thirtyDaysAgo, now);
        log.info("Ingresos de contratos (últimos 30 días): {} (null significa que no hay datos)", invoiceRevenue);
        
        // Sumar ambos ingresos (o usar 0.0 si alguno es null)
        Double totalRevenue = (bookingRevenue != null ? bookingRevenue : 0.0) + 
                             (invoiceRevenue != null ? invoiceRevenue : 0.0);
        log.info("Ingresos TOTALES (últimos 30 días): {}", totalRevenue);
        
        // KPIs de Contratos
        long activeContracts = rentalContractRepository.countByContractStatus(ContractStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(DAYS_AHEAD_FOR_EXPIRING_CONTRACTS);
        List<ContractStatus> statusesForExpiringCheck = Arrays.asList(ContractStatus.ACTIVE, ContractStatus.CONFIRMED);
        long contractsExpiringSoon = rentalContractRepository.countActiveOrConfirmedContractsExpiringBetween(
                statusesForExpiringCheck,
                today,
                thirtyDaysFromNow
        );

        return KpiCardsDto.builder()
                .activeClients(activeClients)
                .activeProviders(activeProviders)
                .publishedSpaces(publishedSpaces)
                .reservationsThisMonth(reservationsThisMonth)
                .totalRevenueLast30Days(totalRevenue)
                .activeContracts(activeContracts)
                .contractsExpiringSoon(contractsExpiringSoon)
                .build();
    }

    @Override
    public List<MonthlyRevenueDto> getMonthlyRevenue(int lastNMonths) {
        log.debug("Fetching monthly revenue for the last {} months", lastNMonths);
        int validLastNMonths = Math.max(1, lastNMonths);
        LocalDateTime startDateOfRange = LocalDate.now().minusMonths(validLastNMonths - 1L).withDayOfMonth(1).atStartOfDay();

        // Mapa para almacenar los ingresos por mes
        Map<YearMonth, Double> revenueByMonth = new HashMap<>();
        
        // Obtener datos de ingresos mensuales de reservas
        List<Object[]> bookingRevenue = bookingRepository.findMonthlyRevenue(startDateOfRange);
        log.info("Retrieved {} monthly booking revenue records", bookingRevenue.size());
        
        // Procesar datos de reservas
        if (bookingRevenue != null) {
            for (Object[] row : bookingRevenue) {
                if (row != null && row.length == 3 && row[0] instanceof Number && row[1] instanceof Number && row[2] instanceof Number) {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    double revenue = ((Number) row[2]).doubleValue();
                    
                    YearMonth yearMonth = YearMonth.of(year, month);
                    revenueByMonth.merge(yearMonth, revenue, Double::sum);
                    log.debug("Booking revenue for {}: {}", yearMonth, revenue);
                }
            }
        }
        
        // Obtener datos de ingresos mensuales de contratos
        List<Object[]> contractRevenue = invoiceRepository.findMonthlyRevenue(startDateOfRange);
        log.info("Retrieved {} monthly contract revenue records", contractRevenue.size());
        
        // Procesar datos de contratos
        if (contractRevenue != null) {
            for (Object[] row : contractRevenue) {
                if (row != null && row.length == 3 && row[0] instanceof Number && row[1] instanceof Number && row[2] instanceof Number) {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    double revenue = ((Number) row[2]).doubleValue();
                    
                    YearMonth yearMonth = YearMonth.of(year, month);
                    revenueByMonth.merge(yearMonth, revenue, Double::sum);
                    log.debug("Contract revenue for {}: {}", yearMonth, revenue);
                }
            }
        }
        
        // Mostrar totales para depuración
        for (Map.Entry<YearMonth, Double> entry : revenueByMonth.entrySet()) {
            log.info("Total revenue for {}: {}", entry.getKey(), entry.getValue());
        }

        // Generar la lista de resultados con todos los meses, incluso los que no tienen ingresos
        List<MonthlyRevenueDto> result = new ArrayList<>();
        YearMonth monthToIterateFrom = YearMonth.now().minusMonths(validLastNMonths - 1);

        for (int i = 0; i < validLastNMonths; i++) {
            YearMonth currentLoopMonth = monthToIterateFrom.plusMonths(i);
            double monthRevenue = revenueByMonth.getOrDefault(currentLoopMonth, 0.0);
            log.debug("Month {}: Revenue = {}", currentLoopMonth, monthRevenue);
            result.add(new MonthlyRevenueDto(
                    currentLoopMonth.format(YEAR_MONTH_FORMATTER),
                    monthRevenue
            ));
        }
        return result;
    }

    @Override
    public List<ReservationsBySpaceTypeDto> getReservationsBySpaceType() {
        log.debug("Fetching reservations by space type");
        return bookingRepository.findReservationsCountBySpaceType().stream()
                .filter(row -> row != null && row.length == 2 && row[0] instanceof String && row[1] instanceof Long)
                .map(row -> new ReservationsBySpaceTypeDto((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationsByZoneDto> getReservationsByZone() {
        log.debug("Fetching reservations by zone");
        return bookingRepository.findReservationsCountByZone().stream()
                .filter(row -> row != null && row.length == 2 && row[0] instanceof String && row[1] instanceof Long)
                .map(row -> new ReservationsByZoneDto((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    @Override
    public List<PeakHoursDto> getPeakReservationHours() {
        log.debug("Fetching peak reservation hours");
        List<Object[]> rawData = bookingRepository.findReservationCountsByInitHour(RELEVANT_BOOKING_STATUSES_FOR_PEAK_HOURS);

        Map<Integer, Long> reservationsByHourOfDay = rawData.stream()
                .filter(row -> row != null && row.length == 2 && row[0] instanceof LocalTime && row[1] instanceof Long)
                .collect(Collectors.groupingBy(
                        row -> ((LocalTime) row[0]).getHour(),
                        Collectors.summingLong(row -> (Long) row[1])
                ));

        List<PeakHoursDto> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            result.add(new PeakHoursDto(hour, reservationsByHourOfDay.getOrDefault(hour, 0L)));
        }
        return result;
    }
}
