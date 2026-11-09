package romsanbryan.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "aquarium_life_entries", indexes = {
        @Index(name = "idx_aquarium_life_entries_aquarium_category", columnList = "aquarium_id,category")
})
public class AquariumLife {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aquarium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Aquarium aquarium;

    @Column(name = "category", nullable = false, length = 10)
    private String category;

    @Column(name = "common_name", nullable = false, length = 150)
    private String commonName;

    @Column(name = "scientific_name", length = 150)
    private String scientificName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "recommended_parameters", columnDefinition = "text")
    private String recommendedParameters;

    @Column(name = "technical_sheet_url", length = 2048)
    private String technicalSheetUrl;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Aquarium getAquarium() {
        return aquarium;
    }

    public void setAquarium(Aquarium aquarium) {
        this.aquarium = aquarium;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getRecommendedParameters() {
        return recommendedParameters;
    }

    public void setRecommendedParameters(String recommendedParameters) {
        this.recommendedParameters = recommendedParameters;
    }

    public String getTechnicalSheetUrl() {
        return technicalSheetUrl;
    }

    public void setTechnicalSheetUrl(String technicalSheetUrl) {
        this.technicalSheetUrl = technicalSheetUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
