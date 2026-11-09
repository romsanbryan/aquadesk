package romsanbryan.data.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_aquarium_active", columnList = "aquarium_id,active")
})
public class Notifications {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private Aquarium aquarium;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(nullable = false, length = 100)
    private String parameter;

    @Column(nullable = false)
    private Integer value;

    @Column(nullable = false)
    private Boolean active;

    /**
     * Bitmask representing weekdays when this notification should fire.
     * Bit 0 = Monday ... Bit 6 = Sunday
     */
    @Column(name = "weekdays", nullable = false)
    private Integer weekdays = 0;

    public Integer getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(Integer weekdays) {
        this.weekdays = weekdays != null ? weekdays : 0;
    }

    /**
     * Checks whether the notification is active on the given weekday.
     */
    public boolean isActiveOn(DayOfWeek day) {
        if (day == null) return false;
        int index = day.getValue() - 1; // DayOfWeek.MONDAY == 1 -> bit 0
        int mask = 1 << index;
        return (getWeekdays() & mask) != 0;
    }

    /**
     * Returns the configured weekdays.
     */
    public Set<DayOfWeek> getWeekdaysAsSet() {
        Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            if (isActiveOn(d)) set.add(d);
        }
        return set;
    }

    public void addWeekday(DayOfWeek day) {
        if (day == null) return;
        int index = day.getValue() - 1;
        this.weekdays = this.getWeekdays() | (1 << index);
    }

    public void removeWeekday(DayOfWeek day) {
        if (day == null) return;
        int index = day.getValue() - 1;
        this.weekdays = this.getWeekdays() & ~(1 << index);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
}
