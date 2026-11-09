package romsanbryan.aquadesk.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record InfoRequest(
        @JsonProperty("fecha") LocalDate date,
        String type,
        String status,
        String observation
) {
}