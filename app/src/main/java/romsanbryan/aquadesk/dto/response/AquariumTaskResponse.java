package romsanbryan.aquadesk.dto.response;

import romsanbryan.data.entities.AquariumTask;

import java.time.LocalDate;

public record AquariumTaskResponse(
        Integer id,
        String title,
        String description,
        LocalDate actionDate,
        String eventType
) {
    public AquariumTaskResponse(AquariumTask task) {
        this(task.getId(), task.getTitle(), task.getDescription(), task.getActionDate(), task.getEventType());
    }
}
