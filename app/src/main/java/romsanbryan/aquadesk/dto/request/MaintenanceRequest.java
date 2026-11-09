package romsanbryan.aquadesk.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenanceRequest(
        @JsonProperty("fecha") LocalDate date,
        @NotNull @PositiveOrZero @JsonAlias("watter") Integer water,
        @PositiveOrZero @JsonAlias("prime") BigDecimal primeDose,
        Boolean glass,
        @Size(max = 255, message = "Pruning must be at most 255 characters.")
        String pruning,
        @Size(max = 500, message = "Observations must be at most 500 characters.")
        String observations,
        Boolean f1Sponge,
        Boolean f1Perlon,
        Boolean f1Mech,
        Boolean f1Bio,
        Boolean f1BioMech,
        Boolean f2Sponge,
        Boolean f2Perlon,
        Boolean f2Mech,
        Boolean f2Bio,
        Boolean f2BioMech
) {
}
