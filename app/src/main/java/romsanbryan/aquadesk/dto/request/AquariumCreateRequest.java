package romsanbryan.aquadesk.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record AquariumCreateRequest(
        @NotBlank @Size(max = 150)
        String name,
        @Size(max = 100)
        String type,
        @PositiveOrZero @Digits(integer = 8, fraction = 2)
        BigDecimal volume,
        @Size(max = 50)
        String glassThickness,
        @Size(max = 100)
        String size,
        @Size(max = 100)
        String heater,
        @Size(max = 100)
        String light,
        @Size(max = 255)
        String substrate,
        List<@NotNull @Valid AquariumFilterRequest> filters
) {
}
