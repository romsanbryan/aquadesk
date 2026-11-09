package romsanbryan.aquadesk.dto.response;

import romsanbryan.aquadesk.dto.FilterDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LastMaintenanceResponse(
        LocalDate fecha,
        Integer water,
        BigDecimal primeDose,
        String observations,
        Boolean glass,
        String pruning,
        List<FilterDto> filters
) {
}