package romsanbryan.aquadesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LifeRequest(
        @NotBlank @Size(max = 150) String commonName,
        @NotBlank @Pattern(regexp = "(?i)fish|plant") String category,
        @Size(max = 150) String scientificName,
        @NotNull @Positive Integer quantity,
        @Size(max = 4000) String recommendedParameters,
        @Size(max = 2048) String technicalSheetUrl,
        @Size(max = 4000) String notes,
        LocalDate date
) {
}
