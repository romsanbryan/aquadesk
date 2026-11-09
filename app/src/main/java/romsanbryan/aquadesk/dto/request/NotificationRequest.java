package romsanbryan.aquadesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NotificationRequest(
        @NotBlank @Size(max = 100)
        String parameter,
        @NotNull @Positive
        Integer value,
        Boolean active,
        List<String> weekdaysList,
        Integer weekdays
) {
}