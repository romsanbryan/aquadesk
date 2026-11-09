package romsanbryan.aquadesk.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventDetails(
        Long id,
        String title,
        LocalDateTime start,
        LocalDateTime end,
        BigDecimal co2Value,
        String co2Product
) {
}