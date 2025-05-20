package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.dashboard.admin.KpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.MonthlyRevenueDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.PeakHoursDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.ReservationsBySpaceTypeDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.ReservationsByZoneDto;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface DashboardAdminService {
    KpiCardsDto getKpiCardsData();
    List<MonthlyRevenueDto> getMonthlyRevenue(int lastNMonths);
    List<ReservationsBySpaceTypeDto> getReservationsBySpaceType();
    List<ReservationsByZoneDto> getReservationsByZone();
    List<PeakHoursDto> getPeakReservationHours();
}
