package romsanbryan.data.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parameter_ranges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_parameter_ranges_aquarium_parameter", columnNames = {"aquarium_id", "parameter"})
})
public class ParametersRange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String parameter;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private Aquarium aquarium;

    @Column(name = "level_1", precision = 10, scale = 2)
    private BigDecimal level1;

    @Column(name = "level_2", precision = 10, scale = 2)
    private BigDecimal level2;

    @Column(name = "level_3", precision = 10, scale = 2)
    private BigDecimal level3;

    @Column(name = "level_4", precision = 10, scale = 2)
    private BigDecimal level4;

    @Column(name = "level_5", precision = 10, scale = 2)
    private BigDecimal level5;

    @Column(length = 50)
    private String unit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public BigDecimal getLevel1() {
        return level1;
    }

    public void setLevel1(BigDecimal level1) {
        this.level1 = level1;
    }

    public BigDecimal getLevel2() {
        return level2;
    }

    public void setLevel2(BigDecimal level2) {
        this.level2 = level2;
    }

    public BigDecimal getLevel3() {
        return level3;
    }

    public void setLevel3(BigDecimal level3) {
        this.level3 = level3;
    }

    public BigDecimal getLevel4() {
        return level4;
    }

    public void setLevel4(BigDecimal level4) {
        this.level4 = level4;
    }

    public BigDecimal getLevel5() {
        return level5;
    }

    public void setLevel5(BigDecimal level5) {
        this.level5 = level5;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
