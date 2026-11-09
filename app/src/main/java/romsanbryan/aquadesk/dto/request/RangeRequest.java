package romsanbryan.aquadesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RangeRequest(
        @NotBlank @Size(max = 100)
        String parameter,
        BigDecimal level1,
        BigDecimal level2,
        BigDecimal level3,
        BigDecimal level4,
        BigDecimal level5,
        @Size(max = 50)
        String unit
) {
}
