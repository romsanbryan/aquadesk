package romsanbryan.aquadesk.dto.response;

import romsanbryan.data.entities.AquariumFilter;

import java.time.LocalDateTime;

public record AquariumFilterResponse(
        Integer id,
        String name,
        String materialBiological,
        String materialMechanical,
        String materialBioMechanical,
        boolean sponge,
        boolean carbonActive,
        boolean perlon,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public AquariumFilterResponse(AquariumFilter filter) {
        this(
                filter.getId(),
                filter.getName(),
                filter.getMaterialBiological(),
                filter.getMaterialMechanical(),
                filter.getMaterialBioMechanical(),
                filter.isSponge(),
                filter.isCarbonActive(),
                filter.isPerlon(),
                filter.getCreatedAt(),
                filter.getUpdatedAt()
        );
    }
}
