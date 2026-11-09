package romsanbryan.aquadesk.dto.response;


import romsanbryan.aquadesk.dto.EventDetails;

import java.time.LocalDateTime;
import java.util.List;


public record EventResponse(
        Long id,
        String title,
        LocalDateTime start,
        LocalDateTime end,
        String color,
        String description,
        boolean allDay,
        String type,
        int count,
        List<EventDetails> items
) {
    public EventResponse {
        if (title == null || title.isBlank()) title = "(Untitled)";
    }
}