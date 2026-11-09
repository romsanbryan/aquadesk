package romsanbryan.data.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "event_treatments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_treatments_event_id", columnNames = "event_id")
})
public class Co2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "dose", nullable = false, precision = 38, scale = 2)
    private BigDecimal value;

    @Column(name = "product_name", nullable = false, length = 100)
    private String product;

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

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }
}