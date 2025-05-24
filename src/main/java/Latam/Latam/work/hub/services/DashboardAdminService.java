package Latam.Latam.work.hub.services;

import Latam.Latam.work.hub.dtos.common.dashboard.admin.KpiCardsDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.MonthlyRevenueDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.PeakHoursDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.ReservationsBySpaceTypeDto;
import Latam.Latam.work.hub.dtos.common.dashboard.admin.TopSpacesDto;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface DashboardAdminService {
    KpiCardsDto getKpiCardsData();
    List<MonthlyRevenueDto> getMonthlyRevenue(int lastNMonths);
    List<ReservationsBySpaceTypeDto> getReservationsBySpaceType();
    List<PeakHoursDto> getPeakReservationHours();
    List<ReservationsBySpaceTypeDto> getRentalContractsBySpaceType();
    List<TopSpacesDto> getTop5Spaces();
}
