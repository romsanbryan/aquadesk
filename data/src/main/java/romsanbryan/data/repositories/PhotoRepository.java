package romsanbryan.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romsanbryan.data.entities.Photo;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Integer> {

    @Query("SELECT p FROM Photo p WHERE p.eventId = :eventId")
    List<Photo> findByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT p FROM Photo p JOIN romsanbryan.data.entities.Event e ON e.id = p.eventId " +
            "WHERE e.aquarium.id = :aquariumId ORDER BY p.createdAt DESC")
    Page<Photo> findAllByAquariumId(@Param("aquariumId") Integer aquariumId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Photo p WHERE p.eventId IN " +
            "(SELECT e.id FROM romsanbryan.data.entities.Event e WHERE e.aquarium.id = :aquariumId)")
    void deleteAllByAquariumId(@Param("aquariumId") Integer aquariumId);
}