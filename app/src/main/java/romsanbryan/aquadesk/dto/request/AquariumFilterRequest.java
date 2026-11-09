package romsanbryan.aquadesk.dto.request;

import jakarta.validation.constraints.Size;

public record AquariumFilterRequest(
        @Size(max = 50)
        String name,
        @Size(max = 255)
        String materialBiological,
        @Size(max = 255)
        String materialMechanical,
        @Size(max = 255)
        String materialBioMechanical,
        boolean sponge,
        boolean carbonActive,
        boolean perlon
) {
}
