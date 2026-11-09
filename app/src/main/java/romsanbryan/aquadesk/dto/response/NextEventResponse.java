package romsanbryan.aquadesk.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record NextEventResponse(
        String type,
        @JsonProperty("ultimaFecha") LocalDate lastDate,
        @JsonProperty("proximaFecha") LocalDate nextDate,
        @JsonProperty("diasRestantes") long remainingDays
) {
}