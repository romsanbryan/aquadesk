package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romsanbryan.data.entities.Filtre;

import java.util.List;

@Repository
public interface FiltreRepository extends JpaRepository<Filtre, Integer> {
    @Query("SELECT f FROM Filtre f WHERE f.idMaintenance = :maintenanceId ORDER BY f.id ASC")
    List<Filtre> findByMaintenanceId(@Param("maintenanceId") Integer maintenanceId);

    @Query("SELECT f FROM Filtre f WHERE f.idMaintenance IN :maintenanceIds ORDER BY f.id ASC")
    List<Filtre> findByMaintenanceIdIn(@Param("maintenanceIds") List<Integer> maintenanceIds);

}