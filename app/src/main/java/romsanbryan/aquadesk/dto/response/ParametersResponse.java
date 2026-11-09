package romsanbryan.aquadesk.dto.response;

import java.math.BigDecimal;

public record ParametersResponse(
        BigDecimal nh3,
        BigDecimal no2,
        BigDecimal no3,
        BigDecimal ph,
        BigDecimal kh,
        BigDecimal gh,
        String estado
) {
}