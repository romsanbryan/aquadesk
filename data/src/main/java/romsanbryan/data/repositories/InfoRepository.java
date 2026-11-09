package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import romsanbryan.data.entities.Info;

import java.util.List;
import java.util.Optional;

public interface InfoRepository extends JpaRepository<Info, Integer> {

    @Query("SELECT i FROM Info i WHERE i.idEvent = :idEvent")
    Optional<Info> findByIdEvent(@Param("idEvent") Integer idEvent);

    @Query("SELECT i FROM Info i JOIN romsanbryan.data.entities.Event e ON e.id = i.idEvent " +
            "WHERE i.idEvent = :idEvent AND e.aquarium.id = :aquariumId")
    Optional<Info> findByIdEventAndAquariumId(@Param("idEvent") Integer idEvent,
                                               @Param("aquariumId") Integer aquariumId);

    @Query("SELECT i FROM Info i JOIN romsanbryan.data.entities.Event e ON e.id = i.idEvent " +
            "WHERE e.aquarium.id = :aquariumId")
    List<Info> findAllByAquariumId(@Param("aquariumId") Integer aquariumId);


    @Query("SELECT i FROM Info i JOIN romsanbryan.data.entities.Event e ON e.id = i.idEvent " +
            "WHERE LOWER(i.observation) = LOWER(:observation) " +
            "ORDER BY e.startDate DESC")
    List<Info> findByObservationIgnoreCaseOrderByEventStartDateDesc(@Param("observation") String observation);

    @Query("SELECT i FROM Info i JOIN romsanbryan.data.entities.Event e ON e.id = i.idEvent " +
            "WHERE LOWER(i.observation) = LOWER(:observation) AND e.aquarium.id = :aquariumId " +
            "ORDER BY e.startDate DESC")
    List<Info> findByObservationAndAquariumId(@Param("observation") String observation,
                                               @Param("aquariumId") Integer aquariumId);

    @Modifying
    @Query("DELETE FROM Info i WHERE i.idEvent IN " +
            "(SELECT e.id FROM romsanbryan.data.entities.Event e WHERE e.aquarium.id = :aquariumId)")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);
}
