package romsanbryan.aquadesk.dto.response;

import romsanbryan.data.entities.Aquarium;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AquariumResponse(
        Integer id,
        String name,
        String type,
        BigDecimal volume,
        String glassThickness,
        String size,
        String heater,
        String light,
        String substrate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AquariumFilterResponse> filters
) {
    public AquariumResponse(Aquarium aquarium) {
        this(
                aquarium.getId(),
                aquarium.getName(),
                aquarium.getType(),
                aquarium.getVolume(),
                aquarium.getGlassThickness(),
                aquarium.getSize(),
                aquarium.getHeater(),
                aquarium.getLight(),
                aquarium.getSubstrate(),
                aquarium.getCreatedAt(),
                aquarium.getUpdatedAt(),
                aquarium.getFilters().stream().map(AquariumFilterResponse::new).toList()
        );
    }
}
