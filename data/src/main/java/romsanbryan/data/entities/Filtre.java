package romsanbryan.data.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "maintenance_filters", indexes = {
        @Index(name = "idx_maintenance_filters_maintenance_id", columnList = "maintenance_id")
})
public class Filtre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "maintenance_id", nullable = false)
    private Integer idMaintenance;

    @Column(name = "filter_name", nullable = false, length = 100)
    private String filtre;

    @Column(name = "sponge")
    private Boolean sponge;

    @Column(name = "perlon")
    private Boolean perlon;

    @Column(name = "mechanical")
    private Boolean mech;

    @Column(name = "biological")
    private Boolean bio;

    @Column(name = "biological_mechanical")
    private Boolean bioMech;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMaintenanceId() {
        return idMaintenance;
    }

    public void setMaintenanceId(Integer maintenanceId) {
        this.idMaintenance = maintenanceId;
    }

    public String getFilterName() {
        return filtre;
    }

    public void setFilterName(String filterName) {
        this.filtre = filterName;
    }

    public Boolean getSponge() {
        return sponge;
    }

    public void setSponge(Boolean sponge) {
        this.sponge = sponge;
    }

    public Boolean getPerlon() {
        return perlon;
    }

    public void setPerlon(Boolean perlon) {
        this.perlon = perlon;
    }

    public Boolean getMech() {
        return mech;
    }

    public void setMech(Boolean mech) {
        this.mech = mech;
    }

    public Boolean getBio() {
        return bio;
    }

    public void setBio(Boolean bio) {
        this.bio = bio;
    }

    public Boolean getBioMech() {
        return bioMech;
    }

    public void setBioMech(Boolean bioMech) {
        this.bioMech = bioMech;
    }
}