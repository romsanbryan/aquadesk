package romsanbryan.aquadesk.dto.response;

import romsanbryan.aquadesk.dto.FilterDto;
import romsanbryan.data.entities.Maintenance;

import java.math.BigDecimal;
import java.util.List;

public record MaintenanceResponse(
        Integer id,
        Integer water,
        BigDecimal primeDose,
        Boolean glass,
        String pruning,
        String observations,
        List<FilterDto> filters
) {
    public MaintenanceResponse(Maintenance m, List<romsanbryan.data.entities.Filtre> filterEntities) {
        this(
                m.getId(),
                m.getWater(),
                m.getPrimeDose(),
                m.getGlass(),
                m.getPruning(),
                m.getObservations(),
                filterEntities.stream().map(FilterDto::new).toList()
        );
    }
}