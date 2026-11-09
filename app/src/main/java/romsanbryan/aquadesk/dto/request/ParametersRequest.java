package romsanbryan.aquadesk.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ParametersRequest(
        @JsonProperty("fecha") LocalDate date,
        BigDecimal nh3,
        BigDecimal no2,
        BigDecimal no3,
        BigDecimal ph,
        BigDecimal kh,
        BigDecimal gh,
        BigDecimal temp
) {
}