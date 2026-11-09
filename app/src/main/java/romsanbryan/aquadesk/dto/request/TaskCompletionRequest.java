package romsanbryan.aquadesk.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskCompletionRequest(
        @NotNull LocalDate date,
        String infoType,
        @Size(max = 50) String status,
        String observation,
        @Size(max = 100) String product,
        @Size(max = 50) String value,
        @PositiveOrZero Integer water,
        @PositiveOrZero BigDecimal primeDose,
        Boolean glass,
        String pruning,
        String maintenanceObservations,
        Boolean f1Sponge,
        Boolean f1Perlon,
        Boolean f1Mech,
        Boolean f1Bio,
        Boolean f1BioMech,
        Boolean f2Sponge,
        Boolean f2Perlon,
        Boolean f2Mech,
        Boolean f2Bio,
        Boolean f2BioMech,
        BigDecimal nh3,
        BigDecimal no2,
        BigDecimal no3,
        BigDecimal ph,
        BigDecimal kh,
        BigDecimal gh,
        BigDecimal temp
) {
}
