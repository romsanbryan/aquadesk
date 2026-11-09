package romsanbryan.data.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_aquarium_start_date_type", columnList = "aquarium_id,start_date,event_type")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "event_type", nullable = false, length = 50)
    private String type;

    @Column(name = "clickable", nullable = false)
    private Boolean clickeable = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private Aquarium aquarium;
    // Event.java — additional relationship
    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Co2 co2;
    // Event.java — additional relationship
    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Maintenance maintenance;

    public Maintenance getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(Maintenance maintenance) {
        this.maintenance = maintenance;
    }

    public Co2 getCo2() {
        return co2;
    }

    public void setCo2(Co2 co2) {
        this.co2 = co2;
    }

    // Constructors
    public Event() {
    }

    public Event(String title, LocalDate startDate, String type) {
        this.title = title;
        this.startDate = startDate;
        this.type = type;
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getClickeable() {
        return clickeable;
    }

    public void setClickeable(Boolean clickeable) {
        this.clickeable = clickeable;
    }

    public Aquarium getAquarium() {
        return aquarium;
    }

    public void setAquarium(Aquarium aquarium) {
        this.aquarium = aquarium;
    }

    @PrePersist
    private void assignDefaultAquariumForLegacyWrites() {
        if (aquarium == null) {
            Aquarium defaultAquarium = new Aquarium();
            defaultAquarium.setId(1);
            aquarium = defaultAquarium;
        }
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startDate=" + startDate +
                ", type='" + type + '\'' +
                ", clickeable=" + clickeable +
                '}';
    }
}