package romsanbryan.data.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "event_details", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_details_event_id", columnNames = "event_id")
})
public class Info {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "event_id", nullable = false)
    private Integer idEvent;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @Column(name = "detail_type", length = 100)
    private String type;

    @Column(length = 50)
    private String status;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdEvent() {
        return idEvent;
    }

    public void setIdEvent(Integer idEvent) {
        this.idEvent = idEvent;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}