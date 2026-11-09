package romsanbryan.aquadesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AquariumTaskRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull LocalDate actionDate,
        @NotBlank @Pattern(regexp = "info|live|co2|manteiment|parameters|photo") String eventType
) {
}
