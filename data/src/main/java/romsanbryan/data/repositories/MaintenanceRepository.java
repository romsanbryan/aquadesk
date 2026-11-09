package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romsanbryan.data.entities.Maintenance;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Integer> {

    @Query("""
                SELECT m FROM Maintenance m 
                JOIN m.event e 
                ORDER BY e.startDate DESC 
                LIMIT 1
            """)
    Optional<Maintenance> findLatestMaintenance();

    @Query("""
                SELECT m FROM Maintenance m
                JOIN m.event e
                WHERE e.aquarium.id = :aquariumId
                ORDER BY e.startDate DESC
                LIMIT 1
            """)
    Optional<Maintenance> findLatestMaintenanceByAquariumId(@Param("aquariumId") Integer aquariumId);


    @Query("SELECT m FROM Maintenance m WHERE m.event.id = :idEvent")
    Optional<Maintenance> findByIdEvent(@Param("idEvent") Integer idEvent);

    @Query("SELECT m FROM Maintenance m JOIN m.event e WHERE m.event.id = :idEvent AND e.aquarium.id = :aquariumId")
    Optional<Maintenance> findByIdEventAndAquariumId(@Param("idEvent") Integer idEvent,
                                                     @Param("aquariumId") Integer aquariumId);

    List<Maintenance> findAllByEventAquariumIdOrderByEventStartDateAsc(Integer aquariumId);

    @Modifying
    @Query("DELETE FROM Maintenance m WHERE m.event.aquarium.id = :aquariumId")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);

    @Modifying
    @Query(value = "DELETE FROM maintenance_filters WHERE maintenance_id IN " +
            "(SELECT m.id FROM event_maintenances m JOIN events e ON e.id = m.event_id WHERE e.aquarium_id = :aquariumId)",
            nativeQuery = true)
    void deleteFiltersByAquariumId(@Param("aquariumId") Integer aquariumId);
}
