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
        // CORREGIDO: Ahora incluye facturas PAID e ISSUED (antes solo PAID)
        Double contractGrossSpending = invoiceRepository.sumGrossSpendingFromContractsByClientId(
                client.getId(), thirtyDaysAgo, now
        );
        
        // DEBUG LOGS para gastos brutos
        log.info("=== DEBUG GASTOS BRUTOS CLIENTE {} ===", client.getId());
        log.info("Periodo: {} a {}", thirtyDaysAgo, now);
        log.info("Gastos reservas: {}", bookingGrossSpending);
        log.info("Gastos contratos (CORREGIDO - incluye PAID e ISSUED): {}", contractGrossSpending);
        
        Double totalGrossSpent = (bookingGrossSpending != null ? bookingGrossSpending : 0.0) + 
                                (contractGrossSpending != null ? contractGrossSpending : 0.0);
        
        log.info("Total gastos brutos: {}", totalGrossSpent);
        
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
        log.info("=== DEBUG REEMBOLSOS CLIENTE {} ===", client.getId());
        log.info("Periodo: {} a {}", thirtyDaysAgo, now);
        log.info("Reembolsos depósitos contratos: {}", contractDepositRefunds);
        log.info("Reembolsos reservas: {}", bookingRefunds);
        log.info("Reembolsos facturas contratos: {}", contractInvoiceRefunds);
        
        Double totalRefunds = (contractDepositRefunds != null ? contractDepositRefunds : 0.0) + 
                             (bookingRefunds != null ? bookingRefunds : 0.0) +
                             (contractInvoiceRefunds != null ? contractInvoiceRefunds : 0.0);
        
        log.info("Total reembolsos: {}", totalRefunds);
        log.info("=== FIN DEBUG REEMBOLSOS ===");
        
        // === GASTOS NETOS ===
        Double totalNetSpent = totalGrossSpent - totalRefunds;
        
        log.info("Total gastos netos: {}", totalNetSpent);
        log.info("=== FIN DEBUG GASTOS BRUTOS ===");
        
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

        // === COMPARACIÓN DIRECTA ENTRE MÉTODOS ===
        log.info("=== COMPARANDO MÉTODOS DE CÁLCULO ===");
        
        // Método NUEVO (bruto)
        log.info("MÉTODO NUEVO - Gastos brutos: ${}", totalGrossSpent);
        log.info("  - Reservas brutas: ${}", bookingGrossSpending);
        log.info("  - Contratos brutos: ${}", contractGrossSpending);
        
        // Método ANTIGUO (neto)
        log.info("MÉTODO ANTIGUO - Gastos netos: ${}", totalSpent);
        log.info("  - Reservas antiguas: ${}", bookingSpending);
        log.info("  - Contratos antiguos: ${}", contractSpending);
        
        // Reembolsos
        log.info("REEMBOLSOS TOTALES: ${}", totalRefunds);
        log.info("  - Depósitos contratos: ${}", contractDepositRefunds);
        log.info("  - Reservas: ${}", bookingRefunds);
        log.info("  - Facturas contratos: ${}", contractInvoiceRefunds);
        
        // Cálculo neto
        log.info("CÁLCULO NETO: ${} - ${} = ${}", totalGrossSpent, totalRefunds, totalNetSpent);
        
        // Diferencias
        double diffGrossVsOld = (totalGrossSpent != null ? totalGrossSpent : 0.0) - (totalSpent != null ? totalSpent : 0.0);
        log.info("DIFERENCIA (Bruto vs Antiguo): ${}", diffGrossVsOld);
        log.info("=== FIN COMPARACIÓN ===");

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

        log.info("=== DEBUG GASTOS MENSUALES CLIENTE {} ===", client.getId());
        log.info("Período de consulta: desde {} hasta ahora", startDateOfRange);
        log.info("Solicitando {} meses de datos", validLastNMonths);

        Map<YearMonth, Double> spendingByMonth = new HashMap<>();
        
        // === CAMBIO A GASTOS BRUTOS PARA COINCIDIR CON KPI CARDS ===
        log.info("=== CALCULANDO GASTOS BRUTOS MENSUALES CLIENTE {} ===", client.getId());
        log.info("Período de consulta: desde {} hasta ahora", startDateOfRange);
        log.info("*** IMPORTANTE: USANDO MÉTODO BRUTO PARA COINCIDIR CON KPI CARDS ***");
        
        // Gastos BRUTOS en reservas (mismo método que KPI cards)
        log.info("--- CONSULTANDO GASTOS BRUTOS DE RESERVAS ---");
        List<Object[]> bookingGrossSpending = bookingRepository.findMonthlyGrossSpendingByClient(client.getId(), startDateOfRange);
        log.info("Gastos BRUTOS de reservas: {} registros", bookingGrossSpending != null ? bookingGrossSpending.size() : 0);
        
        if (bookingGrossSpending != null) {
            for (Object[] row : bookingGrossSpending) {
                if (row != null && row.length == 3 && row[0] instanceof Number && row[1] instanceof Number && row[2] instanceof Number) {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    double spending = ((Number) row[2]).doubleValue();
                    
                    log.info("Reserva BRUTA: Año {}, Mes {}, Gasto: ${}", year, month, spending);
                    
                    YearMonth yearMonth = YearMonth.of(year, month);
                    spendingByMonth.merge(yearMonth, spending, Double::sum);
                }
            }
        }
        
        // Gastos BRUTOS en contratos (mismo método que KPI cards)
        log.info("--- CONSULTANDO GASTOS BRUTOS DE CONTRATOS ---");
        List<Object[]> contractGrossSpending = invoiceRepository.findMonthlyGrossSpendingByClientContracts(client.getId(), startDateOfRange);
        log.info("Gastos BRUTOS de contratos: {} registros", contractGrossSpending != null ? contractGrossSpending.size() : 0);
        
        if (contractGrossSpending != null) {
            for (Object[] row : contractGrossSpending) {
                if (row != null && row.length == 3 && row[0] instanceof Number && row[1] instanceof Number && row[2] instanceof Number) {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    double spending = ((Number) row[2]).doubleValue();
                    
                    log.info("Contrato BRUTO: Año {}, Mes {}, Gasto: ${}", year, month, spending);
                    
                    YearMonth yearMonth = YearMonth.of(year, month);
                    spendingByMonth.merge(yearMonth, spending, Double::sum);
                } else {
                    log.warn("Fila de contrato BRUTO inválida: {}", Arrays.toString(row));
                }
            }
        } else {
            log.warn("contractGrossSpending es NULL - No se encontraron datos de contratos BRUTOS");
        }

        // === VERIFICACIÓN CONTRA KPI CARDS ===
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();
        
        // Gastos de contratos en período de 30 días (mismo método que KPI cards)
        Double contractGrossSpending30Days = invoiceRepository.sumGrossSpendingFromContractsByClientId(
                client.getId(), thirtyDaysAgo, now);
        log.info("*** VERIFICACIÓN KPI vs GRÁFICO ***");
        log.info("KPI Cards - Gastos contratos últimos 30 días: ${}", contractGrossSpending30Days);
        
        // Gastos de reservas en período de 30 días (mismo método que KPI cards)
        Double bookingGrossSpending30Days = bookingRepository.sumGrossSpendingByClientInDateRange(
                client.getId(), thirtyDaysAgo, now);
        log.info("KPI Cards - Gastos reservas últimos 30 días: ${}", bookingGrossSpending30Days);
        
        Double totalKpiSpending = (contractGrossSpending30Days != null ? contractGrossSpending30Days : 0.0) + 
                                 (bookingGrossSpending30Days != null ? bookingGrossSpending30Days : 0.0);
        log.info("KPI Cards - TOTAL gastos últimos 30 días: ${}", totalKpiSpending);

        // === RESUMEN GRÁFICO POR MES ===
        log.info("--- RESUMEN GASTOS BRUTOS POR MES ---");
        for (Map.Entry<YearMonth, Double> entry : spendingByMonth.entrySet()) {
            log.info("Mes {}: Total BRUTO ${}", entry.getKey().format(YEAR_MONTH_FORMATTER), entry.getValue());
        }

        // Sumar todo el período del gráfico para comparar
        Double totalGraphSpending = spendingByMonth.values().stream().mapToDouble(Double::doubleValue).sum();
        log.info("GRÁFICO - TOTAL gastos desde {}: ${}", startDateOfRange, totalGraphSpending);
        
        log.info("*** DIFERENCIA ESPERADA: El KPI es últimos 30 días, el gráfico es {} meses ***", validLastNMonths);

        List<ClientMonthlySpendingDto> result = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        
        log.info("--- CONSTRUYENDO RESULTADO FINAL ---");
        for (int i = validLastNMonths - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            Double spending = spendingByMonth.getOrDefault(targetMonth, 0.0);
            log.info("Mes objetivo {}: Gasto final ${}", targetMonth.format(YEAR_MONTH_FORMATTER), spending);
            result.add(new ClientMonthlySpendingDto(
                    targetMonth.format(YEAR_MONTH_FORMATTER), 
                    spending
            ));
        }
        
        log.info("=== FIN DEBUG GASTOS MENSUALES ===");
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