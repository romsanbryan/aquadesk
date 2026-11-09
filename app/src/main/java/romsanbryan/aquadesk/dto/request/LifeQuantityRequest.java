package romsanbryan.aquadesk.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LifeQuantityRequest(
        @NotNull Integer change,
        LocalDate date
) {
}
