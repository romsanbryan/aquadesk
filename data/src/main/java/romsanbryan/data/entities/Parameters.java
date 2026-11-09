package romsanbryan.data.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "event_parameters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_parameters_event_id", columnNames = "event_id")
})
public class Parameters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Integer idEvent;

    @Column(precision = 5, scale = 2)
    private BigDecimal nh3;

    @Column(precision = 5, scale = 2)
    private BigDecimal no2;

    @Column(precision = 5, scale = 2)
    private BigDecimal no3;

    @Column(precision = 4, scale = 2)
    private BigDecimal ph;

    @Column(precision = 5, scale = 2)
    private BigDecimal kh;

    @Column(precision = 5, scale = 2)
    private BigDecimal gh;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temp;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdEvent() {
        return idEvent;
    }

    public void setIdEvent(Integer idEvent) {
        this.idEvent = idEvent;
    }

    public BigDecimal getNh3() {
        return nh3;
    }

    public void setNh3(BigDecimal nh3) {
        this.nh3 = nh3;
    }

    public BigDecimal getNo2() {
        return no2;
    }

    public void setNo2(BigDecimal no2) {
        this.no2 = no2;
    }

    public BigDecimal getNo3() {
        return no3;
    }

    public void setNo3(BigDecimal no3) {
        this.no3 = no3;
    }

    public BigDecimal getPh() {
        return ph;
    }

    public void setPh(BigDecimal ph) {
        this.ph = ph;
    }

    public BigDecimal getKh() {
        return kh;
    }

    public void setKh(BigDecimal kh) {
        this.kh = kh;
    }

    public BigDecimal getGh() {
        return gh;
    }

    public void setGh(BigDecimal gh) {
        this.gh = gh;
    }

    public BigDecimal getTemp() {
        return temp;
    }

    public void setTemp(BigDecimal temp) {
        this.temp = temp;
    }
}
