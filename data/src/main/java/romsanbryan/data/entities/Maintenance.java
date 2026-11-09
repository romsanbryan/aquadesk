package romsanbryan.data.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "event_maintenances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_maintenances_event_id", columnNames = "event_id")
})
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "water", nullable = false)
    private Integer water;

    @Column(name = "prime_dose", precision = 38, scale = 2)
    private BigDecimal primeDose;

    @Column(name = "observations", nullable = false, length = 500)
    private String observations;

    @Column(name = "glass_cleaned", nullable = false)
    private Boolean glass;

    @Column(name = "pruning", length = 255)
    private String pruning;

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Integer getWater() {
        return water;
    }

    public void setWater(Integer water) {
        this.water = water;
    }

    public BigDecimal getPrimeDose() {
        return primeDose;
    }

    public void setPrimeDose(BigDecimal primeDose) {
        this.primeDose = primeDose;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Boolean getGlass() {
        return glass;
    }

    public void setGlass(Boolean glass) {
        this.glass = glass;
    }

    public String getPruning() {
        return pruning;
    }

    public void setPruning(String pruning) {
        this.pruning = pruning;
    }
}