package romsanbryan.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "aquarium_filters", indexes = {
        @Index(name = "idx_aquarium_filters_aquarium_id", columnList = "aquarium_id")
})
public class AquariumFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private Aquarium aquarium;

    @Column(length = 50)
    private String name;

    @Column(name = "material_biological", length = 255)
    private String materialBiological;

    @Column(name = "material_mechanical", length = 255)
    private String materialMechanical;

    @Column(name = "material_biological_mechanical", length = 255)
    private String materialBioMechanical;

    @Column(nullable = false)
    private boolean sponge;

    @Column(name = "activated_carbon", nullable = false)
    private boolean carbonActive;

    @Column(nullable = false)
    private boolean perlon;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Aquarium getAquarium() { return aquarium; }
    public void setAquarium(Aquarium aquarium) { this.aquarium = aquarium; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMaterialBiological() { return materialBiological; }
    public void setMaterialBiological(String value) { this.materialBiological = value; }
    public String getMaterialMechanical() { return materialMechanical; }
    public void setMaterialMechanical(String value) { this.materialMechanical = value; }
    public String getMaterialBioMechanical() { return materialBioMechanical; }
    public void setMaterialBioMechanical(String value) { this.materialBioMechanical = value; }
    public boolean isSponge() { return sponge; }
    public void setSponge(boolean sponge) { this.sponge = sponge; }
    public boolean isCarbonActive() { return carbonActive; }
    public void setCarbonActive(boolean carbonActive) { this.carbonActive = carbonActive; }
    public boolean isPerlon() { return perlon; }
    public void setPerlon(boolean perlon) { this.perlon = perlon; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
