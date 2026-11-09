package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romsanbryan.data.entities.Co2;

import java.util.List;
import java.util.Optional;

@Repository
public interface Co2Repository extends JpaRepository<Co2, Integer> {
    Optional<Co2> findByEventId(Integer eventId);

    Optional<Co2> findByEventIdAndEventAquariumId(Integer eventId, Integer aquariumId);
    List<Co2> findAllByEventAquariumIdOrderByEventStartDateAsc(Integer aquariumId);

    @Query("SELECT e FROM Co2 e WHERE e.event.id = :idEvent")
    Optional<Co2> findByIdEvent(@Param("idEvent") Integer idEvent);

    // Returns the latest treatment record for a product (case-insensitive),
    // ordenado por la fecha del evento descendente.
    Optional<Co2> findFirstByProductIgnoreCaseOrderByEventStartDateDesc(String product);

    Optional<Co2> findFirstByProductIgnoreCaseAndEventAquariumIdOrderByEventStartDateDesc(
            String product, Integer aquariumId);

    @Modifying
    @Query("DELETE FROM Co2 c WHERE c.event.aquarium.id = :aquariumId")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);
}