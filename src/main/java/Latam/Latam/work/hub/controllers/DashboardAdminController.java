package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.dashboard.admin.KpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.MonthlyRevenueDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.PeakHoursDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.ReservationsBySpaceTypeDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.ReservationsByZoneDto;
import Latam.Latam.work.hub.services.DashboardAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("/api/dashboard-admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardAdminController {
    private final DashboardAdminService dashboardAdminService;

    /**
     * HU1: Endpoint para obtener los datos de las Tarjetas de KPI.
     */
    @GetMapping("/kpi-cards")
    public ResponseEntity<KpiCardsDto> getKpiCards() {
        // Asegúrate que el método en el servicio se llame getKpiCardsData() o ajústalo.
        KpiCardsDto kpiData = dashboardAdminService.getKpiCardsData();
        return ResponseEntity.ok(kpiData);
    }

    /**
     * HU2: Endpoint para el gráfico de líneas de ingresos mensuales.
     * @param months Número de meses hacia atrás (opcional, defecto: 12).
     */
    @GetMapping("/monthly-revenue")
    public ResponseEntity<List<MonthlyRevenueDto>> getMonthlyRevenue(
            @RequestParam(name = "months", defaultValue = "12", required = false) int months) {
        // Validación básica
        if (months <= 0 || months > 60) {
            months = 12;
        }
        List<MonthlyRevenueDto> revenueData = dashboardAdminService.getMonthlyRevenue(months);
        return ResponseEntity.ok(revenueData);
    }

    /**
     * HU3: Endpoint para el gráfico de barras de reservas por tipo de espacio.
     */
    @GetMapping("/reservations-by-space-type")
    public ResponseEntity<List<ReservationsBySpaceTypeDto>> getReservationsBySpaceType() {
        List<ReservationsBySpaceTypeDto> data = dashboardAdminService.getReservationsBySpaceType();
        return ResponseEntity.ok(data);
    }

    /**
     * HU4: Endpoint para el mapa de calor de reservas por provincia/zona.
     */
    @GetMapping("/reservations-by-zone")
    public ResponseEntity<List<ReservationsByZoneDto>> getReservationsByZone() {
        List<ReservationsByZoneDto> data = dashboardAdminService.getReservationsByZone();
        return ResponseEntity.ok(data);
    }

    /**
     * HU5: Endpoint para el histograma de horarios más alquilados.
     */
    @GetMapping("/peak-reservation-hours")
    public ResponseEntity<List<PeakHoursDto>> getPeakReservationHours() {
        List<PeakHoursDto> data = dashboardAdminService.getPeakReservationHours();
        return ResponseEntity.ok(data);
    }
}