package romsanbryan.aquadesk.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TreatmentRequest(
        @NotBlank @Size(max = 100)
        @JsonProperty("nombre") String name,
        @NotBlank @Size(max = 50)
        @JsonProperty("valor") String value,
        @JsonProperty("fecha") LocalDate date
) {
}
