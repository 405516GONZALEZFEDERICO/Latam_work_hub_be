package Latam.Latam.work.hub.controllers;

import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientKpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientMonthlySpendingDto;
import Latam.Latam.work.hub.dtos.common.dashboard.cliente.ClientBookingsByTypeDto;
import Latam.Latam.work.hub.services.DashboardClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard-client")
@PreAuthorize("hasRole('CLIENTE')")
@RequiredArgsConstructor
public class DashboardClientController {
    private final DashboardClientService dashboardClientService;

    /**
     * Endpoint para obtener los datos de las Tarjetas de KPI del cliente.
     */
    @GetMapping("/kpi-cards")
    public ResponseEntity<ClientKpiCardsDto> getKpiCards(@RequestParam String uid) {
        ClientKpiCardsDto kpiData = dashboardClientService.getKpiCardsData(uid);
        return ResponseEntity.ok(kpiData);
    }

    /**
     * Endpoint para el gráfico de líneas de gastos mensuales del cliente.
     * @param uid UID del cliente
     * @param months Número de meses hacia atrás (opcional, defecto: 12).
     */
    @GetMapping("/monthly-spending")
    public ResponseEntity<List<ClientMonthlySpendingDto>> getMonthlySpending(
            @RequestParam String uid,
            @RequestParam(name = "months", defaultValue = "12", required = false) int months) {
        if (months <= 0 || months > 60) {
            months = 12;
        }
        List<ClientMonthlySpendingDto> spendingData = dashboardClientService.getMonthlySpending(uid, months);
        return ResponseEntity.ok(spendingData);
    }

    /**
     * Endpoint para obtener las reservas por tipo de espacio del cliente.
     */
    @GetMapping("/bookings-by-space-type")
    public ResponseEntity<List<ClientBookingsByTypeDto>> getBookingsBySpaceType(@RequestParam String uid) {
        List<ClientBookingsByTypeDto> data = dashboardClientService.getBookingsBySpaceType(uid);
        return ResponseEntity.ok(data);
    }
} 