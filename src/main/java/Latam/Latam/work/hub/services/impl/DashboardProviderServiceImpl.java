package Latam.Latam.work.hub.services.impl;

import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderKpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderMonthlyRevenueDto;
import Latam.Latam.work.hub.dtos.common.dashboard.proveedor.ProviderSpacePerformanceDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.enums.BookingStatus;
import Latam.Latam.work.hub.enums.ContractStatus;
import Latam.Latam.work.hub.repositories.BookingRepository;
import Latam.Latam.work.hub.repositories.InvoiceRepository;
import Latam.Latam.work.hub.repositories.RentalContractRepository;
import Latam.Latam.work.hub.repositories.SpaceRepository;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.DashboardProviderService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardProviderServiceImpl implements DashboardProviderService {
    private static final Logger log = LoggerFactory.getLogger(DashboardProviderServiceImpl.class);

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final RentalContractRepository rentalContractRepository;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<BookingStatus> RELEVANT_BOOKING_STATUSES = Arrays.asList(
            BookingStatus.CONFIRMED, BookingStatus.ACTIVE, BookingStatus.PENDING_PAYMENT
    );

    @Override
    public ProviderKpiCardsDto getKpiCardsData(String providerUid) {
        log.debug("Fetching KPI cards data for provider: {}", providerUid);

        UserEntity provider = userRepository.findByFirebaseUid(providerUid)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        // Total de espacios del proveedor
        long totalSpaces = spaceRepository.countByOwnerId(provider.getId());

        // Contratos activos
        long activeContracts = rentalContractRepository.countActiveContractsByProviderId(provider.getId());

        // Reservas del mes actual
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        long reservationsThisMonth = bookingRepository.countReservationsByProviderInDateRange(
                provider.getId(), startOfMonth, endOfMonth, RELEVANT_BOOKING_STATUSES
        );

        // Ingresos de los últimos 30 días
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();
        
        log.info("Calculando ingresos para proveedor {} desde {} hasta {}", 
                provider.getId(), thirtyDaysAgo, now);
        
        // === INGRESOS BRUTOS ===
        // RESERVAS: Contamos directamente desde BookingEntity (NO desde InvoiceEntity para evitar duplicación)
        Double bookingGrossRevenue = bookingRepository.sumGrossRevenueByProviderInDateRange(
                provider.getId(), thirtyDaysAgo, now
        );
        log.info("Ingresos BRUTOS de reservas para proveedor {}: {}", provider.getId(), bookingGrossRevenue);
        
        // CONTRATOS: Solo facturas de contratos (NO de reservas para evitar duplicación)
        Double contractGrossRevenue = invoiceRepository.sumGrossRevenueFromContractsByProviderId(
                provider.getId(), thirtyDaysAgo, now
        );
        log.info("Ingresos BRUTOS de contratos para proveedor {}: {}", provider.getId(), contractGrossRevenue);
        
        Double totalGrossRevenue = (bookingGrossRevenue != null ? bookingGrossRevenue : 0.0) + 
                                  (contractGrossRevenue != null ? contractGrossRevenue : 0.0);
        log.info("Total ingresos BRUTOS para proveedor {}: {}", provider.getId(), totalGrossRevenue);
        
        // === REEMBOLSOS ===
        // Solo contar reembolsos reales procesados
        Double contractDepositRefunds = rentalContractRepository.sumDepositRefundsByProviderId(
                provider.getId(), thirtyDaysAgo, now
        );
        Double bookingRefunds = bookingRepository.sumTotalRefundsByProviderInDateRange(
                provider.getId(), thirtyDaysAgo, now
        );
        // Reembolsos en facturas SOLO de contratos (NO de reservas para evitar duplicación)
        Double contractInvoiceRefunds = invoiceRepository.sumInvoiceRefundsFromContractsByProviderId(
                provider.getId(), thirtyDaysAgo, now
        );
        
        Double totalRefunds = (contractDepositRefunds != null ? contractDepositRefunds : 0.0) + 
                             (bookingRefunds != null ? bookingRefunds : 0.0) +
                             (contractInvoiceRefunds != null ? contractInvoiceRefunds : 0.0);
        log.info("Reembolsos de depósitos para proveedor {}: {}", provider.getId(), contractDepositRefunds);
        log.info("Reembolsos de reservas para proveedor {}: {}", provider.getId(), bookingRefunds);
        log.info("Reembolsos de facturas de contratos para proveedor {}: {}", provider.getId(), contractInvoiceRefunds);
        log.info("Total reembolsos REALES para proveedor {}: {}", provider.getId(), totalRefunds);
        
        // === INGRESOS NETOS ===
        Double totalNetRevenue = totalGrossRevenue - totalRefunds;
        log.info("Total ingresos NETOS para proveedor {}: {}", provider.getId(), totalNetRevenue);
        
        // Método anterior para compatibilidad
        Double bookingRevenue = bookingRepository.sumRevenueByProviderInDateRange(
                provider.getId(), thirtyDaysAgo, now
        );
        log.info("Ingresos de reservas (método anterior) para proveedor {}: {}", provider.getId(), bookingRevenue);
        
        Double contractRevenue = invoiceRepository.sumRevenueByProviderId(
                provider.getId(), thirtyDaysAgo, now
        );
        log.info("Ingresos de contratos (método anterior) para proveedor {}: {}", provider.getId(), contractRevenue);
        
        Double totalRevenue = (bookingRevenue != null ? bookingRevenue : 0.0) + 
                             (contractRevenue != null ? contractRevenue : 0.0);
        log.info("Total ingresos (método anterior) para proveedor {}: {}", provider.getId(), totalRevenue);

        // Espacios ocupados vs disponibles
        long spacesOccupied = spaceRepository.countOccupiedSpacesByOwnerId(provider.getId());
        long spacesAvailable = totalSpaces - spacesOccupied;
        
        // Tasa de ocupación
        Double occupancyRate = totalSpaces > 0 ? (double) spacesOccupied / totalSpaces * 100 : 0.0;

        return ProviderKpiCardsDto.builder()
                .totalSpaces(totalSpaces)
                .activeContracts(activeContracts)
                .reservationsThisMonth(reservationsThisMonth)
                // Nuevos campos diferenciados
                .totalGrossRevenueLast30Days(totalGrossRevenue)
                .totalNetRevenueLast30Days(totalNetRevenue)
                .totalRefundsLast30Days(totalRefunds)
                // Campo deprecated para compatibilidad
                .totalRevenueLast30Days(totalNetRevenue) // Usamos neto para mantener consistencia
                .spacesOccupied(spacesOccupied)
                .spacesAvailable(spacesAvailable)
                .occupancyRate(occupancyRate)
                .build();
    }

    @Override
    public List<ProviderMonthlyRevenueDto> getMonthlyRevenue(String providerUid, int lastNMonths) {
        log.debug("Fetching monthly revenue for provider: {} for the last {} months", providerUid, lastNMonths);
        
        UserEntity provider = userRepository.findByFirebaseUid(providerUid)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        int validLastNMonths = Math.max(1, lastNMonths);
        LocalDateTime startDateOfRange = LocalDate.now().minusMonths(validLastNMonths - 1L).withDayOfMonth(1).atStartOfDay();

        Map<YearMonth, Double> revenueByMonth = new HashMap<>();
        
        // Ingresos de reservas
        List<Object[]> bookingRevenue = bookingRepository.findMonthlyRevenueByProvider(provider.getId(), startDateOfRange);
        if (bookingRevenue != null) {
            for (Object[] row : bookingRevenue) {
                if (row != null && row.length == 3 && row[0] instanceof Number && row[1] instanceof Number && row[2] instanceof Number) {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    double revenue = ((Number) row[2]).doubleValue();
                    
                    YearMonth yearMonth = YearMonth.of(year, month);
                    revenueByMonth.merge(yearMonth, revenue, Double::sum);
                }
            }
        }
        
        // Ingresos de contratos
        List<Object[]> contractRevenue = invoiceRepository.findMonthlyRevenueByProvider(provider.getId(), startDateOfRange);
        if (contractRevenue != null) {
            for (Object[] row : contractRevenue) {
                if (row != null && row.length == 3 && row[0] instanceof Number && row[1] instanceof Number && row[2] instanceof Number) {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    double revenue = ((Number) row[2]).doubleValue();
                    
                    YearMonth yearMonth = YearMonth.of(year, month);
                    revenueByMonth.merge(yearMonth, revenue, Double::sum);
                }
            }
        }

        List<ProviderMonthlyRevenueDto> result = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        
        for (int i = validLastNMonths - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            Double revenue = revenueByMonth.getOrDefault(targetMonth, 0.0);
            result.add(new ProviderMonthlyRevenueDto(
                    targetMonth.format(YEAR_MONTH_FORMATTER), 
                    revenue
            ));
        }
        
        return result;
    }

    @Override
    public List<ProviderSpacePerformanceDto> getSpacePerformance(String providerUid) {
        log.debug("Fetching space performance for provider: {}", providerUid);
        
        UserEntity provider = userRepository.findByFirebaseUid(providerUid)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        List<Object[]> spacePerformanceData = spaceRepository.findSpacePerformanceByProvider(provider.getId());
        
        return spacePerformanceData.stream()
                .filter(row -> row != null && row.length == 5)
                .map(row -> new ProviderSpacePerformanceDto(
                        (String) row[0], // spaceName
                        ((Number) row[1]).longValue(), // totalBookings
                        ((Number) row[2]).longValue(), // totalContracts
                        ((Number) row[3]).doubleValue(), // totalRevenue
                        ((Number) row[4]).doubleValue() // occupancyRate
                ))
                .collect(Collectors.toList());
    }
} 