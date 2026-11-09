package romsanbryan.data.entities;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "aquarium_tasks", indexes = {
        @Index(name = "idx_aquarium_tasks_aquarium_date", columnList = "aquarium_id,action_date")
})
public class AquariumTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private Aquarium aquarium;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "action_date", nullable = false)
    private LocalDate actionDate;

    @Column(name = "event_type", nullable = false, length = 50, columnDefinition = "varchar(50) default 'info'")
    private String eventType = "info";

    @Version
    private Integer version;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Aquarium getAquarium() { return aquarium; }
    public void setAquarium(Aquarium aquarium) { this.aquarium = aquarium; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getActionDate() { return actionDate; }
    public void setActionDate(LocalDate actionDate) { this.actionDate = actionDate; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}
